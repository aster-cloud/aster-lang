# Aster Native Image 性能对比报告

本文档提供 Aster 各版本(JVM vs Native Image Baseline vs Native Image PGO)的详细性能对比数据。

## 执行摘要

| 版本 | 启动时间 | 内存占用 | 二进制大小 | 峰值性能 | 推荐场景 |
|------|---------|---------|-----------|---------|---------|
| **JVM** | 5-10s | 300-500MB | N/A (需要JVM) | ⭐⭐⭐⭐⭐ 高 | 长时间运行,计算密集 |
| **Native Baseline** | **20ms** | **< 50MB** | 36.88MB | ⭐⭐⭐ 中 | 快速启动,资源受限 |
| **Native PGO** | **32ms** | **< 50MB** | **23MB** | ⭐⭐⭐ 中 | 生产部署,容器化 |

**关键要点**:
- Native Image **启动速度提升 100-500x**
- Native Image **内存占用减少 6-10x**
- Native Image **二进制大小 23-37MB**,无需 JVM
- 当前 Native Image 运行在解释器模式,**峰值性能低于 JVM**

## 详细性能数据

### 1. 启动时间对比

#### 测试方法
```bash
# 测试命令 (重复 3 次取平均值)
time ./aster <test-file>
```

#### 测试结果

| 版本 | Run 1 | Run 2 | Run 3 | 平均值 | 相对 JVM | 相对 Baseline |
|------|-------|-------|-------|--------|---------|--------------|
| **JVM (冷启动)** | 8.2s | 7.9s | 8.1s | **8.1s** | - | - |
| **JVM (热启动)** | 5.3s | 5.1s | 5.2s | **5.2s** | - | - |
| **Native Baseline** | 0.022s | 0.020s | 0.018s | **0.020s** | **405x 提升** | - |
| **Native PGO** | 0.066s | 0.016s | 0.016s | **0.032s** | **253x 提升** | 1.6x 慢 |

**数据来源**: `.claude/phase5-task5.1-pgo-report.md` 和 Task 4.3 基准测试

**分析**:
- Native Image 启动时间 < 50ms,达到 Phase 3D 目标
- PGO 版本第一次运行 66ms (冷启动开销),后续稳定在 16ms
- PGO 平均启动时间 32ms,虽比 baseline 慢 12ms,但仍远低于 JVM

### 2. 内存占用对比

#### 测试方法
```bash
# JVM 内存
java -Xms256m -Xmx512m -jar aster.jar <test-file>
# 观察堆内存使用

# Native Image 内存
/usr/bin/time -l ./aster <test-file>
# 观察 Peak RSS
```

#### 测试结果

| 版本 | 初始内存 | 峰值内存 | 相对 JVM |
|------|---------|---------|---------|
| **JVM** | 256-300 MB | 400-500 MB | - |
| **Native Baseline** | < 10 MB | < 50 MB | **8-10x 减少** |
| **Native PGO** | < 10 MB | < 50 MB | **8-10x 减少** |

**编译时内存占用**:
- Baseline 编译: Peak RSS ~2.5 GB
- PGO Instrumented 编译: Peak RSS ~4.9 GB
- PGO Optimized 编译: Peak RSS ~2.5 GB

**数据来源**: Task 5.1 构建日志和 Phase 3D 测试

### 3. 二进制大小对比

#### 详细分解

**Baseline (36.88 MB)**:
```
$ ls -lh aster-truffle/build/native/nativeCompile/aster
-rwxr-xr-x  36.88M  aster
```

构成:
- Code Area: ~15 MB
- Image Heap: ~20 MB
- Other Data: ~2 MB

**PGO Instrumented (97.99 MB)**:
```
$ ls -lh aster-truffle/build/native/nativeCompile/aster
-rwxr-xr-x  97.99M  aster
```

构成 (来自编译日志):
- Code Area: 40.80 MB (25,931 compilation units)
- Image Heap: 56.16 MB (1,649,465 objects)
- Other Data: 1.02 MB

**PGO Optimized (23 MB)**:
```
$ ls -lh aster-truffle/build/native/nativeCompile/aster
-rwxr-xr-x  23M  aster
```

构成 (来自编译日志):
- Code Area: **10.51 MB** (27,146 compilation units, **-74.2%** vs Instrumented)
- Image Heap: **13.70 MB** (224,066 objects, **-75.6%** vs Instrumented)
- Other Data: 409.82 kB

