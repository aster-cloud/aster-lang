# Getting Started with Aster

欢迎使用 Aster！本指南将帮助您在 1 小时内完成环境安装、编写第一个程序并理解 Aster 的核心概念。

## Prerequisites（前置要求）

在开始之前，请确保您的系统满足以下要求：

- **Node.js 22+**：Aster 编译器使用 TypeScript 实现，需要现代 Node.js 运行时
  - 检查版本：`node --version`
  - 下载地址：https://nodejs.org/
- **npm**：Node.js 包管理器（通常随 Node.js 一起安装）
- **Java 21+**（可选）：如果您需要使用 JVM 后端或运行 Gradle 示例
  - 检查版本：`java --version`
- **操作系统**：推荐 macOS 或 Linux（Windows 通过 WSL 也可使用）

## Installation（安装）

### 1. 克隆仓库

```bash
git clone https://github.com/wontlost-ltd/aster-lang.git
cd aster-lang
```

### 2. 安装依赖

```bash
npm install
```

这将安装所有必要的 TypeScript 依赖和开发工具。

### 3. 构建编译器

```bash
npm run build
```

构建过程将：
- 编译 TypeScript 源代码到 `dist/` 目录
- 生成 PEG 解析器（使用 peggy）
- 准备所有 CLI 工具和 LSP 服务器

**预期输出**：如果构建成功，您将看到 TypeScript 编译输出，且没有错误信息。

### 4. 验证安装

运行快速测试以确认安装成功：

```bash
npm run test:golden
```

如果看到 ✓ 测试通过的消息，说明安装成功！

## Your First Policy（第一个策略程序）

让我们从一个简单的 "Hello World" 程序开始。

### 创建文件

在项目根目录创建 `hello.cnl`：

```bash
cat > hello.cnl << 'EOF'
This module is tutorial.hello.

To sayHello, produce Text:
  Return "Hello, Aster!".
EOF
```

### 解析到 AST

运行以下命令查看 Aster 如何解析您的代码：

```bash
node dist/scripts/cli.js hello.cnl
```

**预期输出**：您将看到程序的 AST（抽象语法树）JSON 表示，包含模块声明和函数定义。

### 降级到 Core IR

Aster 使用一个小型、严格的 Core IR（中间表示）作为优化和后端生成的基础：

```bash
node dist/scripts/emit-core.js hello.cnl
```

**预期输出**：Core IR JSON，展示了函数的规范化表示。

### 代码说明

```text
This module is tutorial.hello.
```
- 每个 Aster 文件必须以模块声明开头
- 模块名使用点分隔符（dotted identifier）

```text
To sayHello, produce Text:
  Return "Hello, Aster!".
```
- `To <name>` 定义一个函数
- `produce Text` 声明返回类型
- 函数体使用 2 空格缩进
- `Return` 语句返回值并结束函数执行

## Your First Workflow（第一个工作流程）

现在让我们创建一个更真实的程序，展示 Aster 的类型系统和效果标注。

### 创建用户问候程序

创建 `greet_user.cnl`：

```text
This module is tutorial.greet.

Define User with id: Text, name: Text.

To greet with user: User?, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```

### 代码说明

```text
Define User with id: Text, name: Text.
```
- 定义一个数据类型 `User`，包含两个字段
- Aster 使用结构化类型，所有字段都是非空的（除非显式标记为可选）

```text
To greet with user: User?, produce Text:
```
- `user: User?` 表示参数可能为 null（`User?` 是 `Maybe of User` 的语法糖）
- Aster 默认非空，可空性必须显式声明

```text
Match user:
  When null, Return "Hi, guest".
  When User(id, name), Return "Welcome, {name}".
```
- `Match` 语句实现模式匹配
- `When null` 处理空值情况
- `When User(id, name)` 解构 User 对象，绑定字段到变量
- `{name}` 是字符串插值语法

### 运行程序

```bash
# 解析并查看 AST
node dist/scripts/cli.js greet_user.cnl

# 生成 Core IR
node dist/scripts/emit-core.js greet_user.cnl

# 运行黄金测试（如果添加了期望输出）
npm run test:golden
```

