# Workflow 确定性合约

本文档说明 Aster Workflow Runtime 的确定性保证、API 使用规范、重放语义与限制，帮助开发者正确编写可重放的 workflow 代码。

---

## 1. 确定性保证承诺

Aster Workflow 提供**确定性执行（Deterministic Execution）**保证：

> **相同的输入和事件序列，无论执行多少次，都会产生完全相同的状态转换和输出结果。**

### 1.1 保证范围

✅ **已保证**：
- Workflow 执行产生的所有决策（分支选择、循环次数、任务调度顺序）
- 时间相关操作（当前时间、延迟触发）
- 随机数生成
- UUID 生成
- 任务完成顺序和状态转换

❌ **不保证**：
- 外部服务调用的响应内容（需通过 idempotent API 设计保证）
- 物理资源状态（CPU 负载、内存使用）
- 非确定性系统调用（文件 I/O、网络延迟）

### 1.2 应用场景

确定性保证支持以下关键场景：

1. **崩溃恢复（Crash Recovery）**：进程崩溃后从最近快照恢复，继续执行产生相同结果
2. **调试与复现（Debugging）**：使用相同事件流重放 workflow，精确复现问题
3. **审计与合规（Auditing）**：证明 workflow 执行结果的可验证性和一致性
4. **测试与验证（Testing）**：编写确定性测试，消除随机性导致的不稳定

---

## 2. DeterminismContext API 使用指南

`DeterminismContext` 提供三个核心 API 用于确定性决策：

### 2.1 确定性时钟（Deterministic Clock）

**正确用法**：
```java
DeterminismContext ctx = new DeterminismContext();

// ✅ 正确：使用确定性时钟
Instant now = ctx.clock().now();
long currentMillis = now.toEpochMilli();

// ✅ 正确：计算相对时间
Instant future = ctx.clock().now().plusSeconds(300);
```

**错误用法**：
```java
// ❌ 错误：使用系统时钟（非确定性）
Instant now = Instant.now();
long currentMillis = System.currentTimeMillis();
```

**重放行为**：
- 记录模式：`clock().now()` 记录调用时的真实系统时间
- 重放模式：`clock().now()` 返回记录的时间序列，按调用顺序依次返回

### 2.2 确定性 UUID 生成

**正确用法**：
```java
// ✅ 正确：使用确定性 UUID
UUID id = ctx.uuid().randomUUID();
String workflowId = id.toString();
```

**错误用法**：
```java
// ❌ 错误：使用标准 UUID（非确定性）
UUID id = UUID.randomUUID();
```

**重放行为**：
- 记录模式：生成新的随机 UUID 并记录
- 重放模式：返回记录的 UUID 序列

### 2.3 确定性随机数生成

**正确用法**：
```java
// ✅ 正确：使用确定性随机数
int choice = ctx.random().nextInt(10);
double probability = ctx.random().nextDouble();

// ✅ 正确：随机选择（带 source 标识）
String selectedItem = items.get(ctx.random().nextInt("item-selector", items.size()));
```

**错误用法**：
```java
// ❌ 错误：使用全局 Random（非确定性）
Random rand = new Random();
int choice = rand.nextInt(10);

// ❌ 错误：使用 Math.random()
double probability = Math.random();
```

**重放行为**：
- 记录模式：生成新的随机数并按 source 分组记录
- 重放模式：返回对应 source 的记录序列

### 2.4 完整示例

```java
public class DeterministicWorkflow {
    private final DeterminismContext ctx;

    public DeterministicWorkflow(DeterminismContext ctx) {
        this.ctx = ctx;
    }

    public void execute() {
        // 确定性时间判断
        Instant start = ctx.clock().now();
        if (start.getEpochSecond() % 2 == 0) {
            executePathA();
        } else {
            executePathB();
        }

        // 确定性随机决策
        if (ctx.random().nextDouble() > 0.5) {
            retryTask();
        }

        // 确定性 ID 生成
        String taskId = ctx.uuid().randomUUID().toString();
        scheduleTask(taskId);
    }
}
```

---

## 3. 确定性操作清单

### 3.1 安全操作（Safe）

