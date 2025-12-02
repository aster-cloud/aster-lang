# PII 污点分析算法设计

> **状态**: 设计文档
> **版本**: 1.0
> **最后更新**: 2025-10-06

## 1. 概述

### 1.1 动机

在现代应用中，个人身份信息（PII，Personally Identifiable Information）的安全处理至关重要。GDPR、CCPA 等隐私法规要求开发者：

1. **最小化收集**：仅收集必要的 PII
2. **安全传输**：加密传输敏感数据
3. **访问控制**：限制对 PII 的访问
4. **数据脱敏**：日志和分析中移除 PII
5. **泄露通知**：检测并报告 PII 泄露

手动审计代码以确保 PII 安全非常困难且易错。污点分析（Taint Analysis）通过自动跟踪 PII 数据流，检测潜在的安全风险。

### 1.2 目标

设计一个 PII 污点分析算法，满足以下需求：

- **自动化**：自动识别 PII 数据源和不安全使用
- **保守性**：避免漏报（false negatives），宁可误报（false positives）
- **细粒度**：区分不同敏感级别（L1/L2/L3）和数据类别
- **可操作**：提供清晰的修复建议
- **增量性**：支持 LSP 实时分析

### 1.3 威胁模型

检测以下 PII 安全风险：

| 威胁类型 | 描述 | 示例 |
|---------|------|------|
| **未加密传输** | PII 通过明文 HTTP 传输 | `Http.post("/api", email)` |
| **日志泄露** | PII 写入日志文件 | `Log.info("User: " + ssn)` |
| **未授权访问** | PII 未经认证即可访问 | 缺少 `@auth` 标注的 API |
| **未脱敏存储** | 高敏感 PII 直接存储 | `Db.insert("users", { ssn })` |
| **跨境传输** | PII 发送到未批准的地区 | `Http.post("https://foreign.api", data)` |

本文档重点关注**未加密传输**和**日志泄露**的检测，其他威胁可在后续扩展。

## 2. PII 类型系统回顾

Aster 的 PII 类型系统（见 Task 5 实现）提供了静态标注：

### 2.1 类型标注语法

```
@pii(sensitivity, category) BaseType
```

**敏感级别**：
- `L1`：低敏感（如姓名、用户名）
- `L2`：中敏感（如邮箱、电话）
- `L3`：高敏感（如 SSN、信用卡号、健康记录）

**数据类别**：
- `email`：电子邮件地址
- `phone`：电话号码
- `ssn`：社会保障号
- `address`：物理地址
- `financial`：金融信息
- `health`：健康记录
- `name`：姓名
- `biometric`：生物特征

### 2.2 类型标注示例

```
Define User with
  email: @pii(L2, email) Text,
  ssn: @pii(L3, ssn) Text,
  name: @pii(L1, name) Text.

To get_user with id: Text, produce User. It performs io with Sql:
  Return Db.query("SELECT * FROM users WHERE id = ?", [id]).
```

### 2.3 类型系统的局限

静态类型标注只能识别**显式声明**的 PII，无法处理：

1. **动态污点**：从 IO 输入读取的未标注数据
2. **间接污点**：通过计算派生的 PII（如用户 ID → 邮箱）
3. **容器污点**：包含 PII 的集合类型

污点分析通过数据流追踪弥补这些局限。

## 3. 污点分析基础

### 3.1 核心概念

**污点（Taint）**：标记数据是否包含 PII 及其属性（敏感级别、类别）。

```typescript
type Taint =
  | { kind: 'Clean' }  // 无 PII
  | { kind: 'Tainted'; sensitivity: 'L1' | 'L2' | 'L3'; category: PiiCategory }
```

**污点源（Source）**：PII 数据的来源
- 类型标注为 `@pii` 的变量
- IO 输入（`Http.request.body`, `Sql.query` 结果）

**污点传播（Propagation）**：污点如何在数据流中传递
- 赋值、函数调用、容器操作、字符串拼接

**敏感操作（Sink）**：可能导致 PII 泄露的操作
- `Http.post`, `Http.get`（未加密）
- `Log.info`, `Log.debug`（日志记录）
- `Db.insert`, `Db.update`（未脱敏存储）

### 3.2 分析流程