## Adding Effects（添加效果标注）

Aster 的核心特性之一是**编译时效果跟踪**。让我们创建一个执行 I/O 操作的函数。

### 创建带效果的程序

创建 `login_demo.cnl`：

```text
This module is tutorial.auth.

Define User with id: Text, name: Text.

Define AuthErr as one of InvalidCreds, Locked.

To login with username: Text, password: Text, produce Result of User and AuthErr. It performs io:
  Let ok be AuthRepo.verify(username, password).
  If not(ok),:
    Return err of InvalidCreds.
  Return ok of User with id = UUID.randomUUID(), name = username.
```

### 代码说明

```text
Define AuthErr as one of InvalidCreds, Locked.
```
- 定义枚举类型（sum type），表示可能的认证错误

```text
produce Result of User and AuthErr. It performs io:
```
- `Result of User and AuthErr` 表示函数返回一个 Result 类型（类似 Rust 或 Haskell）
- `It performs io` 声明此函数执行 I/O 效果
- **重要**：调用 `AuthRepo.verify` 和 `UUID.randomUUID` 等 I/O 函数时，当前函数必须声明 `io` 效果

```text
Return err of InvalidCreds.
Return ok of User with id = ..., name = ...
```
- `err of <value>` 构造 Result 的错误分支
- `ok of <value>` 构造 Result 的成功分支

### 效果系统规则

Aster 使用**效果格**（effect lattice）来跟踪副作用：

- `∅`（纯函数，无副作用）
- `CPU`（纯计算，无 I/O）
- `IO[*]`（任意 I/O 操作）
- `IO[Http]`、`IO[Sql]`、`IO[Files]` 等（细粒度能力）

**规则**：
1. 纯函数不能调用有效果的函数
2. `CPU` 函数不能调用 `IO` 函数
3. 效果在编译时强制检查，防止意外副作用

### 验证效果

```bash
# 如果移除 "It performs io"，编译器会报错
node dist/scripts/cli.js login_demo.cnl
```

尝试删除 `It performs io` 并重新运行，您会看到类型检查错误！

## Running Tests（运行测试）

Aster 使用**黄金测试**（golden tests）来验证编译器行为的正确性。

### 黄金测试工作流程

1. **编写 CNL 程序**：例如 `cnl/examples/my_test.cnl`
2. **生成期望输出**：
   ```bash
   # 生成 AST 期望输出
   node dist/scripts/cli.js cnl/examples/my_test.cnl > cnl/examples/expected_my_test.ast.json

   # 生成 Core IR 期望输出
   node dist/scripts/emit-core.js cnl/examples/my_test.cnl > cnl/examples/expected_my_test_core.json
   ```

3. **运行测试**：
   ```bash
   npm run test:golden
   ```

测试框架会自动：
- 解析所有 `cnl/examples/*.cnl` 文件
- 比较实际输出与 `expected_*.ast.json` 和 `expected_*_core.json`
- 报告任何差异

### 示例：测试您的程序

```bash
# 将您的 greet_user.cnl 复制到 examples
cp greet_user.cnl cnl/examples/

# 生成期望输出
node dist/scripts/cli.js cnl/examples/greet_user.cnl > cnl/examples/expected_greet_user.ast.json
node dist/scripts/emit-core.js cnl/examples/greet_user.cnl > cnl/examples/expected_greet_user_core.json

# 运行测试
npm run test:golden
```

## Next Steps（下一步）

恭喜！您已经掌握了 Aster 的基础知识。接下来可以：

### 探索更多示例

查看 `cnl/examples/` 目录中的示例程序：

- `cnl/examples/greet.cnl` - 简单的问候函数（模式匹配）
- `cnl/examples/login.cnl` - 认证逻辑（Result 类型 + I/O 效果）
- `cnl/examples/fetch_dashboard.cnl` - 异步并发（`Start` 和 `Wait`）
- `cnl/examples/policy_demo.cnl` - 策略引擎演示

### 阅读文档

