# Getting Started with Aster

欢迎使用 Aster！本指南提供两种入门路径：

- **快速体验**（5 分钟）：使用 Docker/Podman 立即运行示例，无需安装任何工具
- **完整安装**（1 小时）：搭建开发环境，深入学习编译器和语言特性

---

## 🚀 快速体验（推荐新手）

如果您想立即体验 Aster 而无需配置环境，使用容器化方案最快捷：

### 前置要求

仅需安装 **Docker** 或 **Podman**（二选一）：
- Podman: https://podman.io/getting-started/installation
- Docker: https://docs.docker.com/get-docker/

### 运行示例程序

#### 示例 1: Fibonacci 数列

```bash
# 使用 Podman (推荐)
podman run --rm \
  -v $(pwd)/benchmarks:/benchmarks:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /benchmarks/core/fibonacci_20_core.json \
  --func=fibonacci -- 10

# 预期输出: 6765
```

```bash
# 使用 Docker (如果您更熟悉 Docker)
docker run --rm \
  -v $(pwd)/benchmarks:/benchmarks:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /benchmarks/core/fibonacci_20_core.json \
  --func=fibonacci -- 10
```

#### 示例 2: 自定义 Core IR 文件

```bash
# 运行您自己的 Core IR JSON 文件
podman run --rm \
  -v /path/to/your/code.json:/workspace/code.json:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /workspace/code.json \
  --func=main
```

### 容器化方案优势

| 特性 | 说明 |
|------|------|
| ⚡ 启动速度 | < 1 秒（GraalVM Native Image） |
| 📦 镜像大小 | 163 MB（包含运行时） |
| 🔒 隔离性 | 容器化，不污染本地环境 |
| 🌍 跨平台 | 支持 Linux、macOS、Windows (WSL) |

