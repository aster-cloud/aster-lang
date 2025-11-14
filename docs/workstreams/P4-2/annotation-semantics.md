# P4-2 注解语义框架设计

**日期**：2025-11-14 06:51 NZST  
**执行者**：Codex

## 目录
- [1. PII 注解语义设计](#1-pii-注解语义设计)
  - [1.1 设计目标与约束](#11-设计目标与约束)
  - [1.2 类型传播规则](#12-类型传播规则)
  - [1.3 赋值兼容性矩阵](#13-赋值兼容性矩阵)
  - [1.4 输出限制与敏感接收方](#14-输出限制与敏感接收方)
  - [1.5 类型检查算法（伪代码）](#15-类型检查算法伪代码)
  - [1.6 代码生成与元数据保留](#16-代码生成与元数据保留)
  - [1.7 参考资料](#17-参考资料)
- [2. Capability 注解语义设计](#2-capability-注解语义设计)
  - [2.1 设计目标与命名空间](#21-设计目标与命名空间)
  - [2.2 效果多态与签名语法](#22-效果多态与签名语法)
  - [2.3 调用检查与上下文要求](#23-调用检查与上下文要求)
  - [2.4 Manifest 验证连接](#24-manifest-验证连接)
  - [2.5 Java 端补齐点](#25-java-端补齐点)
  - [2.6 类型检查算法（伪代码）](#26-类型检查算法伪代码)
  - [2.7 参考资料](#27-参考资料)
- [3. 双栈一致性保证](#3-双栈一致性保证)
- [4. 渐进式迁移路径](#4-渐进式迁移路径)

---

## 1. PII 注解语义设计

### 1.1 设计目标与约束
- **绑定式类型标注**：沿用 `TypePii`（`src/types.ts:369-378`）与 `CoreModel.Annotation`（`aster-core/.../CoreModel.java:137-146`）结构，将 `@pii(level, category)` 视为**包裹类型**而非独立语法糖。
- **等级与类别双维度**：`level ∈ {L1 < L2 < L3}` 表示敏感度，`category` 与 `PiiDataCategory` 枚举保持一致（邮箱/金融/健康等）。
- **运行时可见元数据**：Java ASM emitter 必须把 PII 信息写入 `RuntimeVisible(Type)Annotations`，以便策略引擎、DTO、Policy VM 重用。
- **性能约束**：类型检查阶段仅对包含 `TypePii` 的类型执行额外流分析，避免对纯净模块产生额外成本；缓存 PII lattice 判定结果。
- **无安全退让**：遵循 Phase 4 方针，禁止“允许日志但打印警告”式策略，输出限制只有“允许/拒绝/先调用脱敏 API”三态。

### 1.2 类型传播规则
1. **函数签名**  
   - 参数或返回值可以写成 `@pii(L2, email) Text`。编译器在函数类型中保留 `piiMeta = {level, category}`。
   - 泛型函数可允许 `T: Pii` 约束，将 `TypeVar` 与 `TypePii` 组合，便于 effect/PII 联动。
2. **调用传播**  
   - 如果被调函数返回 PII，调用点推断结果附带同级 `piiMeta`。若调用者立刻将结果赋给普通类型，判定为降级违规。
   - 内建脱敏算子（例如 `redact(levelTarget)` 或 `aggregate()`）返回新的 `TypePii`，level 可以降低但必须遵守 lattice（详见 1.3）。
3. **表达式组合**  
   - 算术/字符串操作遵循“最敏感者优先”：`L3 ⊕ L1 → L3`。类别合并产生集合，供后续输出校验使用。
   - 分支/模式匹配中，如任一分支产出 PII，则整个表达式返回合并后的 PII。
4. **容器与高阶类型**  
   - `List<@pii(L2, financial) Money>` 允许，但容器类型自身不自动成为 PII，只有在访问元素时传播属性。
   - `Result<@pii(L3, biometric) Data, E>`：在 `await`、`match` 解构时把 PII 元数据继续附加到绑定变量。

### 1.3 赋值兼容性矩阵

| 左值等级 | 允许的右值等级 | 备注 |
| --- | --- | --- |
| `Plain`（无 PII） | 仅 `Plain` | 任何 PII→Plain 赋值报错（`PII_ASSIGN_DOWNGRADE`）。 |
| `L1` | `Plain`（自动提升为 `L1`）、`L1` | `Plain` 右值会被视为 `L1` 并记录来源，方便后续追踪。 |
| `L2` | `L2`、`L3` 禁止 | 只能接收 `L2`，若 `L1` 赋给 `L2` 则隐式升级（并标记 `PII_IMPLICIT_UPLEVEL` warning）。 |
| `L3` | `L3` | 最高级别禁止任何降级。 |

类别兼容性：  
- 默认要求 **精确匹配**（email 只能赋给 email）。  
- 如果目标声明为 `category = any`，允许集合向下兼容，但需记录具体来源集合用于输出限制。  
- 多类别聚合时记录集合，并阻止把集合赋给单一类别变量。

### 1.4 输出限制与敏感接收方
1. **敏感 sink 定义**  
   - 内建 IO：`print`, `log`, `http.*`, `sql.*`, `workflow step`、`emit` 等都标记 sink 分类（控制台、持久化、网络）。  
   - 自定义函数可通过 `@sink(kind = 'log')` 声明。
2. **规则**  
   - `level >= L2` 数据 **禁止** 输出到 `log/print`。  
   - `L3` 数据只有在 `redact(L3 → L1)` 或 `tokenize()` 之后才能进入任何 sink。  
   - 输出到网络/数据库必须同时声明 `@io with Http/Sql` 并拥有 `Capability` 校验通过。  
   - 允许配置 `@piiAllow(level<=L1, sinks=[audit])` 注解在函数头上，供特殊审计场景使用。
3. **诊断**  
   - 缺失脱敏：`PII_SINK_UNSANITIZED`。  
   - 缺失 capability：交给 `EFF_CAP_MISSING`（第二节）。  
   - 未声明 `@pii` 却从 sink 捕获：`PII_SINK_UNKNOWN`，提示开发者补注解以追踪。

### 1.5 类型检查算法（伪代码）

```text
procedure checkModulePII(module):
  lattice = {Plain < L1 < L2 < L3}
  for each func in module.funcs:
    env = new Map<Symbol, PiiMeta>()
    seedEnvWithParams(func, env)
    traverseBlock(func.body, env)

procedure traverseBlock(block, env):
  for stmt in block:
    match stmt.kind:
      case Assignment(lhs, rhs):
        rhsMeta = inferExprPii(rhs, env)
        lhsMeta = env.get(lhs) or metaFromType(lhs.type)
        if violatesAssignment(lhsMeta, rhsMeta):
          report(PII_ASSIGN_DOWNGRADE, stmt.span, details)
        env[lhs] = merge(lhsMeta, rhsMeta)
      case Call(target, args):
        callMeta = lookupFuncSignature(target)
        foreach (param, arg) in zip(callMeta.params, args):
          argMeta = inferExprPii(arg, env)
          if violatesAssignment(param.meta, argMeta):
            report(PII_ARG_VIOLATION, arg.span, details)
        if callMeta.returnMeta:
          return annotate(callMeta.returnMeta)
      case Sink(kind, value):
        meta = inferExprPii(value, env)
        if not allowed(kind, meta):
          report(PII_SINK_UNSANITIZED, stmt.span, {kind, meta})
      case Block/Nested:
        traverseBlock(stmt.body, env.clone())

function inferExprPii(expr, env):
  match expr.kind:
    case Literal: return Plain
    case Var(name): return env.get(name) or metaFromType(expr.type)
    case Binary(lhs, rhs):
      return merge(inferExprPii(lhs), inferExprPii(rhs))
    case Call(...):
      // handled在 Call 节点
```

算法实现细节：
- TypeScript 与 Java 共用 `pii_meta.ts` / `PiiMeta.java` 工具模块，包含 lattice 判断与 merge 函数，确保性能一致。
- `merge` 产生 `(maxLevel, unionCategories)` 并缓存结果以避免重复计算。
- `inferExprPii` 返回 `Plain`（无 PII）或 `{level, categories, sourceSpan}`。

### 1.6 代码生成与元数据保留
1. **Core IR**  
   - `TypePii` 在 CoreLowering 阶段转换为 `Core.TypeName` + `CoreModel.Annotation{name='pii', params={level, category}}`。  
   - 函数/字段上的注解放入 `Core.Func.annotations`、`Core.Param.annotations`。
2. **TypeScript Emitter**  
   - `core-ir/serialize.ts` 将 `Annotation` 序列化到 JSON，CLI/LSP 共享。  
   - CLI 在生成 DTO/Manifest 时保留 `piiMeta` 供 CLI/LSP 输出。
3. **Java TypeChecker → ASM**  
   - `aster-core` TypeChecker 读取 `Annotation` 并生成 `PiiMeta` 对象，与 TypeScript 共享 lattice。  
   - `aster-asm-emitter` 增加 `addPiiAnnotation(MethodVisitor mv, PiiMeta meta)`，写入 `@AsterPii(level=\"L2\", category={\"email\"})`（`Retention=CLASS`）。  
   - `quarkus-policy-api`、Policy VM 可经由反射/ASM Reads 还原 PII metadata，实现运行时拦截。

### 1.7 参考资料
- Harpocrates 对“把隐私策略绑定到类型”的做法提供了 PII 随数据传播的范式，适用于我们在函数签名层保留 level/category（[Harpocrates, 2024](https://arxiv.org/abs/2411.06317)）。  
- “A Type System for Data Privacy Compliance” 强调静态合规检查需要在语言层提供等级推理，印证我们采用的 lattice 与 sink 校验（[Baramashetru et al., 2025](https://arxiv.org/abs/2508.03831)）。  
- “Practical Type-Based Taint Checking and Inference” 证明类型化污点分析可模块化且高性能，是本方案中 `inferExprPii`/`merge` 设计的依据（[Karimipour et al., 2023](https://drops.dagstuhl.de/storage/00lipics/lipics...))。  
- Cocoon、Duet 等隐私语言实践强调“最敏感者优先”和“不可静默降级”，对应我们在 1.2/1.3 的合并与赋值策略（[Cocoon, 2024](https://arxiv.org/abs/2311.00097)，[Duet, 2019](https://arxiv.org/pdf/1909.02481)）。

---

## 2. Capability 注解语义设计

### 2.1 设计目标与命名空间
- 统一 `@io`, `@async`, `@sql`, `@http`, `@secrets` 等能力到 `CapabilityKind` 列表，由 `Core.Func` 的 `effectCaps`/`effectCapsExplicit` 承载（`src/typecheck.ts:420-455` 已有 TS 端逻辑）。
- 区分“**效果**”与“**能力**”：
  - 效果（effect）描述全局副作用类别（IO、CPU、Async）。
  - 能力（capability）描述访问特定资源的权限（Http、Sql、KV、Secrets）。
  - `@io` 是效果，`@io with Http,Sql` 将能力挂载到 `io` 效果之上。
- 命名空间：`@capability::<Resource>` 约定用于未来扩展（如 `@capability::Storage(S3)`）。

### 2.2 效果多态与签名语法
- **语法**：`It performs {effectRow} [with CapabilityList]`。例如  
  `It performs io, async with Http, Secrets`。
- **效果多态**：支持 `It performs {E}`，其中 `E` 为 effect row 变量，调用点通过 `where` 约束实例化：  
  `fn run<E>(task: () -> E T) performs E`。TypeChecker 需将 `E` 展开为调用实参的 effect 集。
- **组合规则**：  
  - Effect row 使用集合语义，并保持顺序用于诊断输出。  
  - Default：无声明时默认为 `CPU`，禁止隐式 IO。  
  - `async` 效果隐含 `cpu`，与 `io` 并列。

### 2.3 调用检查与上下文要求
1. **调用侧**  
   - 调用者 effect row 必须是被调者 effect row 的**超集**。  
   - 对于 capability，调用者 `CapabilitySet` 必须覆盖被调者 `uses` 集合（含 workflow step 合并）。
2. **闭包/高阶函数**  
   - 捕获效果随闭包类型传播：`() -> @io Http` 等同于函数类型上附带 effect row。
3. **Workflow 特例**  
   - `workflow` block 自动注入 `io` 效果。  
   - Step/compensate 体的 capability 归并到函数头（延续 `checkEffects` 中的 `collectWorkflows` 逻辑）。
4. **诊断**  
   - 缺失 effect：`EFF_MISSING_IO`, `EFF_MISSING_CPU`（已存在）。  
   - 缺失 capability：`EFF_CAP_MISSING`。  
   - 多余 capability：保留 info 级别，指导开发者缩减权限。

### 2.4 Manifest 验证连接
- TypeScript 现有 `typecheckModuleWithCapabilities` 通过 manifest（YAML/JSON）声明可用能力。  
- 新语义要求：
  1. Manifest 声明 -> 模块默认 Capability 集合。  
  2. 函数声明 -> 对 manifest 集合求子集。  
  3. `src/typecheck.ts` 中的 `checkCapabilities` 扩展为：当函数声明了 capability 但 manifest 未授权时，报 `WORKFLOW_UNDECLARED_CAPABILITY`。
- Manifest 解析层向 Java 侧输出同构 JSON，Java TypeChecker 读取后执行同样的集合判断。

### 2.5 Java 端补齐点
- `aster-core/src/main/java/aster/core/typecheck/TypeChecker.java` 需新增 `CapabilityAnalyzer`：  
  - 扫描 `Core.Func.effectCaps/effectCapsExplicit`。  
  - 调用 `CapabilityCollector` 分析 `Core.Expr`，与 TypeScript `collectCapabilities` 行为一致。  
  - 复用 `shared/error_codes.json` 的 `E027/E028/EFF_CAP_*` 以保持诊断一致。  
- `aster-asm-emitter` 将函数效果写入 `@AsterEffects(effects=["io","async"], capabilities=["Http"])`，供运行时审查/策略引擎读取。

### 2.6 类型检查算法（伪代码）

```text
procedure checkModuleCapabilities(module, manifestCaps):
  for func in module.funcs:
    declaredEff = normalizeEffectRow(func.effects)
    declaredCaps = new Set(func.effectCaps)
    usedCaps = collectCapabilities(func.body)
    usedEff = collectEffects(func.body)

    if not declaredEff.superset(usedEff):
      report(EFF_MISSING_xxx)

    missingCaps = usedCaps - declaredCaps
    if missingCaps not empty:
      report(EFF_CAP_MISSING, func.span, {missingCaps})

    superfluousCaps = declaredCaps - usedCaps
    if superfluousCaps not empty:
      report(EFF_CAP_SUPERFLUOUS, func.span, {superfluousCaps})

    if not manifestCaps.containsAll(declaredCaps):
      report(WORKFLOW_UNDECLARED_CAPABILITY, func.span, details)

    if func.workflowSteps:
      checkWorkflowSteps(func, declaredCaps)
```

辅助函数：
- `collectCapabilities`：遍历 AST，识别内建 IO、workflow step、外部调用的 `CapabilityKind`。与 TypeScript 实现共享一套“调用→capability”映射表。
- `normalizeEffectRow`：在 TS/Java 共享的 `EffectRow` 模块中实现排序与去重，并缓存以降低比对成本。
- `checkWorkflowSteps`：针对 step/compensate 体执行二次分析，沿用 TS 中的 `reportWorkflowCapabilityViolation` 语义。

### 2.7 参考资料
- “Effect system” 条目与 Xavier Leroy 的《Type and Effect Systems》章节提供了 effect row/subtyping 的理论基础，我们据此实现 effect 超集判定与 row 多态（[Effect system, 2025](https://en.wikipedia.org/wiki/Effect_system)；[Leroy, 2017](http://xavierleroy.org/control-structures/book/main015.html)）。  
- Austral 语言的 capability 设计展示了“函数签名显式声明所需权限”的工程实践，验证了我们在函数头声明能力的必要性（[Austral Capabilities, 2023](https://borretti.me/article/how-capabilities-work)).  
- CMU 比较研究强调 capability 模块系统在落地时需配套 manifest/最小权限策略，支撑我们在 2.4 的 manifest 对齐要求（[Goyal et al., 2024](https://kilthub.cmu.edu/articles/conference_contribution/)).  
- Capability-based security 总结了“能力是不可伪造的引用”，提醒我们在 IR/字节码阶段必须以注解形式保留证据（[Wikipedia, 2025](https://en.wikipedia.org/wiki/Capability-based_security)）。

---

## 3. 双栈一致性保证
- **统一错误码**：所有新增诊断（`PII_ASSIGN_DOWNGRADE`, `PII_SINK_UNSANITIZED`, `PII_ARG_VIOLATION` 等）必须先写入 `shared/error_codes.json`，并同步生成 TypeScript `ErrorCode` 枚举与 Java `ErrorCode` enum。
- **诊断结构对齐**：延续 `.claude/context-question-1.json` 中对诊断格式的分析，将 PII/Capability 检查产出的 `data` 字段约定为 `{level, categories, sinkKind, capability}`，两侧 JSON 完全一致。
- **Cross-stack 测试**：扩展 `scripts/cross_validate.sh`，在 AST diff 之后追加 `diagnostics/pii/*.json` 对比；当 TypeScript/Java 结果不一致时输出 diff 并阻塞 CI。
- **共享分析库**：在 `packages/shared/` 新增 `pii-meta.ts`（编译出 `.d.ts` + `.js`）与 Java 对应的 `PiiMeta.java`，通过 wasm/JSON 表驱动保持 lattice、sink 配置一致。Capability 侧同理抽离 `EffectRow`、`CapabilitySet`。
- **回归样例**：`test/type-checker/golden/pii_*` `capability_*` 目录下每个案例运行 `tsc typecheck` 与 `java TypeCheckerCli`，产出的诊断 JSON 存储到 `test/type-checker/golden/<name>.ts.json` / `.java.json`，供 cross-validate diff。

---

## 4. 渐进式迁移路径
1. **阶段 0（观察期）**  
   - TypeChecker 默认 **记录但不阻断**：新增 `--enforce-pii` 与 `ENFORCE_CAPABILITIES` 开关默认关闭，仅输出 warning。  
   - CLI 增加 `--pii-report` 生成 `pii-usage.json`，供团队评估已有代码中注解覆盖率。
2. **阶段 1（新代码强制）**  
   - 在 `.shrimp/tasks.json` 中为新模块标记 `requirePiiAnnotations = true`。  
   - TypeChecker 对新模块启用强制模式（按 manifest）。旧模块仍允许 warning。
3. **阶段 2（全局启用）**  
   - 打开 `--enforce-pii` / `ENFORCE_CAPABILITIES`，所有模块必须满足规则。  
   - 结合 cross-stack diff，确保 TS/Java 诊断完全一致后方可发布。
4. **阶段 3（运行时审计）**  
   - ASM emitter 将注解写入字节码后，Policy VM/Quarkus 服务读取并阻断运行时违规调用。  
   - CLI/LSP 提供“跳转到 PII 来源”“列出能力使用树”等调试能力。

**向后兼容性**  
- AST/IR 不做破坏性更改：`TypePii`/`Annotation` 已存在，只是被 TypeChecker 跳过。  
- 通过 feature flag 控制 enforcement，允许逐文件 opt-in。  
- 性能：PII & capability 检查都在单次 AST 遍历中完成（合并 `collectEffects`、`collectCapabilities`），并缓存 lattice 结果，保证编译时间增长 <10%。

**分阶段实施计划（概述）**
1. **W1**：补齐 TypeScript TypeChecker（PII 流、sink 判定、错误码）+ golden case。  
2. **W2**：Java TypeChecker 同步实现 + cross-stack diff pipeline。  
3. **W3**：ASM emitter/DTO 输出 PII/Effect 注解 + runtime 消费示例。  
4. **W4**：启用 feature flag、更新 manifest、在 docs/testing.md 记录新测试命令。  
5. **W5**：将 cross-stack 验证接入 CI，并启动阶段 1 强制。

---