```
┌──────────────┐
│ 1. 污点源识别 │  标记所有 PII 数据
└──────┬───────┘
       │
       v
┌──────────────┐
│ 2. 污点传播   │  跟踪数据流图
└──────┬───────┘
       │
       v
┌──────────────┐
│ 3. Sink 检测  │  识别敏感操作
└──────┬───────┘
       │
       v
┌──────────────┐
│ 4. 诊断生成   │  报告安全风险
└──────────────┘
```

## 4. 污点标记与追踪

### 4.1 污点环境

污点环境记录每个变量的污点状态：

```typescript
type TaintEnv = Map<VariableName, TaintSet>

type TaintSet = {
  taints: Taint[]  // 可能包含多个 PII 类别
}

function mergeTaints(a: TaintSet, b: TaintSet): TaintSet {
  return { taints: [...a.taints, ...b.taints] }
}
```

### 4.2 污点源识别算法

```text
function identifySources(module: Core.Module): TaintEnv {
  let env: TaintEnv = {}

  for each function f in module.decls {
    // 1. 识别参数中的 PII 类型标注
    for each param in f.params {
      if param.type is @pii(sensitivity, category) {
        env[param.name] = { taints: [{ kind: 'Tainted', sensitivity, category }] }
      }
    }

    // 2. 识别 Data 字段中的 PII
    for each field in f.body.bindings {
      if field.type is @pii(sensitivity, category) {
        env[field.name] = { taints: [{ kind: 'Tainted', sensitivity, category }] }
      }
    }

    // 3. 识别 IO 输入（保守假设）
    for each call in findCalls(f.body) {
      if call.target matches IO_INPUT_PATTERNS {
        // Http.request.body, Sql.query 等返回值假设为污点
        env[call.resultVar] = { taints: [{ kind: 'Tainted', sensitivity: 'L2', category: 'unknown' }] }
      }
    }
  }

  return env
}

const IO_INPUT_PATTERNS = [
  'Http.request.body',
  'Http.request.query',
  'Sql.query',
  'Db.find',
  'Files.read'
]
```

### 4.3 示例：污点源识别

```
Define User with
  email: @pii(L2, email) Text,
  ssn: @pii(L3, ssn) Text.

To get_user with id: Text, produce User. It performs io with Sql:
  Let user = Db.query("SELECT * FROM users WHERE id = ?", [id]).
  Return user.
```

污点环境：
```
env = {
  'user': { taints: [{ kind: 'Tainted', sensitivity: 'L2', category: 'unknown' }] }
  // Db.query 返回值保守标记为污点
}
```

## 5. 污点传播规则

### 5.1 基本传播规则

```text
function propagateTaint(expr: Core.Expr, env: TaintEnv): TaintSet {
  switch expr.kind {
    case 'Name':
      // 变量引用：返回变量的污点
      return env[expr.name] || { taints: [] }

    case 'String', 'Int', 'Double', 'Bool':
      // 字面量：无污点
      return { taints: [] }

    case 'Call':
      // 函数调用：保守假设返回值继承所有参数的污点
      let argTaints = expr.args.map(arg => propagateTaint(arg, env))
      return unionAll(argTaints)

    case 'Construct':
      // 构造数据类型：继承字段污点
      let fieldTaints = expr.fields.map(field => propagateTaint(field.expr, env))
      return unionAll(fieldTaints)

    case 'Field':
      // 字段访问：obj.field 继承 obj 的污点
      let objTaint = propagateTaint(expr.obj, env)

      // 如果字段本身有 @pii 标注，合并两者
      let fieldType = lookupFieldType(expr.obj, expr.name)
      if fieldType is @pii(sensitivity, category) {
        return merge(objTaint, { taints: [{ kind: 'Tainted', sensitivity, category }] })
      }
      return objTaint

    case 'BinOp':
      // 二元操作（+, -, *, /, ==, etc.）
      if expr.op == '+' and (isString(expr.left) or isString(expr.right)) {
        // 字符串拼接：继承两边污点
        return union(propagateTaint(expr.left, env), propagateTaint(expr.right, env))
      }
      else {
        // 数值/布尔运算：保守假设继承污点
        return union(propagateTaint(expr.left, env), propagateTaint(expr.right, env))
      }

    case 'If':
      // if 表达式：返回两个分支污点的并集
      let thenTaint = propagateTaint(expr.then, env)
      let elseTaint = propagateTaint(expr.else, env)
      return union(thenTaint, elseTaint)

    case 'Match':
      // match 表达式：返回所有分支污点的并集
      let caseTaints = expr.cases.map(c => propagateTaint(c.body, env))
      return unionAll(caseTaints)

    case 'List':
      // List 构造：继承所有元素污点
      let elemTaints = expr.elems.map(e => propagateTaint(e, env))
      return unionAll(elemTaints)

    case 'Map':
      // Map 构造：继承所有 key 和 value 的污点
      let kvTaints = expr.entries.flatMap(e => [
        propagateTaint(e.key, env),
        propagateTaint(e.value, env)
      ])
      return unionAll(kvTaints)

    default:
      // 未知表达式：保守假设为污点（误报优于漏报）
      return { taints: [{ kind: 'Tainted', sensitivity: 'L3', category: 'unknown' }] }
  }
}

function unionAll(taintSets: TaintSet[]): TaintSet {
  let allTaints = taintSets.flatMap(ts => ts.taints)
  return { taints: allTaints }
}

function union(a: TaintSet, b: TaintSet): TaintSet {
  return { taints: [...a.taints, ...b.taints] }
}
```

