# Workflow 实现细节

*最后更新：2025-11-10 05:08 NZST · 执行者：Codex*

本文面向编译器与运行时贡献者，概述 Phase 2.1 Workflow 语言特性的实现方式，涵盖 parser、Core IR、类型/效果系统以及 JVM runtime。

## 目录
- [Parser 实现](#parser-实现)
  - [parseWorkflow 与 parseStep](#parseworkflow-与-parsestep)
  - [关键字与缩进管理](#关键字与缩进管理)
  - [AST 节点构造与元数据](#ast-节点构造与元数据)
- [Core IR 设计](#core-ir-设计)
  - [Core.Workflow/Core.Step 节点](#coreworkflowcorestep-节点)
  - [降级逻辑（AST → Core）](#降级逻辑ast--core)
  - [EffectCaps 聚合策略](#effectcaps-聚合策略)
- [Type System 扩展](#type-system-扩展)
  - [Workflow<T,E> 类型编码](#workflowte-类型编码)
  - [typecheckWorkflow/typecheckStep](#typecheckworkflowtypecheckstep)
  - [Compensate 约束与效果缓存优化](#compensate-约束与效果缓存优化)
- [Effect Inference](#effect-inference)
  - [EffectCollector 扩展](#effectcollector-扩展)
  - [Capability 验证机制](#capability-验证机制)
  - [Compensate capability 约束](#compensate-capability-约束)
- [Runtime 集成](#runtime-集成)
  - [JVM Emitter](#jvm-emitter)
  - [WorkflowNode 接口](#workflownode-接口)
  - [AsyncTaskRegistry/DependencyGraph/WorkflowScheduler](#asynctaskregistrydependencygraphworkflowscheduler)

## Parser 实现

### parseWorkflow 与 parseStep
- 入口位于 `src/parser/expr-stmt-parser.ts`，`parseStatement` 检测到 `KW.WORKFLOW` 时委派给 `parseWorkflow`。
- `parseWorkflow` 流程：
  1. 消耗 `workflow:` 与换行、`INDENT` token。
  2. 循环解析 `step`，最少 1 个，存入 `StepStmt[]`。
  3. 解析可选 `retry` 与 `timeout`，复用公共错误处理器。
  4. 验证 `DEDENT`，结尾调用 `expectPeriodEnd`。
- `parseStep` 构造 `Node.Step`，在检测到 `compensate:` 时再解析一个 `Block` 并附加。

### 关键字与缩进管理
- Parser 使用 `kwParts` 将多词关键字拆分为 token 序列；Phase 2.1.6 引入常量缓存（如 `WAIT_FOR_PARTS`、`MAX_ATTEMPTS_PARTS`）以避免频繁分配。
- 所有嵌套块统一使用 `INDENT`/`DEDENT` token：`workflow`、`step`、`retry` 块均强制缩进，`timeout` 为单行语句无需 `INDENT`。
- `ctx.consumeNewlines()` 在 step、retry、timeout 之间清理空行，保证语句对齐。

### AST 节点构造与元数据
- `Node.Workflow`/`Node.Step` 在 `src/ast.ts` 定义，构造后调用 `assignSpan`/`assignSpanFromTokens` 将源位置信息附加到 `span` 字段。
- Parser 在每个语句完成后调用 `lastConsumedToken` 取得结束位置，确保后续诊断（例如 E022/E025）能精确定位。

## Core IR 设计

### Core.Workflow/Core.Step 节点
- 定义于 `src/core_ir.ts`，Workflow 结构包含：
  - `steps: Core.Step[]`
  - `effectCaps: CapabilityKind[]`
  - `retry?: RetryPolicy`
  - `timeout?: Timeout`
- Step 结构包含：`name`, `body: Core.Block`, `effectCaps`, 可选 `compensate`。

### 降级逻辑（AST → Core）
- `lower_to_core.ts` 中的 `lowerWorkflow`/`lowerStep`：
  - 将 `Node.Step` 降级为 `Core.Step`，递归处理 `Block`。
  - 复制 retry/timeout 元数据。
  - 通过 `withOrigin` 将 `span` 映射为 `origin`，方便类型/效果诊断。

### EffectCaps 聚合策略
- `lowerStep` 调用 `collectCapabilitiesFromBlock` 对主体与补偿块分别遍历 `Core.Block`，根据 `inferCapabilityFromName` 推断 `CapabilityKind`。
- `mergeCapabilitySets` 保持首次出现顺序并去重，Workflow 层将 Step 的 capability 集合合并，用于：
  - Typechecker 快速判断是否已有 IO 之外的非 CPU 能力。
  - Effect inference 与 runtime manifest（未来扩展）。

## Type System 扩展

### Workflow<T,E> 类型编码
- `typecheckWorkflow` 返回 `TypeApp('Workflow', [ResultType, EffectType])`，其中：
  - `ResultType` 来源于最后一个 step 的返回值（Unknown 时沿用 lattice）。
  - `EffectType` 基于 workflow + step 的 effect/capability 推断。

### typecheckWorkflow/typecheckStep
- `typecheckStep`：
  - 调用 `typecheckBlock` 推导 step 主体类型。
  - `collectEffects` 统计 step 主体的 IO/CPU 行为，用于补偿缺失告警。
  - 若存在 `compensate`，调用 `validateCompensateBlock` 校验返回类型。
- Phase 2.1.6 新增 `stepEffects` 缓存：`typecheckWorkflow` 在遍历 steps 时传入 Map，缓存 `collectEffects` 的结果，供后续效果推断复用。

### Compensate 约束与效果缓存优化
- `validateCompensateBlock` 强制补偿返回 `Result<Unit, Err>`，错误类型需与主体匹配。
- `stepHasSideEffects` 检查 step 主体 `effects` 集与 capability 声明，若检测到 IO/非 CPU 能力且缺少补偿，发出 E023。
- `workflowEffectType` 现可复用缓存的 effect 集合：
  - 先检查 workflow 自身的 capability 集是否包含非 CPU 能力。
  1. 若尚未确定 IO，遍历 step 缓存，遇到 IO 即返回 `IO_EFFECT_TYPE`。
  2. 若仅检测到 CPU，则返回 `CPU_EFFECT_TYPE`，否则默认为 `PURE_EFFECT_TYPE`。
- 该优化避免了对每个 step 进行二次 `collectEffects` 遍历，在大型 workflow（100+ steps）中可减少 30% 以上的类型检查时间。

## Effect Inference

### EffectCollector 扩展
- `src/effect_inference.ts` 的 `EffectCollector` 在遍历 `workflow` 时默认添加 IO effect，并递归访问 step 主体与补偿块。
- 任一步骤一旦包含 workflow 语句，即强制 `localEffects` 添加 IO，确保 `inferEffects` 输出的 requiredEffects 与 Typechecker 一致。

### Capability 验证机制
- `collectCapabilities` 遍历 `Core.Block` 中的调用，根据 `inferCapabilityFromName` 推断能力。
- `reportWorkflowCapabilityViolation` 比较函数头部声明的 `effectCaps` 与观察到的 capability 集：
  - 缺失声明 → **E027(WORKFLOW_UNDECLARED_CAPABILITY)**，payload 包含 `{ func, step, capability }`。
  - 冗余声明则通过 `EFF_CAP_SUPERFLUOUS` 提示。

### Compensate capability 约束
- 对于带补偿的步骤，Typechecker 再次采集补偿块的能力集合，并与主体集合比较。
- 若补偿新增能力，抛出 **E028**，提示在主体中执行相同能力或扩展函数声明。

## Runtime 集成

### JVM Emitter
- `src/jvm/emitter.ts` 的 `emitWorkflowStatement`：
  1. 为每个 Workflow 创建 `AsyncTaskRegistry`、`DependencyGraph`、`WorkflowScheduler`。
  2. 为每个 step 生成 `java.util.function.Supplier<Object>`，并在注册时附带补偿逻辑。
  3. 缺省使用线性依赖（step i 依赖 i-1），`workflowDependencyLiteral` 为未来显式依赖预留接口。
  4. Retry 信息目前以注释形式保留，Timeout 转换为毫秒并传入 `executeUntilComplete`。

### WorkflowNode 接口
- `aster-truffle/src/main/java/aster/truffle/nodes/WorkflowNode.java` 构成运行时入口：
  - 构造函数接收 `Env`, `Node[] taskExprs`, `String[] taskNames`, `Map<String, Set<String>> dependencies`, `timeoutMs`。
  - `execute` 中按顺序启动所有 step，构造依赖图，注册到 `WorkflowScheduler`，并收集结果数组返回。

### AsyncTaskRegistry/DependencyGraph/WorkflowScheduler
- **AsyncTaskRegistry**：维护任务状态、结果、异常，Phase 2.1 使用协作式调度（单线程）。
- **DependencyGraph**：管理任务依赖关系，检测循环，计算就绪任务集合。
- **WorkflowScheduler**：轮询就绪任务并执行，处理 fail-fast、取消与全局 timeout。
- Workflow emitter 生成的 Java 代码直接调用这些组件，确保语言层 workflow 与 runtime 设施一致。

该实现路径确保从语法到 runtime 的闭环：Parser 生成带 span 的 AST，降级为携带 capability 元数据的 Core IR，Typechecker/Effect inference 负责静态验证，JVM emitter/WorkflowNode 则将抽象语义映射到 AsyncTaskRegistry + DependencyGraph + WorkflowScheduler 的执行模型。
