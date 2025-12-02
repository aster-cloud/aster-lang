# Aster Native 应用构建指南

- 日期：2025-10-25 17:34 NZST
- 执行者：Codex

## 概述

- 功能介绍：`aster native` 命令基于 GraalVM Native Image 将 CNL 程序与 CLI 封装为独立可执行文件，消除 JVM 启动成本并缩短冷启动延迟。
- 支持的编译器后端：默认 TypeScript 后端（`ASTER_COMPILER=typescript`）与 Java 后端（`ASTER_COMPILER=java`）均可生成原生映像，内部通过统一 Core IR 管道共享。
- GraalVM 要求：需要 GraalVM JDK 25 或更新版本，并安装 `native-image` 组件；命令行环境需在 PATH 中优先使用 GraalVM 可执行文件。

## 快速开始

### 前置条件

- 安装 GraalVM JDK 25+，建议使用 `gu install native-image` 启用原生工具链。
- 确保 `JAVA_HOME` 指向 GraalVM，`native-image --version` 可以正确输出版本信息。
- 若要启用 Java 后端，请设置 `ASTER_COMPILER=java` 并提前构建 CLI 依赖。

### 构建 CLI Native 可执行文件

```bash
./gradlew :aster-lang-cli:nativeCompile
```

- 该任务会自动收集所有模块产物，生成位于 `aster-lang-cli/build/native/nativeCompile/aster-lang-cli` 的可执行文件。
- 构建期间会应用 Phase A~C 产出的反射、资源与 funcHints 配置，确保 CLI 在原生环境下可运行。

### 构建用户程序 Native 可执行文件

```bash
aster native myapp.cnl --output myapp
```

- 命令默认使用 TypeScript 编译器后端；若需切换 Java 后端，设置 `ASTER_COMPILER=java aster native ...`。
- CLI 会在内部完成 parse → lower → compile → jar → native-image 流程，产出与源文件同名的可执行文件。

## 架构说明

### Phase A: 配置收集

- `reflect-config.json`：通过 GraalVM agent 多轮采集累计 90 条命中记录，经过筛选后最终保留 36 条核心反射条目，覆盖 `aster.core.*`、`aster.cli.*` 与 `com.fasterxml.jackson.*`。
- `resource-config.json`：定义 5 类资源模式，包括 `package.json`、`package-lock.json`、`scripts/**`、`dist/scripts/**` 以及 `META-INF/services/**`。
- 配置来源：借助 `native-image-agent` 捕捉 CLI 常用操作（help/version/compile），结合手工审查剔除 Gradle 噪声项，仅保留运行期必需条目。

### Phase B: funcHints 优化

- 基于 Core IR 的类型提示：`JavaCompilerBackend.buildFuncHints` 会扫描 Core 模块中的函数返回类型，为 ASM emitter 提供 JVM 原始类型提示。
- 支持的原始类型：`Int → I`、`Long → J`、`Double/Float → D`、`Bool/Boolean → Z`，其余类型自动回落为引用类型。
- 性能优化原理：提前传递 funcHints 使 ASM emitter 生成更紧凑的字节码，避免运行时装箱拆箱，提高 native-image 在热点函数上的性能表现。

### Phase C: Native-Image 配置

- 反射配置详解：保留 AST/Core IR 数据类和 Jackson 扩展实现，剔除 Gradle Daemon 相关条目，保证镜像体积与初始化时间。
- 资源配置详解：通过通配符注入 NPM 产物与 ServiceLoader 资源，确保 CLI 在无文件系统访问权限的环境下依旧可以加载脚本。
- 为什么不需要代理配置：原生 CLI 全链路未使用 JDK 动态代理；agent 捕获的 Gradle 代理仅在构建期出现，因此未纳入运行时配置。

### Phase D: aster native 命令

- 命令用法：`aster native <source.cnl> [--output <path>] [--main <Class>] [--emit-dir <dir>]`。
- 选项说明：
  - `--output/-o`：指定生成的可执行文件路径。
  - `--compiler`：显式选择后端（等价于设置环境变量）。
  - `--emit-dir`：复用已有 JVM 字节码产物，跳过编译阶段。
- 构建流程：解析参数 → 生成 Core IR → 通过 funcHints 优化字节码 → 调用 `native-image` 应用 Phase A~C 的配置 → 产出可执行文件并写入报告。

## 已知限制

- `JarHotSwapRunner` 无法在 native 模式下工作（GraalVM 不支持运行时动态类加载）。
- `aster run --watch` 在 native 可执行文件内不可用，原因同上。
- 构建过程依赖 GraalVM 工具链与本地 C/C++ 链接器，CI 需提前准备环境。
- `./gradlew build` 仍依赖缺失的 `test/cnl/stdlib/finance/*.aster` 样例；native 流程不受影响，但全量构建会失败。

## 故障排除

- 常见错误：
  - `native-image` 未找到 → 确认 GraalVM 已安装并在 PATH 中，运行 `gu list` 检查 `native-image` 组件。
  - 缺少反射条目导致启动失败 → 检查 `META-INF/native-image/reflect-config.json` 是否遗漏新增数据结构，可参考 Phase A agent 流程重新采集。
  - 构建失败提示缺失 `test/cnl/stdlib/...` → 当前仓库未提供该策略资产，可临时跳过 `:quarkus-policy-api:generateAsterJar` 或补充测试资源。
- 性能调优建议：
  - 添加 `--initialize-at-build-time` 到自定义模块，减少运行时初始化开销。
  - 使用 `-H:+ReportExceptionStackTraces` 排查 native 运行期异常，便于定位缺失配置。

## 实现细节

- 修改文件清单：
  - `aster-lang-cli/src/main/java/aster/cli/compiler/JavaCompilerBackend.java`：添加 Phase B `funcHints` 构建逻辑。
  - `aster-lang-cli/src/main/resources/META-INF/native-image/reflect-config.json`：更新精简后的反射条目。
  - `aster-lang-cli/src/main/resources/META-INF/native-image/resource-config.json`：加入 `package-lock.json`、`dist/scripts/**` 等资源模式。
  - `docs/workstreams/native-cli/operations-log.md`：记录 Phase A~E 的执行日志。
- 关键代码说明：
  - `buildFuncHints` 根据 Core IR 函数返回类型生成 JVM 原生类型映射，生成包级 Hint 表并传递给 ASM emitter。
  - CLI native 任务通过 Gradle `nativeCompile` 连接 `native-image`，自动引用上述配置文件。
- 测试覆盖：`ASTER_COMPILER=java ./gradlew :aster-lang-cli:test` 与默认后端测试均通过；`./gradlew build` 仍受缺失示例阻塞，需人工补齐资源或跳过任务。

## 参考资料

- GraalVM Native Image 官方文档：https://www.graalvm.org/latest/reference-manual/native-image/
- 阶段报告：`.claude/phase-a-agent-report.md`、`.claude/phase-b-status-report.md`、`.claude/phase-c-config-report.md`
- 指南配套操作日志：`docs/workstreams/native-cli/operations-log.md`