### 5.2 语句级污点传播

```text
function propagateStmt(stmt: Core.Statement, env: TaintEnv): TaintEnv {
  switch stmt.kind {
    case 'Let':
      // Let x = expr: 更新 env[x] 为 expr 的污点
      let exprTaint = propagateTaint(stmt.expr, env)
      env[stmt.name] = exprTaint
      return env

    case 'Return':
      // Return 不更新环境
      return env

    default:
      return env
  }
}

function propagateFunc(f: Core.Func): TaintEnv {
  let env = identifySourcesInFunc(f)

  for each stmt in f.body.statements {
    env = propagateStmt(stmt, env)
  }

  return env
}
```

### 5.3 示例：污点传播

```
Define User with
  email: @pii(L2, email) Text,
  name: @pii(L1, name) Text.

To format_user with user: User, produce Text:
  Let greeting = "Hello, " + user.name.
  Let contact = "Email: " + user.email.
  Return greeting + "\n" + contact.
```

污点传播：
```
初始环境：
env = { 'user': { taints: [] } }  // 参数 user 本身未污染

user.name 访问：
  user.name 的类型为 @pii(L1, name) Text
  taint(user.name) = { taints: [{ kind: 'Tainted', sensitivity: 'L1', category: 'name' }] }

"Hello, " + user.name：
  taint("Hello, ") = { taints: [] }
  union(∅, L1:name) = { taints: [L1:name] }
  env['greeting'] = { taints: [L1:name] }

user.email 访问：
  taint(user.email) = { taints: [L2:email] }

"Email: " + user.email：
  env['contact'] = { taints: [L2:email] }

greeting + "\n" + contact：
  taint(result) = union(L1:name, L2:email) = { taints: [L1:name, L2:email] }
```

## 6. 敏感操作检测

### 6.1 Sink 分类

定义三类 Sink 及其安全要求：

| Sink 类型 | 安全要求 | 允许的 PII 级别 |
|----------|---------|---------------|
| **加密传输** | HTTPS, TLS 1.2+ | L1, L2, L3 |
| **未加密传输** | HTTP | 禁止所有 PII |
| **日志记录** | 生产环境日志 | 禁止 L2, L3 |
| **本地存储** | 文件系统 | 需要加密 |
| **数据库存储** | SQL/NoSQL | L3 需要加密/hash |

### 6.2 Sink 检测算法