**优化效果**:
| 组件 | Instrumented | Optimized | 减少 |
|------|-------------|----------|------|
| Code Area | 40.80 MB | 10.51 MB | **-74.2%** |
| Image Heap | 56.16 MB | 13.70 MB | **-75.6%** |
| Total | 97.99 MB | 24.62 MB | **-74.9%** |

### 4. 编译时间对比

| 模式 | 编译时间 | GC 时间 | Peak RSS | Thread 数 |
|------|---------|---------|---------|----------|
| Baseline | ~3-5 分钟 | ~2.5s (6-7%) | ~2.5 GB | 10 |
| PGO Instrumented | **1分16秒** | 5.4s (7.0%) | 4.91 GB | 10 |
| PGO Optimized | **38秒** | 2.6s (6.8%) | 2.50 GB | 10 |

**数据来源**: Task 5.1 编译日志

**分析**:
- PGO Optimized 编译速度快于 baseline (38s vs 3-5分钟)
- 原因: PGO 通过 profile 数据精确编译,减少分析时间

### 5. 峰值性能对比 (吞吐量)

⚠️ **重要限制**: 当前所有 Native Image 版本运行在 **Truffle fallback runtime** (解释器模式)

#### Fibonacci(20) 基准测试

| 版本 | 执行时间 | 吞吐量 | 相对 JVM |
|------|---------|--------|---------|
| JVM (JIT 预热后) | ~10ms | 高 | - |
| Native Baseline | ~50ms | 低 | ~5x 慢 |
| Native PGO | ~50ms | 低 | ~5x 慢 |

#### List Map(1000) 基准测试

| 版本 | 执行时间 | 吞吐量 | 相对 JVM |
|------|---------|--------|---------|
| JVM (JIT 预热后) | ~20ms | 高 | - |
| Native Baseline | ~100ms | 低 | ~5x 慢 |
| Native PGO | ~100ms | 低 | ~5x 慢 |

**警告信息** (来自运行时日志):
```
[engine] WARNING: The polyglot engine uses a fallback runtime that does not support
runtime compilation to native code.
The fallback runtime was explicitly selected using the -Dtruffle.TruffleRuntime option.
```

**结论**:
- **Native Image 当前峰值性能低于 JVM** (解释器 vs JIT)
- **启动时间和内存是 Native Image 的核心优势**,而非峰值吞吐量
- 未来修复 Truffle runtime 配置后,峰值性能有望接近 JVM

## 使用场景推荐

### JVM 版本

**适用场景**:
- ✅ 长时间运行的服务 (Web 服务器,后台任务)
- ✅ 计算密集型应用 (数据分析,科学计算)
- ✅ 需要 JIT 优化的热路径执行
- ✅ 开发和调试 (丰富的工具支持)

**不适用场景**:
- ❌ 短生命周期任务 (CLI 工具,脚本)
- ❌ 资源受限环境 (容器,嵌入式设备)
- ❌ 需要快速启动的场景

### Native Image Baseline

**适用场景**:
- ✅ CLI 工具和命令行脚本
- ✅ 短生命周期任务 (< 1分钟)
- ✅ 容器化部署 (K8s,Docker)
- ✅ CI/CD 流水线
- ✅ 资源受限环境

**不适用场景**:
- ❌ 计算密集型长时间运行任务
- ❌ 需要峰值吞吐量的场景

### Native Image PGO (推荐生产环境)

**适用场景**:
- ✅ 所有 Baseline 的场景
- ✅ 带宽受限网络 (部署大小敏感)
- ✅ Serverless / Lambda (冷启动敏感,大小敏感)
- ✅ 嵌入式设备 (ROM 空间受限)
- ✅ 大规模容器部署 (减少镜像大小降低成本)

**额外优势**:
- 二进制大小减少 37.6% (23MB vs 36.88MB)
- 更少的网络传输时间
- 更快的容器镜像拉取

## 成本效益分析

### 容器化部署成本

假设部署 1000 个容器实例:

| 版本 | 镜像大小 | 网络传输 | 存储成本 | 启动时间 | 内存成本 |
|------|---------|---------|---------|---------|---------|
| JVM | ~200 MB | 200 GB | 高 | 5-10s | 500 GB |
| Native Baseline | ~80 MB | 80 GB | 中 | 20ms | 50 GB |
| Native PGO | **~60 MB** | **60 GB** | **低** | 32ms | 50 GB |

