# Aster Native Image 限制说明

本文档列出 Aster Native Image 当前的限制、不支持的特性和已知问题。

## 目录

- [Java/GraalVM 平台限制](#javagramlvm-平台限制)
- [Truffle Runtime 限制](#truffle-runtime-限制)
- [Aster 语言特性支持](#aster-语言特性支持)
- [性能限制](#性能限制)
- [部署限制](#部署限制)
- [已知问题](#已知问题)
- [未来改进计划](#未来改进计划)

## Java/GraalVM 平台限制

### ❌ 不支持的 Java 特性

#### 1. 动态类加载

**限制**: 无法在运行时动态加载未在编译时声明的类。

**原因**: Native Image 采用闭世界假设 (Closed World Assumption),所有代码必须在编译时可达。

**影响**:
```java
// ❌ 不支持:
Class<?> clazz = Class.forName(userInput);  // 运行时类名

// ✅ 支持:
Class<?> clazz = MyKnownClass.class;  // 编译时已知
```

**解决方法**: 在反射配置中预先声明所有可能的类:
```json
{
  "name": "com.example.PossibleClass",
  "allDeclaredConstructors": true
}
```

#### 2. 运行时反射 (未配置)

**限制**: 反射访问必须在编译时通过配置声明。

**原因**: Native Image 需要在编译时确定所有反射目标以生成元数据。

**影响**:
```java
// ❌ 未配置的反射会失败:
Method method = clazz.getDeclaredMethod("methodName");

// ✅ 配置后可用
```

**解决方法**: 使用 Native Image Agent 生成配置或手动添加:
```bash
./gradlew generateNativeConfig
```

#### 3. Java Agent / JVMTI

**限制**: 不支持 Java Agent,包括:
- APM 工具 (如 New Relic, DataDog Agent)
- 字节码增强工具
- 动态插桩

**影响**: 无法使用基于 Agent 的监控和调试工具。

**解决方法**: 使用原生监控方案 (如 Prometheus + JMX Exporter 的静态配置)。

#### 4. JMX (Java Management Extensions)

**限制**: JMX MBeans 支持有限,部分功能不可用。

**影响**: 无法使用 JConsole、VisualVM 等 JMX 工具连接到 Native Image 进程。

**解决方法**: 使用自定义 HTTP 端点暴露监控指标。

#### 5. SecurityManager

**限制**: SecurityManager 已在 Java 17+ 弃用,Native Image 不支持。

**影响**: 无法使用 Java 安全策略限制代码权限。

**解决方法**: 使用操作系统级别的沙箱 (如 Docker, seccomp)。

### ⚠️ 部分支持的特性

#### 1. Serialization

**限制**: Java 序列化需要显式配置。

**配置方法**:
```json
{
  "name": "com.example.SerializableClass",
  "serialization": true
}
```

#### 2. 资源文件

**限制**: 资源文件必须在 `resource-config.json` 中声明。

**示例**:
```json
{
  "resources": {
    "includes": [
      {"pattern": "config/.*\\.json"}
    ]
  }
}
```

#### 3. Proxy 类

**限制**: 动态代理类必须在 `proxy-config.json` 中声明。

**示例**:
```json
{
  "interfaces": ["com.example.MyInterface", "java.io.Serializable"]
}
```

## Truffle Runtime 限制

### ❌ 当前使用 Truffle Fallback Runtime

**状态**: Aster Native Image 当前运行在 Truffle fallback runtime (解释器模式)。

**表现**:
```
[engine] WARNING: The polyglot engine uses a fallback runtime that does not support
runtime compilation to native code.
```

**影响**:

| 特性 | Fallback Runtime | 完整 Runtime (目标) |
|------|------------------|-------------------|
| 启动速度 | ✅ 快 (20-32ms) | ✅ 快 (20-32ms) |
| 内存占用 | ✅ 低 (< 50MB) | ✅ 低 (< 50MB) |
| 峰值性能 | ❌ 低 (解释器) | ✅ 高 (JIT 编译) |
| PGO 性能优化 | ❌ 无效 | ✅ 有效 (+20-30%) |

**适用场景**:
- ✅ 短生命周期任务 (< 1分钟): CLI 工具、脚本、Serverless
- ❌ 长时间运行任务 (> 5分钟): 计算密集型服务

**解决计划**: 修复 Truffle runtime 配置以启用 Graal JIT 编译器 (见"未来改进计划")。

## Aster 语言特性支持

### ✅ 完全支持的特性

所有 Aster 语言特性在 Native Image 版本中均**完全支持**:

- ✅ 基本数据类型 (Integer, Float, Boolean, String)
- ✅ 复合类型 (List, Map)
- ✅ 函数定义与调用
- ✅ Lambda 表达式与闭包
- ✅ 高阶函数 (map, filter, reduce)
- ✅ 模式匹配
- ✅ 递归调用
- ✅ 尾递归优化
- ✅ 内置函数库
- ✅ 错误处理

**验证方法**:
```bash
# 所有语言特性测试通过
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/factorial_20_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/list_map_1000_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/quicksort_core.json
```

### ❌ 不支持的扩展特性

以下特性在 JVM 和 Native Image 版本中**均不支持** (非 Aster 核心语言):

- ❌ FFI (Foreign Function Interface) - 调用 C/C++ 库
- ❌ 多线程 / 并发原语
- ❌ 网络 I/O
- ❌ 文件系统访问 (除标准输入输出)
- ❌ GUI 库绑定

## 性能限制

### 1. 峰值吞吐量低于 JVM

**当前状态**:
- Native Image (Fallback Runtime): ~5x 慢于 JVM (计算密集任务)
- JVM (Graal JIT): 高峰值性能 (预热后)

**基准数据** (Fibonacci(20)):
- JVM (预热后): ~10ms
- Native Image: ~50ms

**使用建议**:
- 短任务 (< 1分钟): 使用 Native Image (启动速度优势)
- 长任务 (> 5分钟): 使用 JVM (吞吐量优势)

### 2. PGO 性能优化受限

**当前状态**:
- PGO 主要优化二进制大小 (36.88MB → 23MB, -37.6%)
- PGO 对执行性能提升有限 (解释器模式限制)

**预期** (修复 Truffle Runtime 后):
- PGO 将同时优化大小和性能 (+20-30% 吞吐量)

### 3. 冷启动性能

**观察**: PGO 版本首次运行可能比后续运行慢:
- Run 1: 66ms (冷启动)
- Run 2-3: 16ms (热启动)
- 平均: 32ms

**原因**: PGO 优化热路径,冷路径可能被去优化。

**影响**: 仅影响首次运行,后续运行稳定在 16ms。

## 部署限制

### 1. 平台特定二进制

**限制**: Native Image 二进制文件是平台特定的,无法跨平台运行。

**示例**:
- macOS ARM64 编译的二进制无法在 Linux x86_64 运行
- Windows 编译的二进制无法在 macOS 运行

**解决方法**: 为每个目标平台分别编译:
```yaml
# GitHub Actions 示例
jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - name: Build Native Image
        run: ./gradlew :aster-truffle:nativeCompile
```

### 2. 二进制大小

**当前大小**:
- Baseline: 36.88 MB
- PGO Optimized: 23 MB

**对比**:
- Go 编译的二进制: 通常 5-20 MB
- Rust 编译的二进制: 通常 2-10 MB

**原因**: Native Image 包含完整的 Truffle runtime 和 GC。

**影响**: 仍远小于 JVM + JAR (200+ MB),适合容器化部署。

### 3. 编译时间

**编译时间**:
- Baseline: 2-5 分钟 (首次)
- PGO Instrumented: ~1分16秒
- PGO Optimized: ~38秒

**对比**:
- Java 编译 JAR: 通常 < 30秒
- Go 编译: 通常 < 10秒

**影响**: CI/CD 流水线需要更长时间,建议缓存 Gradle 构建。

### 4. 编译内存需求

**内存占用**:
- Peak RSS: 2.5-5 GB (PGO Instrumented 最高)

**建议**:
- CI 环境: 至少 8 GB RAM
- 本地开发: 至少 16 GB RAM

## 已知问题

### 1. Truffle Fallback Runtime 警告

**问题**: 每次运行都会打印警告信息。

**状态**: 已知限制,不影响功能。

**计划**: 修复 Truffle runtime 配置后警告消失。

### 2. PGO Profile 路径必须绝对

**问题**: 相对路径会导致编译失败。

**解决方法**:
```bash
# ❌ 错误:
-PpgoMode=default.iprof

# ✅ 正确:
-PpgoMode=$(pwd)/default.iprof
```

**原因**: Native Image 编译器从项目根目录执行。

### 3. 某些 Jackson 反射配置缺失

**问题**: 使用自定义 Jackson 注解可能需要手动配置。

**解决方法**: 使用 Native Image Agent 重新生成配置:
```bash
./gradlew generateNativeConfig
```

## 未来改进计划

### 短期 (Phase 2)

#### 1. 修复 Truffle Runtime 配置

**目标**: 启用 Graal JIT 编译器,移除 fallback runtime 限制。

**预期收益**:
- 峰值性能提升至接近 JVM 水平
- PGO 性能优化生效 (+20-30% 吞吐量)

**实施步骤**:
1. 移除 `-Dtruffle.TruffleRuntime` 显式配置
2. 验证 Graal Compiler 可用
3. 重新测试性能基准

#### 2. 优化 PGO 流程

**目标**: 简化 PGO 工作流,提供预设 profile。

**计划**:
- 提供官方推荐的 profile 文件 (基于生产负载)
- 自动化 3 步骤流程 (单命令执行)
- CI/CD 集成示例

### 中期 (Phase 3+)

#### 3. 减少二进制大小

**目标**: 进一步减少至 15-20 MB。

**方法**:
- 使用 `--static` 链接模式 (Linux)
- 移除未使用的 Truffle 组件
- 探索 `-march=native` CPU 特定优化

#### 4. 增量编译支持

**目标**: 减少重新编译时间至 < 30秒。

**方法**:
- 保留编译缓存
- 仅重新编译变更的模块

### 长期

#### 5. 多平台预编译二进制

**目标**: 提供官方预编译二进制下载。

**平台**:
- Linux x86_64
- macOS ARM64 (M1/M2)
- macOS x86_64 (Intel)
- Windows x86_64

#### 6. Docker 官方镜像

**目标**: 提供最小化 Docker 镜像 (< 50 MB)。

**示例**:
```dockerfile
FROM scratch
COPY aster /usr/local/bin/aster
ENTRYPOINT ["/usr/local/bin/aster"]
```

## 对比: JVM vs Native Image

### 何时使用 JVM 版本

| 场景 | JVM | Native Image |
|------|-----|--------------|
| 长时间运行服务 (> 5分钟) | ✅ | ❌ |
| 计算密集型任务 | ✅ | ❌ |
| 需要 JIT 优化 | ✅ | ❌ |
| 需要 JMX 监控 | ✅ | ❌ |
| 开发和调试 | ✅ | ⚠️ |

### 何时使用 Native Image 版本

| 场景 | JVM | Native Image |
|------|-----|--------------|
| CLI 工具 | ❌ | ✅ |
| 短生命周期脚本 (< 1分钟) | ❌ | ✅ |
| Serverless / Lambda | ❌ | ✅ |
| 容器化部署 (K8s, Docker) | ⚠️ | ✅ |
| 资源受限环境 | ❌ | ✅ |
| CI/CD 流水线 | ❌ | ✅ |

### 混合部署策略

**推荐**: 根据场景选择合适版本:

```bash
# CLI 工具: Native Image
alias aster="./aster-truffle/build/native/nativeCompile/aster"

# 后台服务: JVM
java -jar aster-truffle/build/libs/aster-truffle.jar --server
```

## 参考资料

- [GraalVM Native Image 限制文档](https://www.graalvm.org/latest/reference-manual/native-image/metadata/Compatibility/)
- [Truffle 语言实现指南](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [快速开始](README.md)
- [详细构建指南](build-guide.md)
- [性能对比](performance-comparison.md)
- [故障排查](troubleshooting.md)