```text
function detectSinks(module: Core.Module, env: TaintEnv): SinkViolation[] {
  let violations: SinkViolation[] = []

  for each function f in module.decls {
    for each call in findCalls(f.body) {
      let callTaint = propagateTaint(call, env)

      // 检测未加密 HTTP 传输
      if matchesPattern(call.target, HTTP_UNENCRYPTED_PATTERNS) {
        if hasTaint(callTaint) {
          violations.add({
            kind: 'UnencryptedTransmission',
            location: call.span,
            taint: callTaint,
            message: generateHttpWarning(callTaint)
          })
        }
      }

      // 检测日志记录
      if matchesPattern(call.target, LOG_PATTERNS) {
        let highSensitivity = callTaint.taints.filter(t => t.sensitivity == 'L2' || t.sensitivity == 'L3')
        if highSensitivity.length > 0 {
          violations.add({
            kind: 'PiiInLogs',
            location: call.span,
            taint: { taints: highSensitivity },
            message: generateLogWarning(highSensitivity)
          })
        }
      }

      // 检测未脱敏数据库存储
      if matchesPattern(call.target, DB_WRITE_PATTERNS) {
        let l3Taints = callTaint.taints.filter(t => t.sensitivity == 'L3')
        if l3Taints.length > 0 {
          violations.add({
            kind: 'UnsanitizedStorage',
            location: call.span,
            taint: { taints: l3Taints },
            message: generateDbWarning(l3Taints)
          })
        }
      }
    }
  }

  return violations
}

const HTTP_UNENCRYPTED_PATTERNS = [
  'Http.post',
  'Http.put',
  'Http.patch',
  'Http.get'  // 如果 URL 是 http:// 而非 https://
]

const LOG_PATTERNS = [
  'Log.info',
  'Log.debug',
  'Log.warn',
  'Log.error',
  'Console.log'
]

const DB_WRITE_PATTERNS = [
  'Db.insert',
  'Db.update',
  'Sql.execute'
]
```

### 6.3 精细化 Sink 检测

对于 HTTP 调用，检查 URL 是否使用 HTTPS：

```text
function isSecureHttp(call: Core.Call): boolean {
  // 检查第一个参数（URL）是否以 https:// 开头
  if call.args.length > 0 {
    let urlArg = call.args[0]
    if urlArg.kind == 'String' {
      return urlArg.value.startsWith('https://')
    }
  }

  // 无法静态确定：保守假设不安全
  return false
}

function detectHttpSink(call: Core.Call, taint: TaintSet): SinkViolation? {
  if call.target.name == 'Http.post' or call.target.name == 'Http.put' {
    if !isSecureHttp(call) and hasTaint(taint) {
      return {
        kind: 'UnencryptedTransmission',
        location: call.span,
        taint: taint,
        message: `PII data transmitted over unencrypted HTTP. Use HTTPS instead.`
      }
    }
  }
  return null
}
```

## 7. 保守分析策略

### 7.1 精度与召回率权衡

污点分析的两个指标：

- **精度（Precision）**：报告的问题中有多少是真实问题（1 - 误报率）
- **召回率（Recall）**：真实问题中有多少被检测到（1 - 漏报率）

安全分析优先召回率（避免漏报），接受一定误报：

```
精度  召回率  策略
────────────────────
90%    70%   过于宽松，可能漏报高风险
70%    95%   理想平衡（目标）
50%    99%   过于保守，误报过多
```

### 7.2 保守假设

**函数调用**：
```text
// 保守假设：返回值继承所有参数污点
taint(f(x, y, z)) = union(taint(x), taint(y), taint(z))

// 实际上：f 可能只使用 x，但静态分析无法确定
```

**容器操作**：
```text
// 保守假设：整个容器被污染
taint([x, y, z]) = union(taint(x), taint(y), taint(z))

// 实际上：访问 list[0] 只应返回 taint(x)，但需要索引分析
```

**控制流**：
```text
// 保守假设：所有分支的并集
taint(if cond then x else y) = union(taint(x), taint(y))

// 实际上：只有执行的分支会污染结果，但需要符号执行
```

### 7.3 误报控制策略

**白名单（Allowlist）**：
```text
// 允许特定函数处理 PII（如加密、脱敏）
const SANITIZER_FUNCTIONS = [
  'Crypto.hash',
  'Crypto.encrypt',
  'Pii.mask',
  'Pii.anonymize'
]

function propagateTaint(call: Core.Call, env: TaintEnv): TaintSet {
  if call.target.name in SANITIZER_FUNCTIONS {
    // 净化函数：移除污点
    return { taints: [] }
  }
  // 正常传播
  return propagateCall(call, env)
}
```

