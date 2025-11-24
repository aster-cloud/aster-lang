> 更新：2025-11-22 16:57 NZST · 执行者：Codex

# Truffle Backend 说明

## 设计理念
Truffle backend 基于 GraalVM Truffle 框架构建，目标是在纯计算场景下提供最高吞吐与最低延迟。通过将 Aster AST 映射为 Truffle AST Node，可让 GraalVM JIT 进行 AST 内联、特化与逃逸分析，获得接近手写 Java 的性能表现。该 backend 仅关注确定性、可推理的 CPU 计算，不提供与外界交互的能力，从而降低安全面与实现复杂度。

## 支持功能
- ✅ 纯 CPU 计算与数据转换，包含数值、集合、Pattern Matching、Result/Option 等内建结构。
- ✅ GraalVM 特化优化：Tail Call 优化、Partial Evaluation、Inlining、Loop Peeling。
- ✅ 与 Aster TypeChecker 对齐的 effect/capability 校验，确保 pure/async/io 区分在 runtime 保持一致。
- ✅ 与 Aster 其他 backend 共享的内建库（List/Map/Result 等）语义。

## 关键限制
- ❌ 不支持任何 IO effect 操作，所有 `IO.*` 内建函数在运行期直接抛出 UnsupportedOperationException。
- ❌ 不支持 `Http.*`、`Db.*`、`AuthRepo.*` 等依赖外部系统的命名空间。
- ❌ 不提供文件、网络、数据库或 Secrets 访问能力。
- ❌ 不支持长生命周期的异步事件（仅提供 await 占位实现返回原值）。
- ⚠️ 所有 effect 必须在编译期静态确定；Truffle backend 不实现动态 capability 调度。

### IO 操作限制列表
参考现有 stdlib 契约，以下操作均不可用：
- `IO.print(Text msg)`：返回 Text，Truffle backend 中直接抛错，建议切换 Java/TypeScript backend。
- `IO.readLine()`：原设计应返回 Text，在 Truffle backend 不可调用。
- `IO.readFile(Text path)`：原设计返回 `Result<Text, Text>`；在 Truffle backend 无文件访问能力。
- `IO.writeFile(Text path, Text contents)`：原设计返回 Bool；Truffle backend 无写入能力。
对于 Http/Db 命名空间，可参考 backend 对比文档中的替代方案。

## 推荐使用场景
- 🔹 高性能计算：批量规则编排、实时风控评分、需要 microsecond 级响应的同步调用。
- 🔹 多语言互操作：依托 GraalVM，可与 Java/JavaScript/R 原生互调，适用于嵌入式场景。
- 🔹 安全隔离：由于无 IO，适合在受限环境运行敏感算法，避免数据泄露风险。

避免使用 Truffle backend 的场景：
- ❌ 任何需要文件、网络、数据库 IO 的工作负载。
- ❌ 需要访问 Secrets、AI 模型或第三方 API 的流程。
- ❌ 需要原生线程/异步协同的后台任务。

## 技术细节
- 实现位置：`aster-truffle/src/main/java/aster/truffle/runtime`，核心入口 `Builtins.java`、`AsterLanguage.java`。
- 依赖：`org.graalvm.truffle:truffle-api:25.0.0`、`org.graalvm.sdk:graal-sdk:25.0.0` 等。
- 编译：使用 Gradle `:aster-truffle:compileJava` 目标；需 GraalVM 25 toolchain。
- 与前端接口：TypeChecker 将 effect/capability 元数据注入 AST，Truffle backend 基于元数据决定运行期校验。
- 错误处理：不支持 IO 的 builtin 均通过统一的 `ioNotSupportedMessage` 抛出 `UnsupportedOperationException`，引导用户切换 backend。

## 进一步阅读
- [Backend 对比](./backend-comparison.md)
- [Retry 语义](./retry-semantics.md)
- [Determinism 契约](./determinism-contract.md)
