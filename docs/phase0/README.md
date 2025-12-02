# Phase 0 核心特性使用指南

> 更新时间：2025-11-15 21:29 NZST · 执行者：Codex

本指南汇总 Phase 0 四大基础能力——防篡改审计日志、确定性契约、幂等性辅助工具与 PII 日志策略——的整体使用方式。详细章节分布在本目录的子文档，主文档聚焦快速入门、架构关系、最佳实践、测试覆盖与常见问题。

## 快速开始

### 防篡改审计日志（写入 + 验证）

```java
// ✅ 正确示例：写入审计日志并对链路进行验证
import io.aster.policy.audit.AuditLogger;
import io.aster.audit.chain.AuditChainVerifier;
import io.aster.audit.chain.ChainVerificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class AuditQuickStart {
    @Inject
    AuditLogger auditLogger;

    @Inject
    AuditChainVerifier verifier;

    public void recordAndVerify() {
        // 记录策略评估事件（AuditLogger 内部会触发 PII 脱敏和异步事件）
        auditLogger.logPolicyEvaluation("pricing.module", "evaluateOrder", "tenant-alpha", 45, true);

        // 验证最近 10 分钟内的哈希链
        ChainVerificationResult result = verifier.verifyChain(
            "tenant-alpha",
            Instant.now().minus(Duration.ofMinutes(10)),
            Instant.now()
        );
        if (!result.isValid()) {
            throw new IllegalStateException(result.getReason());
        }
    }
}
```

### 确定性契约（DeterminismContext 门面）

```java
// ✅ 正确示例：通过 DeterminismContext 使用 clock/uuid/random
import io.aster.workflow.DeterminismContext;
import io.aster.workflow.PostgresWorkflowRuntime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DeterministicAction {
    @Inject
    PostgresWorkflowRuntime runtime;

    public void execute() {
        DeterminismContext ctx = runtime.getDeterminismContext();
        Instant now = ctx.clock().now();              // 使用确定性时钟
        UUID workflowId = ctx.uuid().randomUUID();    // 使用确定性 UUID
        long entropy = ctx.random().nextLong("retry");// 使用确定性随机数
        // 业务逻辑……
    }
}
```

### 幂等性辅助工具（IdempotencyKeyManager）

```java
// ✅ 正确示例：获取幂等性键并在完成后释放
import io.aster.workflow.IdempotencyKeyManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class TaskService {
    @Inject
    IdempotencyKeyManager keyManager;

    public void startTask(String key, String workflowId) {
        Optional<String> occupiedBy = keyManager.tryAcquire(key, workflowId, Duration.ofMinutes(5));
        if (occupiedBy.isPresent()) {
            throw new IllegalStateException("幂等性键已被 " + occupiedBy.get() + " 占用");
        }
        try {
            // 执行真正的任务
        } finally {
            keyManager.release(key); // 避免泄漏
        }
    }
}
```

### PII 敏感信息日志策略（PIIRedactor）

```java
// ✅ 正确示例：记录 PII 但在日志中自动脱敏
import com.wontlost.aster.policy.PIIRedactor;

public final class LoginAuditFormatter {
    private static final PIIRedactor REDACTOR = new PIIRedactor();

    public String toSafeMessage(String email, String ssn, String ip) {
        String raw = "login email=%s ssn=%s ip=%s".formatted(email, ssn, ip);
        return REDACTOR.redact(raw); // 输出中只会出现掩码
    }
}
```

## 架构概览

```
          +--------------------------+
          | 业务代码 / Workflow 调度 |
          +-----------+--------------+
                      | DeterminismContext（clock/uuid/random ThreadLocal）
                      v
        +-------------+--------------+
        | PostgresWorkflowRuntime    |
        | - IdempotencyKeyManager    |
        | - TenantContext 传播       |
        +-------------+--------------+
                      | AuditEvent
                      v
        +-------------+--------------+       +-------------------------+
        | AuditLogger + PIIRedactor  | ----> | AuditEventListener      |
        +-------------+--------------+       | - 哈希链 prev/current   |
                      |                      | - SHA256 链路验证       |
                      |                      +-----------+-------------+
                      |                                  |
                      v                                  v
          +-----------+--------------+        +----------+-----------+
          | tamper-evident AuditLog  |        | AuditChainVerifier   |
          +--------------------------+        +----------------------+
```

