# Aster 语言规范

**版本**: 0.2.0
**状态**: 草案 (Draft) | Last updated: 2025-10-07 12:06 NZST | Maintainer: Codex
**最后更新**: 2025-10-06

---

## 1. 词法结构

### 1.1 字符集

Aster 源代码使用 UTF-8 编码。词法分析器自动跳过 UTF-8 BOM（字节序标记 `0xFEFF`）如果存在。

### 1.2 空白与缩进

**空白字符**：空格 (` `) 和制表符 (`\t`) 用于分隔 token，但在语法上不显著（除缩进外）。

**缩进规则**：
- Aster 使用**基于缩进的块结构**（类似 Python）
- 每个缩进级别必须是**2 个空格**的倍数
- 缩进增加生成 `INDENT` token
- 缩进减少生成 `DEDENT` token
- 不一致的缩进导致编译错误

**换行符**：
- 支持 `\n` (LF)、`\r` (CR)、`\r\n` (CRLF)
- 换行符生成 `NEWLINE` token

### 1.3 注释

**单行注释**：
```
# 这是注释（井号风格）
// 这也是注释（双斜线风格）
```

注释从 `#` 或 `//` 开始，延续到行尾。

### 1.4 标识符

**标识符语法**（EBNF）：
```text
Identifier     ::= IdentStart IdentContinue*
IdentStart     ::= [A-Za-z]
IdentContinue  ::= [A-Za-z0-9]
```

**类型标识符**（首字母大写）：
```text
TypeIdentifier ::= [A-Z] IdentContinue*
```

**点分标识符**（用于模块名和完全限定名）：
```text
DottedIdent    ::= Identifier ('.' Identifier)*
```

**示例**：
- 普通标识符：`user`, `fetchDashboard`, `id`
- 类型标识符：`User`, `Result`, `AuthErr`
- 点分标识符：`demo.policy_demo`, `Http.get`

### 1.5 关键字

保留关键字（不能用作标识符）：
```
To          Define      This        Module      Is          Use
As          With        Of          And         Or          Produce
Return      If          Then        Else        Match       When
Within      Scope       Start       Async       Wait        For
One         Null        True        False       Ok          Err
Performs    Io          Cpu         It
```

**多词关键字**：
- `This module is`
- `It performs`
- `One of`
- `Wait for`
- `Within a scope`
- `Start as async`

### 1.6 字面量

**整数字面量**：
```text
IntLiteral ::= [0-9]+
```
示例：`0`, `42`, `1000`

**浮点字面量**：
```text
DoubleLiteral ::= [0-9]+ '.' [0-9]+
```
示例：`3.14`, `0.5`, `2.718`

**布尔字面量**：
```text
BoolLiteral ::= 'true' | 'false'
```

**空字面量**：
```text
NullLiteral ::= 'null'
```

**文本字面量（字符串）**：
```text
TextLiteral ::= '"' StringChar* '"'
StringChar  ::= [^"\n] | '\"' | '\n' | '\t' | '\\'
```

**字符串插值**（在文本字面量中）：
```
"Welcome, {name}"
"User ID: {u.id}"
```

### 1.7 标点符号

```
.    点号（语句结束、成员访问）
:    冒号（类型注解、块开始）
,    逗号（参数分隔、列表元素分隔）
(    左圆括号
)    右圆括号
[    左方括号
]    右方括号
```

---

## 2. 语法规则（EBNF）

### 2.1 模块结构

```text
Module       ::= ModuleDecl? Declaration*

ModuleDecl   ::= 'This module is' DottedIdent '.'

Declaration  ::= Import
               | DataDecl
               | EnumDecl
               | FuncDecl
```

**示例**：
```
This module is app.

Use demo.utils.
Define User with id: Text, name: Text.
To greet with user: User?, produce Text: ...
```

### 2.2 导入声明

```text
Import       ::= 'Use' DottedIdent ('as' Identifier)? '.'
```

**示例**：
```
Use std.io.
Use demo.auth as Auth.
```

### 2.3 数据类型声明

**记录类型**（Product Type）：
```text
DataDecl     ::= 'Define' TypeIdent 'with' FieldList '.'
FieldList    ::= Field (',' Field)*
Field        ::= Identifier ':' Type
```

**示例**：
```
Define User with id: Text, name: Text.
Define Point with x: Int, y: Int.
```