以下操作在 workflow 中是确定性的，可安全使用：

| 操作类型 | 示例 | 说明 |
|---------|------|------|
| 纯函数计算 | `Math.max(a, b)`, `String.format()` | 无副作用的计算 |
| DeterminismContext API | `ctx.clock().now()`, `ctx.random().nextInt()` | 确定性决策 API |
| 不可变数据结构 | `List.of()`, `Map.copyOf()` | 只读数据结构 |
| 确定性排序 | `list.sort(Comparator.naturalOrder())` | 明确排序规则 |
| 局部变量操作 | `int x = a + b;` | 方法内局部状态 |

### 3.2 不安全操作（Unsafe）

以下操作是非确定性的，禁止在 workflow 代码中使用：

| 操作类型 | 错误示例 | 替代方案 |
|---------|---------|---------|
| 系统时钟 | `System.currentTimeMillis()` | `ctx.clock().now()` |
| 标准 UUID | `UUID.randomUUID()` | `ctx.uuid().randomUUID()` |
| 标准随机数 | `Math.random()`, `new Random()` | `ctx.random().nextInt()` |
| 环境变量 | `System.getenv("PATH")` | 配置注入或启动时读取 |
| 系统属性 | `System.getProperty("os.name")` | 配置注入 |
| 文件 I/O | `Files.readAllBytes(path)` | 通过外部服务抽象 |
| 网络调用 | `HttpClient.send()` | 使用 idempotent API + 重试 |
| 线程 ID | `Thread.currentThread().getId()` | 使用逻辑 ID |
| HashMap 迭代顺序 | `map.keySet().forEach()` | 使用 `LinkedHashMap` 或排序 |

### 3.3 外部服务调用规范

外部服务（数据库、REST API、消息队列）本身不是确定性的，但可通过以下模式保证幂等性：

**推荐模式**：
```java
// ✅ 使用 idempotency key 保证幂等性
String idempotencyKey = ctx.uuid().randomUUID().toString();
PaymentResult result = paymentService.charge(amount, idempotencyKey);

// ✅ 将外部调用结果记录为事件
eventStore.appendEvent(workflowId, "PaymentCompleted", Map.of(
    "result", result,
    "idempotencyKey", idempotencyKey
));
```

**反模式**：
```java
// ❌ 无幂等性保证，重放时可能重复扣款
PaymentResult result = paymentService.charge(amount);
```

---

## 4. 重放语义说明

### 4.1 决策序列记录

Workflow 执行时，所有确定性决策调用（clock、uuid、random）会被记录到 `DeterminismSnapshot`：

```java
{
  "clockTimes": [
    "2025-11-18T03:30:00Z",
    "2025-11-18T03:30:05Z"
  ],
  "uuids": [
    "a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7",
    "f8e7d6c5-b4a3-4210-9f8e-7d6c5b4a3210"
  ],
  "randoms": {
    "default": [0.7234, 0.1245, 0.9876],
    "item-selector": [3, 7, 2]
  }
}
```

### 4.2 持久化机制

决策序列通过以下两种方式持久化：

1. **周期性快照**（每 100 个事件或 5 分钟）：
   - 触发时机：`PostgresEventStore.saveSnapshot()`
   - 存储位置：`workflow_state.clock_times` (JSONB 字段)

2. **Workflow 完成时**：
   - 触发时机：`completeWorkflow()` / `failWorkflow()`
   - 存储位置：`workflow_state.clock_times`

### 4.3 重放流程

崩溃恢复时的重放流程：

```
1. 加载最近的快照 → 获取 DeterminismSnapshot
2. 进入重放模式 → ctx.clock().enterReplayMode(clockTimes)
                   ctx.uuid().enterReplayMode(uuids)
                   ctx.random().enterReplayMode(randoms)
3. 从快照点重放事件 → 重新执行 workflow 逻辑
4. 确定性 API 返回记录值 → 产生相同的决策序列
5. 继续执行新逻辑 → 切换回记录模式
```