- **语言概览**：[docs/guide/language-overview.md](./language-overview.md)
- **语法参考**：[docs/reference/language-specification.md](../reference/language-specification.md)
- **效果系统**：[docs/reference/effects.md](../reference/effects.md)
- **能力系统**：[docs/guide/capabilities.md](./capabilities.md)
- **LSP 教程**：[docs/guide/lsp-tutorial.md](./lsp-tutorial.md)

### 尝试 JVM 后端

如果您安装了 Java 21+，可以将 Aster 程序编译为 JVM 字节码：

```bash
# 生成 .class 文件
node dist/scripts/emit-classfiles.js cnl/examples/greet.cnl

# 创建 JAR 包
node dist/scripts/jar-jvm.js

# 运行示例（需要 Gradle）
npm run greet:run
```

### 使用 LSP（编辑器集成）

启动 Aster 语言服务器以获得编辑器支持（悬停提示、跳转定义、重命名等）：

```bash
node dist/src/lsp/server.js --stdio
```

查看 [LSP 教程](./lsp-tutorial.md) 了解如何配置 VS Code 集成。

### 贡献代码

阅读 `CONTRIBUTING.md` 了解如何参与 Aster 开发：

```bash
# 运行完整 CI 检查
npm run ci
```

## Troubleshooting（故障排除）

### 常见问题 1：构建失败

**症状**：`npm run build` 报错

**可能原因**：
- Node.js 版本过低（需要 22+）
- npm 依赖损坏

**解决方法**：
```bash
# 检查 Node.js 版本
node --version  # 应该 >= 22.0.0

# 清理并重新安装
rm -rf node_modules dist
npm install
npm run build
```

### 常见问题 2：测试失败

**症状**：`npm run test:golden` 报告 diff

**可能原因**：
- 期望输出文件过时
- 代码改动导致输出变化

**解决方法**：
```bash
# 更新所有黄金测试期望输出
npm run test:golden:update

# 检查 git diff 确认变化是预期的
git diff cnl/examples/expected_*.json

# 如果变化合理，提交更新
git add cnl/examples/expected_*.json
git commit -m "Update golden test expectations"
```

### 常见问题 3：效果类型检查错误

**症状**：编译器报告 "Effect mismatch" 或 "Missing effect declaration"

**原因**：函数调用了有效果的函数，但自身未声明相应效果

**解决方法**：
```text
# ❌ 错误示例
To processData, produce Text:
  Let result be Http.fetch("https://api.example.com").  # Http.fetch 需要 io 效果
  Return result.

# ✅ 正确示例
To processData, produce Text. It performs io:
  Let result be Http.fetch("https://api.example.com").
  Return result.
```

**规则提醒**：
- 调用 I/O 函数必须声明 `It performs io`
- 查看 [effects.md](../reference/effects.md) 了解完整的效果系统规则

### 常见问题 4：模式匹配不完整

**症状**：编译器警告 "Non-exhaustive pattern match"

**原因**：`Match` 语句未覆盖所有可能的情况

**解决方法**：
```text
# ❌ 不完整的模式匹配
To handle with result: Result of User and AuthErr, produce Text:
  Match result:
    When ok(user), Return user.name.
    # 缺少 err 分支！

# ✅ 完整的模式匹配
To handle with result: Result of User and AuthErr, produce Text:
  Match result:
    When ok(user), Return user.name.
    When err(InvalidCreds), Return "Invalid credentials".
    When err(Locked), Return "Account locked".
```

### 常见问题 5：找不到 CLI 命令

**症状**：`node dist/scripts/cli.js` 报告 "Cannot find module"

**原因**：未运行 `npm run build`

**解决方法**：
```bash
# 构建编译器
npm run build

# 如果问题仍存在，检查 dist 目录
ls -la dist/scripts/
```

### 获取帮助

如果遇到其他问题：

1. **查看示例程序**：`cnl/examples/` 中有 50+ 个经过测试的示例
2. **阅读文档**：`docs/` 目录包含完整的语言和工具文档
3. **查看 CI 脚本**：`package.json` 中的 `scripts` 部分展示了所有可用命令
4. **提交 Issue**：https://github.com/wontlost-ltd/aster-lang/issues

---

**预计学习时间**：按照本指南完成所有步骤大约需要 **45-60 分钟**。

祝您使用 Aster 愉快！🚀