**枚举类型**（Sum Type）：
```text
EnumDecl     ::= 'Define' TypeIdent 'as one of' VariantList '.'
VariantList  ::= TypeIdent (',' TypeIdent)*
```

**示例**：
```
Define AuthErr as one of InvalidCreds, Locked.
Define Color as one of Red, Green, Blue.
```

### 2.4 函数声明

```text
FuncDecl     ::= 'To' Identifier TypeParams? ParamList? ',' 'produce' Type Effect? Body

TypeParams   ::= 'of' TypeIdent ('and' TypeIdent)*

ParamList    ::= 'with' Parameter (',' Parameter)*
Parameter    ::= Identifier ':' Type

Effect       ::= '.' 'It performs' EffectList
               | 'It performs' EffectList ':'
EffectList   ::= EffectKind ('with' CapabilityList)?
EffectKind   ::= 'io' | 'cpu' | ('io' 'with' CapabilityList)
CapabilityList ::= Capability ('and' Capability)*
Capability   ::= 'Http' | 'Sql' | 'Time' | 'Files' | 'Secrets' | 'AiModel'

Body         ::= ':' NEWLINE INDENT Statement+ DEDENT
               | '.'
```

**示例**：
```
To greet with user: User?, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".

To fetchDashboard with u: User, produce Result of Dash and AuthErr. It performs io:
  Start profile as async ProfileSvc.load(u.id).
  Wait for profile.
  Return ok of Dash(profile, timeline).
```

### 2.5 类型表达式

```text
Type         ::= TypeName
               | TypeVar
               | TypeApp
               | NullableType
               | OptionType
               | ResultType
               | ListType
               | MapType
               | FuncType

TypeName     ::= TypeIdent
TypeVar      ::= TypeIdent
TypeApp      ::= TypeIdent '<' Type (',' Type)* '>'
NullableType ::= Type '?'
OptionType   ::= 'Option' 'of' Type
ResultType   ::= 'Result' 'of' Type 'and' Type
ListType     ::= 'List' 'of' Type
MapType      ::= 'Map' 'of' Type 'and' Type
FuncType     ::= '(' Type (',' Type)* ')' '->' Type
```

**示例**：
```
Text                          # 基础类型
User                          # 用户定义类型
T                             # 类型变量
List<Int>                     # 泛型类型应用
User?                         # 可空类型
Option of Text                # Option 类型
Result of User and AuthErr    # Result 类型
Map of Text and Int           # Map 类型
```

### 2.6 语句

```text
Statement    ::= LetStmt
               | ReturnStmt
               | IfStmt
               | MatchStmt
               | StartStmt
               | WaitStmt
               | WithinStmt
               | ExprStmt

LetStmt      ::= 'Let' Pattern '=' Expression '.'
ReturnStmt   ::= 'Return' Expression '.'
IfStmt       ::= 'If' Expression ',' 'then' Block ('else' Block)?
MatchStmt    ::= 'Match' Expression ':' NEWLINE INDENT Case+ DEDENT
Case         ::= 'When' Pattern ',' Statement
StartStmt    ::= 'Start' Identifier 'as async' Expression '.'
WaitStmt     ::= 'Wait for' Identifier ('and' Identifier)* '.'
WithinStmt   ::= 'Within a scope:' NEWLINE INDENT Statement+ DEDENT
ExprStmt     ::= Expression '.'
```

**示例**：
```
Let x = 42.
Return "Hello".
If x > 0, then Return "positive".
Match user:
  When null, Return "guest".
  When User(id, name), Return name.
Start profile as async ProfileSvc.load(id).
Wait for profile and timeline.
```

### 2.7 表达式

```text
Expression   ::= PrimaryExpr
               | CallExpr
               | FieldAccessExpr
               | ConstructExpr
               | LambdaExpr
               | BinaryExpr
               | AwaitExpr

PrimaryExpr  ::= Identifier
               | IntLiteral
               | DoubleLiteral
               | BoolLiteral
               | NullLiteral
               | TextLiteral
               | ListLiteral
               | MapLiteral
               | '(' Expression ')'

CallExpr     ::= Expression '(' ArgList? ')'
ArgList      ::= Expression (',' Expression)*

FieldAccessExpr ::= Expression '.' Identifier

ConstructExpr ::= 'ok' 'of' Expression
                | 'err' 'of' Expression
                | TypeIdent '(' ArgList? ')'

LambdaExpr   ::= '{' ParamList? ',' Expression '}'

BinaryExpr   ::= Expression BinOp Expression
BinOp        ::= '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='
               | 'and' | 'or'

AwaitExpr    ::= 'await' '(' Expression ')'

ListLiteral  ::= '[' (Expression (',' Expression)*)? ']'
MapLiteral   ::= 'Map' '(' (MapEntry (',' MapEntry)*)? ')'
MapEntry     ::= Expression ':' Expression
```