**恭喜！** 您已经成功运行了 Aster 程序。如果您需要修改 Aster 源码或开发新功能，请继续阅读[完整安装指南](#installation完整安装)。

---

## 📚 完整安装（开发环境）

### Prerequisites（前置要求）

在开始之前，请确保您的系统满足以下要求：

- **Node.js 22+**：Aster 编译器使用 TypeScript 实现，需要现代 Node.js 运行时
  - 检查版本：`node --version`
  - 下载地址：https://nodejs.org/
- **npm**：Node.js 包管理器（通常随 Node.js 一起安装）
- **Java 25 LTS**（推荐）或 **Java 21+**（最低要求）：用于 JVM 后端和 Gradle 构建
  - 检查版本：`java --version`
  - 推荐下载：[GraalVM CE 25](https://www.graalvm.org/downloads/)
- **操作系统**：推荐 macOS 或 Linux（Windows 通过 WSL 也可使用）

### Installation（完整安装）

#### 1. 克隆仓库

```bash
git clone https://github.com/wontlost-ltd/aster-lang.git
cd aster-lang
```

#### 2. 安装依赖

```bash
npm install
```

这将安装所有必要的 TypeScript 依赖和开发工具。

#### 3. 构建编译器

```bash
npm run build
```

构建过程将：
- 编译 TypeScript 源代码到 `dist/` 目录
- 生成 PEG 解析器（使用 peggy）
- 准备所有 CLI 工具和 LSP 服务器

**预期输出**：如果构建成功，您将看到 TypeScript 编译输出，且没有错误信息。

#### 4. 验证安装

运行快速验证以确认安装成功：

```bash
# 方式 1: 检查 CLI 版本
node dist/scripts/cli.js --version

# 方式 2: 解析简单示例
echo 'This module is test. To id, produce Int: Return 1.' > /tmp/test.aster
node dist/scripts/cli.js /tmp/test.aster
```

如果看到 JSON AST 输出，说明安装成功！✅

**可选**: 运行完整测试套件（需要 2-3 分钟）：

```bash
npm run test:golden
```

## Your First Policy（第一个策略程序）

让我们从一个简单的 "Hello World" 程序开始。

### 创建文件

在项目根目录创建 `hello.aster`：

```bash
cat > hello.aster << 'EOF'
This module is tutorial.hello.

To sayHello, produce Text:
  Return "Hello, Aster!".
EOF
```

### 解析到 AST

运行以下命令查看 Aster 如何解析您的代码：

```bash
node dist/scripts/cli.js hello.aster
```

**预期输出**：您将看到程序的 AST（抽象语法树）JSON 表示，包含模块声明和函数定义。

### 降级到 Core IR

Aster 使用一个小型、严格的 Core IR（中间表示）作为优化和后端生成的基础：

```bash
node dist/scripts/emit-core.js hello.aster > hello_core.json
```

**预期输出**：Core IR JSON 文件 `hello_core.json`，展示了函数的规范化表示。

### 运行程序

现在让我们实际运行这个程序并看到输出！Aster 提供三种运行方式：

#### 方式 1: 使用 Docker/Podman (最快)

```bash
# 使用 Podman (推荐)
podman run --rm \
  -v $(pwd):/workspace:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /workspace/hello_core.json \
  --func=sayHello

# 预期输出: Hello, Aster!
```

```bash
# 使用 Docker
docker run --rm \
  -v $(pwd):/workspace:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /workspace/hello_core.json \
  --func=sayHello
```

#### 方式 2: 使用 Node.js CLI (开发模式)

```bash
node dist/scripts/aster.js truffle hello.aster --func=sayHello

# 预期输出: Hello, Aster!
```

#### 方式 3: 使用 Native Image (如已构建)

如果您已经构建了 Native Image：

```bash
./aster-truffle/build/native/nativeCompile/aster hello_core.json --func=sayHello

# 预期输出: Hello, Aster!
# 启动时间: ~44ms
```

**恭喜！** 您已经成功运行了第一个 Aster 程序！🎉

### 运行方式对比

| 运行方式 | 启动时间 | 适用场景 | 优势 |
|---------|---------|---------|------|
| **Docker/Podman** | ~50ms | 生产部署、CI/CD | 隔离性好、跨平台 |
| **Node.js CLI** | ~2-5秒 | 开发调试 | 无需构建镜像 |
| **Native Image** | ~44ms | 独立分发 | 启动最快、单文件 |

**推荐**: 新手使用 Docker/Podman 体验，开发时使用 Node.js CLI。

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

创建 `greet_user.aster`：

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
node dist/scripts/cli.js greet_user.aster

# 生成 Core IR
node dist/scripts/emit-core.js greet_user.aster

# 运行黄金测试（如果添加了期望输出）
npm run test:golden
```

## Adding Effects（添加效果标注）

Aster 的核心特性之一是**编译时效果跟踪**。让我们创建一个执行 I/O 操作的函数。

### 创建带效果的程序

创建 `login_demo.aster`：

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
node dist/scripts/cli.js login_demo.aster
```

尝试删除 `It performs io` 并重新运行，您会看到类型检查错误！

## Running Tests（运行测试）

Aster 使用**黄金测试**（golden tests）来验证编译器行为的正确性。

### 黄金测试工作流程

1. **编写 CNL 程序**：例如 `test/cnl/examples/my_test.aster`
2. **生成期望输出**：
   ```bash
   # 生成 AST 期望输出
   node dist/scripts/cli.js test/cnl/examples/my_test.aster > test/cnl/examples/expected_my_test.ast.json

   # 生成 Core IR 期望输出
   node dist/scripts/emit-core.js test/cnl/examples/my_test.aster > test/cnl/examples/expected_my_test_core.json
   ```

3. **运行测试**：
   ```bash
   npm run test:golden
   ```

测试框架会自动：
- 解析所有 `test/cnl/examples/*.aster` 文件
- 比较实际输出与 `expected_*.ast.json` 和 `expected_*_core.json`
- 报告任何差异

### 示例：测试您的程序

```bash
# 将您的 greet_user.aster 复制到 examples
cp greet_user.aster test/cnl/examples/

# 生成期望输出
node dist/scripts/cli.js test/cnl/examples/greet_user.aster > test/cnl/examples/expected_greet_user.ast.json
node dist/scripts/emit-core.js test/cnl/examples/greet_user.aster > test/cnl/examples/expected_greet_user_core.json

# 运行测试
npm run test:golden
```

## Next Steps（下一步）

恭喜！您已经掌握了 Aster 的基础知识。接下来可以：

### 探索更多示例

查看 `test/cnl/examples/` 目录中的示例程序：

- `test/cnl/examples/greet.aster` - 简单的问候函数（模式匹配）
- `test/cnl/examples/login.aster` - 认证逻辑（Result 类型 + I/O 效果）
- `test/cnl/examples/fetch_dashboard.aster` - 异步并发（`Start` 和 `Wait`）
- `test/cnl/examples/policy_demo.aster` - 策略引擎演示

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
node dist/scripts/emit-classfiles.js test/cnl/examples/greet.aster

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
git diff test/cnl/examples/expected_*.json

# 如果变化合理，提交更新
git add test/cnl/examples/expected_*.json
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

### 常见问题 6: Docker/Podman 镜像拉取失败

**症状**: `podman pull` 或 `docker pull` 报错 "unable to resolve image" 或超时

**可能原因**:
- 网络问题，无法访问 ghcr.io
- 镜像不存在或标签错误

**解决方法**:
```bash
# 检查网络连接
ping ghcr.io

# 检查镜像是否存在
podman search ghcr.io/wontlost-ltd/aster-truffle

# 如果拉取失败，可以本地构建
podman build -f Dockerfile.truffle -t aster/truffle:latest .
```

### 常见问题 7: 容器运行权限错误

**症状**: "permission denied" 或 "cannot open file" 错误

**可能原因**:
- SELinux 或卷挂载权限问题
- 文件路径不正确

**解决方法**:
```bash
# 确保使用绝对路径
podman run -v $(pwd)/benchmarks:/benchmarks:ro ...

# 如果仍然失败，检查 SELinux 状态
getenforce

# 临时禁用 SELinux 标签检查 (仅用于测试，不推荐生产)
podman run --security-opt label=disable ...

# 或者使用 :z 选项 (Podman 特有)
podman run -v $(pwd)/benchmarks:/benchmarks:ro,z ...
```

### 常见问题 8: 容器内找不到文件

**症状**: "No such file or directory" 错误

**原因**: 卷挂载路径不匹配

**解决方法**:
```bash
# 确认宿主机文件存在
ls -la benchmarks/core/fibonacci_20_core.json

# 使用绝对路径挂载
podman run -v /full/path/to/benchmarks:/benchmarks:ro ...

# 检查容器内路径
podman run --rm -v $(pwd)/benchmarks:/benchmarks:ro \
  aster/truffle:latest \
  ls -la /benchmarks/core/
```

### 获取帮助

如果遇到其他问题：

1. **查看示例程序**：`test/cnl/examples/` 中有 50+ 个经过测试的示例
2. **阅读文档**：`docs/` 目录包含完整的语言和工具文档
3. **查看 CI 脚本**：`package.json` 中的 `scripts` 部分展示了所有可用命令
4. **Docker/Podman 文档**：
   - Podman: https://docs.podman.io/
   - Docker: https://docs.docker.com/
5. **提交 Issue**：https://github.com/wontlost-ltd/aster-lang/issues

---

**预计学习时间**：按照本指南完成所有步骤大约需要 **45-60 分钟**。

祝您使用 Aster 愉快！🚀