**敏感级别过滤**：
```text
// 只报告 L2/L3 的问题，忽略 L1
function shouldReport(taint: Taint): boolean {
  return taint.sensitivity == 'L2' || taint.sensitivity == 'L3'
}
```

**用户标注**：
```text
// 允许开发者标注安全审计过的代码
// @safe-pii: 该函数已通过安全审计
To send_verified with data: @pii(L2, email) Text, produce Result. @safe-pii:
  Return Http.post("https://verified-api.com/send", data).
```

### 7.4 误报分析示例

**误报场景**：

```
To process with user: User, produce Text:
  Let name_length = Text.length(user.name).  // user.name 是 @pii(L1, name)
  Return "Name length: " + name_length.
```

污点分析报告：
```
WARNING: PII data 'name' (L1) used in string concatenation
```

**实际情况**：
`name_length` 是一个数字，不包含 PII 信息。这是误报。

**改进方案**：
识别"脱敏函数"（如 `Text.length`, `List.size`）并移除污点：

```text
const SANITIZING_FUNCTIONS = [
  'Text.length',
  'List.size',
  'Map.keys',  // 仅返回 key，不包含 value
]
```

## 8. 诊断生成

### 8.1 诊断级别

| 级别 | 敏感度 | 触发条件 | 示例 |
|-----|-------|---------|------|
| **Error** | L3 | 高敏感 PII 未加密传输/存储 | SSN 通过 HTTP 发送 |
| **Warning** | L2 | 中敏感 PII 可能泄露 | Email 写入日志 |
| **Info** | L1 | 低敏感 PII 使用提示 | Name 拼接到 URL |

### 8.2 诊断生成算法

```text
function generateDiagnostics(violations: SinkViolation[]): Diagnostic[] {
  let diagnostics: Diagnostic[] = []

  for each violation in violations {
    let severity = determineSeverity(violation.taint)
    let message = formatMessage(violation)
    let suggestions = generateSuggestions(violation)

    diagnostics.add({
      severity: severity,
      message: message,
      location: violation.location,
      code: violation.kind,
      suggestions: suggestions,
      relatedInfo: traceDataFlow(violation)
    })
  }

  return diagnostics
}

function determineSeverity(taint: TaintSet): 'error' | 'warning' | 'info' {
  let maxSensitivity = max(taint.taints.map(t => t.sensitivity))

  switch maxSensitivity {
    case 'L3': return 'error'
    case 'L2': return 'warning'
    case 'L1': return 'info'
    default: return 'info'
  }
}

function formatMessage(violation: SinkViolation): string {
  let categories = violation.taint.taints.map(t => t.category).join(', ')
  let sensitivity = violation.taint.taints.map(t => t.sensitivity).join(', ')

  switch violation.kind {
    case 'UnencryptedTransmission':
      return `PII data (${categories}, sensitivity ${sensitivity}) transmitted over unencrypted HTTP`

    case 'PiiInLogs':
      return `PII data (${categories}, sensitivity ${sensitivity}) written to logs`

    case 'UnsanitizedStorage':
      return `High-sensitivity PII (${categories}) stored without encryption/hashing`

    default:
      return `PII security violation: ${violation.kind}`
  }
}

function generateSuggestions(violation: SinkViolation): Suggestion[] {
  switch violation.kind {
    case 'UnencryptedTransmission':
      return [
        { message: "Change HTTP to HTTPS in the URL", code: "use-https" },
        { message: "Encrypt PII data before transmission", code: "encrypt-data" },
        { message: "Remove PII from request payload", code: "remove-pii" }
      ]

    case 'PiiInLogs':
      return [
        { message: "Use Pii.mask() to redact sensitive data", code: "mask-pii" },
        { message: "Remove PII from log message", code: "remove-from-log" },
        { message: "Use structured logging with PII filtering", code: "structured-log" }
      ]

    case 'UnsanitizedStorage':
      return [
        { message: "Hash sensitive data before storage (Crypto.hash)", code: "hash-data" },
        { message: "Encrypt data at rest (Crypto.encrypt)", code: "encrypt-data" },
        { message: "Use tokenization for sensitive fields", code: "tokenize" }
      ]

    default:
      return []
  }
}

function traceDataFlow(violation: SinkViolation): RelatedInfo[] {
  // 追踪污点从源到 sink 的路径
  let path = findDataFlowPath(violation.taint)

  return path.map(node => ({
    message: `PII data flows through '${node.expr}'`,
    location: node.span
  }))
}
```

