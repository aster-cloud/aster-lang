# 工作流重试机制技术文档

## 概述

本文档描述 Aster 工作流重试机制的运行时实现，包括架构设计、事件日志格式、重放机制和性能特征。

**实现版本**：P0-5
**最后更新**：2025-11-18

## 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Workflow Execution                      │
└──────────────────────────┬──────────────────────────────────┘
                           │ Step Failure
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              AsyncTaskRegistry.onTaskFailed()                │
│  - 检查 RetryPolicy                                          │
│  - 检查尝试次数 vs maxAttempts                               │
│  - 计算 backoff 延迟（使用 DeterminismContext）              │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌─────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ PostgreSQL  │  │  DelayedTask     │  │  Throw          │
│ Event Store │  │  Priority Queue  │  │  MaxRetries     │
│             │  │                  │  │  Exception      │
│ 记录:        │  │  scheduleRetry() │  │                 │
│ - attempt   │  │  ↓               │  │  (attempt >=    │
│ - backoff   │  │  Timer 触发      │  │   maxAttempts)  │
│ - reason    │  │  ↓               │  │                 │
│             │  │  resumeWorkflow()│  │                 │
└─────────────┘  └──────────────────┘  └─────────────────┘
```

### 重试流程

1. **失败检测**
   AsyncTaskRegistry 捕获步骤执行异常，调用 `onTaskFailed(taskId, exception, isReplay)`

2. **策略查询**
   从 `retryPolicies` 映射中查找任务的 RetryPolicy：
   ```java
   RetryPolicy policy = retryPolicies.get(taskId);
   int attempt = attemptCounters.getOrDefault(taskId, 1);
   ```

3. **尝试次数检查**
   ```java
   if (attempt >= policy.maxAttempts) {
     throw new MaxRetriesExceededException(policy.maxAttempts, exception.getMessage(), exception);
   }
   ```

4. **Backoff 计算**
   ```java
   long delayMs;
   if (isReplay) {
     // 重放模式：使用日志中的 backoff
     delayMs = getBackoffFromEventLog(workflowId, taskId, attempt);
   } else {
     // 正常模式：计算 backoff
     delayMs = calculateBackoff(attempt, policy.backoff, policy.baseDelayMs, determinismContext);
   }
   ```

   **Backoff 公式**：
   - Exponential: `backoffBase = baseDelayMs * 2^(attempt-1)`
   - Linear: `backoffBase = baseDelayMs * attempt`
   - Jitter: `jitter = DeterminismContext.random().nextLong("async-task-backoff") % (baseDelayMs/2)`
   - 最终延迟: `delayMs = backoffBase + jitter`

5. **事件记录**
   ```java
   eventStore.appendEvent(
     workflowId,
     "RETRY_SCHEDULED",
     Map.of("taskId", taskId, "reason", exception.getMessage()),
     attempt + 1,           // attemptNumber
     delayMs,               // backoffDelayMs
     exception.getMessage() // failureReason
   );
   ```

6. **延迟调度**
   ```java
   scheduleRetry(workflowId, delayMs, attempt + 1, exception.getMessage());
   ```

   DelayedTask 加入 PriorityQueue，timer 线程轮询（100ms 间隔），到期后调用 `resumeWorkflow()`

## 事件日志格式

### 数据库 Schema

```sql
-- V5.1.0__add_retry_metadata.sql
ALTER TABLE workflow_events ADD COLUMN IF NOT EXISTS attempt_number INT DEFAULT 1;
ALTER TABLE workflow_events ADD COLUMN IF NOT EXISTS backoff_delay_ms BIGINT;
ALTER TABLE workflow_events ADD COLUMN IF NOT EXISTS failure_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_workflow_events_attempt
ON workflow_events(workflow_id, attempt_number);
```

### 事件记录示例

**RETRY_SCHEDULED 事件**：
```json
{
  "workflowId": "wf-12345",
  "eventType": "RETRY_SCHEDULED",
  "sequence": 5,
  "timestamp": "2025-11-18T10:30:15Z",
  "payload": {
    "taskId": "processPayment",
    "reason": "Connection timeout"
  },
  "attemptNumber": 2,
  "backoffDelayMs": 2347,
  "failureReason": "Connection timeout"
}
```

### 查询 API

```java
// 查询特定尝试的事件
List<WorkflowEventEntity> attempt2Events =
  WorkflowEventEntity.findByWorkflowIdAndAttempt(workflowId, 2);