## 最佳实践

1. **一次性写入、批量验证**：AuditLogger 始终写入完整元数据，再由 AuditChainVerifier 按时间范围分段拉取验证，避免单条记录频繁拉取导致数据库压力。
2. **强制门面调用**：Workflow 代码统一通过 DeterminismContext Facade 访问 clock/uuid/random，ArchUnit 会拦截任何直接调用系统 API 的行为。
3. **幂等性即策略输入**：IdempotencyKeyManager.tryAcquire 返回 Optional，`Optional.empty()` 才代表成功；拿到 key 后必须在 finally 块调用 release。
4. **日志默认脱敏，必要时白名单**：PIIRedactor 默认覆盖 SSN/Email/Phone/CreditCard/IP，新增类型时遵循统一 Pattern+占位符策略并添加测试。

## 测试覆盖

| 特性 | 测试类 | 覆盖场景 |
| --- | --- | --- |
| 防篡改审计链 | `quarkus-policy-api/src/test/java/io/aster/audit/chain/AuditChainVerifierTest.java` | 有效链路、字段篡改、记录删除、伪造插入、空链 |
| 确定性契约 | `quarkus-policy-api/src/test/java/io/aster/workflow/DeterminismArchTest.java` | 拦截 `Instant.now()`、`UUID.randomUUID()`、`new Random()` 等非确定性调用 |
| 幂等性辅助 | `quarkus-policy-api/src/test/java/io/aster/workflow/IdempotencyKeyManagerTest.java` | 首次成功、重复占用提示、10 线程并发抢占只允许单一成功 |
| PII 日志策略 | `quarkus-policy-api/src/test/java/io/aster/policy/logging/PIIRedactionIntegrationTest.java` | SSN/Email/Phone/CreditCard/IP 五种脱敏 + 多类型组合 |

## 常见问题（FAQ）

**Q1：如何添加新的 PII 类型？**  
A：在 `PIIRedactor` 中增加新的 `Pattern` 与 `replaceAll` 逻辑，同时给 `Builder` 增加对应开关，再为新增类型补充 `PIIRedactionIntegrationTest` 用例，确保日志与审计事件都通过 `piiRedactor.redact`。

**Q2：ArchUnit 测试失败怎么办？**  
A：运行 `./gradlew :quarkus-policy-api:test --tests DeterminismArchTest` 查看具体违规，按照测试输出定位直接调用 `Instant.now()`/`UUID.randomUUID()`/`new Random()` 的类，将调用迁移到 `DeterminismContext` 提供的 Facade，再次运行测试确认通过。

**Q3：审计链验证失败如何排查？**  
A：先确认查询窗口与租户 ID 正确，再查看 `ChainVerificationResult.reason`。若包含 `prev_hash mismatch`，说明链断裂（记录被删除或插入次序错误）；若包含 `current_hash tampered`，说明字段被改动，需从数据库对比原字段。必要时使用分页验证 `verifyChainPaginated` 分段定位。

**Q4：IdempotencyKeyManager 会内存泄漏吗？**  
A：默认依赖 Quarkus Caffeine Cache（`quarkus.cache.caffeine."idempotency-keys".maximum-size=10000`、`expire-after-write=1h`），且 `release` 会在任务完成立即清理。仅在非 CDI 场景（fallback ConcurrentHashMap）需要确保手动调用 `release`。

**Q5：如何自定义幂等性键 TTL？**  
A：调整 `quarkus.cache.caffeine."idempotency-keys".expire-after-write=<duration>` 并重启服务；若在多实例环境需要跨进程共享，可同步配置 `quarkus.cache.redis.idempotency-keys.value-type=java.lang.String` 以切换 Redis 存储。

## 相关链接

- [防篡改审计日志详解](./tamper-evident-audit.md)
- [确定性契约指南](./determinism-contract.md)
- [幂等性辅助工具手册](./idempotency-helpers.md)
- [PII 敏感信息日志策略](./pii-logging-policy.md)