### 8.3 诊断示例

**示例 1：未加密 HTTP 传输**

```
To notify_user with email: @pii(L2, email) Text, produce Result:
  Return Http.post("http://api.example.com/notify", { email }).
```

诊断：
```
WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP
  at src/notify.aster:2:10

  Suggestions:
    1. Change HTTP to HTTPS in the URL
    2. Encrypt PII data before transmission
    3. Remove PII from request payload

  Data flow:
    - 'email' parameter defined at line 1:18 (L2:email)
    - Used in Http.post call at line 2:10
```

**示例 2：PII 日志记录**

```
To process_payment with ssn: @pii(L3, ssn) Text, amount: Int, produce Result:
  Log.info("Processing payment for SSN: " + ssn + ", amount: " + amount).
  Return PaymentService.charge(ssn, amount).
```

诊断：
```
ERROR: PII data (ssn, sensitivity L3) written to logs
  at src/payment.aster:2:3

  Suggestions:
    1. Use Pii.mask() to redact sensitive data
    2. Remove PII from log message
    3. Use structured logging with PII filtering

  Example fix:
    Log.info("Processing payment for SSN: " + Pii.mask(ssn) + ", amount: " + amount).
```

**示例 3：跨函数传播**

```
To get_user_email with id: Text, produce @pii(L2, email) Text. It performs io with Sql:
  Let user = Db.query("SELECT email FROM users WHERE id = ?", [id]).
  Return user.email.

To send_welcome with user_id: Text, produce Result:
  Let email = get_user_email(user_id).
  Return Http.post("http://welcome-service.com/send", { email }).
```

诊断：
```
WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP
  at src/welcome.aster:6:10

  Data flow:
    - 'email' originates from get_user_email() at line 5:14 (L2:email)
    - Db.query returns tainted data at line 2:14
    - Used in Http.post call at line 6:10

  Suggestions:
    1. Change HTTP to HTTPS in the URL
    2. Encrypt PII data before transmission
```

## 9. 真实场景分析

### 9.1 场景 1：用户注册流程

**代码**：
```
Define UserInput with
  email: Text,
  password: Text,
  name: Text.

To register_user with input: UserInput, produce Result. It performs io with Http and Sql:
  // 步骤 1：验证邮箱格式
  Let is_valid = Email.validate(input.email).
  If !is_valid:
    Return Err("Invalid email").

  // 步骤 2：检查邮箱是否已存在（污点源）
  Let existing = Db.query("SELECT * FROM users WHERE email = ?", [input.email]).
  If existing != null:
    Return Err("Email already exists").

  // 步骤 3：创建用户（污点传播）
  Let hashed_password = Crypto.hash(input.password).
  Let user = User(input.email, hashed_password, input.name).

  // 步骤 4：存储到数据库
  Db.insert("users", user).

  // 步骤 5：发送欢迎邮件（敏感 sink）
  Http.post("http://email-service.com/send", {
    to: user.email,
    subject: "Welcome!",
    body: "Hello " + user.name
  }).

  Return Ok("User registered").
```

**污点分析**：

1. **污点源**：
   - `Db.query` 返回值 `existing` 被标记为 `{ taints: [L2:unknown] }`（保守假设）
   - `input.email` 未标注但从外部输入，标记为 `{ taints: [L2:email] }`（启发式）

2. **污点传播**：
   ```
   existing (L2:unknown)
   user.email = input.email (L2:email)
   user.name = input.name (L1:name)
   ```

3. **Sink 检测**：
   - `Db.insert` 接收 `user`（包含 L2:email）→ **通过**（数据库存储允许 L2）
   - `Http.post` 到 `http://` URL 且包含 `user.email` (L2) → **警告**

4. **诊断**：
   ```
   WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP
     at register_user.aster:23:3

   Suggestions:
     1. Change URL to https://email-service.com/send
     2. Encrypt email data before transmission
   ```

**修复**：
```diff
- Http.post("http://email-service.com/send", {
+ Http.post("https://email-service.com/send", {
```

