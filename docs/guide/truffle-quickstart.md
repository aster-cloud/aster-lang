# Aster Truffle 快速入门

> 更新时间：2025-11-05 15:31 NZST · 执行者：Codex  
> 适用版本：GraalVM 25 / Aster Truffle Phase 0

Aster Truffle 是基于 GraalVM Truffle 框架实现的 Aster 语言解释器，与传统 ASM 字节码发射器相比，提供更快的峰值性能、更友好的优化能力以及原生镜像支持。本指南面向第一次接触 Truffle 的开发者，帮助你在 5 分钟内跑通第一个程序，并了解如何在 JVM 与 Native Image 模式之间切换。

## 前置条件

- 操作系统：macOS 13+/Linux（x86_64）
- JDK：**GraalVM 25 CE/EE**，已安装 `native-image` 组件（`gu install native-image`）
- Node.js 22+ 与 npm（用于快速生成 Core IR）
- 可选：Docker 24+ 或 Podman 5+（快速体验模式）
- 确认仓库根目录下存在示例文件：
  - `test/cnl/programs/examples/hello.aster`
  - `benchmarks/core/simple_hello_core.json`
  - `benchmarks/core/fibonacci_20_core.json`

## 安装步骤

### 方式 1: Docker（推荐新手）

使用官方镜像 `ghcr.io/wontlost-ltd/aster-truffle:latest`，无需本地安装 JDK 或 Node。

```bash
# 1. 拉取镜像（Docker 或 Podman 均可）
docker pull ghcr.io/wontlost-ltd/aster-truffle:latest

# 2. 运行示例（挂载当前仓库，读取 Core IR）
docker run --rm \
  -v $(pwd)/benchmarks:/benchmarks:ro \
  ghcr.io/wontlost-ltd/aster-truffle:latest \
  /benchmarks/core/simple_hello_core.json

# 预期输出：
# Hello, world!
```

> 镜像内置 GraalVM Native Image 版本的解释器，二进制大小约 **36.26MB**，冷启动约 **50ms**。如需传参，可在路径后追加 `-- funcName -- arg1 arg2`。

### 方式 2: 从源码构建

```bash
# 1. 安装依赖并构建 TypeScript 前端
npm ci
npm run build

# 2. 构建 Truffle JVM 运行器
./gradlew :aster-truffle:installDist

# 3. 可选：预构建原生镜像（二进制输出在 build/native/nativeCompile/aster）
./gradlew --no-configuration-cache :aster-truffle:nativeCompile
```

成功后可直接使用：

- JVM 模式：`./aster-truffle/build/install/aster-truffle/bin/aster-truffle`
- Native 模式：`./aster-truffle/build/native/nativeCompile/aster`

## 第一个程序: Hello World

### 1. 创建程序文件

在仓库根目录新建 `examples/hello/hello.aster`，内容如下：

```aster
This module is app.

To helloMessage, produce Text:
  Return "Hello, Truffle!".
```

> 更快的选择：直接复用仓库自带的 `test/cnl/programs/examples/hello.aster`。

### 2. 运行程序（JVM 模式）

```bash
# 1. 降低到 Core IR JSON
mkdir -p build/quickstart
node dist/scripts/aster.js core test/cnl/programs/examples/hello.aster \
  > build/quickstart/hello_core.json

# 2. 使用 Truffle JVM 解释器执行
./aster-truffle/build/install/aster-truffle/bin/aster-truffle \
  build/quickstart/hello_core.json

# 预期输出：
# Hello, world!
```

若需要指定入口函数，可追加 `--func=<函数名>`；参数通过 `--` 分隔，比如 `--func=helloMessage -- Alice`。

### 3. 运行程序（Native Image 模式）

```bash
# 1. 构建一次原生镜像（二进制约 36.26MB）
./gradlew --no-configuration-cache :aster-truffle:nativeCompile

# 2. 调用原生可执行文件（冷启动 ~50ms）
./aster-truffle/build/native/nativeCompile/aster \
  build/quickstart/hello_core.json

# 预期输出：
# Hello, world!
```

> 如果看到 `enable-native-access` 警告，可在 GraalVM 运行时追加 `--enable-native-access=ALL-UNNAMED` 或设置 `JAVA_TOOL_OPTIONS`.

## 理解执行流程

