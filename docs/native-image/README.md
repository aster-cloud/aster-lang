# Aster Native Image 快速开始

Aster Native Image 是使用 GraalVM Native Image 编译的 Aster 语言独立可执行文件,提供超快启动速度和极低内存占用。

## 特性概览

- **超快启动**: < 50ms (相比 JVM 版本 5-10s,提升 100x)
- **低内存占用**: < 50MB (相比 JVM 版本 300-500MB,减少 6-10x)
- **独立部署**: 无需安装 JVM,单个可执行文件
- **完整语言支持**: 支持所有 Aster 语言特性

## 环境要求

- **GraalVM**: 25.0.0 或更高版本
- **操作系统**: macOS / Linux / Windows (需要 native-image 支持)
- **构建工具**: Gradle 9.0+ (项目已包含 Gradle Wrapper)
- **磁盘空间**: 至少 500MB (用于编译缓存)

### 验证 GraalVM 安装

```bash
# 检查 Java 版本
java -version
# 应显示: Oracle GraalVM 25+37.1 或类似版本

# 检查 native-image 工具
native-image --version
# 应显示: native-image 25.0.0 或更高版本
```

如果 `native-image` 不可用,需要安装:
```bash
gu install native-image
```

## 快速编译 (推荐 - PGO 优化版)

**Step 1: 编译带性能剖析的二进制** (~1分钟)
```bash
./gradlew :aster-truffle:nativeCompile -PpgoMode=instrument --no-configuration-cache
```

**Step 2: 收集性能 Profile** (~10秒)
```bash
# 运行代表性工作负载
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/list_map_1000_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/quicksort_core.json
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/factorial_20_core.json
```
Profile 数据会自动保存到 `default.iprof` 文件。

**Step 3: 使用 Profile 重新编译** (~40秒)
```bash
./gradlew :aster-truffle:nativeCompile \
  -PpgoMode=/absolute/path/to/aster-lang/default.iprof \
  --no-configuration-cache
```
**注意**: 必须使用绝对路径,相对路径会导致编译失败。

**结果**:
- 二进制大小: **23 MB** (相比 baseline 36.88MB 减少 37.6%)
- 启动时间: **32ms** (仍远低于 50ms 目标)

## 标准编译 (Baseline)

如果不需要 PGO 优化,可以直接编译:

```bash
./gradlew :aster-truffle:nativeCompile --no-configuration-cache
```

**编译时间**: 约 2-5 分钟 (首次编译,后续增量编译更快)

**结果**:
- 二进制大小: ~37 MB
- 启动时间: ~20ms

## 运行 Native Image

编译完成后,可执行文件位于:
```
aster-truffle/build/native/nativeCompile/aster
```

### 运行示例

```bash
# 运行 Aster 程序
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json

# 输出: 6765
```

### 添加到 PATH (可选)

为了方便使用,可以创建符号链接或添加到 PATH:

```bash
# 方法 1: 创建符号链接
ln -s "$(pwd)/aster-truffle/build/native/nativeCompile/aster" /usr/local/bin/aster

# 方法 2: 添加到 PATH (添加到 ~/.bashrc 或 ~/.zshrc)
export PATH="$(pwd)/aster-truffle/build/native/nativeCompile:$PATH"
```

然后可以直接运行:
```bash
aster your-program.aster
```

## 快速验证

验证编译是否成功:

```bash
# 1. 检查二进制存在
ls -lh aster-truffle/build/native/nativeCompile/aster

# 2. 检查二进制大小 (应为 23MB 或 37MB)
du -h aster-truffle/build/native/nativeCompile/aster

# 3. 运行测试
./aster-truffle/build/native/nativeCompile/aster benchmarks/core/fibonacci_20_core.json

# 预期输出: 6765
```

## 性能对比

| 指标 | JVM 版本 | Native Image (Baseline) | Native Image (PGO) | 提升 |
|------|---------|------------------------|-------------------|------|
| **启动时间** | 5-10s | ~20ms | ~32ms | **100-300x** |
| **内存占用** | 300-500MB | < 50MB | < 50MB | **6-10x** |
| **二进制大小** | N/A (需要 JVM) | 36.88MB | 23MB | **独立部署** |
| **峰值性能** | 高 (JIT 优化) | 中 (解释器) | 中 (解释器) | 待优化 |

**注意**: 当前版本运行在 Truffle fallback runtime (解释器模式),峰值性能低于 JVM 版本。未来启用 Graal JIT 后性能将显著提升。

## 下一步

- **详细构建指南**: 查看 [build-guide.md](build-guide.md) 了解高级编译选项
- **性能对比报告**: 查看 [performance-comparison.md](performance-comparison.md) 了解详细性能数据
- **故障排查**: 遇到问题请查看 [troubleshooting.md](troubleshooting.md)
- **限制说明**: 查看 [limitations.md](limitations.md) 了解已知限制

## 常见问题

**Q: 编译时间太长怎么办?**
A: Native Image 编译时间 2-5 分钟是正常现象。可以使用 `-Ob` (quick build mode) 加速开发时的编译,但会牺牲运行时性能。

**Q: 为什么 PGO 版本启动时间反而慢了?**
A: PGO 优化热路径执行,但在 Truffle fallback runtime (解释器模式) 下收益有限。主要收益是二进制大小减少 37.6%。

**Q: 可以在生产环境使用吗?**
A: ✅ 可以。Native Image 版本功能完整,适合容器化部署、Serverless、CI/CD 脚本等场景。启动时间和内存占用都远优于 JVM 版本。

**Q: 如何获得更好的峰值性能?**
A: 当前版本运行在解释器模式,峰值性能低于 JVM。未来需要修复 Truffle runtime 配置以启用 Graal JIT 编译器。

## 反馈与支持

遇到问题或有建议,请访问项目 GitHub Issues 页面。