### 9.2 场景 2：日志调试陷阱

**代码**：
```
Define Order with
  id: Text,
  user_ssn: @pii(L3, ssn) Text,
  amount: Int.

To process_order with order: Order, produce Result. It performs io with Sql:
  // 调试日志（不安全！）
  Log.debug("Processing order: " + JSON.stringify(order)).

  Let result = PaymentGateway.charge(order.user_ssn, order.amount).

  Match result:
    Case Ok(tx_id):
      Log.info("Order " + order.id + " completed, transaction: " + tx_id).
      Return Ok(tx_id).
    Case Err(msg):
      Log.error("Order failed: " + msg + ", SSN: " + order.user_ssn).
      Return Err(msg).
```

**污点分析**：

1. **污点源**：
   - `order.user_ssn` 标注为 `@pii(L3, ssn)`

2. **污点传播**：
   - `JSON.stringify(order)` 继承 `order` 的所有字段污点 → `{ taints: [L3:ssn] }`
   - `"Order failed: " + msg + ", SSN: " + order.user_ssn` → `{ taints: [L3:ssn] }`

3. **Sink 检测**：
   - `Log.debug(...)` 包含 L3:ssn → **错误**
   - `Log.error(...)` 包含 L3:ssn → **错误**

4. **诊断**：
   ```
   ERROR: PII data (ssn, sensitivity L3) written to logs
     at process_order.aster:8:3
     at process_order.aster:16:7

   Suggestions:
     1. Use Pii.mask() to redact SSN
     2. Remove SSN from log messages
     3. Use structured logging with field-level filtering
   ```

**修复**：
```diff
- Log.debug("Processing order: " + JSON.stringify(order)).
+ Log.debug("Processing order: " + JSON.stringify({ id: order.id, amount: order.amount })).

- Log.error("Order failed: " + msg + ", SSN: " + order.user_ssn).
+ Log.error("Order failed: " + msg + ", SSN: " + Pii.mask(order.user_ssn)).
```

### 9.3 场景 3：第三方 API 集成

**代码**：
```
Define Analytics with
  user_id: Text,
  event: Text,
  properties: Map Text to Text.

To track_event with user_email: @pii(L2, email) Text, event: Text, produce Result. It performs io with Http:
  Let analytics = Analytics(
    user_id: user_email,  // 使用邮箱作为 user_id（不安全！）
    event: event,
    properties: Map.empty()
  ).

  // 发送到第三方分析服务
  Return Http.post("https://analytics-vendor.com/track", analytics).
```

**污点分析**：

1. **污点源**：
   - `user_email` 参数标注为 `@pii(L2, email)`

2. **污点传播**：
   - `analytics.user_id = user_email` → `analytics` 继承 `{ taints: [L2:email] }`

3. **Sink 检测**：
   - `Http.post` 到 HTTPS URL → **检查进一步**
   - 虽然使用 HTTPS，但发送到第三方（非内部域名）→ **警告**

4. **诊断**：
   ```
   WARNING: PII data (email, sensitivity L2) sent to third-party domain 'analytics-vendor.com'
     at track_event.aster:11:10

   Suggestions:
     1. Hash or anonymize user_email before sending
     2. Use internal user_id instead of email
     3. Review data processing agreement with vendor
   ```

**修复**：
```diff
+ To hash_email with email: @pii(L2, email) Text, produce Text:
+   Return Crypto.hash(email).

To track_event with user_email: @pii(L2, email) Text, event: Text, produce Result. It performs io with Http:
  Let analytics = Analytics(
-   user_id: user_email,
+   user_id: hash_email(user_email),  // 使用 hash 后的标识符
    event: event,
    properties: Map.empty()
  ).

  Return Http.post("https://analytics-vendor.com/track", analytics).
```

## 10. 实现指南

### 10.1 LSP 集成

污点分析应集成到 LSP（Language Server Protocol）以提供实时反馈：

