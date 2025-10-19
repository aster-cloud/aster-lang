# Aster Lang Native CLI

> 更新时间：2025-10-20 00:10 NZDT

GraalVM Native Image 版本的 Aster Language 命令行工具，提供极快的启动速度和小巧的二进制体积。

## 特性

- ⚡ **极速启动**: 启动时间 <10ms，比 JVM 快 100 倍
- 📦 **小巧轻便**: 二进制文件仅 17MB，无需 JRE 依赖
- 🔧 **完整功能**: 支持编译、类型检查、JAR 打包等所有核心功能
- 🎯 **跨平台**: 支持 Linux、macOS、Windows

## 性能指标

| 指标 | 值 |
|------|-----|
| 二进制大小 | 17.31 MB |
| 启动时间 | 7 ms |
| 内存占用 | ~50 MB |

## 构建

### 前提条件

- GraalVM JDK 25+
- Node.js 18+
- Gradle 9+

### 构建 Native Image

```bash
# 构建原生可执行文件
./gradlew :aster-lang-cli:nativeCompile

# 可执行文件位置
# Linux/macOS: aster-lang-cli/build/native/nativeCompile/aster
# Windows: aster-lang-cli\build\native\nativeCompile\aster.exe
```

### 性能验证

```bash
# 检查二进制大小
./gradlew :aster-lang-cli:checkBinarySize

# 测量启动时间
time ./aster-lang-cli/build/native/nativeCompile/aster version
```

## 使用

### 基本命令

```bash
# 查看版本
./aster version

# 查看帮助
./aster help

# 类型检查
./aster typecheck cnl/examples/hello.aster

# 编译为 JVM 字节码
./aster compile cnl/examples/hello.aster --output ./build/classes

# 生成 JAR 包
./aster jar cnl/examples/hello.aster --output ./build/hello.jar

# 解析输出 AST
./aster parse cnl/examples/hello.aster

# 降级到 Core IR
./aster core cnl/examples/hello.aster
```

### 高级选项

```bash
# 指定 capability 配置
./aster typecheck app.aster --caps capabilities.json

# JSON 格式输出诊断信息
./aster typecheck app.aster --json

# 自定义输出目录
./aster compile app.aster --output /custom/path
```

### 环境变量

```bash
# 能力配置文件
export ASTER_CAPS=/path/to/capabilities.json

# 效果配置文件
export ASTER_EFFECT_CONFIG=/path/to/effects.json

# 能力效果校验（0=关闭，1=开启）
export ASTER_CAP_EFFECTS_ENFORCE=1
```

## 命令参考

### `compile` - 编译

编译 CNL 源文件为 JVM 字节码。

```bash
aster compile <file> [--output <dir>] [--json]
```

**参数**:
- `<file>`: CNL 源文件路径
- `--output <dir>`: 输出目录（默认: `build/jvm-classes`）
- `--json`: 以 JSON 格式输出结果

### `typecheck` - 类型检查

执行类型检查，验证代码正确性。

```bash
aster typecheck <file> [--caps <json>] [--json]
```

**参数**:
- `<file>`: CNL 源文件路径
- `--caps <json>`: Capability 配置文件路径
- `--json`: 以 JSON 格式输出诊断信息

### `jar` - 打包

生成独立的 JAR 包。

```bash
aster jar [<file>] [--output <file>]
```

**参数**:
- `<file>`: CNL 源文件路径（可选，复用上次编译结果）
- `--output <file>`: 输出 JAR 文件路径（默认: `build/aster-out/aster.jar`）

### `parse` - 解析

仅解析源文件，输出 AST JSON。

```bash
aster parse <file> [--json]
```

### `core` - 降级

将源文件降级到 Core IR，输出 JSON。

```bash
aster core <file> [--json]
```

### `version` - 版本

显示 CLI 版本信息。

```bash
aster version
```

### `help` - 帮助

显示帮助信息。

```bash
aster help
```

## 架构

### 代码结构

```
aster-lang-cli/
├── src/main/java/aster/cli/
│   ├── Main.java              # 入口和命令路由
│   ├── CommandHandler.java    # 命令处理逻辑
│   ├── TypeScriptBridge.java  # TypeScript 编译器桥接
│   ├── DiagnosticFormatter.java  # 诊断信息格式化
│   ├── PathResolver.java      # 路径解析和验证
│   ├── VersionReader.java     # 版本信息读取
│   └── CommandLineParser.java # 参数解析
└── src/test/java/aster/cli/
    ├── TypeScriptBridgeTest.java
    └── MainIntegrationTest.java
```

### 集成方案

Native CLI 通过 subprocess 调用 TypeScript 编译器（`npm run` 命令），复用既有编译管线：

```
Native CLI → TypeScriptBridge → npm run → TypeScript 编译器 → ASM 发射器 → .class 文件
```

**优点**:
- 快速实现，复用成熟代码
- 保持与 TypeScript CLI 的功能一致性
- 便于维护和升级

**长期规划**:
逐步将编译阶段迁移到 Java，最终实现纯 Java 编译管线，完全移除 Node.js 依赖。

### 技术亮点

- **Java 25 特性**: Virtual Threads, Records, Sequenced Collections
- **GraalVM 优化**: `-O3`, `--gc=serial`, `-march=native`
- **职责分离**: 7个独立类，每个类职责单一
- **测试覆盖**: 单元测试 + 集成测试

## 常见问题

### 1. 找不到 node 或 npm

**错误**: `系统错误: 找不到 node 可执行文件`

**解决**:
```bash
# 确保 node 和 npm 在 PATH 中
which node
which npm

# 或设置绝对路径
export PATH=/path/to/node/bin:$PATH
```

### 2. 编译失败

**错误**: `编译失败: file:line:col: error: message`

**解决**:
- 检查源文件语法
- 查看错误信息中的文件位置和错误描述
- 使用 `--json` 选项获取详细诊断信息

### 3. 二进制文件过大

**解决**:
```bash
# 启用 UPX 压缩（需要安装 upx）
upx --best ./aster

# 或使用 PGO 优化
./gradlew :aster-lang-cli:nativeCompile --pgo
```

### 4. 启动速度慢

可能原因：
- 首次运行（操作系统缓存未加载）
- 磁盘 I/O 慢
- 反病毒软件扫描

**解决**:
- 多次运行取平均值
- 排除到白名单
- 使用 SSD

## 测试

```bash
# 运行所有测试
./gradlew :aster-lang-cli:test

# 运行单个测试
./gradlew :aster-lang-cli:test --tests MainIntegrationTest
```

单元测试覆盖参数解析与子进程桥接逻辑，集成测试验证 CLI 对 `cnl/examples` 样例的实际编译效果。

## 链接

- [Aster Lang 主项目](../)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [设计文档](../.claude/native-cli-design.md)
- [完成报告](../.claude/native-cli-completion-report.md)