// 查询工作流的重试上下文
WorkflowStateEntity state = WorkflowStateEntity.findByWorkflowId(workflowId);
Map<String, Object> retryContext = state.getRetryContext();
// retryContext: {"currentAttempt": 2, "lastBackoff": 2000}
```

## 重放机制

### 重放一致性保证

工作流重放时，必须保证重试行为完全一致（相同的 backoff 延迟）。实现方式：

1. **DeterminismContext 集成**
   calculateBackoff 使用 DeterminismContext.random() 生成确定性 jitter：
   ```java
   long jitter = ctx.random().nextLong("async-task-backoff") % jitterBound;
   ```

   相同种子 → 相同随机数序列 → 相同 jitter → 相同 backoff

2. **日志优先模式**
   重放时，直接从事件日志读取 backoff_delay_ms，跳过计算：
   ```java
   if (isReplay) {
     delayMs = getBackoffFromEventLog(workflowId, taskId, attempt);
   }
   ```

3. **状态恢复**
   重放前，调用 `restoreRetryState(events)` 恢复 attemptCounters：
   ```java
   for (WorkflowEvent event : events) {
     if ("RETRY_SCHEDULED".equals(event.getEventType())) {
       String taskId = (String) event.getPayload().get("taskId");
       int attemptNumber = event.getAttemptNumber();
       attemptCounters.put(taskId, attemptNumber);
     }
   }
   ```

### 重放验证

ChaosSchedulerTest 包含 20+ 重放一致性测试场景：
- 单次重试成功
- 达到最大次数
- 混合重试场景（部分成功、部分失败）
- 验证方法：相同种子 → 相同事件序列 → 相同 backoff 值

## 性能特征

### Timer 开销

- **实现**：PriorityQueue + 100ms 轮询线程
- **时间复杂度**：
  - scheduleRetry: O(log n)（堆插入）
  - 轮询检查: O(log n)（堆弹出）
- **空间开销**：每个 DelayedTask 约 64 bytes
- **延迟精度**：±100ms（轮询间隔）

### 事件存储开销

- **写入延迟**：PostgreSQL INSERT，约 1-3ms (p99)
- **索引开销**：idx_workflow_events_attempt 复合索引
- **存储开销**：
  - attemptNumber: 4 bytes
  - backoffDelayMs: 8 bytes
  - failureReason: 可变长度（TEXT）

### 性能目标（P0-2 验收标准）

- **p99 延迟 < 100ms**：包含重试机制开销
- **重试开销 < 10%**：相比无重试的基准场景
- **测试方法**：JMH benchmark（WorkflowRetryBenchmark）

**基准场景**：
- No retry: 单步骤，无失败
- Retry once: 第1次失败，第2次成功
- Retry three times: 连续失败3次

## 代码关键路径

### 1. 重试策略注册

```java
// AsyncTaskRegistry.java
public void registerTaskWithRetry(
    String taskId,
    Callable<Object> task,
    Set<String> dependencies,
    RetryPolicy retryPolicy
) {
  retryPolicies.put(taskId, retryPolicy);
  attemptCounters.put(taskId, 1);
  registerTask(taskId, task, dependencies);
}
```

### 2. Backoff 计算

```java
// AsyncTaskRegistry.java:909
private long calculateBackoff(
    int attempt,
    String strategy,
    long baseDelayMs,
    DeterminismContext ctx
) {
  long normalizedAttempt = Math.max(1, attempt);
  long backoffBase;
  if ("exponential".equalsIgnoreCase(strategy)) {
    backoffBase = (long) (baseDelayMs * Math.pow(2, normalizedAttempt - 1));
  } else {
    backoffBase = baseDelayMs * normalizedAttempt;
  }
  long jitterBound = Math.max(0L, baseDelayMs / 2);
  DeterminismContext targetCtx = ctx != null ? ctx : this.determinismContext;
  long jitter = 0L;
  if (jitterBound > 0) {
    long raw = targetCtx.random().nextLong("async-task-backoff");
    jitter = Math.floorMod(raw, jitterBound);
  }
  return backoffBase + jitter;
}
```

### 3. 延迟任务调度

```java
// AsyncTaskRegistry.java:809
public void scheduleRetry(
    String workflowId,
    long delayMs,
    int attemptNumber,
    String failureReason
) {
  long triggerAt = System.currentTimeMillis() + delayMs;
  delayQueueLock.lock();
  try {
    DelayedTask task = new DelayedTask(workflowId, triggerAt, attemptNumber, failureReason);
    delayQueue.offer(task);
  } finally {
    delayQueueLock.unlock();
  }
}
```

### 4. Timer 轮询

```java
// AsyncTaskRegistry.java:pollDelayedTasks
private void pollDelayedTasks() {
  while (running) {
    long now = System.currentTimeMillis();
    delayQueueLock.lock();
    try {
      while (!delayQueue.isEmpty() && delayQueue.peek().triggerAtMs <= now) {
        DelayedTask task = delayQueue.poll();
        resumeWorkflow(task.workflowId);
      }
    } finally {
      delayQueueLock.unlock();
    }
    Thread.sleep(100); // 100ms 轮询间隔
  }
}
```

## 测试覆盖

### 单元测试

- **BackoffCalculatorTest** (aster-truffle)
  测试 backoff 计算公式正确性、jitter 范围、确定性

- **RetryExecutionTest** (aster-truffle)
  测试成功重试、最大次数、异常字段、元数据记录

### 集成测试

- **WorkflowRetryIntegrationTest** (quarkus-policy-api)
  端到端测试：失败→重试→成功、达到最大次数、多步骤混合、重放一致性

- **PostgresEventStoreRetryTest** (quarkus-policy-api)
  测试事件存储的 retry metadata 持久化、查询、快照集成

### 混沌测试

- **ChaosSchedulerTest** (aster-truffle)
  20+ 重放一致性场景，验证确定性重试

### 性能测试

- **WorkflowRetryBenchmark** (quarkus-policy-api-benchmarks)
  JMH benchmark，测量 p50/p95/p99 延迟，验证 <100ms 目标

## 已知限制

1. **Timer 精度**：100ms 轮询间隔，短延迟（<100ms）可能不精确
2. **单机调度**：DelayedTask 队列在内存中，不支持分布式调度
3. **重放模式检测**：当前需手动传递 isReplay 标志，未自动检测
4. **BaseDelay 硬编码**：默认 1000ms，暂不支持配置（计划 P1 支持）

## 未来优化

- **P1**：支持配置 baseDelay（当前硬编码 1000ms）
- **P1**：支持分布式 timer（基于 PostgreSQL NOTIFY）
- **P2**：支持自适应 backoff（基于历史成功率）
- **P2**：支持重试熔断（连续失败达到阈值后停止重试）

## 参考资料

- [Workflow 语法指南](../language/workflow.md) - 重试配置语法
- [P0-5 完成报告](.claude/p0-5-completion-report.md) - 实现总结
- [DeterminismContext 设计](./determinism-context.md) - 重放一致性基础