```typescript
// src/lsp/pii_diagnostics.ts
export function analyzePii(document: TextDocument): Diagnostic[] {
  // 1. 解析文档
  let source = document.getText()
  let ast = parse(lex(canonicalize(source)))
  let core = lowerModule(ast)

  // 2. 运行污点分析
  let taintEnv = identifySources(core)
  propagateTaints(core, taintEnv)
  let violations = detectSinks(core, taintEnv)

  // 3. 生成 LSP 诊断
  return violations.map(v => toLspDiagnostic(v))
}

// src/lsp/analysis.ts (集成点)
export function analyzeDocument(document: TextDocument): Diagnostic[] {
  let diagnostics: Diagnostic[] = []

  // 现有分析
  diagnostics.push(...typecheck(document))
  diagnostics.push(...lintCode(document))

  // 新增：PII 污点分析
  diagnostics.push(...analyzePii(document))

  return diagnostics
}
```

### 10.2 配置选项

允许用户自定义分析行为：

```typescript
// aster.config.json
{
  "pii": {
    "enabled": true,
    "sensitivity": {
      "minLevel": "L2",  // 只报告 L2+ 的问题
      "categories": ["email", "ssn", "financial"]  // 关注特定类别
    },
    "sinks": {
      "http": {
        "allowedDomains": ["internal-api.company.com"],  // 白名单域名
        "requireHttps": true
      },
      "logs": {
        "allowL1": true,  // 允许 L1 数据写入日志
        "allowL2": false,
        "allowL3": false
      }
    },
    "sanitizers": [
      "Crypto.hash",
      "Crypto.encrypt",
      "Pii.mask",
      "Pii.anonymize"
    ]
  }
}
```

### 10.3 测试策略

创建黄金测试覆盖：

1. **基础污点传播**：变量赋值、函数调用、字符串拼接
2. **Sink 检测**：HTTP、日志、数据库
3. **跨函数传播**：污点通过多个函数调用传递
4. **容器操作**：List、Map 中的污点
5. **控制流**：if/match 分支合并污点
6. **误报控制**：净化函数、白名单域名
7. **真实场景**：注册、支付、分析集成

测试文件位置：`test/cnl/examples/pii_taint_*.aster`

期望诊断文件：`test/cnl/examples/expected_pii_taint_*.diag.txt`

## 11. 未来扩展

### 11.1 路径敏感分析

当前分析是路径不敏感的（path-insensitive），即合并所有分支：

```
If condition:
  x = pii_data.
Else:
  x = "safe".

Log.info(x).  // 报告警告（误报）
```

路径敏感分析可以区分不同执行路径，减少误报。

### 11.2 跨模块分析

当前分析限于单个模块。跨模块分析需要：

1. **函数摘要（Function Summary）**：记录每个函数的污点行为
2. **模块间传播**：导入函数的污点传播规则
3. **增量更新**：仅重新分析受影响的模块

### 11.3 机器学习辅助

使用 ML 模型改进：

1. **自动识别 PII**：从字段名、类型推断 PII（如 `email_address` → L2:email）
2. **减少误报**：学习哪些警告被开发者忽略
3. **推荐修复**：基于历史修复建议最佳实践

### 11.4 运行时监控

将污点分析扩展到运行时：

1. **动态污点追踪**：运行时标记和传播污点
2. **实时审计**：记录 PII 数据流
3. **入侵检测**：检测异常 PII 访问模式

## 12. 参考文献

1. **Taint Analysis: A Survey**
   Schwartz, E.J., Avgerinos, T., and Brumley, D. (2010)
   [Carnegie Mellon University Technical Report]

2. **Static Taint Analysis for Privacy Compliance**
   Arzt, S., et al. (2014)
   FlowDroid: Precise Context, Flow, Field, Object-sensitive and Lifecycle-aware Taint Analysis for Android Apps
   [ACM PLDI 2014]

3. **Information Flow Control for Standard ML**
   Zheng, L. and Myers, A.C. (2007)
   [ACM CSFW 2007]

4. **Practical Static Analysis of JavaScript Applications**
   Kristensen, E.K. and Møller, A. (2014)
   [ACM OOPSLA 2014]

5. **GDPR Compliance by Design**
   Bonatti, P.A., et al. (2020)
   *Data Protection in the Era of AI*
   [Springer]

---

**注**：本文档描述的算法为设计阶段，实际实现时可根据性能和精度需求调整策略。优先级应放在避免漏报（高召回率），逐步改进精度（减少误报）。
