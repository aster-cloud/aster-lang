# Workflow 并发执行模型

*最后更新：2025-11-10 · Phase 2.5 完成*

本文档详细说明 Aster Language workflow 的并发执行机制、补偿策略、事件模型和性能考虑。

## 目录
- [并发执行模型](#并发执行模型)
- [补偿策略](#补偿策略)
- [事件模型](#事件模型)
- [性能考虑](#性能考虑)
- [最佳实践](#最佳实践)

## 并发执行模型

### DAG 调度原理

Workflow 步骤之间通过 `depends on` 语法构建有向无环图（DAG），运行时调度器根据依赖关系并发执行所有就绪的步骤。

**依赖图构建流程**：
1. **编译期**：Parser 解析 `depends on ["step_a", "step_b"]` 并写入 Core IR
2. **运行期**：`AsyncTaskRegistry` 构建内部 `DependencyGraph`
3. **调度期**：`WorkflowScheduler` 批量提交所有就绪任务到线程池

**示例**：
```aster
workflow:
  step check_stock:
    return ok of "checked".

  step order_supplier_a depends on ["check_stock"]:
    return ok of "ordered from A".

  step order_supplier_b depends on ["check_stock"]:
    return ok of "ordered from B".

  step update_inventory depends on ["order_supplier_a", "order_supplier_b"]:
    return ok of "inventory updated".
```

**执行流程**：
```
check_stock (串行)
    ↓
    ├─→ order_supplier_a ─┐
    └─→ order_supplier_b ─┤ (并发执行)
                          ↓
                   update_inventory (等待两者完成)
```

### 线程池配置

**默认配置**：
- 线程池大小 = `Runtime.getRuntime().availableProcessors()` (CPU 核数)
- 最小线程数 = 1（确保单核环境可用）
- 线程池类型 = `Executors.newFixedThreadPool()`

**单线程模式**：
- 当线程池大小为 1 时，调度器自动退化为串行执行
- 无 `depends on` 的旧 workflow 与 Phase 2.0 行为保持一致

### 循环检测

**编译期检测**：
- TypeChecker 在 workflow 类型检查阶段执行 DFS 检测循环依赖
- 检测到循环时报告 `WORKFLOW_CIRCULAR_DEPENDENCY` 错误

**运行期检测**：
- `DependencyGraph.addTask()` 执行 DFS 验证
- 发现循环时抛出 `IllegalArgumentException("Circular dependency detected ...")`

**死锁检测**：
- 若所有任务未完成但没有就绪节点，判定为死锁
- 抛出 `IllegalStateException("Deadlock detected")`

### 向后兼容

**隐式依赖推导**：
- 缺省 `depends on` 时，编译器自动为 step 添加对前一步骤的依赖
- 保持 Phase 2.0 的串行执行语义

**串行兼容示例**：
```aster
workflow:
  step first:
    return ok of "one".

  step second:  // 自动依赖 "first"
    return ok of "two".

  step third:   // 自动依赖 "second"
    return ok of "three".
```

## 补偿策略

### LIFO 原则

Workflow 失败时，已完成的步骤按 **LIFO（Last In, First Out）** 顺序执行补偿，确保最新的副作用最先被撤销。

**补偿堆栈**：
```
执行顺序：A → B → C → (D 失败)
补偿顺序：C → B → A  (LIFO)
```

### 并发步骤的补偿顺序

并发步骤的补偿顺序由 `completedAt` 时间戳决定：

**规则**：
- 按 `completedAt` **降序**排序（最晚完成的最先补偿）
- 同一批次的步骤完成顺序由线程池执行顺序决定
- 补偿顺序与真实提交顺序一致，无需显式声明优先级

**示例**：
```
并发执行：
  - step_b 完成于 T1 (早)
  - step_c 完成于 T2 (晚)

补偿顺序：step_c → step_b  (T2 > T1，后完成先补偿)
```

### 补偿失败处理

**重试机制**：
- 补偿失败时不会自动重试
- Workflow 状态标记为 `COMPENSATION_FAILED`
- 需要人工介入或外部重试逻辑

**幂等性要求**：
- 补偿逻辑必须设计为幂等操作
- 支持 WorkflowScheduler 在重试/重放时安全执行

## 事件模型

### Workflow 事件类型

所有 workflow 状态变更通过事件存储持久化：

| 事件类型 | 触发时机 | Payload 字段 |
|---------|---------|-------------|
| `WORKFLOW_STARTED` | workflow 开始执行 | `workflowId`, `metadata` |
| `STEP_STARTED` | 步骤开始执行 | `stepId`, `dependencies`, `startedAt` |
| `STEP_COMPLETED` | 步骤成功完成 | `stepId`, `result`, `completedAt`, `hasCompensation` |
| `STEP_FAILED` | 步骤执行失败 | `stepId`, `error`, `completedAt` |
| `COMPENSATION_SCHEDULED` | 补偿已调度 | `stepId`, `scheduledAt` |
| `COMPENSATION_COMPLETED` | 补偿成功完成 | `stepId`, `completedAt` |
| `WORKFLOW_COMPLETED` | workflow 成功完成 | `result` |
| `WORKFLOW_FAILED` | workflow 执行失败 | `error` |
| `WORKFLOW_COMPENSATED` | workflow 补偿完成 | `compensatedSteps` |

### Dependencies 字段

**Phase 2.5 新增**：所有 step 事件的 payload 包含 `dependencies` 字段，记录该步骤的依赖列表。

**JSON Schema**：
```json
{
  "stepId": "order_supplier_a",
  "dependencies": ["check_stock"],
  "status": "COMPLETED",
  "completedAt": "2025-11-10T06:48:59.522Z",
  "result": "ordered from A",
  "hasCompensation": true
}
```

### 事件序列完整性

**并发写入安全**：
- 使用数据库 `BIGSERIAL` 生成全局递增的 `sequence` 序列号
- 保证并发 step 事件写入时 sequence 连续且唯一
- 无跳号、无乱序

**序列验证**：
```sql
SELECT sequence FROM workflow_events
WHERE workflow_id = ?
ORDER BY sequence;

-- 验证：sequence[i] = sequence[i-1] + 1
```

## 性能考虑

### 线程池大小

**推荐配置**：
- **默认**：等于 CPU 核数（适合 CPU 密集型任务）
- **I/O 密集型**：可增加至 2-4 倍 CPU 核数
- **低并发场景**：减少至 1-2 个线程降低上下文切换开销

**调整方式**：
```java
// 自定义线程池大小
AsyncTaskRegistry registry = new AsyncTaskRegistry();
registry.setThreadPoolSize(8);
```

### DAG 复杂度限制

**建议**：
- 单个 workflow 的步骤数不超过 **20 个**
- DAG 深度不超过 **5 层**
- 单个步骤的依赖数不超过 **10 个**

**复杂度过高的影响**：
- 依赖图构建耗时增加
- 调度开销增大
- 事件存储压力增加

**解决方案**：
- 拆分为多个子 workflow
- 使用消息队列解耦步骤

### 并发窗口优化

**最佳实践**：
- 将慢速 I/O 操作（HTTP 调用、数据库查询）放在独立的步骤中
- 使用 `depends on` 显式声明依赖，最大化并发机会
- 避免过度并行化短时 CPU 任务（线程切换开销 > 并行收益）

**反模式**：
```aster
// ❌ 避免：过度并行化
workflow:
  step a:
    return ok of "fast".  // 10ms

  step b depends on ["a"]:
    return ok of "fast".  // 10ms

  step c depends on ["a"]:
    return ok of "fast".  // 10ms
```

**优化后**：
```aster
// ✅ 推荐：合并快速步骤
workflow:
  step process:
    Let result_a be ok of "fast".
    Let result_b be ok of "fast".
    Let result_c be ok of "fast".
    return ok of result_c.
```

### 补偿性能

**补偿执行特点**：
- 补偿按 LIFO 顺序**串行执行**（避免并发回滚冲突）
- 每个补偿步骤独立提交到线程池
- 补偿总耗时 = Σ(单步补偿耗时)

**优化建议**：
- 设计轻量级补偿逻辑（< 100ms）
- 避免补偿中执行复杂业务逻辑
- 使用异步消息队列延迟处理非紧急补偿

## 最佳实践

### 并发模式选择

**Fan-out 模式**：
- **适用**：多个独立任务依赖同一前置步骤
- **示例**：库存检查后并发调用多个供应商
- **优势**：最大化并发度

**Diamond 模式**：
- **适用**：两条并行分支汇聚到同一后续步骤
- **示例**：风险评分 + 优惠查询 → 汇总决策
- **优势**：清晰的数据流

**Serial 模式**：
- **适用**：步骤间有强依赖关系
- **示例**：验证 → 预留 → 扣款 → 确认
- **优势**：简单可靠

### 依赖声明原则

**显式优于隐式**：
```aster
// ✅ 推荐：显式声明依赖
step merge depends on ["branch_a", "branch_b"]:
  ...

// ❌ 避免：依赖隐式顺序
step merge:  // 隐式依赖 branch_b
  ...
```

**最小化依赖**：
```aster
// ✅ 推荐：只依赖直接前驱
step finalize depends on ["step_c"]:
  ...

// ❌ 避免：过度声明传递依赖
step finalize depends on ["step_a", "step_b", "step_c"]:
  ...
```

### 错误处理

**快速失败**：
- 任意步骤失败立即终止 workflow
- 未执行的步骤自动标记为 `CANCELLED`
- 触发 LIFO 补偿链

**补偿设计**：
- 每个有副作用的步骤必须提供 `compensate` 块
- 补偿逻辑应保证幂等性
- 记录补偿失败原因以便人工介入

### 监控与调试

**关键指标**：
- Workflow 执行时长
- 步骤并发度（峰值并发任务数）
- 补偿触发频率
- 事件序列完整性

**调试工具**：
- 事件存储查询（按 `sequence` 排序）
- 依赖图可视化（GraphViz）
- 线程池监控（活跃线程数、队列长度）

---

**相关文档**：
- [Workflow 语法指南](language/workflow.md)
- [Event Store 设计](architecture/event-store.md)
- [AsyncTaskRegistry API](api/async-task-registry.md)