### 2.8 模式匹配

```text
Pattern      ::= NullPattern
               | IntPattern
               | NamePattern
               | CtorPattern

NullPattern  ::= 'null'
IntPattern   ::= IntLiteral
NamePattern  ::= Identifier
CtorPattern  ::= TypeIdent '(' PatternList? ')'
PatternList  ::= Pattern (',' Pattern)*
```

**示例**：
```
When null, ...               # 空模式
When 0, ...                  # 整数模式
When x, ...                  # 名称模式（绑定变量）
When User(id, name), ...     # 构造器模式
```

---

## 3. 类型系统

### 3.1 基础类型

| 类型     | 描述           | 示例值         |
|----------|----------------|----------------|
| `Int`    | 32位整数       | `0`, `42`, `-10` |
| `Long`   | 64位整数       | `1000000L`     |
| `Double` | 64位浮点数     | `3.14`, `0.5`  |
| `Float`  | 32位浮点数     | `2.718f`       |
| `Bool`   | 布尔值         | `true`, `false` |
| `Text`   | UTF-8 文本     | `"Hello"`      |

### 3.2 复合类型

**可空类型**（Nullable Types）：
```
T?   ≡   T | null
```
示例：`User?`, `Text?`

**Option 类型**：
```
Option<T> = Some(T) | None
```
示例：`Option of Text`

**Result 类型**：
```
Result<T, E> = Ok(T) | Err(E)
```
示例：`Result of User and AuthErr`

**列表类型**：
```
List<T>
```
示例：`List of Int`, `List of Text`

**映射类型**：
```
Map<K, V>
```
示例：`Map of Text and Int`

### 3.3 泛型（类型参数化）

**函数泛型**：
```
To identity of T with value: T, produce T:
  Return value.

To map of T and U with f: {T, U}, list: List of T, produce List of U:
  ...
```

**约束**：类型变量在函数签名中首次出现时自动推断。

### 3.4 类型推导规则

Aster 使用**局部类型推断**：
- 函数签名必须显式标注类型
- 局部变量类型可推导（从初始化表达式）
- 函数调用的类型参数可推导

**示例**：
```
# 显式类型
To greet with user: User, produce Text: ...

# 推导局部变量类型
Let name = user.name.  # name: Text（从 User.name 推导）
```

### 3.5 子类型关系

**可空类型子类型**：
```
T  <:  T?
```

**Option 类型无隐式转换**：
```
T  ≮:  Option<T>  （必须显式构造 Some(x)）
```

**Result 类型无隐式转换**：
```
T  ≮:  Result<T, E>  （必须显式构造 Ok(x)）
```

---

## 4. 效果系统

### 4.1 效果种类

Aster 区分两种基本效果：
- **`cpu`**：纯计算（可能消耗 CPU 但无 I/O）
- **`io`**：I/O 操作（网络、文件、数据库等）

**效果格**（Effect Lattice）：
```
∅  ⊑  cpu  ⊑  io[*]
```

- 无效果函数 (`∅`) 可被 `cpu` 函数调用
- `cpu` 函数可被 `io` 函数调用
- `io` 函数**不能**被 `cpu` 函数调用（编译错误）

### 4.2 效果标注语法

**基本效果**：
```
To compute, produce Int. It performs cpu:
  Return 42.

To fetch, produce Text. It performs io:
  Return Http.get("https://example.com").
```

**能力细化**（Capability-based I/O）：
```
To fetchUser, produce User. It performs io with Http and Sql:
  Let data = Http.get("/user").
  Let user = Db.save(data).
  Return user.
```

**支持的能力**：
- `Http`：HTTP 网络请求
- `Sql`：SQL 数据库操作
- `Time`：时间/时钟访问
- `Files`：文件系统访问
- `Secrets`：密钥/凭证访问
- `AiModel`：AI 模型调用

### 4.3 效果检查规则

