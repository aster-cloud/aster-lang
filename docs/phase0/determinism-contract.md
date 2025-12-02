# 确定性契约

> 更新时间：2025-11-15 21:29 NZST · 执行者：Codex

## 背景

Workflow 引擎需要支持 **Workflow Replay** 与 **故障恢复**：当执行失败或重放历史事件时，系统必须保证所有时间、UUID、随机数决策完全一致，以便：

- 重放历史事件时生成的结果与原执行一致，便于调试与回放；
- Crash 恢复后可以继续执行剩余步骤，而不会因为新的 UUID、Random 值导致状态漂移；
- 多租户环境中，通过 ThreadLocal 隔离避免不同 Workflow 互相污染确定性轨迹。

## Facade 模式实现

`DeterminismContext` 封装了 `ReplayDeterministicClock`、`ReplayDeterministicUuid`、`ReplayDeterministicRandom` 三个门面：

- **clock()**：记录 `Instant` 序列并在 replay 模式下回放，超过记录数量会报错。
- **uuid()**：ThreadLocal 缓存 + 500 条上限，记录 `UUID` 序列并支持 `enterReplayMode()`。
- **random()**：按 source 名称划分序列，任何随机行为都必须标记 source，避免交叉干扰。

Workflow 必须只依赖 `DeterminismContext` 暴露的 API（由运行时注入），禁止直接访问 `Instant.now()`、`UUID.randomUUID()` 或 `new Random()`。

## ArchUnit 自动验证

`quarkus-policy-api/src/test/java/io/aster/workflow/DeterminismArchTest.java` 定义了三条强制规则：

1. Workflow 包下的类除 `ReplayDeterministicClock` 外禁止调用 `Instant.now()`；
2. 同理禁止调用 `UUID.randomUUID()`（排除 `ReplayDeterministicUuid`）；
3. 禁止直接构造 `java.util.Random`，必须通过 `DeterminismContext.random()` 获取。

运行 `./gradlew :quarkus-policy-api:test --tests DeterminismArchTest` 即可捕获违规类与方法行号。

## ThreadLocal 隔离

`PostgresWorkflowRuntime` 在调度时会：

- 将当前 `workflowId` 与新的 `DeterminismContext` 绑定到 `ThreadLocal`（`setCurrentWorkflowId`、`setDeterminismContext`）；
- 在 `completeWorkflow` / `failWorkflow` 中调用 `persistDeterminismSnapshot` 将 `clock/uuid/random` 序列序列化到数据库（JSONB），便于 replay；
- 无论成功或失败都会 `clearDeterminismContext` 与 `clearCurrentWorkflowId`，避免线程复用时泄漏；
- 读取确定性 API 时若 ThreadLocal 尚未绑定，会退化到 `globalDeterminismContext`，保障调度线程安全。

## 使用示例

```java
// ✅ 正确示例：所有非确定性 API 均走 DeterminismContext
import io.aster.workflow.DeterminismContext;
import io.aster.workflow.PostgresWorkflowRuntime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class InvoiceWorkflow {
    @Inject
    PostgresWorkflowRuntime runtime;

    public void execute() {
        DeterminismContext ctx = runtime.getDeterminismContext();
        Instant now = ctx.clock().now();                  // 记录并可重放
        UUID requestId = ctx.uuid().randomUUID();         // 记录 UUID 序列
        long shuffle = ctx.random().nextLong("routing");  // 以 source 名称隔离随机数
        // 使用 now/requestId/shuffle 执行业务逻辑……
    }
}

// ❌ 错误示例：直接调用系统时钟与 UUID，ArchUnit 会拦截
import java.time.Instant;
import java.util.UUID;

public class InvoiceWorkflowLegacy {
    public void execute() {
        Instant now = Instant.now();          // 非确定性
        UUID requestId = UUID.randomUUID();   // 非确定性
        // 运行时 replay 将无法重现
    }
}
```

## 常见陷阱

| 场景 | 问题 | 规避方式 |
| --- | --- | --- |
| 将 `DeterminismContext` 缓存为静态字段 | ThreadLocal 会被绕过，导致跨租户污染 | 始终从运行时请求上下文 (`runtime.getDeterminismContext()`) |
| 在构造函数记录时间 | 构造时尚未绑定 ThreadLocal，导致记录落入全局上下文 | 将非确定性调用延迟到 `execute()` 等生命周期内 |
| 未持久化 Determinism 快照 | Crash 后无法 replay 原时间线 | 确保调用 `PostgresWorkflowRuntime.completeWorkflow/ failWorkflow`，内部会持久化序列 |
| 手动清理 ThreadLocal | 误删运行时在 finally 中清理的上下文，导致下一步读取为 null | 除非自定义运行器，否则无需显式调用 `clearDeterminismContext` |