**示例**：
```java
// 恢复 workflow
DeterminismContext ctx = new DeterminismContext();
DeterminismSnapshot snapshot = loadSnapshotFromDatabase(workflowId);

// 进入重放模式
ctx.clock().enterReplayMode(snapshot.getClockTimes());
ctx.uuid().enterReplayMode(snapshot.getUuids());
ctx.random().enterReplayMode(snapshot.getRandoms());

// 重放 workflow（将产生相同的决策）
workflow.replay(ctx);
```

---

## 5. 限制与注意事项

### 5.1 决策序列上限

为防止内存溢出，每个决策源设置了上限：

| 决策类型 | 上限 | 超限行为 |
|---------|------|---------|
| Clock times | 500 次调用 | 抛出 `IllegalStateException` |
| UUIDs | 500 次调用 | 抛出 `IllegalStateException` |
| Random (per source) | 500 次调用/source | 抛出 `IllegalStateException` |

**建议**：
- 避免在循环中频繁调用确定性 API
- 使用批量操作减少调用次数
- 将决策逻辑前置到 workflow 启动时

**错误示例**：
```java
// ❌ 错误：循环中 1000 次调用（超过上限）
for (int i = 0; i < 1000; i++) {
    UUID id = ctx.uuid().randomUUID(); // 第 501 次调用抛异常
}
```

**改进方案**：
```java
// ✅ 正确：批量生成后缓存
List<UUID> ids = new ArrayList<>();
for (int i = 0; i < Math.min(taskCount, 500); i++) {
    ids.add(ctx.uuid().randomUUID());
}
// 使用缓存的 ID
```

### 5.2 重放模式限制

重放模式下的限制：

1. **序列耗尽**：重放时调用次数超过记录序列长度
   - **错误**：`IllegalStateException: Replay sequence exhausted`
   - **原因**：代码逻辑变更导致调用次数增加
   - **解决**：确保代码逻辑与原始执行一致，或清理快照重新执行

2. **顺序不匹配**：重放时调用顺序与记录不一致
   - **原因**：if/else 分支变更、循环逻辑修改
   - **解决**：保持 workflow 代码版本一致，避免重放时修改逻辑

### 5.3 性能考量

决策日志对性能的影响：

| 操作 | 开销 | 优化建议 |
|------|------|---------|
| `ctx.clock().now()` | 10-20 ns | 可忽略 |
| `ctx.uuid().randomUUID()` | 50-100 ns | 批量生成 |
| `ctx.random().nextInt()` | 30-50 ns | 合理使用 |
| 快照序列化 | 1-5 ms (500 条记录) | 周期性触发，影响小 |
| 快照持久化 | 10-50 ms (JSONB 写入) | 异步执行 |

**性能最佳实践**：
- 减少不必要的确定性 API 调用
- 将随机数生成前置到 workflow 初始化阶段
- 避免在热路径中频繁调用 `ctx.clock().now()`

---

## 6. 故障排查指南

### 6.1 常见错误

#### 错误 1：IllegalStateException: Replay sequence exhausted

**完整错误**：
```
java.lang.IllegalStateException: Replay sequence exhausted for clock times
at aster.runtime.workflow.ReplayDeterministicClock.now()
```

**原因**：
- 重放时 `ctx.clock().now()` 调用次数超过记录的序列长度
- 代码逻辑变更导致新增了时钟调用

**排查步骤**：
1. 检查快照中记录的 `clockTimes` 长度
2. 比对原始代码与当前代码的差异
3. 确认是否在重放期间修改了 workflow 代码

**解决方案**：
```java
// 方案 1：清理快照，从头执行
eventStore.saveSnapshot(workflowId, 0, null);

// 方案 2：检查代码版本一致性
// 确保 workflow 代码与快照记录时的版本一致
```

#### 错误 2：Random sequence exhausted for source 'xxx'

**完整错误**：
```
java.lang.IllegalStateException: Random sequence exhausted for source 'item-selector'
```

**原因**：
- 某个 random source 的调用次数超过记录序列
- 循环次数变化或条件分支变更

**解决方案**：
- 检查该 source 的使用位置
- 确认循环次数或分支逻辑是否一致

#### 错误 3：超过 500 次调用限制

**完整错误**：
```
java.lang.IllegalStateException: Exceeded maximum clock records (500)
```