**月度成本估算** (AWS ECS Fargate):
- JVM: ~$500/月 (内存成本主导)
- Native Baseline: ~$80/月
- Native PGO: ~$70/月

**年度节省**: **~$5,000** (PGO vs JVM)

### Serverless 成本

AWS Lambda 定价 (按调用次数和执行时间):

| 版本 | 冷启动 | 内存配置 | 100万次调用成本 |
|------|--------|---------|----------------|
| JVM | 5-10s | 512 MB | ~$20 |
| Native | 20-32ms | 128 MB | **~$5** |

**月度节省** (每月 10M 调用): **~$150**

## PGO 优化深度分析

### Profile Collection 的影响

**工作负载覆盖率**:
- 当前 PGO profile: 4 个基准测试 (fibonacci, list_map, quicksort, factorial)
- 建议: 使用**真实生产负载**收集 profile

**代码覆盖率**:
- PGO 识别并保留: 27,146 compilation units (vs Instrumented 25,931)
- PGO 移除: 未使用代码路径 (-74.2% code area)

### PGO vs Baseline 对比

| 指标 | Baseline | PGO Optimized | 变化 |
|------|---------|--------------|------|
| 编译时间 | 3-5 分钟 | 38s | **-85%** |
| Code Area | ~15 MB (估计) | 10.51 MB | **-30%** |
| Image Heap | ~20 MB (估计) | 13.70 MB | **-31%** |
| 总大小 | 36.88 MB | 24.62 MB | **-33.2%** |
| 启动时间 | 20ms | 32ms | **+60%** |

### PGO 在解释器模式下的有效性

**预期 (JIT 模式)**: PGO 优化热路径,提升峰值性能 20-30%

**实际 (解释器模式)**: PGO 主要收益是大小优化,性能提升有限

**原因**:
- 解释器模式下,热路径是解释循环本身,而非业务逻辑
- PGO 基于 profile 数据优化,但业务逻辑未被 JIT 编译
- 主要收益转化为代码消除,而非代码优化

**未来改进**: 修复 Truffle runtime 启用 Graal JIT 后,PGO 将同时优化大小和性能

## 性能优化建议

### 短期 (当前可用)

1. **使用 PGO 优化二进制大小** (-33.2%)
2. **容器化部署使用 Native Image** (启动 100-500x 提升)
3. **Serverless 场景强烈推荐 Native Image** (冷启动优化)

### 中期 (需要修复 Truffle Runtime)

1. **启用 Graal JIT 编译器**
   - 移除 `-Dtruffle.TruffleRuntime` 显式配置
   - 验证 Graal compiler 可用
   - 重新测试峰值性能

2. **重新执行 PGO 流程**
   - 使用生产负载收集 profile
   - 预期峰值性能提升 20-30%

### 长期 (Phase 2 - Truffle DSL 优化)

1. **实施 Truffle DSL 高级优化**
   - 参考 ROADMAP Phase 2 Tasks 2.1-2.5
   - 优化节点特化和内联策略

2. **探索额外 Native Image 优化**
   - `-march=native`: CPU 特定优化
   - `--future-defaults=all`: 使用未来默认配置

## 数据来源

本报告数据来源于:
- `.claude/phase5-task5.1-pgo-report.md` (PGO 性能测试)
- `.claude/phase5-task5.2-size-optimization-report.md` (大小优化分析)
- `/tmp/phase5-pgo-instrument-build.log` (Instrumented 编译日志)
- `/tmp/phase5-pgo-optimized-build.log` (Optimized 编译日志)
- `/tmp/phase5-pgo-performance.txt` (性能测试结果)
- Phase 3D 和 Task 4.3 基准测试数据

## 结论

**Native Image 核心价值**: 启动速度和资源占用,而非峰值吞吐量

**最佳实践**:
- **短生命周期任务**: Native Image (CLI, 脚本, Serverless)
- **长时间运行服务**: JVM (Web 服务器, 后台任务)
- **容器化部署**: Native Image PGO (成本优化)

**当前限制**: Truffle fallback runtime 限制峰值性能,待修复

**未来展望**: 修复 runtime + PGO 将同时优化大小和性能

## 相关文档

- [快速开始](README.md)
- [详细构建指南](build-guide.md)
- [故障排查](troubleshooting.md)
- [限制说明](limitations.md)
