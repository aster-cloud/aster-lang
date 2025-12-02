# Aster Core IR 规范

> 最后更新：2025-11-05 20:32（NZST） · 执行：Codex  
> 参考来源：`aster-truffle/src/main/java/aster/truffle/Loader.java`、`aster-truffle/src/main/java/aster/truffle/core/CoreModel.java`、`docs/truffle-architecture.md`、`test/e2e/golden/core/*.json`

## 1. 概述

Aster Core IR（Intermediate Representation）是编译器前端与多种后端之间的通用抽象语法表示层。它将源语言的语义投射为与执行引擎无关的 JSON 结构，使后端（例如 Truffle 解释器、TypeScript 解释器、JVM 代码生成器等）可以共享同一份中间格式。

Core IR 的目标：
- **稳定的交换格式**：所有工具链模块都以 JSON 形式消费与产出 Core IR，保证跨语言、跨进程的兼容性。
- **语义完整**：覆盖函数、控制流、数据结构、模式匹配、异步任务等语言特性。
- **易于扩展**：通过 `kind` 枚举和结构化字段定义新节点，同时保持向后兼容。
- **Truffle 友好**：`Loader.java` 将 Core IR 节点映射为 Graal Truffle AST 节点，直接驱动 GraalVM 执行。

本规范面向 Core IR 的生产者和消费者，描述 JSON 编码规则、32 个节点类型的字段与语义、类型系统、模式匹配规则以及示例程序。

## 2. JSON 格式规范

### 2.1 基本约定
- 所有节点均为带有 `kind` 字段的 JSON 对象，`kind` 的值区分节点类型。
- JSON 使用 UTF-8 编码，字段名采用小驼峰。
- 未知字段必须被忽略：`Loader` 配置了 `FAIL_ON_UNKNOWN_PROPERTIES = false`，因此可以安全地附加调试或元数据字段（如 `loc`、`annotations`）。
- 缺省值：除非另有说明，数组为空数组，引用类型字段可为 `null` 或直接省略。
- 枚举值（如 effect 名称）使用字符串表示，与 Aster 语言规范保持一致。

### 2.2 通用结构

#### Module
```json
{
  "kind": "Module",
  "name": "app",
  "decls": [ <Decl> ... ]
}
```
- `name`：模块名称，可为 `null`。
- `decls`：声明数组，顺序即为定义顺序。

#### Block
```json
{
  "kind": "Block",
  "statements": [ <Stmt> ... ]
}
```
- `statements`：语句列表。`Loader` 会按顺序执行，遇到 `Return` 立即停止。

#### Param / Field / Case
- `Param`：`{ "name": "x", "type": <Type>, "annotations": [] }`
- `Field`：`{ "name": "id", "type": <Type>, "annotations": [] }`
- `Case`：`{ "kind": "Case", "pattern": <Pattern>, "body": <Stmt> }`

以上结构在多个节点中复用，字段含义在对应节点章节补充。

### 2.3 附加元数据
- `annotations`：数组，出现在参数、字段、声明上，供静态分析使用，当前 Truffle 后端忽略。
- `effectCaps` / `effectCapsExplicit`：函数可声明能力上限（参见 `expected_fetch_dashboard_core.json`），`Loader` 暂不消费但会透传。
- 位置字段（`loc` 等）尚未标准化，此处留空。

## 3. 节点类型总览（32 项）

| 类别 | 节点列表 |
|------|----------|
| 表达式（15） | Name · Literal · Lambda · Call · Construct · If · Match · Block · Ok · Err · Some · None · Start · Await · Wait |
| 语句（9） | Let · Set · Return · Exec · If · Match · Block · Start · Wait |
| 模式（4） | PName · PLiteral · PConstruct · PWildcard |
| 声明（4） | Function · Let（顶层）· Data · Import |

> 注：部分 `kind` 名称与 `CoreModel` 类名存在差异（如 `Func` 对应 `Function`），本文在节点章节中给出 JSON 取值与语义映射。

## 4. 表达式节点

以下表达式可出现在所有 `Expr` 位置，以及语句节点的子字段。除非另有说明，表达式求值的结果将作为父节点的返回值。

