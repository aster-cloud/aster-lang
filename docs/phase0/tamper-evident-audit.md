# 防篡改审计日志

> 更新时间：2025-11-15 21:29 NZST · 执行者：Codex

## 工作原理

AuditEventListener 在持久化 `AuditLog` 时会根据租户维度构建哈希链：

1. **Genesis Block**：同一租户的第一条记录 `prev_hash=null`；之后每条记录的 `prev_hash` 指向上一条记录的 `current_hash`。
2. **哈希构建**：使用 `DigestUtils.sha256Hex()` 计算 `current_hash`。输入字段顺序固定，任何字符级别的变动都能被检测。
3. **租户隔离**：`AuditLog.findLatestHash(tenantId)` 仅返回当前租户最末尾的 hash，避免跨租户竞争。

当链路异常（插入、删除或篡改）时，验证器会立即发现 `prev_hash mismatch` 或 `current_hash tampered`，并返回断点时间戳。

## 哈希计算规则

字段拼接顺序必须与 `AuditEventListener.computeHashChain()` 一致：

```
content = prevHash
        + eventType
        + timestamp
        + tenantId
        + policyModule
        + policyFunction
        + success
currentHash = SHA256(content)
```

注意事项：

- `prevHash` 允许为 `null`（Genesis Block），其他字段遇到 `null` 会降级为空字符串。
- timestamp 采用 ISO-8601 字符串，比较时请保持时区一致。
- `success` 转换为 `Boolean#toString()`，因此 `true` / `false` 均为小写。

## API 使用示例

```java
// ✅ 正确示例：分页校验链路并在失败时中断发布
import io.aster.audit.chain.AuditChainVerifier;
import io.aster.audit.chain.ChainVerificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class AuditChainGuard {
    @Inject
    AuditChainVerifier verifier;

    public void verifyBeforeExport(String tenantId) {
        ChainVerificationResult result = verifier.verifyChainPaginated(
            tenantId,
            Instant.now().minus(14, ChronoUnit.DAYS),
            Instant.now(),
            500 // 分页大小
        );
        if (!result.isValid()) {
            throw new IllegalStateException(
                "审计链断裂，位置=" + result.getBrokenAt() + ", 原因=" + result.getReason()
            );
        }
    }
}

// ❌ 错误示例：查询全量数据且忽略验证结果
public class AuditChainGuardLegacy {
    @Inject
    AuditChainVerifier verifier;

    public void verifyAllTenants() {
        ChainVerificationResult result = verifier.verifyChain("tenant-a", Instant.EPOCH, Instant.now());
        // 忽略 result.isValid() 会让下游误以为链路可靠，同时全表扫描 Instant.EPOCH -> Now 会触发慢查询
    }
}
```

## 验证流程

1. **确定查询窗口**：根据审计需求选择时间范围与租户 ID。推荐按天或按周滑窗。
2. **获取记录**：`verifyChain` 会按 `timestamp ASC, id ASC` 加载记录，并跳过旧的无哈希数据。
3. **比对 prevHash**：若 `log.prevHash` 与 `expectedPrevHash` 不相等，则断定为链断裂（记录被删除或插入）。
4. **重算 currentHash**：使用相同顺序重算 SHA256 并与存量比较，检测字段被修改。
5. **返回结果**：若全部记录通过，返回 `ChainVerificationResult.valid(records)`；否则返回 `invalid`，携带断点和原因。

## 性能建议

- **分页验证**：日志量超过 1000 条时使用 `verifyChainPaginated`，逐页推进 `expectedPrevHash`，避免一次性加载超大结果。
- **限制时间范围**：为每次验证设定最大窗口（如 24 小时），多天数据可按天循环，降低数据库 I/O。
- **跳过遗留记录**：旧版本没有哈希字段时会自动跳过，可通过 `Log.debugf("Skipping legacy record...")` 观察迁移进度。
- **异步批处理**：长区间验证可交给批处理任务，通过 `CompletableFuture` 并发校验多个租户。

## 故障排查

| 现象 | 常见原因 | 解决建议 |
| --- | --- | --- |
| `prev_hash mismatch` | 中间记录被删除、插入或 out-of-order 插入 | 检查数据库审计表是否存在手工操作；合并排序条件为 `timestamp ASC, id ASC`；通过 `pgAudit` 查明来源 |
| `current_hash tampered` | 字段被修改或未应用 PIIRedactor 导致值变化 | 对比 `AuditLog` 旧值与现值，结合 `AuditLogger` 输出确认是否存在补写流程 |
| 验证时间过长 | 查询窗口过大或缺少索引 | 在 `tenant_id, timestamp` 上确认索引存在；使用 `verifyChainPaginated` 分段处理 |
| 结果为 valid 但业务方仍怀疑被改 | 可能在窗口外或未涵盖全部租户 | 扩大时间窗口并在 `ChainVerificationResult.recordsVerified` 基础上校对期望条数 |
