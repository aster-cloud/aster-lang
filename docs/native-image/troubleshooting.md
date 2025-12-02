# Aster Native Image 故障排查指南

本文档提供 Aster Native Image 常见问题的诊断和解决方案。

## 目录

- [编译问题](#编译问题)
- [运行时问题](#运行时问题)
- [性能问题](#性能问题)
- [PGO 相关问题](#pgo-相关问题)
- [调试技巧](#调试技巧)

## 编译问题

### 1. ClassNotFoundException 或 NoSuchMethodException

**症状**:
```
Error: Class com.example.SomeClass not found
或
Error: Method 'methodName' not found
```

**原因**: 反射配置不完整,Native Image 在构建时无法发现通过反射访问的类或方法。

**解决方法**:

**步骤 1**: 检查反射配置文件
```bash
cat aster-truffle/src/main/resources/META-INF/native-image/reflect-config.json
```

**步骤 2**: 使用 Native Image Agent 重新生成配置
```bash
# 运行代表性工作负载以收集元数据
./gradlew generateNativeConfig
```

这会自动执行:
```bash
java -agentlib:native-image-agent=config-output-dir=aster-truffle/src/main/resources/META-INF/native-image \
  -jar aster-truffle/build/libs/aster-truffle.jar \
  benchmarks/core/fibonacci_20_core.json \
  benchmarks/core/factorial_12_core.json \
  benchmarks/core/list_map_1000_core.json
```

**步骤 3**: 手动添加缺失的类 (如果 Agent 未检测到)
```json
{
  "name": "com.example.SomeClass",
  "allDeclaredConstructors": true,
  "allPublicConstructors": true,
  "allDeclaredMethods": true,
  "allPublicMethods": true,
  "allDeclaredFields": true,
  "allPublicFields": true
}
```

**步骤 4**: 重新编译
```bash
./gradlew :aster-truffle:nativeCompile --no-configuration-cache
```

### 2. 资源文件找不到

**症状**:
```
Error: Resource 'config/settings.json' not found
或
java.io.FileNotFoundException: /path/to/resource
```

**原因**: 资源配置缺失,Native Image 默认不包含所有资源文件。

**解决方法**:

**步骤 1**: 检查资源配置
```bash
cat aster-truffle/src/main/resources/META-INF/native-image/resource-config.json
```

**步骤 2**: 添加缺失的资源模式
```json
{
  "resources": {
    "includes": [
      {"pattern": "config/.*\\.json"},
      {"pattern": "templates/.*\\.aster"},
      {"pattern": "META-INF/.*"}
    ]
  }
}
```

**步骤 3**: 确保资源在 `src/main/resources/` 目录下
```bash
ls -R aster-truffle/src/main/resources/
```

**步骤 4**: 重新编译并验证
```bash
./gradlew :aster-truffle:nativeCompile --no-configuration-cache
./aster-truffle/build/native/nativeCompile/aster your-test.json
```

### 3. 编译时间过长 (> 10分钟)

**症状**: 编译超过 10 分钟仍未完成。

**正常编译时间**:
- Baseline: 2-5 分钟
- PGO Instrumented: 1-2 分钟
- PGO Optimized: < 1 分钟

**可能原因**:

**原因 1: 内存不足导致 GC 频繁**

检查编译日志中的 GC 时间:
```bash
# 正常: GC 时间 < 10% 总时间
# 示例: 2.6s (6.8% of total time) in 546 GCs

# 异常: GC 时间 > 20% 总时间
# 示例: 45.2s (35.4% of total time) in 2341 GCs
```

**解决方法**: 增加 JVM 堆内存
```bash
export JAVA_OPTS="-Xmx16g"
./gradlew :aster-truffle:nativeCompile --no-configuration-cache
```

**原因 2: CPU 性能限制**

Native Image 编译默认使用所有可用 CPU 核心:
```bash
# 编译日志显示:
# 10 thread(s) (100.0% of 10 available processor(s))
```

**解决方法**: 限制核心数以释放资源
```bash
# 在 build.gradle.kts 中添加:
buildArgs.add("-J-XX:ActiveProcessorCount=4")
```

**原因 3: 磁盘 I/O 瓶颈**

**解决方法**: 将 `build/` 目录移至 SSD
```bash
# 创建符号链接到 SSD
mkdir /path/to/ssd/aster-build
ln -s /path/to/ssd/aster-build aster-truffle/build
```

### 4. 编译失败: "Unsupported features"

**症状**:
```
Error: Unsupported features in 3 methods
Detailed message:
Error: Detected a MBean server in the image heap...
```

**原因**: 使用了 Native Image 不支持的 Java 特性。

**常见不支持特性**:
- 动态类加载 (`Class.forName` 未配置)
- JMX / MBeans
- JVMTI / Agent
- InvokeDynamic (部分场景)

**解决方法**:

**步骤 1**: 检查错误日志获取具体类和方法
```bash
# 查看完整错误信息
tail -100 /tmp/native-image-*.log
```

**步骤 2**: 选择解决策略

**策略 A: 添加到反射配置** (如果是反射问题)
```json
{
  "name": "com.example.ProblematicClass",
  "allDeclaredConstructors": true
}
```

**策略 B: 推迟到运行时报告** (如果特性不影响核心功能)
```kotlin
// 在 build.gradle.kts 中添加:
buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
```

**策略 C: 移除或替换不支持的代码**
```java
// 避免:
Class<?> clazz = Class.forName(dynamicClassName);

// 改为:
Class<?> clazz = KnownClass.class;
```

### 5. PGO Profile 文件路径错误

**症状**:
```
Error: Cannot load profile default.iprof
(reason: java.io.FileNotFoundException: default.iprof (No such file or directory))
```

**原因**: 使用了相对路径,但 Native Image 编译器从项目根目录执行。

**解决方法**:

**使用绝对路径**:
```bash
# 错误:
./gradlew :aster-truffle:nativeCompile -PpgoMode=default.iprof

# 正确:
./gradlew :aster-truffle:nativeCompile \
  -PpgoMode=/Users/your-username/aster-lang/default.iprof

# 或使用 $(pwd):
./gradlew :aster-truffle:nativeCompile \
  -PpgoMode=$(pwd)/default.iprof
```

**验证文件存在**:
```bash
ls -lh default.iprof
# 应显示: -rw-r--r--  7.2M  default.iprof
```

## 运行时问题

### 6. Truffle Fallback Runtime 警告

**症状**:
```
[engine] WARNING: The polyglot engine uses a fallback runtime that does not support
runtime compilation to native code.
The fallback runtime was explicitly selected using the -Dtruffle.TruffleRuntime option.
```

**影响**:
- ✅ 程序可以正常运行
- ❌ 峰值性能低于 JVM (解释器 vs JIT)
- ❌ PGO 无法优化执行性能

**当前状态**: 已知限制,需要修复 Truffle runtime 配置。

**临时解决方法**: 无需处理,功能完整,仅影响长时间运行的计算密集任务。

**长期解决方法** (待实施):
1. 移除显式 `-Dtruffle.TruffleRuntime` 配置
2. 确保 Graal Compiler 可用
3. 重新编译并验证性能提升

### 7. 启动时崩溃: Segmentation Fault

**症状**:
```
Segmentation fault (core dumped)
或
Bus error
```

**可能原因**:

**原因 1: 静态初始化器问题**

某些类在 build-time 初始化,但依赖 runtime 状态。

**解决方法**: 调整初始化时机
```kotlin
// 在 build.gradle.kts 中修改:
buildArgs.add("--initialize-at-build-time=")  // 默认 runtime 初始化
buildArgs.add("--initialize-at-build-time=com.safe.package")  // 仅安全的包
```

**原因 2: 本地库 (JNI) 不兼容**

**解决方法**: 检查是否使用了本地库
```bash
# 查找 JNI 依赖
grep -r "System.loadLibrary" aster-truffle/src/
```

### 8. OutOfMemoryError 在运行时

**症状**:
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**原因**: Native Image 堆内存配置不足 (默认自动调整,但某些场景可能不足)。

**解决方法**:

**步骤 1**: 检查当前内存使用
```bash
/usr/bin/time -l ./aster-truffle/build/native/nativeCompile/aster your-program.json
# 查看: maximum resident set size
```

**步骤 2**: 增加堆内存 (如果确实需要)
```bash
# Native Image 运行时参数
./aster-truffle/build/native/nativeCompile/aster \
  -XX:MaxHeapSize=512m \
  your-program.json
```

**步骤 3**: 优化程序避免内存泄漏
```bash
# 使用 profiler 分析
./aster-truffle/build/native/nativeCompile/aster \
  -Daster.profiler.enabled=true \
  your-program.json
```

## 性能问题

### 9. 启动时间超过 50ms

**症状**: 启动时间 > 50ms,不符合预期。

**正常启动时间**:
- Baseline: ~20ms
- PGO: ~32ms (第一次运行可能 ~66ms)

**诊断步骤**:

**步骤 1**: 多次测试排除系统开销
```bash
for i in {1..5}; do
  time ./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json
done
```

**步骤 2**: 检查文件 I/O
```bash
# 使用 strace (Linux) 或 dtruss (macOS) 分析系统调用
strace -c ./aster-truffle/build/native/nativeCompile/aster your-program.json
```

**步骤 3**: 检查是否有静态初始化器
```bash
# 在编译日志中查找:
grep "clinit" /tmp/native-image-*.log
```

**解决方法**: 移动耗时初始化到懒加载。

### 10. 峰值性能低于 JVM

**症状**: 长时间运行的计算任务比 JVM 慢 5-10x。

**原因**: 当前运行在 Truffle fallback runtime (解释器模式)。

**预期行为**:
- Native Image 优势: 启动速度、内存占用
- JVM 优势: 峰值吞吐量 (JIT 优化)

**适用场景**:
- ✅ Native Image: 短生命周期任务 (< 1分钟)
- ✅ JVM: 长时间运行任务 (> 5分钟)

**解决方法**: 根据场景选择合适版本
```bash
# 短任务: 使用 Native Image
./aster-truffle/build/native/nativeCompile/aster script.json

# 长任务: 使用 JVM
java -jar aster-truffle/build/libs/aster-truffle.jar script.json
```

## PGO 相关问题

### 11. PGO Profile 数据不完整

**症状**: PGO 优化效果不明显,或二进制大小没有显著减少。

**原因**: Profile 收集时未覆盖关键代码路径。

**解决方法**:

**步骤 1**: 使用生产负载收集 Profile
```bash
# 不仅仅是基准测试
./aster-truffle/build/native/nativeCompile/aster real-world-workload-1.json
./aster-truffle/build/native/nativeCompile/aster real-world-workload-2.json
./aster-truffle/build/native/nativeCompile/aster real-world-workload-3.json
```

**步骤 2**: 检查 Profile 文件大小
```bash
ls -lh default.iprof
# 应为 5-10 MB,如果 < 1MB 说明数据不足
```

**步骤 3**: 合并多个 Profile
```bash
# GraalVM Native Image 自动合并同名 .iprof 文件
# 只需多次运行 instrumented 版本
```

### 12. PGO 编译后性能反而下降

**症状**: PGO 优化版本比 baseline 慢。

**可能原因**:

**原因 1: Profile 数据不匹配实际工作负载**

PGO 基于 profile 优化热路径,但如果实际工作负载不同,可能误优化。

**解决方法**: 使用匹配的 profile 数据
```bash
# 删除旧 profile
rm default.iprof

# 使用实际工作负载重新收集
./gradlew :aster-truffle:nativeCompile -PpgoMode=instrument --no-configuration-cache
./aster-truffle/build/native/nativeCompile/aster actual-workload.json
./gradlew :aster-truffle:nativeCompile -PpgoMode=$(pwd)/default.iprof --no-configuration-cache
```

**原因 2: Truffle fallback runtime 限制**

当前 PGO 主要优化二进制大小,性能优化受限于解释器模式。

**解决方法**: 接受当前限制,等待 Truffle runtime 修复。

## 调试技巧

### 启用详细日志

**编译时详细输出**:
```kotlin
// 在 build.gradle.kts 中添加:
buildArgs.add("--verbose")
buildArgs.add("-H:+PrintAnalysisStatistics")
buildArgs.add("-H:+PrintImageObjectTree")
```

**运行时调试**:
```bash
# Truffle 日志
./aster-truffle/build/native/nativeCompile/aster \
  --log.level=FINE \
  your-program.json

# GC 日志
./aster-truffle/build/native/nativeCompile/aster \
  -XX:+PrintGC \
  -XX:+VerboseGC \
  your-program.json
```

### 生成诊断报告

**Build Report**:
```kotlin
// 在 build.gradle.kts 中添加:
buildArgs.add("-H:+BuildReport")
buildArgs.add("-H:BuildReportFile=/tmp/build-report.html")
```

查看报告:
```bash
open /tmp/build-report.html
```

### 使用 GDB 调试 (Linux)

```bash
# 编译带调试信息的版本
./gradlew :aster-truffle:nativeCompile \
  -PbuildArgs="-g" \
  --no-configuration-cache

# 使用 GDB
gdb ./aster-truffle/build/native/nativeCompile/aster
(gdb) run benchmarks/core/fibonacci_20_core.json
(gdb) bt  # 查看堆栈
```

### 使用 LLDB 调试 (macOS)

```bash
lldb ./aster-truffle/build/native/nativeCompile/aster
(lldb) run benchmarks/core/fibonacci_20_core.json
(lldb) bt  # 查看堆栈
```

## 获取帮助

如果以上方法无法解决问题:

1. **检查 GraalVM 官方文档**: https://www.graalvm.org/latest/reference-manual/native-image/
2. **查看编译日志**: `/tmp/native-image-*.log`
3. **提交 Issue**: 包含完整错误信息、编译日志和复现步骤
4. **GraalVM Slack**: https://graalvm.slack.com/ (#native-image 频道)

## 相关文档

- [快速开始](README.md)
- [详细构建指南](build-guide.md)
- [性能对比](performance-comparison.md)
- [限制说明](limitations.md)