### 4.1 Name
```json
{ "kind": "Name", "name": "user.name" }
```
- `name`：标识符或限定名。
- 语义：在当前作用域查找变量；若名称匹配枚举变体，则返回 `{ "_enum": <EnumName>, "value": <Variant> }`。
- 示例：`test/e2e/golden/core/expected_fetch_dashboard_core.json` 中引用 `u.id`。

### 4.2 Literal
Core IR 将所有字面量统一视为 `Literal` 类别，具体 JSON 以不同 `kind` 区分：

| 子类 | JSON 示例 | 语义 |
|------|-----------|------|
| `String` | `{"kind":"String","value":"hello"}` | 文本常量 |
| `Bool` | `{"kind":"Bool","value":true}` | 布尔 |
| `Int` | `{"kind":"Int","value":42}` | 32 位整数 |
| `Long` | `{"kind":"Long","value":9007199254740991}` | 64 位整数 |
| `Double` | `{"kind":"Double","value":3.14}` | 双精度浮点 |
| `Null` | `{"kind":"Null"}` | 空值 |

### 4.3 Lambda
```json
{
  "kind": "Lambda",
  "params": [ { "name": "x", "type": { "kind": "TypeName", "name": "Text" } } ],
  "ret": { "kind": "TypeName", "name": "Text" },
  "retType": { "kind": "TypeName", "name": "Text" },  // 兼容旧字段，可选
  "body": { "kind": "Block", "statements": [ ... ] },
  "captures": [ "pfx" ]
}
```
- `ret`：返回类型。`retType` 为旧字段，若同时存在必须保持一致。
- `captures`：捕获的外部变量列表。`Loader` 会为捕获变量创建 Frame 槽位。
- 语义：构建闭包，Truffle 会生成 `LambdaRootNode` 与 `CallTarget`。
- 示例：`expected_lambda_cnl_core.json`。

### 4.4 Call
```json
{
  "kind": "Call",
  "target": { "kind": "Name", "name": "Dash" },
  "args": [ { "kind": "Name", "name": "profile" }, ... ]
}
```
- `target`：被调用表达式，可为 Name 或 Lambda。
- `args`：参数数组，可为空。
- 语义：调用函数、内置函数或闭包，Truffle `CallNode` 会自动处理。

### 4.5 Construct
```json
{
  "kind": "Construct",
  "typeName": "Dash",
  "fields": [
    { "name": "profile", "expr": { "kind": "Name", "name": "profile" } },
    { "name": "timeline", "expr": { "kind": "Name", "name": "timeline" } }
  ]
}
```
- 语义：返回带 `_type` 字段的有序 Map（`ConstructNode`），用于模拟记录或枚举构造器。
- 字段会按声明顺序插入，保证模式匹配可按位对齐。

### 4.6 If（表达式）
```json
{
  "kind": "If",
  "cond": <Expr>,
  "thenBlock": { "kind": "Block", "statements": [ <Stmt> ... ] },
  "elseBlock": { "kind": "Block", "statements": [ ... ] } // 可为 null
}
```
- 语义：条件为真时执行 `thenBlock`，否则执行 `elseBlock`。表达式求值结果为已执行分支的返回值；若分支只包含语句且未显式 `Return`，则结果为 `null`。
- `Loader`：`IfNode` 使用 `Exec.toBool` 进行真值判定。
- 示例：`expected_if_param_core.json`。

### 4.7 Match（表达式）
```json
{
  "kind": "Match",
  "expr": <Expr>,
  "cases": [
    { "kind": "Case", "pattern": <Pattern>, "body": <Stmt 或 Scope> },
    ...
  ]
}
```
- 语义：按顺序尝试模式，首个匹配成功的分支执行并返回结果。若分支体为语句，默认返回 `null`，可通过在分支内使用 `Return` 返回值。
- 参考：`expected_greet_core.json`。

### 4.8 Block（表达式）
- JSON 结构同 §2.2。
- 语义：顺序执行语句数组，遇到 `Return` 时传播返回值，否则结果为 `null`。
- 用于在表达式位置包裹多条语句。

### 4.9 Ok / Err / Some / None
```json
{ "kind": "Ok", "expr": <Expr> }
{ "kind": "Err", "expr": <Expr> }
{ "kind": "Some", "expr": <Expr> }
{ "kind": "None" }
```
- 语义：将值封装为 `Result` 或 `Option` 变体，底层编码为 Map（参见 `ResultNodes`）。
- 典型用法：`expected_fetch_dashboard_core.json` 返回 `Ok`.

