# Aster Native Image 详细构建指南

本文档提供 Aster Native Image 的详细编译步骤、高级配置选项和最佳实践。

## 目录

- [环境准备](#环境准备)
- [编译模式](#编译模式)
- [高级配置选项](#高级配置选项)
- [构建优化技巧](#构建优化技巧)
- [CI/CD 集成](#cicd-集成)
- [故障排查](#故障排查)

## 环境准备

### 1. 安装 GraalVM

**方式 1: SDKMAN (推荐 - Linux/macOS)**
```bash
# 安装 SDKMAN
curl -s "https://get.sdkman.io" | bash

# 安装 GraalVM
sdk install java 25-graal

# 设置为默认 JDK
sdk default java 25-graal
```

**方式 2: 手动下载**
1. 访问 [GraalVM Downloads](https://www.graalvm.org/downloads/)
2. 下载 GraalVM 25.0.0 或更高版本
3. 解压并设置环境变量:
   ```bash
   export JAVA_HOME=/path/to/graalvm
   export PATH=$JAVA_HOME/bin:$PATH
   ```

### 2. 安装 Native Image

```bash
# 检查是否已安装
native-image --version

# 如果未安装,使用 gu (GraalVM Updater) 安装
gu install native-image
```

### 3. 平台特定要求

**macOS**:
- Xcode Command Line Tools: `xcode-select --install`
- 确认 C 编译器可用: `cc --version`

**Linux**:
- GCC/G++ 编译器: `sudo apt-get install build-essential` (Debian/Ubuntu)
- Zlib 开发库: `sudo apt-get install zlib1g-dev`

**Windows**:
- Visual Studio 2019 或更高版本 (需要 C++ 工作负载)
- 或使用 Microsoft Visual C++ Build Tools

### 4. 验证环境

```bash
# 验证 Java 版本
java -version
# 输出应包含: Oracle GraalVM 25+37.1

# 验证 native-image
native-image --version
# 输出应包含: native-image 25.0.0

# 验证 C 编译器
cc --version
# macOS: Apple clang 或 GCC
# Linux: GCC
```

## 编译模式

Aster Native Image 支持三种编译模式:

### 模式 1: Baseline (标准编译)

**适用场景**: 快速开发、测试、简单部署

**命令**:
```bash
./gradlew :aster-truffle:nativeCompile --no-configuration-cache
```

**特点**:
- ✅ 编译时间: 2-5 分钟
- ✅ 二进制大小: ~37 MB
- ✅ 启动时间: ~20ms
- ✅ 无需额外步骤

**输出位置**:
```
aster-truffle/build/native/nativeCompile/aster
```

### 模式 2: PGO (Profile-Guided Optimization) - 推荐

**适用场景**: 生产部署、容器化环境、追求最小二进制大小

**步骤**:

**Step 1: Instrumented Build** (编译带性能剖析的版本)
```bash
./gradlew clean
./gradlew :aster-truffle:nativeCompile -PpgoMode=instrument --no-configuration-cache
```
- 编译时间: ~1分16秒
- 二进制大小: ~98 MB (包含剖析代码)

**Step 2: Profile Collection** (收集性能数据)
```bash
# 运行代表性工作负载
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/list_map_1000_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/quicksort_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/factorial_20_core.json

# 其他自定义工作负载...
```
- Profile 数据自动保存到 `default.iprof`
- 文件大小: ~7.2 MB

**Step 3: Optimized Build** (使用 profile 重新编译)
```bash
./gradlew :aster-truffle:nativeCompile \
  -PpgoMode=$(pwd)/default.iprof \
  --no-configuration-cache
```
**重要**: 必须使用绝对路径,因为 Native Image 编译器从项目根目录执行。

**特点**:
- ✅ 编译时间: ~38秒 (Step 3)
- ✅ 二进制大小: **23 MB** (-37.6% vs baseline)
- ✅ 代码区域优化: -74.2% (40.80MB → 10.51MB)
- ✅ 镜像堆优化: -75.6% (56.16MB → 13.70MB)
- ⚠️ 启动时间: ~32ms (略慢于 baseline,但仍 < 50ms 目标)

### 模式 3: PGO + Size Optimization (极限优化)

**适用场景**: 嵌入式设备、极端带宽限制、ROM 空间受限

**命令**:
```bash
./gradlew :aster-truffle:nativeCompile \
  -PpgoMode=$(pwd)/default.iprof \
  -PsizeOptimization=true \
  --no-configuration-cache
```

**特点**:
- ✅ 二进制大小: ~20-21 MB (估计,再减少 5-10%)
- ❌ 调试信息: 已移除 (影响堆栈跟踪)
- ❌ 字符集: 仅包含必需字符集 (可能影响某些字符处理)
- ⚠️ 风险: 中等,仅在极端场景使用

**不推荐原因**:
- 边际收益有限 (23MB → 20MB,仅节省 3MB)
- 移除调试信息影响生产问题诊断
- 字符集限制可能破坏某些功能

## 高级配置选项

### Gradle 参数说明

**1. `-PpgoMode` (Profile-Guided Optimization)**

```bash
# Instrumented build (收集 profile)
-PpgoMode=instrument

# Optimized build (使用 profile)
-PpgoMode=/absolute/path/to/profile.iprof
```

**2. `-PsizeOptimization` (极限大小优化)**

```bash
# 启用额外优化标志
-PsizeOptimization=true
```

启用的优化:
- `-O3`: 最高优化级别 (注: 默认已是 O3)
- `--gc=serial`: Serial GC (注: 默认已是 serial)
- `-H:+StripDebugInfo`: 移除调试信息
- `-H:-AddAllCharsets`: 仅包含必需字符集
- `-H:+RemoveUnusedSymbols`: 移除未使用符号

**3. `--no-configuration-cache`**

禁用 Gradle 配置缓存,确保编译参数正确应用。

**4. `--rerun-tasks`**

强制重新执行所有任务,即使 Gradle 认为它们是最新的。

```bash
./gradlew :aster-truffle:nativeCompile --rerun-tasks
```

### Native Image 高级参数

如需手动添加 Native Image 参数,可以修改 `aster-truffle/build.gradle.kts`:

```kotlin
buildArgs.add("--verbose")              // 详细输出
buildArgs.add("-H:+PrintAnalysisStatistics")  // 打印分析统计
buildArgs.add("-march=native")          // CPU 特定优化 (牺牲可移植性)
buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")  // 运行时报告不支持的元素
```

## 构建优化技巧

### 1. 增量编译

Native Image 支持增量编译,但需要保持 `build/` 目录:

```bash
# 首次编译 (慢)
./gradlew :aster-truffle:nativeCompile

# 仅修改少量代码后 (快)
./gradlew :aster-truffle:nativeCompile
```

### 2. 并行编译

GraalVM Native Image 自动使用多核并行编译:

```bash
# 默认使用 100% 可用处理器核心
# 编译日志显示: 10 thread(s) (100.0% of 10 available processor(s))
```

如需限制核心数:
```bash
-J-XX:ActiveProcessorCount=4  # 使用 4 个核心
```

### 3. 内存调优

默认 Native Image 使用 80% 可用 RAM:

```bash
# 编译日志显示: 22.27GB of memory (32.4% of system memory)
```

如遇内存不足,可以增加 JVM 堆:
```bash
export JAVA_OPTS="-Xmx8g"  # 最大 8GB 堆内存
./gradlew :aster-truffle:nativeCompile
```

### 4. 快速构建模式 (开发)

使用 `-Ob` (quick build mode) 加速开发时的编译:

```kotlin
// 在 build.gradle.kts 中添加
buildArgs.add("-Ob")  // Quick build mode
```

**注意**: 牺牲运行时性能,仅用于开发测试。

## CI/CD 集成

### GitHub Actions 示例

```yaml
name: Build Native Image

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Setup GraalVM
      uses: graalvm/setup-graalvm@v1
      with:
        java-version: '25'
        distribution: 'graalvm'
        github-token: ${{ secrets.GITHUB_TOKEN }}
        native-image-job-reports: 'true'

    - name: Build Native Image (Baseline)
      run: |
        ./gradlew :aster-truffle:nativeCompile --no-configuration-cache

    - name: Test Native Image
      run: |
        ./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json

    - name: Upload Binary
      uses: actions/upload-artifact@v3
      with:
        name: aster-native
        path: aster-truffle/build/native/nativeCompile/aster
```

### Docker 构建示例

```dockerfile
FROM ghcr.io/graalvm/graalvm-ce:ol9-java25

# 安装依赖
RUN microdnf install -y gcc gcc-c++ zlib-devel

# 复制项目
COPY . /app
WORKDIR /app

# 编译 Native Image
RUN ./gradlew :aster-truffle:nativeCompile --no-configuration-cache

# 最小化最终镜像
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.0
COPY --from=0 /app/aster-truffle/build/native/nativeCompile/aster /usr/local/bin/aster
ENTRYPOINT ["aster"]
```

## 故障排查

### 常见问题

**1. `ClassNotFoundException` 或 `NoSuchMethodException`**

**原因**: 反射配置不完整

**解决方法**:
- 检查 `META-INF/native-image/reflect-config.json`
- 使用 Native Image Agent 重新生成配置:
  ```bash
  java -agentlib:native-image-agent=config-output-dir=META-INF/native-image \
    -jar aster-truffle.jar your-program.aster
  ```

**2. 资源文件找不到**

**原因**: 资源配置缺失

**解决方法**:
- 检查 `META-INF/native-image/resource-config.json`
- 确保资源文件在 `resources/` 目录下

**3. 编译时间过长 (> 10分钟)**

**可能原因**:
- 内存不足导致 GC 频繁
- CPU 性能限制

**解决方法**:
```bash
# 检查编译日志中的 GC 时间
# 正常: 2.6s (6.8% of total time) in 546 GCs
# 异常: > 20% 时间在 GC

# 增加堆内存
export JAVA_OPTS="-Xmx16g"
```

**4. Profile 文件路径错误**

**错误信息**:
```
Error: Cannot load profile default.iprof (reason: java.io.FileNotFoundException)
```

**解决方法**:
```bash
# 使用绝对路径
-PpgoMode=$(pwd)/default.iprof

# 或
-PpgoMode=/Users/your-username/project/default.iprof
```

**5. 编译失败: "Unsupported features"**

**原因**: 使用了 Native Image 不支持的 Java 特性

**解决方法**:
- 检查错误日志中的不支持特性
- 添加 fallback 或替代实现
- 使用 `-H:+ReportUnsupportedElementsAtRuntime` 推迟到运行时报告

更多故障排查信息,请查看 [troubleshooting.md](troubleshooting.md)。

## 性能基准

### 编译时间对比

| 模式 | 编译时间 | 增量编译 |
|------|---------|---------|
| Baseline | 2-5 分钟 | ~1 分钟 |
| PGO Instrumented | ~1分16秒 | ~40秒 |
| PGO Optimized | ~38秒 | ~20秒 |

### 二进制大小对比

| 模式 | 大小 | vs Baseline |
|------|------|------------|
| Baseline | 36.88 MB | - |
| PGO Instrumented | 97.99 MB | +165% |
| PGO Optimized | **23 MB** | **-37.6%** |
| PGO + Size Opt | ~20-21 MB (估计) | ~-43% |

### 启动时间对比

| 模式 | 启动时间 | vs JVM |
|------|---------|--------|
| JVM | 5-10s | - |
| Native Baseline | ~20ms | **250-500x** |
| Native PGO | ~32ms | **156-312x** |

## 最佳实践

### 1. 选择合适的编译模式

- **开发/测试**: Baseline (快速编译)
- **生产部署**: PGO (最小二进制,降低带宽和存储成本)
- **极端场景**: PGO + Size Opt (嵌入式设备,慎用)

### 2. Profile 数据管理

- 使用**真实生产负载**收集 profile (而非测试用例)
- 定期更新 profile 数据 (每季度或主要功能变更后)
- 保存 profile 文件到版本控制 (可复现构建)

### 3. CI/CD 构建策略

- **开发分支**: Baseline (快速反馈)
- **主分支/发布**: PGO (生产质量)
- **缓存 Gradle 和 Native Image 构建缓存**: 加速 CI

### 4. 监控编译指标

关键指标:
- 编译时间: 应 < 5 分钟 (Baseline) 或 < 2 分钟 (PGO)
- 二进制大小: 应 < 50 MB
- Peak RSS: 应 < 5 GB

### 5. 版本控制

提交到版本控制的文件:
- ✅ `META-INF/native-image/*.json` (配置文件)
- ✅ `default.iprof` (如果稳定)
- ❌ `build/` 目录
- ❌ 编译后的二进制文件

## 参考资料

- [GraalVM Native Image 官方文档](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Profile-Guided Optimization 指南](https://www.graalvm.org/latest/reference-manual/native-image/optimizations-and-performance/PGO/)
- [Truffle 语言实现框架](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)

## 下一步

- **性能对比**: 查看 [performance-comparison.md](performance-comparison.md)
- **故障排查**: 查看 [troubleshooting.md](troubleshooting.md)
- **限制说明**: 查看 [limitations.md](limitations.md)