1. **调用约束**：
   - `cpu` 函数只能调用 `cpu` 或无效果函数
   - `io` 函数可调用任何函数
   - `io[C]` 函数只能调用 `io[C']` 函数，其中 `C' ⊆ C`

2. **能力子集检查**：
   ```
   io[Http]        ⊑  io[Http, Sql]
   io[Http, Sql]   ⊑  io[*]
   ```

3. **编译时强制**：
   效果违规导致**编译错误**（非警告）。

**示例**：
```
# ❌ 错误：cpu 函数调用 io 函数
To badCompute, produce Int. It performs cpu:
  Return Http.get("/data").  # ERROR: Effect violation

# ✅ 正确：io 函数调用 cpu 函数
To goodFetch, produce Int. It performs io:
  Let x = compute().  # OK（假设 compute 是 cpu）
  Return x.
```

### 4.4 异步与并发

**结构化并发**（Structured Concurrency）：
```
Within a scope:
  Start profile as async ProfileSvc.load(u.id).
  Start timeline as async FeedSvc.timeline(u.id).
  Wait for profile and timeline.
  Return Dash(profile, timeline).
```

**规则**：
- `Start` 创建异步任务，返回句柄
- `Wait` 等待所有指定任务完成
- 所有 `Start` 的任务必须在作用域结束前 `Wait`（编译器警告）

---

## 5. 语义规则

### 5.1 求值顺序

**语句求值**：
- 按顺序（从上到下）求值
- 每个语句执行后可能改变作用域状态

**表达式求值**：
- **严格求值**（Strict Evaluation）：参数在函数调用前求值
- **从左到右**：多个参数按声明顺序求值

**短路求值**：
- 逻辑运算符 `and`、`or` 短路求值
  ```
  If (x != null) and (x.name == "admin"), ...
  ```

### 5.2 作用域规则

**词法作用域**（Lexical Scoping）：
- 变量绑定在声明处引入
- 内层作用域可访问外层变量
- 同名变量遮蔽（Shadowing）

**示例**：
```
Let x = 10.
Within a scope:
  Let x = 20.  # 遮蔽外层 x
  Return x.    # 返回 20
```

### 5.3 模式匹配语义

**穷尽性检查**（Exhaustiveness Checking）：
- `Match` 表达式必须覆盖所有可能的模式
- 枚举类型未覆盖所有变体 → 编译警告

**绑定语义**：
```
Match user:
  When User(id, name), Return name.  # id, name 绑定到字段值
```

**匹配顺序**：
- 按 `When` 子句顺序匹配
- 第一个匹配成功的分支执行

### 5.4 空值处理

**显式空检查**：
```
Match user:
  When null, Return "guest".
  When User(id, name), Return name.
```

**`?` 操作符**（计划中）：
```
user?.name  # 如果 user 为 null，返回 null；否则返回 user.name
```

### 5.5 错误处理

**Result 类型强制检查**：
```
Let result = fetchUser(id).
Match result:
  When Ok(user), Return user.
  When Err(e), Return handleError(e).
```

**`await` 表达式**（自动解包）：
```
Let user = await(fetchUser(id)).  # 如果 Err，抛出异常
```

---

## 6. 代码示例

### 示例 1：基础问候函数
```
This module is app.

Define User with id: Text, name: Text.

To greet with user: User?, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```
**文件**: `test/cnl/examples/greet.aster`

### 示例 2：异步仪表板获取
```
Define User with id: Text, name: Text.
Define Dash with profile: Text, timeline: Text.
Define AuthErr as one of InvalidCreds, Locked.

To fetchDashboard with u: User, produce Result of Dash and AuthErr. It performs io:
  Start profile as async ProfileSvc.load(u.id).
  Start timeline as async FeedSvc.timeline(u.id).
  Wait for profile and timeline.
  Return ok of Dash(profile, timeline).
```
**文件**: `test/cnl/examples/fetch_dashboard.aster`

### 示例 3：策略引擎示例
```
This module is demo.policy_demo.

To demonstratePolicyEngine, produce Text:
  Return "Policy engine demonstration: Access granted".

To runPolicyTest1, produce Text:
  Return "Test 1: Admin access granted".
```
**文件**: `test/cnl/examples/policy_demo.aster`