### 4.10 Start（表达式）
```json
{
  "kind": "Start",
  "name": "profileTask",
  "expr": <Expr>
}
```
- 语义：启动异步任务，将 `expr` 包装为 `Runnable` 注册到 `AsyncTaskRegistry`，返回 `task_id`（字符串）。
- 执行要求：调用方函数必须声明 `Async` effect；`Loader` 会在 `StartNode` 中校验。
- 若 `name` 非空，任务 ID 会写入当前环境。

### 4.11 Await
```json
{ "kind": "Await", "expr": <Expr> }
```
- 语义：等待 `expr` 产生的 `task_id` 对应任务完成并返回结果，失败时抛出异常。
- `AwaitNode` 会轮询注册表并调度待执行任务。

### 4.12 Wait（表达式）
```json
{
  "kind": "Wait",
  "names": [ "profile", "timeline" ]
}
```
- 语义：等待多个任务完成，不返回结果（恒为 `null`）。通常与 `Start` 配合使用。
- `names`：任务 ID 所在变量名数组，可为空数组（无操作）。

## 5. 语句节点

语句可出现在 `Block.statements`、`Scope.statements`、`Case.body` 等位置。多数语句在 Truffle 中以 `Node` 形式执行，必要时通过 `Exec.exec` 统一调度。

### 5.1 Let
```json
{
  "kind": "Let",
  "name": "profile",
  "expr": <Expr>
}
```
- 语义：求值表达式并绑定到不可变局部变量。`Loader` 会优先使用 Frame 槽位（`LetNodeGen`），否则回退到环境映射（`LetNodeEnv`）。

### 5.2 Set
```json
{ "kind": "Set", "name": "profile", "expr": <Expr> }
```
- 语义：对已存在的变量执行可变赋值。Truffle 优先使用 Frame 槽位更新，否则写入环境。
- 典型场景：可变状态或异步任务结果回写。

### 5.3 Return
```json
{ "kind": "Return", "expr": <Expr> }
```
- 语义：立即结束当前函数或 Lambda，返回表达式结果。实现上抛出 `ReturnNode.ReturnException`。

### 5.4 Exec
```json
{ "kind": "Exec", "expr": <Expr> }
```
- 语义：在语句上下文执行表达式并忽略结果。当前 Truffle Loader 尚未生成 `Exec`，但规范保留此节点以便支持仅需副作用的表达式语句。建议生产者仅在确实需要丢弃返回值时使用。

### 5.5 If（语句）
- JSON 同 4.6。
- 语义：与表达式版一致，只是结果通常被忽略。在语句上下文中使用时，常见模式是在分支内部放置 `Return` 或副作用语句。

### 5.6 Match（语句）
- JSON 同 4.7。
- 语义：与表达式版一致，常用于模式驱动的控制流。若要在语句上下文返回值，可在匹配分支内调用 `Return`。

### 5.7 Block（语句）
- 当 `Block` 直接出现在语句上下文时，可视作复合语句。
- 内部 `statements` 按顺序执行。`Loader` 在 `Scope` 语句中也会构造 `Block`，二者语义相同。
- `Scope`：`CoreModel` 额外提供 `{"kind":"Scope","statements":[...]}` 以表示嵌套作用域，Truffle Loader 会将其转换成 `Block`，因此规范中统一视为 `Block` 语句的编码形式。

### 5.8 Start（语句）
- JSON 同 4.10。
- 语义：与表达式版一致，常见写法是在语句块中启动任务并依赖变量名保存任务 ID。

### 5.9 Wait（语句）
- JSON 同 4.12。
- 语义：与表达式版一致，可独立作为语句使用。

## 6. 模式节点

Core IR 模式节点用于 `Match` 的 `pattern` 字段。`Loader` 将其转换为 `MatchNode.PatternNode`。以下命名使用规范名称与实际 JSON 映射：

### 6.1 PName（`PatName`）
```json
{ "kind": "PatName", "name": "User" }
```
- `name`：匹配的标识符。
- 语义：若 `name` 为 `_`，视为通配符；否则匹配枚举变体名、构造器 `_type` 或任意非空值（回退行为见 `MatchNode.PatNameNode`）。

