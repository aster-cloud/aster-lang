# Aster 语言错误码参考

本文档列出了 Aster 语言编译器和类型检查器使用的所有错误码。

**总计**: 60 个错误码

## 按类别分类

### 异步编程 (async)

共 5 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E500** `ASYNC_START_NOT_WAITED` | 🔴 error | Started async task '&#123;task&#125;' not waited | 对启动的异步任务调用 wait，确保执行完毕。 |
| **E501** `ASYNC_WAIT_NOT_STARTED` | 🔴 error | Waiting for async task '&#123;task&#125;' that was never started | 确认 wait 的任务名称在 Start 中正确出现。 |
| **E502** `ASYNC_DUPLICATE_START` | 🔴 error | Async task '&#123;task&#125;' started multiple times (&#123;count&#125; occurrences) | 避免重复启动同名任务，可复用已有任务或改用新名称。 |
| **E503** `ASYNC_DUPLICATE_WAIT` | 🟡 warning | Async task '&#123;task&#125;' waited multiple times (&#123;count&#125; occurrences) | 确保每个任务仅等待一次，或使用单独的同步机制。 |
| **E504** `ASYNC_WAIT_BEFORE_START` | 🔴 error | Wait for async task '&#123;task&#125;' occurs before any matching start | 在 wait for 之前先执行 start，并确保两者位于兼容的控制路径。 |

### 能力系统 (capability)

共 7 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E027** `WORKFLOW_UNDECLARED_CAPABILITY` | 🔴 error | Workflow '&#123;func&#125;' step '&#123;step&#125;' uses capability &#123;capability&#125; that is not declared on the function header. | 在 `It performs io with ...` 中列出 &#123;capability&#125;（例如 Http、Sql、Secrets），或调整 step 代码避免调用未授权能力。 |
| **E028** `COMPENSATE_NEW_CAPABILITY` | 🔴 error | Compensate block for step '&#123;step&#125;' in function '&#123;func&#125;' introduces new capability &#123;capability&#125; that does not appear in the main step body. | Compensate 只能重复主体已使用的能力；如需额外调用，请将相同行为移至主体或在主体中声明该 capability。 |
| **E300** `CAPABILITY_NOT_ALLOWED` | 🔴 error | Function '&#123;func&#125;' requires &#123;cap&#125; capability but manifest for module '&#123;module&#125;' denies it. | 更新能力清单或修改函数实现以符合限制。 |
| **E301** `EFF_CAP_MISSING` | 🔴 error | Function '&#123;func&#125;' uses &#123;cap&#125; capability but header declares [&#123;declared&#125;]. | 在函数头部声明实际使用到的能力。 |
| **E302** `EFF_CAP_SUPERFLUOUS` | 🔵 info | Function '&#123;func&#125;' declares &#123;cap&#125; capability but it is not used. | 移除未使用的能力声明以保持清晰。 |
| **E303** `CAPABILITY_INFER_MISSING_IO` | 🔴 error | Function '&#123;func&#125;' uses IO capabilities [&#123;capabilities&#125;] but is missing @io effect (e.g., &#123;calls&#125;). | 在函数头部声明 `It performs io ...`，或移除相关调用保持纯度。 |
| **E304** `CAPABILITY_INFER_MISSING_CPU` | 🔴 error | Function '&#123;func&#125;' performs CPU capability calls (e.g., &#123;calls&#125;) but declares neither @cpu nor @io effect. | 为函数添加 @cpu 或 @io 效果以覆盖 CPU 能力。 |

### 效果系统 (effect)

共 12 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E023** `WORKFLOW_COMPENSATE_MISSING` | 🟡 warning | Step '&#123;step&#125;' performs side effects but does not define a compensate block. | 为包含 IO 副作用的 step 提供 compensate 块以便回滚。 |
| **E026** `WORKFLOW_MISSING_IO_EFFECT` | 🔴 error | Workflow '&#123;func&#125;' must declare @io effect before using a 'workflow' block. | 在函数 '&#123;func&#125;' 的头部添加 `It performs io ...`（可同时声明 capability），否则编译器拒绝 workflow 语句。 |
| **E200** `EFF_MISSING_IO` | 🔴 error | Function '&#123;func&#125;' may perform I/O but is missing @io effect. | 为具有 IO 行为的函数声明 @io 效果。 |
| **E201** `EFF_MISSING_CPU` | 🔴 error | Function '&#123;func&#125;' may perform CPU-bound work but is missing @cpu (or @io) effect. | 为 CPU 密集型函数声明 @cpu 或 @io 效果。 |
| **E202** `EFF_SUPERFLUOUS_IO_CPU_ONLY` | 🔵 info | Function '&#123;func&#125;' declares @io but only CPU-like work found; @io subsumes @cpu and may be unnecessary. | 若函数仅执行 CPU 工作，可移除多余的 @io 声明。 |
| **E203** `EFF_SUPERFLUOUS_IO` | 🟡 warning | Function '&#123;func&#125;' declares @io but no obvious I/O found. | 确认是否需要 @io；若无 IO 行为可移除。 |
| **E204** `EFF_SUPERFLUOUS_CPU` | 🟡 warning | Function '&#123;func&#125;' declares @cpu but no obvious CPU-bound work found. | 移除多余的 @cpu 声明或增加相应的 CPU 工作。 |
| **E205** `EFF_INFER_MISSING_IO` | 🔴 error | 函数 '&#123;func&#125;' 缺少 @io 效果声明，推断要求 IO。 | 根据推断结果为函数添加 @io 效果。 |
| **E206** `EFF_INFER_MISSING_CPU` | 🔴 error | 函数 '&#123;func&#125;' 缺少 @cpu 效果声明，推断要求 CPU（或 @io）。 | 根据推断结果补齐 @cpu 或 @io 效果。 |
| **E207** `EFF_INFER_REDUNDANT_IO` | 🟡 warning | 函数 '&#123;func&#125;' 声明了 @io，但推断未发现 IO 副作用。 | 确认是否需要保留 @io 声明。 |
| **E208** `EFF_INFER_REDUNDANT_CPU` | 🟡 warning | 函数 '&#123;func&#125;' 声明了 @cpu，但推断未发现 CPU 副作用。 | 若无 CPU 副作用，可删除 @cpu 声明。 |
| **E209** `EFF_INFER_REDUNDANT_CPU_WITH_IO` | 🟡 warning | 函数 '&#123;func&#125;' 同时声明 @cpu 和 @io；由于需要 @io，@cpu 可移除。 | 保留 @io 即可满足需求，移除多余的 @cpu。 |

### PII 隐私保护 (pii)

共 3 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E400** `PII_HTTP_UNENCRYPTED` | 🔴 error | PII data transmitted over HTTP without encryption | 使用加密通道（HTTPS）或脱敏处理后再传输 PII 数据。 |
| **E401** `PII_ANNOTATION_MISSING` | 🔴 error | PII annotation missing for value flowing into '&#123;sink&#125;' | 为敏感数据添加 @pii 标注以便跟踪。 |
| **E402** `PII_SENSITIVITY_MISMATCH` | 🟡 warning | PII sensitivity mismatch: required &#123;required&#125;, got &#123;actual&#125; | 调整数据的敏感级别或更新流程要求。 |

### 作用域与导入 (scope)

共 3 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E029** `WORKFLOW_UNKNOWN_STEP_DEPENDENCY` | 🔴 error | Workflow step '&#123;step&#125;' depends on undefined step '&#123;dependency&#125;'. | 仅引用当前 workflow 中已声明的步骤名称，或修正依赖拼写。 |
| **E100** `DUPLICATE_IMPORT_ALIAS` | 🟡 warning | Duplicate import alias '&#123;alias&#125;'. | 为不同的导入使用唯一别名，避免覆盖。 |
| **E101** `UNDEFINED_VARIABLE` | 🔴 error | Undefined variable: &#123;name&#125; | 在使用变量前先声明并初始化。 |

### 类型系统 (type)

共 30 个错误码

| 错误码 | 严重性 | 消息模板 | 解决方案 |
|--------|--------|----------|----------|
| **E001** `TYPE_MISMATCH` | 🔴 error | Type mismatch: expected &#123;expected&#125;, got &#123;actual&#125; | 检查类型标注与表达式的推断结果是否一致。 |
| **E002** `TYPE_MISMATCH_ASSIGN` | 🔴 error | Type mismatch assigning to '&#123;name&#125;': &#123;expected&#125; vs &#123;actual&#125; | 确认变量先前绑定的类型与当前赋值结果一致。 |
| **E003** `RETURN_TYPE_MISMATCH` | 🔴 error | Return type mismatch: expected &#123;expected&#125;, got &#123;actual&#125; | 检查函数返回语句与声明的返回类型是否一致。 |
| **E004** `TYPE_VAR_UNDECLARED` | 🔴 error | Type variable '&#123;name&#125;' is used in '&#123;func&#125;' but not declared in its type parameters. | 在函数签名的 of 子句中显式声明使用到的类型变量。 |
| **E005** `TYPE_PARAM_UNUSED` | 🟡 warning | Type parameter '&#123;name&#125;' on '&#123;func&#125;' is declared but not used. | 移除未使用的类型参数，避免造成误导。 |
| **E006** `TYPEVAR_LIKE_UNDECLARED` | 🔴 error | Type variable-like '&#123;name&#125;' is used in '&#123;func&#125;' but not declared; declare it with 'of &#123;name&#125;'. | 对于看起来像类型变量的名称，务必在 of 子句中声明。 |
| **E007** `TYPEVAR_INCONSISTENT` | 🔴 error | Type variable '&#123;name&#125;' inferred inconsistently: &#123;previous&#125; vs &#123;actual&#125; | 确认类型推断的多个使用点产出相同的具体类型。 |
| **E008** `IF_BRANCH_MISMATCH` | 🔴 error | If分支返回类型不一致: then分支 &#123;thenType&#125; vs else分支 &#123;elseType&#125; | 确保 if 两个分支返回类型保持一致。 |
| **E009** `MATCH_BRANCH_MISMATCH` | 🔴 error | Match case return types differ: &#123;expected&#125; vs &#123;actual&#125; | 检查 match 每个分支的返回类型是否统一。 |
| **E010** `INTEGER_PATTERN_TYPE` | 🔴 error | Integer pattern used on non-Int scrutinee (&#123;scrutineeType&#125;) | 仅在 Int 类型的匹配表达式中使用整数模式。 |
| **E011** `UNKNOWN_FIELD` | 🔴 error | Unknown field '&#123;field&#125;' for &#123;type&#125; | 检查构造体或数据类型的字段名称是否正确。 |
| **E012** `FIELD_TYPE_MISMATCH` | 🔴 error | Field '&#123;field&#125;' expects &#123;expected&#125;, got &#123;actual&#125; | 校验字段初始化表达式的类型是否匹配声明。 |
| **E013** `MISSING_REQUIRED_FIELD` | 🔴 error | 构造 &#123;type&#125; 缺少必需字段 '&#123;field&#125;' | 为数据构造提供声明中的所有必需字段。 |
| **E014** `NOT_CALL_ARITY` | 🔴 error | not(...) expects 1 argument | 调整 not 调用的参数数量为 1。 |
| **E015** `AWAIT_TYPE` | 🟡 warning | await expects Maybe&lt;T&gt; or Result&lt;T,E&gt;, got &#123;type&#125; | 仅对 Maybe 或 Result 类型调用 await。 |
| **E016** `DUPLICATE_ENUM_CASE` | 🟡 warning | Duplicate enum case '&#123;case&#125;' in match on &#123;type&#125;. | 移除重复的枚举分支，保持匹配语句简洁。 |
| **E017** `NON_EXHAUSTIVE_MAYBE` | 🟡 warning | Non-exhaustive match on Maybe type; missing &#123;missing&#125; case. | 为 Maybe 匹配补齐 null 与非 null 分支。 |
| **E018** `NON_EXHAUSTIVE_ENUM` | 🟡 warning | Non-exhaustive match on &#123;type&#125;; missing: &#123;missing&#125; | 补充所有未覆盖的枚举分支，或添加通配符。 |
| **E019** `AMBIGUOUS_INTEROP_NUMERIC` | 🟡 warning | Ambiguous interop call '&#123;target&#125;': mixing numeric kinds (Int=&#123;hasInt&#125;, Long=&#123;hasLong&#125;, Double=&#123;hasDouble&#125;). Overload resolution may widen/box implicitly. | 统一互操作调用的参数数值类型，避免隐式装箱与拓宽。 |
| **E020** `LIST_ELEMENT_TYPE_MISMATCH` | 🔴 error | List literal element type mismatch: expected &#123;expected&#125;, got &#123;actual&#125; | 确保列表字面量中的所有元素类型一致。 |
| **E021** `OPTIONAL_EXPECTED` | 🔴 error | Optional value required here: expected Maybe or Option, but got &#123;actual&#125; | 传入 Maybe/Option 类型或显式包装值。 |
| **E022** `WORKFLOW_COMPENSATE_TYPE` | 🔴 error | Compensate block for step '&#123;step&#125;' must return Result&lt;Unit, &#123;expectedErr&#125;&gt;, got &#123;actual&#125; | 确保补偿块返回 Result&lt;Unit, E&gt;，其中 E 为 step 错误类型。 |
| **E024** `WORKFLOW_RETRY_INVALID` | 🔴 error | Workflow retry max attempts must be greater than zero (actual: &#123;maxAttempts&#125;). | 设置 retry.maxAttempts 为正整数。 |
| **E025** `WORKFLOW_TIMEOUT_INVALID` | 🔴 error | Workflow timeout must be greater than zero milliseconds (actual: &#123;milliseconds&#125;). | 配置 timeout 秒数为正值，确保补偿逻辑可被触发。 |
| **E030** `WORKFLOW_CIRCULAR_DEPENDENCY` | 🔴 error | Workflow contains circular step dependency: &#123;cycle&#125; | 移除或重构循环依赖，确保步骤可拓扑排序执行。 |
| **E070** `PII_ASSIGN_DOWNGRADE` | 🔴 error | 禁止将 PII 数据赋给较低等级目标: &#123;source&#125; -&gt; &#123;target&#125; | 使用脱敏函数或为目标变量声明匹配的 @pii 等级。 |
| **E072** `PII_SINK_UNSANITIZED` | 🔴 error | PII 等级 &#123;level&#125; 数据未脱敏即输出到 &#123;sinkKind&#125; | 在输出前调用 redact() 或 tokenize() 以降低敏感度。 |
| **E073** `PII_ARG_VIOLATION` | 🔴 error | PII 参数类型不匹配: 期望 &#123;expected&#125;, 实际 &#123;actual&#125; | 检查函数签名，确保 PII 等级与类别一致。 |
| **W071** `PII_IMPLICIT_UPLEVEL` | 🟡 warning | 检测到隐式 PII 等级提升: &#123;source&#125; -&gt; &#123;target&#125; | 为等级变化添加显式类型注解以便审计。 |
| **W074** `PII_SINK_UNKNOWN` | 🟡 warning | 可能有 PII 数据流向 &#123;sinkKind&#125; 但缺少注解 | 为数据增加 @pii 注解以追踪敏感数据流。 |

## 附录

### 严重性级别

- 🔴 **error**: 阻止编译，必须修复
- 🟡 **warning**: 不阻止编译，但建议修复
- 🔵 **info**: 信息提示，可选择性处理

### 占位符说明

错误消息模板中的 `{name}` 形式表示占位符，运行时会被具体值替换。例如：
- `{expected}`、`{actual}`: 期望类型与实际类型
- `{func}`、`{name}`: 函数名或变量名
- `{capability}`: 能力名称（如 Http、Sql）

### 错误码编号规则

- **E001-E099**: 类型系统错误
- **E100-E199**: 作用域与导入错误
- **E200-E299**: 效果系统错误
- **E300-E399**: 能力系统错误
- **E400-E499**: PII 隐私相关错误
- **E500-E599**: 异步编程错误
- **W0xx**: 警告级别错误码（使用 W 前缀）

---

*本文档由 `scripts/generate_error_code_docs.ts` 自动生成*