| 阶段 | 描述 | 主要命令/文件 |
| --- | --- | --- |
| CNL → AST | 将 `.aster` 自然语言语法解析为抽象语法树 | `node dist/scripts/cli.js` |
| AST → Core IR | 生成精简、类型化的 Core IR JSON | `node dist/scripts/aster.js core` |
| Core IR → Truffle AST | `Loader` 将 JSON 构建为 Truffle 节点树 | `aster-truffle` 模块 |
| 执行 | Truffle AST 解释执行，热点路径由 GraalVM JIT/Native 优化 | JVM `polyglot.Context` 或 Native Image |

当前覆盖率：**30/32 个 Core IR 节点（93.75%）**，Truffle DSL 类型特化已启用。

## 示例程序

### Fibonacci 计算

- Core IR 文件：`benchmarks/core/fibonacci_20_core.json`
- 默认入口：`main`，调用 `fib(20)`，期望输出 `6765`

```bash
# JVM 模式（首次运行含 Gradle 启动约 1.2s，热身后显著加速）
./aster-truffle/build/install/aster-truffle/bin/aster-truffle \
  benchmarks/core/fibonacci_20_core.json

# Native 模式（实测 real ≈ 0.08s，常驻内存约 46 MB）
./aster-truffle/build/native/nativeCompile/aster \
  benchmarks/core/fibonacci_20_core.json
```

> 预热提示：JIT 模式前几次运行会明显慢于原生镜像，达到热身后吞吐可比 ASM Emitter 快 **20-30%**。

### 模式匹配示例

- CNL 示例：`test/cnl/programs/examples/greet.aster`

```bash
# 降低到 Core IR
node dist/scripts/aster.js core test/cnl/programs/examples/greet.aster \
  > build/quickstart/greet_core.json

# 调用 greet(User?) -> Text
./aster-truffle/build/install/aster-truffle/bin/aster-truffle \
  build/quickstart/greet_core.json --func=greet -- User-1 "Aster Dev"

# 预期输出：
# Welcome, Aster Dev
```

模式匹配在 Truffle DSL 中具有针对 `null` 与代数数据类型的特化，实现等价于 Core IR 匹配语义。

## 常见问题 (FAQ)

**Q: Truffle 和 ASM Emitter 有什么区别？**  
A: ASM 直接生成 JVM 字节码，启动快但维护成本高；Truffle 通过 AST 解释 + JIT，依托 GraalVM 自动优化。Truffle 支持原生镜像、DSL 特化、易调试，峰值性能在热身后可超越 ASM 约 20-30%。

**Q: JVM 模式与 Native Image 什么时候选？**  
A: 开发调试、需要即时编译的场景用 JVM；部署到 CLI/函数或追求极致启动时间时选择原生镜像（单文件 36.26MB，冷启动约 50ms）。

**Q: `nativeCompile` 报配置缓存错误怎么办？**  
A: 使用 `./gradlew --no-configuration-cache :aster-truffle:nativeCompile`；该命令已在指南中验证可通过。

**Q: 执行时如何传递函数参数？**  
A: 使用 `--func=<name>` 指定入口，参数写在 `--` 之后，例如 `... --func=greet -- User-1 "Aster Dev"`。所有参数会自动尝试解析为 `Int/Long/Double/Boolean`。

**Q: 如何关闭 `enable-native-access` 警告？**  
A: 在 JVM 模式添加 `--enable-native-access=ALL-UNNAMED`（Gradle 可通过 `ORG_GRADLE_OPTS` 设置），Native 模式使用 `JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"` 再运行。

**Q: 哪里可以查看更多示例与性能数据？**  
A: CNL 示例位于 `test/cnl/programs/`，Core IR 基准在 `benchmarks/core/`，性能评估详见 `docs/truffle-performance-benchmarks.md`。

## 下一步

- 阅读语言参考：`docs/guide/language-overview.md`
- 深入理解 Truffle 架构：`docs/truffle-architecture.md`
- 按照 `docs/truffle-performance-benchmarks.md` 调整 JIT 热身与优化开关
- 参考 `docs/guide/quickstart.md` 了解 CLI 与其他后端
- 浏览更多示例与测试：`test/cnl/programs/`、`docs/guide/examples.md`

## 参考资料

- `docs/guide/quickstart.md`：整体 CLI 与构建流程
- `docs/truffle-architecture.md`：Truffle 节点映射与运行时设计
- `docs/truffle-performance-benchmarks.md`：性能测试方法与指标
- `docs/performance-guide.md`：后端选择与调优建议
- GraalVM 官方文档：[Language Implementation Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