### 6.2 PLiteral（`PatInt` 等）
```json
{ "kind": "PatInt", "value": 0 }
```
- 语义：按字面量比较，目前 Truffle 实现支持整型匹配（`PatIntNode`），可扩展到更多字面量种类。生产者应按目标类型选择对应 `PatXxx` `kind`。

### 6.3 PConstruct（`PatCtor`）
```json
{
  "kind": "PatCtor",
  "typeName": "User",
  "names": [ "id", "name" ],
  "args": [ <Pattern> ... ]  // 可选
}
```
- `typeName`：目标构造器名称，对应值对象的 `_type` 字段。
- `names`：按声明顺序的绑定变量数组，可为空；若值为 `_` 表示忽略。
- `args`：嵌套模式列表，与 `fields` 顺序一致。
- 语义：匹配记录或构造器；匹配成功时按顺序绑定。

### 6.4 PWildcard（`PatName` with `_` 或默认分支）
- 推荐编码：`{ "kind": "PatName", "name": "_" }`。
- `Loader` 在解析未知模式时会退回 `PatName("_")`，因此 `_` 形成通配符语义。

## 7. 声明节点

声明列于 `Module.decls`。JSON `kind` 取值与 `CoreModel` 对应关系如下：

| 规范名称 | JSON kind | 说明 |
|----------|-----------|------|
| Function | `Func` | 函数声明 |
| Let（顶层） | `Let`（保留） | 预留的模块级常量声明，目前 CoreModel 尚未实现 |
| Data | `Data` / `Enum` | 自定义数据类型、枚举 |
| Import | `Import` | 模块导入 |

### 7.1 Function（`Func`）
```json
{
  "kind": "Func",
  "name": "fetchDashboard",
  "typeParams": [ "T" ],
  "params": [ <Param> ... ],
  "ret": <Type>,
  "effects": [ "IO", "Async" ],
  "effectCaps": [ "Http", "Sql" ],
  "effectCapsExplicit": false,
  "body": <Block>
}
```
- `typeParams`：类型参数名称数组。
- `params`：函数形参，支持注解。
- `effects`：函数声明的所需 effect。
- `effectCaps`：运行时允许的 effect 能力上限，缺省为平台默认集合。
- `body`：语句块。

### 7.2 顶层 Let（保留）
```json
{
  "kind": "Let",
  "name": "pi",
  "expr": { "kind": "Double", "value": 3.1415 }
}
```
- 语义：在模块级别绑定常量。由于 `CoreModel` 尚未建模顶层 Let，该节点目前不会被 Truffle Loader 解析；若后续实现，需要在 `Loader.buildProgramInternal` 中提前注册符号。
- 规范建议在实现前将此节点限制在编译期扩展中使用。

### 7.3 Data（记录）与 Enum

#### Data
```json
{
  "kind": "Data",
  "name": "User",
  "fields": [ <Field> ... ]
}
```
- `fields`：字段定义，包含 `type` 与可选 `annotations`。
- 语义：声明结构体或记录类型。当前 Truffle 后端尚未直接消费，但其他后端可据此生成构造器。

#### Enum
```json
{
  "kind": "Enum",
  "name": "AuthErr",
  "variants": [ "InvalidCreds", "Locked" ]
}
```
- 语义：声明枚举。虽然规范列表未单列 `Enum`，但 JSON 实际存在，Truffle Loader 会为每个变体登记 `_enum` 映射，便于 `Name` 解析。

### 7.4 Import
```json
{
  "kind": "Import",
  "path": "demo.utils",
  "alias": "Utils"
}
```
- 语义：导入其他模块。当前 Truffle Loader 忽略导入（需由前端或运行时处理）。

## 8. 类型系统

Core IR 类型描述位于 `CoreModel.Type` 层次。所有类型对象都有 `kind` 字段。

| kind | 字段 | 说明 |
|------|------|------|
| `TypeName` | `name` | 命名类型，例如 `Text`、`Int`、自定义数据类型。 |
| `TypeVar` | `name` | 类型变量，用于泛型。 |
| `TypeApp` | `base`、`args[]` | 类型应用，例如 `Result<T, E>`。当前 JSON 尚未使用，但规范保留。 |
| `Maybe` | `type` | 可空类型包装，语义上与 Option 相似，常用于自动可空。 |
| `Option` | `type` | 显式 Option。 |
| `Result` | `ok`、`err` | 二元结果类型。 |
| `List` | `type` | 列表。 |
| `Map` | `key`、`val` | 键值映射。 |
| `FuncType` | `params[]`、`ret` | 函数类型签名，主要用于高阶函数或注解。 |

