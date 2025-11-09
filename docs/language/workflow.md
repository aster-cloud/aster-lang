# Workflow 语法指南

*最后更新：2025-11-10 05:07 NZST · 执行者：Codex*

Workflow 语法以 CNL（Controlled Natural Language）描述可补偿、可观测的业务流程。本指南面向策略作者与运营人员，覆盖关键字规则、配置块写法、完整示例与最佳实践。

## 目录
- [基础语法](#基础语法)
  - [Workflow 关键字与结构](#workflow-关键字与结构)
  - [Step 定义与命名](#step-定义与命名)
  - [Compensate 块语义](#compensate-块语义)
  - [Retry 与 Timeout 配置](#retry-与-timeout-配置)
- [完整示例](#完整示例)
  - [线性订单流程](#线性订单流程)
  - [补偿驱动的错误恢复](#补偿驱动的错误恢复)
  - [Timeout 配置示例](#timeout-配置示例)
  - [Retry 策略示例](#retry-策略示例)
- [最佳实践](#最佳实践)
  - [何时编写 Compensate](#何时编写-compensate)
  - [可恢复 Workflow 设计指南](#可恢复-workflow-设计指南)
  - [Effect/Capability 声明要点](#effectcapability-声明要点)
  - [常见错误与排查](#常见错误与排查)

## 基础语法

### Workflow 关键字与结构
- 入口语法：在函数体内书写 `workflow:`，后续内容必须缩进两个空格。
- `workflow` 块以句点 `.` 结束，和其他语句保持一致的行尾句点规则。
- Workflow 必须位于声明了 `It performs io ...` 的函数中，编译器会强制要求 IO 效果（错误码 E026）。
- Workflow 至少包含一个 `step`，否则解析阶段直接报错。
- Workflow 元数据（`retry`, `timeout`）写在 step 列表之后，继续沿用缩进块语义。

```text
To processOrder with input: Order, produce Result of Receipt with IO. It performs io with Http and Sql:
  workflow:
    step validate:
      ...
  .
```

### Step 定义与命名
- `step <identifier>:` 定义顺序执行的步骤，建议使用蛇形（`reserve_inventory`）或语义化短语（`charge_payment`）。
- Step 块内部可使用所有常规语句（`Let`, `Return`, `Match`, `start/await` 等）。
- Step 的执行结果为最后一条语句的返回值；通常返回 `Result` 用于表达成功/失败。
- Workflow 默认按声明顺序推导依赖关系；Phase 2.1 尚未提供显式 `depends on` 语法，所有 Step 串行执行。

### Compensate 块语义
- `compensate:` 可选，但一旦步骤执行了 IO/Capability，将触发 **E023(WORKFLOW_COMPENSATE_MISSING)** 警告。
- Compensate 必须返回 `Result<Unit, ErrType>`，其中 `ErrType` 与 step 主体的错误类型一致。类型不匹配会触发 **E022**。
- Compensate 内禁止引入新的能力，违反时触发 **E028(COMPENSATE_NEW_CAPABILITY)**。
- 常见模式：在主体执行资源预留、计费、外部调用后，Compensate 对应释放、退款、撤销。

### Retry 与 Timeout 配置
- `retry:` 为缩进块，包含两条指令：
  - `max attempts: <int>.` （必须 >0）
  - `backoff: exponential|linear.`
- `timeout:` 为单行语句，格式 `timeout: <seconds> seconds.`，编译器会转换为毫秒。
- Retry/Timeout 元数据存入 Core IR，并由 JVM emitter 注释提示运行时策略（Phase 2.2 将真正执行）。
- 配置示例：

```text
  retry:
    max attempts: 3.
    backoff: exponential.

  timeout: 45 seconds.
```

## 完整示例

### 线性订单流程
```text
This module is examples.workflow.linear_order.

To processOrder, produce Result of Text with IO. It performs io:
  
  workflow:
    step validate:
      return ok of "order validated".
    
    step reserve_inventory:
      return ok of "inventory reserved".
      
      compensate:
        return ok of "inventory released".
    
    step charge_payment:
      return ok of "payment charged".
      
      compensate:
        return ok of "payment refunded".
    
    timeout: 30 seconds.
  
  .
```
- 所有 Step 串行执行，默认依赖上一个 Step 的完成结果。
- Timeout 保护整个流程，若 30 秒内未完成，即触发 WorkflowScheduler 的超时取消。

### 补偿驱动的错误恢复
```text
This module is examples.workflow.error_recovery.

To handleErrors, produce Result of Text with IO. It performs io:
  
  workflow:
    step prepare:
      return ok of "prepared".
      
      compensate:
        return ok of "cleanup prepared".
    
    step execute:
      return err of "execution failed".
      
      compensate:
        return ok of "rollback executed".
  
  .
```
- 第二个 Step 返回 `err` 触发 compensations，AsyncTaskRegistry 将执行 `rollback executed` 以还原状态。

### Timeout 配置示例
```text
To syncExternalResources, produce Result of Unit with IO. It performs io with Http and Secrets:
  workflow:
    step fetch_remote:
      ...

    step reconcile_cache:
      ...

    timeout: 120 seconds.
  .
```
- Timeout 值会在 JVM emitter 中转换为 `long workflowTimeoutMs`，WorkflowScheduler 轮询时负责中断。
- 结合 `Secrets` 能力声明，表明任务可能访问凭证管理器。

### Retry 策略示例
```text
To capturePayment, produce Result of Receipt with IO. It performs io with Http:
  workflow:
    step invoke_gateway:
      return Http.charge(paymentRequest).

    retry:
      max attempts: 4.
      backoff: linear.
  .
```
- Retry 对整个 workflow 生效，compiler 确保 `max attempts` 与 `backoff` 均已填写。
- Phase 2.1 JVM emitter以注释保留策略，后续 runtime 将据此包装 `WorkflowScheduler` 调用。

## 最佳实践

### 何时编写 Compensate
- **外部资源状态改变**：库存锁定、支付扣款、异步任务排队等都需要补偿。
- **慢速依赖**：调用第三方接口失败概率高时，先写补偿以便快速回滚。
- **并发步骤**：虽然 Phase 2.1 尚未暴露并行 DSL，仍建议为潜在并发步骤提前设计补偿接口。

### 可恢复 Workflow 设计指南
- **Result-first**：步骤返回 `Result<Ok, Err>`，将错误类型用于补偿和汇总。
- **最小副作用**：在单个 step 中完成对同一系统的操作，避免横跨多个 step 的共享事务。
- **Idempotent Compensate**：补偿逻辑应当可重复执行，保证 WorkflowScheduler 在重试/重放时安全。
- **粒度清晰**：将校验、资源预留、外部调用拆分为独立 step，提升可观测性。

### Effect/Capability 声明要点
- 函数层需声明 `It performs io` 才能使用 workflow；若缺失将触发 **E026**。
- 在 `It performs io with Http and Sql.` 中列举所有会在 step/compensate 里调用的能力。漏写会触发 **E027**。
- Compensate 只能复用主体用过的能力，新增能力会触发 **E028**；必要时将副作用迁回主体。

### 常见错误与排查
| 错误码 | 触发原因 | 排查步骤 |
| --- | --- | --- |
| E022 | Compensate 返回类型与主体错误类型不匹配 | 确认主体 `Result` 的 `Err` 类型，并让补偿返回 `Result<Unit, Err>` |
| E023 | 带副作用的 step 未定义补偿 | 检查该 step 是否使用 IO/Capability；如需跳过，在设计上保证幂等性并添加说明 |
| E024 | `max attempts` ≤ 0 | 改为正整数 |
| E025 | Timeout 非正值或缺少单位 | 使用 `timeout: <n> seconds.` 格式 |
| E026 | 函数缺少 `It performs io` | 在函数头部添加 `It performs io with ...` |
| E027 | 未在函数头声明使用到的 capability | 将 `Http`, `Sql`, `Secrets` 等加入 `It performs io with ...` |
| E028 | Compensate 引入新 capability | 将相关调用移到主体或同步在主体中声明该 capability |

遵循以上语法与实践，可确保 workflow 程序在编译期通过解析、类型检查与效果/能力校验，并在运行时获得清晰的补偿与调度语义。
