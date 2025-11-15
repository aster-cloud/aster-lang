# 幂等性辅助工具

> 更新时间：2025-11-15 21:29 NZST · 执行者：Codex

## 核心概念

`IdempotencyKeyManager` 负责在高并发环境中原子获取幂等性键，确保同一业务键仅被一个实体处理：

- CDI 场景使用 Quarkus Caffeine Cache（`@CacheName("idempotency-keys")`），操作异步但 `tryAcquire` 会同步等待结果；
- 非 CDI / 测试场景自动退化为 `ConcurrentHashMap`，保证线程安全；
- `release()` 可在任务提前结束时清理缓存，减少 TTL 等待。

## tryAcquire 语义

```java
Optional<String> tryAcquire(String key, String entityId, Duration ttl)
```

- `Optional.empty()`：当前 entity 成功占用幂等性键；
- `Optional.of(existingEntityId)`：键已被其他 entity 占用，调用方应立即返回已有结果或排队；
- 参数校验会拒绝 null/负数 TTL，并抛出 `IllegalArgumentException`。

## TTL 配置

默认值读取自 `quarkus-policy-api/src/main/resources/application.properties`：

```
quarkus.cache.caffeine."idempotency-keys".maximum-size=10000
quarkus.cache.caffeine."idempotency-keys".expire-after-write=1h
quarkus.cache.redis.idempotency-keys.value-type=java.lang.String
```

- Caffeine 本地缓存 1 小时自动过期，并限制 10k 个键；
- 若启用 Redis 扩展，可通过 `quarkus.cache.redis.*` 在跨实例间共享幂等性状态；
- TTL 参数传给 `tryAcquire` 仅用于语义表达（例如 API 返回 `Retry-After`），实际过期仍由缓存配置控制。

## 使用场景

1. **API 幂等性**：REST 请求传入 `Idempotency-Key` 请求头，使用 `workflowId` 或 `entityId` 作为 value，确保重复请求读取同一结果。
2. **Workflow 去重**：`PostgresWorkflowRuntime.schedule()` 在写入 `WorkflowStarted` 事件前调用 `tryAcquire`，防止同一 workflowId 多次插入。
3. **分布式锁轻量替代**：短期互斥操作（如避免重复生成报告）可以直接使用幂等性键取代完整的锁组件。

## 完整示例

```java
// ✅ 正确示例：结合配置与代码完整使用
// application.properties
// quarkus.cache.caffeine."idempotency-keys".expire-after-write=30m
// quarkus.cache.caffeine."idempotency-keys".maximum-size=20000

import io.aster.workflow.IdempotencyKeyManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class ReportGenerator {
    @Inject
    IdempotencyKeyManager keyManager;

    public String generate(String idempotencyKey, String workflowId) {
        Optional<String> occupied = keyManager.tryAcquire(idempotencyKey, workflowId, Duration.ofMinutes(30));
        if (occupied.isPresent()) {
            return "报告已由 workflow=" + occupied.get() + " 生成";
        }
        try {
            // …执行耗时逻辑…
            return "生成完成";
        } finally {
            keyManager.release(idempotencyKey); // 防止键长时间占用
        }
    }
}

// ❌ 错误示例：忽略 tryAcquire 返回值且从不释放
public class ReportGeneratorLegacy {
    @Inject
    IdempotencyKeyManager keyManager;

    public void generate(String idempotencyKey, String workflowId) {
        keyManager.tryAcquire(idempotencyKey, workflowId, Duration.ofMinutes(5));
        // 未检查 Optional 直接执行，会允许多个 workflow 并行，同步写入状态
        // 未调用 release 可能导致键一直等到 expire-after-write 才释放，引发堆积
    }
}
```

## 性能特性（Caffeine Cache）

- **O(1) 原子获取**：`cache.get(key, mappingFunction)` 在 Caffeine 内部保证并发安全，只有首个线程会执行映射函数，其余线程获得相同结果。
- **写入延迟低**：本地内存缓存无需远程 RTT，是处理高频 API 的首选；可根据需要启用 Redis 作为共享层。
- **自动淘汰**：`maximum-size` 与 `expire-after-write` 双重限制可避免无界增长；Telemetry 可结合 `CacheStatisticsRecorder` 观察命中率。
- **Fallback 安全**：在单元测试或 CLI 工具中没有 CDI 环境时，`ConcurrentHashMap` 仍可提供线程安全的幂等性语义，只是缺少 TTL，需要显式 `release`。