### 8.1 类型编码注意事项
- 类型对象可递归嵌套，例如 `Result` 的 `ok`、`err` 字段。
- `annotations` 字段可附加在类型节点上（生成端可选）。
- `Maybe`/`Option` 的运行时表示均使用 `Ok`/`Err`/`Some`/`None` Map，语义不同在于类型层面（`Maybe` 通常表示默认可空，`Option` 表示显式）。

## 9. 模式匹配规范

结合 `MatchNode` 实现，模式匹配遵循以下规则：

1. **顺序匹配**：`cases` 按声明顺序尝试，首个成功分支立即执行。
2. **绑定策略**：
   - `PatName`：若名称非 `_`，在匹配成功时将值写入环境。
   - `PatCtor`：先检查 `_type` 是否匹配 `typeName`，然后按顺序绑定 `names` 或递归匹配 `args`。`names` 与 `args` 可混合使用。
3. **字面量比较**：`PatInt` 对整数、长整型、浮点型做值比较；未来若扩展字符串等，遵循同样规则。
4. **Wildcard**：使用 `PatName` + `_`；规范亦允许完全省略分支，在 `Loader` 中将退化为 `_`。
5. **失败行为**：若所有分支均未匹配，`MatchNode` 返回 `null`。使用者应通过穷举或添加 `_` 分支避免空匹配。
6. **副作用与返回**：分支体为语句时，副作用会被保留。如需返回值请使用 `Return` 或者将分支体改写为 `Block` + `Return`。

## 10. 示例程序

以下摘自 `test/e2e/golden/core/expected_fetch_dashboard_core.json`，展示异步与模式匹配的组合：

```json
{
  "kind": "Module",
  "decls": [
    { "kind": "Data", "name": "Dash", "fields": [ ... ] },
    { "kind": "Enum", "name": "AuthErr", "variants": [ "InvalidCreds", "Locked" ] },
    {
      "kind": "Func",
      "name": "fetchDashboard",
      "effects": [ "IO", "Async" ],
      "body": {
        "kind": "Block",
        "statements": [
          { "kind": "Start", "name": "profile", "expr": { "kind": "Call", "target": { "kind": "Name", "name": "ProfileSvc.load" }, "args": [ { "kind": "Name", "name": "u.id" } ] } },
          { "kind": "Start", "name": "timeline", "expr": { "kind": "Call", "target": { "kind": "Name", "name": "FeedSvc.timeline" }, "args": [ { "kind": "Name", "name": "u.id" } ] } },
          { "kind": "Wait", "names": [ "profile", "timeline" ] },
          {
            "kind": "Return",
            "expr": {
              "kind": "Ok",
              "expr": {
                "kind": "Construct",
                "typeName": "Dash",
                "fields": [
                  { "name": "profile", "expr": { "kind": "Name", "name": "profile" } },
                  { "name": "timeline", "expr": { "kind": "Name", "name": "timeline" } }
                ]
              }
            }
          }
        ]
      }
    }
  ]
}
```

该示例体现了以下规范要点：
- 数据声明与枚举声明同时存在。
- 函数声明记录所需 effect。
- 异步任务通过 `Start`/`Wait` 配合，结果由 `Construct` 与 `Ok` 包装。
- 函数以 `Return` 明确返回值。

## 11. 实践建议

- **生产者**（编译器、转换器）应严格按照 `kind` 与字段命名输出 JSON；新增节点前先扩展 `CoreModel` 并更新本文档。
- **消费者**（解释器、工具链）解析时应忽略未知字段，以保持前向兼容。
- **异步语义**：确保声明 `Async` effect 后再使用 `Start`/`Await`/`Wait`，否则 `StartNode` 会抛出运行时错误。
- **模式匹配安全性**：推荐在枚举匹配中显式提供 `_` 分支或者穷举所有 `variants`。
- **调试**：可在 JSON 中附加 `loc`、`debug` 字段，不影响 Truffle 解析。

---

本规范将随着 Core IR 的演进同步更新。若实现中发现与规范不一致的行为，请参阅源码（尤其是 `Loader` 与 `CoreModel`）并反馈以便修订。***