**原因**：
- 单个 workflow 执行中调用 `ctx.clock().now()` 超过 500 次
- 通常发生在循环或递归逻辑中

**解决方案**：
```java
// ❌ 错误：循环中频繁调用
for (Task task : tasks) {
    Instant start = ctx.clock().now(); // 每次循环都调用
    processTask(task);
}

// ✅ 改进：缓存时间或减少调用
Instant batchStart = ctx.clock().now(); // 循环外调用一次
for (Task task : tasks) {
    processTask(task, batchStart); // 传递缓存的时间
}
```

### 6.2 调试技巧

**1. 启用决策日志记录**

```java
// 记录所有确定性决策到日志
DeterminismContext ctx = new DeterminismContext();
ctx.clock().setDebugMode(true);
ctx.random().setDebugMode(true);
ctx.uuid().setDebugMode(true);
```

**2. 比对两次执行的决策序列**

```java
// 第一次执行
DeterminismSnapshot snap1 = recordExecution(workflow);

// 第二次执行（重放）
DeterminismSnapshot snap2 = recordExecution(workflow);

// 比对差异
System.out.println("Clock calls: " + snap1.getClockTimes().size() + " vs " + snap2.getClockTimes().size());
```

**3. 使用混沌测试验证确定性**

参考 `ChaosSchedulerTest.testDeterministicReplay()` 示例：

```java
// 运行两次相同 workflow
ChaosScenarioResult run1 = executeWorkflow(seed);
ChaosScenarioResult run2 = executeWorkflow(seed); // 相同种子

// 验证结果一致
assertEquals(run1.completionOrder, run2.completionOrder);
assertEquals(run1.failingTaskId, run2.failingTaskId);
```

---

## 7. 进阶主题

### 7.1 多线程 Workflow

DeterminismContext 使用 `ThreadLocal` 隔离不同 workflow 的决策序列：

```java
// Workflow A（线程 1）
DeterminismContext ctxA = new DeterminismContext();
workflowA.execute(ctxA); // 独立的决策序列

// Workflow B（线程 2）
DeterminismContext ctxB = new DeterminismContext();
workflowB.execute(ctxB); // 不与 A 冲突
```

### 7.2 部分确定性

某些场景下可能需要部分使用确定性 API：

```java
// 确定性部分：核心业务逻辑
if (ctx.random().nextDouble() > 0.5) {
    executeCriticalPath(ctx);
}

// 非确定性部分：性能监控（可选）
long actualTime = System.currentTimeMillis(); // 允许使用非确定性 API
metricsCollector.record("execution_time", actualTime);
```

**注意**：非确定性操作不应影响 workflow 的状态转换。

### 7.3 版本兼容性

Workflow 代码升级时需注意向后兼容：

```java
// ✅ 兼容：新增可选参数
void execute(DeterminismContext ctx, Optional<Config> config) {
    // 旧版本传 Optional.empty()，新版本传实际值
}

// ❌ 不兼容：修改决策逻辑
void execute(DeterminismContext ctx) {
    // 旧版：if (ctx.random().nextDouble() > 0.5)
    // 新版：if (ctx.random().nextDouble() > 0.8) // 重放失败！
}
```

---

## 8. 参考资料

### 8.1 相关 API 文档

- `aster.runtime.workflow.DeterminismContext`
- `aster.runtime.workflow.ReplayDeterministicClock`
- `aster.runtime.workflow.ReplayDeterministicRandom`
- `aster.runtime.workflow.ReplayDeterministicUuid`
- `quarkus-policy-api/.../DeterminismSnapshot`

### 8.2 相关文档

- [Workflow 重试语义](./retry-semantics.md)
- [事件溯源与快照](../language/workflow.md)

### 8.3 测试用例

参考以下测试了解确定性保证的实际应用：

- `ChaosSchedulerTest.testDeterministicReplay()` - 确定性重放验证
- `RetryExecutionTest` - 确定性时钟在重试中的应用
- `DelayedTaskTest` - 确定性时钟在延迟任务中的应用

---

**最后更新**：2025-11-18
**维护者**：Aster Workflow Runtime Team