### 示例 4：列表操作
```
This module is demo.lists.

To sum with numbers: List of Int, produce Int:
  Let total = 0.
  # 迭代列表（语法糖计划中）
  Return total.

To map of T and U with f: {T, U}, list: List of T, produce List of U:
  # 映射列表元素
  Return [].
```
**文件**: `test/cnl/examples/list_ops.aster`

### 示例 5：枚举穷尽性检查
```
Define Color as one of Red, Green, Blue.

To colorName with c: Color, produce Text:
  Match c:
    When Red, Return "红色".
    When Green, Return "绿色".
    When Blue, Return "蓝色".
```
**文件**: `test/cnl/examples/enum_exhaustiveness.aster`

### 示例 6：效果能力细化
```
To fetchAndSave, produce User. It performs io with Http and Sql:
  Let data = Http.get("/user").
  Let user = Db.save(data).
  Return user.
```
**文件**: `test/cnl/examples/eff_caps_enforce.aster`

### 示例 7：Maybe 和 Result 类型
```
To safeDivide with a: Int, b: Int, produce Option of Int:
  If b == 0, then Return None.
  Return Some(a / b).

To divide with a: Int, b: Int, produce Result of Int and Text:
  If b == 0, then Return err of "Division by zero".
  Return ok of (a / b).
```
**文件**: `test/cnl/examples/stdlib_maybe_result.aster`

### 示例 8：文本操作
```
To concat with a: Text, b: Text, produce Text:
  Return "{a}{b}".

To length with s: Text, produce Int:
  Return Text.length(s).
```
**文件**: `test/cnl/examples/text_ops.aster`

### 示例 9：映射操作
```
To createUser with id: Text, name: Text, produce Map of Text and Text:
  Return Map(
    "id": id,
    "name": name
  ).
```
**文件**: `test/cnl/examples/map_ops.aster`

### 示例 10：Lambda 表达式
```
To filterPositive with numbers: List of Int, produce List of Int:
  Return List.filter(numbers, {x, x > 0}).
```
**文件**: `test/cnl/examples/lambda_cnl.aster`

---

## 7. 扩展特性（计划中）

### 7.1 效果推断
自动推断函数效果而无需显式标注：
```
To compute:  # 自动推断为 cpu
  Return 42.
```

### 7.2 效果多态
泛型函数继承效果参数：
```
To map of E and T and U with f: {T, U with E}, list: List of T, produce List of U with E:
  ...
```

### 7.3 PII 类型标注
敏感数据类型标注：
```
Define User with id: @pii(L2, email) Text, name: Text.
```

### 7.4 高级模式匹配
- OR 模式：`When Red or Green, ...`
- AS 模式：`When User(id, name) as u, ...`
- 守卫：`When x if x > 0, ...`

---

## 8. 语言工具链

### 8.1 编译器管道
```
CNL 源码
  ↓ canonicalize（规范化）
Canonical CNL
  ↓ lex（词法分析）
Tokens
  ↓ parse（语法分析）
AST（抽象语法树）
  ↓ lowerModule（降级）
Core IR（核心中间表示）
  ↓ typecheckModule（类型检查）
类型诊断
  ↓ emitJVM（代码生成）
JVM 字节码
```

### 8.2 命令行工具
```bash
# 编译 CNL 文件
aster compile example.aster

# 类型检查
aster typecheck example.aster

# 运行 REPL
aster repl

# 生成 Core IR
aster emit-core example.aster

# 生成 JVM 字节码
aster emit-jvm example.aster
```

### 8.3 LSP 支持
Aster 提供 Language Server Protocol 支持：
- 语法高亮
- 代码补全
- 类型提示
- 效果违规诊断
- PII 数据流警告（计划中）

---

## 9. 参考文献

- **效果系统**: `docs/reference/effects.md`
- **能力配置**: `src/config/effects.ts`
- **词法分析器**: `src/lexer.ts`
- **语法分析器**: `src/parser.ts`
- **类型检查器**: `src/typecheck.ts`
- **示例代码**: `test/cnl/examples/`

---

## 10. 变更日志

### v0.2.0 (2025-10-06)
- 添加完整的 EBNF 语法定义
- 扩展效果系统文档
- 添加 10+ 代码示例
- 规范化类型系统描述

### v0.1.0 (2024-01-15)
- 初始草案

---

**许可证**: MIT
**维护者**: Aster Language Team
**贡献指南**: 参见 `CLAUDE.md`
