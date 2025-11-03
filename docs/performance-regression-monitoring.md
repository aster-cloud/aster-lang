# Performance Regression Monitoring

本文档说明 Aster 语言性能回归监控系统的使用方法和维护指南。

## 概述

性能基准测试集成到 CI 流水线中，提供持续的性能监控和回归检测能力。基准测试包括三个后端实现：
- **GraalVM JIT**: 优化后的执行（JIT 编译后性能）
- **Truffle Interpreter**: 未优化的 AST 解释执行
- **Pure Java**: 直接字节码生成

## 阈值配置

性能回归阈值定义在 `benchmarks/performance-thresholds.json` 文件中。

### 阈值类型

#### 1. Warning 阈值（警告）
- **用途**: 表明性能可能存在轻微退化，需要关注但不阻塞构建
- **典型值**: 基线的 15-20%
- **触发行为**: 记录警告日志，在 CI 输出中显示

#### 2. Error 阈值（错误）
- **用途**: 表明明确的性能回归，需要立即调查
- **典型值**: 基线的 30-40%
- **触发行为**: 记录错误日志，标记性能问题（但不失败构建）

### 阈值设计原则

1. **允许正常波动**: 15-20% 的警告阈值允许 JVM 预热、系统负载等正常波动
2. **区分真实回归**: 30-40% 的错误阈值确保只有明确的性能退化才会触发
3. **差异化处理**:
   - 执行时间 < 1ms 的基准测试使用更高的阈值（20%），因为相对波动更大
   - 执行时间 > 10ms 的基准测试使用更严格的阈值（15%），因为更稳定

## 基准测试基线

### GraalVM JIT 基线（2025-11-03）

| 基准测试 | 基线 (ms/迭代) | Warning 阈值 | Error 阈值 | 说明 |
|---------|---------------|-------------|-----------|------|
| Fibonacci(20) | 26.25 | 30.19 ms (+15%) | 34.13 ms (+30%) | 递归调用和函数优化 |
| Factorial(12) | 5.0 | 5.75 ms (+15%) | 6.5 ms (+30%) | 循环优化和整数运算 |
| QuickSort(100) | 99.37 | 114.28 ms (+15%) | 129.18 ms (+30%) | 数组操作和排序算法 |
| BinaryTree(15) | 0.074 | 0.089 ms (+20%) | 0.104 ms (+40%) | 对象创建和树遍历 |
| StringOps | 0.288 | 0.331 ms (+15%) | 0.374 ms (+30%) | 文本操作和字符串内置函数 |

### Truffle Interpreter 基线（估算）

| 基准测试 | 基线 (ms/迭代) | Warning 阈值 | Error 阈值 |
|---------|---------------|-------------|-----------|
| Factorial | 1.5 | 1.8 ms (+20%) | 2.1 ms (+40%) |
| Fibonacci | 8.0 | 9.6 ms (+20%) | 11.2 ms (+40%) |
| List.map | 12.0 | 14.4 ms (+20%) | 16.8 ms (+40%) |
| Arithmetic | 0.5 | 0.6 ms (+20%) | 0.7 ms (+40%) |

### Pure Java 基线（估算）

| 基准测试 | 基线 (ms/迭代) | Warning 阈值 | Error 阈值 |
|---------|---------------|-------------|-----------|
| Factorial | 0.3 | 0.345 ms (+15%) | 0.39 ms (+30%) |
| Fibonacci | 1.8 | 2.07 ms (+15%) | 2.34 ms (+30%) |
| List.map | 2.5 | 2.875 ms (+15%) | 3.25 ms (+30%) |
| Result.mapOk | 1.0 | 1.15 ms (+15%) | 1.3 ms (+30%) |
| Result.mapErr | 1.0 | 1.15 ms (+15%) | 1.3 ms (+30%) |

## 使用方法

### 运行基准测试

```bash
# 运行所有基准测试
npm run bench:all

# 运行单个后端
npm run bench:truffle    # Truffle 解释器
npm run bench:java       # Pure Java 字节码
npm run bench:jit        # GraalVM JIT

# CI 集成（非阻塞）
npm run ci:bench
```

### 解读结果

基准测试输出包含：
1. **执行时间**: 每次迭代的平均执行时间（毫秒）
2. **迭代次数**: 测量阶段的迭代次数
3. **与基线比较**: 如果实现了阈值检查，会显示与基线的差异

#### 示例输出

```
=== Fibonacci(20) Heavy (GraalVM JIT) ===
Phase 1: 冷启动 200 次，触发编译...
Phase 2: 追加预热 2000 次，等待优化稳定...
Phase 3: 测量阶段（200 次）...
GraalVM JIT: 26.25 ms/iteration
✓ PASS - Within baseline threshold (26.25 ms < 30.19 ms warning)
```

#### 回归警告示例

```
⚠️ WARNING - Performance degradation detected
Fibonacci(20): 32.5 ms/iteration
Expected: < 30.19 ms (warning threshold)
Baseline: 26.25 ms
Degradation: +23.8%
```

#### 回归错误示例

```
❌ ERROR - Significant performance regression
Fibonacci(20): 38.0 ms/iteration
Expected: < 34.13 ms (error threshold)
Baseline: 26.25 ms
Degradation: +44.8%
Action required: Investigate performance regression
```

## 维护指南

### 何时更新基线

应该在以下情况下更新基线值：

1. **实现性能优化**:
   - 新的编译器优化
   - 改进的内置函数实现
   - 更高效的运行时数据结构

2. **预期的性能变化**:
   - 添加新功能导致的合理性能影响
   - 权衡正确性与性能的设计决策

3. **基础设施变化**:
   - 升级 JVM 版本
   - 升级 GraalVM 版本
   - 变更 CI 环境的硬件配置

### 更新基线的步骤

1. **运行基准测试并收集数据**:
   ```bash
   # 运行多次以确保稳定性
   npm run bench:jit
   npm run bench:truffle
   npm run bench:java
   ```

2. **计算新的基线值**:
   - 取多次运行的中位数或平均值
   - 确保值是可复现的（变异系数 < 5%）

3. **更新 `performance-thresholds.json`**:
   ```json
   {
     "fibonacci_20": {
       "baseline_ms": 25.0,  // 更新后的基线
       "warning_threshold_ms": 28.75,  // baseline * 1.15
       "error_threshold_ms": 32.5      // baseline * 1.30
     }
   }
   ```

4. **记录变更原因**:
   - 在 `CHANGELOG.md` 中记录基线更新
   - 说明性能改进或变化的原因
   - 包含新旧基线的对比

### 调整阈值百分比

如果发现误报或漏报，可以调整阈值百分比：

```json
{
  "fibonacci_20": {
    "baseline_ms": 26.25,
    "warning_threshold_percent": 20,  // 从 15% 提高到 20%
    "error_threshold_percent": 35,    // 从 30% 提高到 35%
    "warning_threshold_ms": 31.5,
    "error_threshold_ms": 35.44
  }
}
```

**调整原则**:
- 增加阈值：如果经常出现误报（正常波动触发警告）
- 降低阈值：如果需要更严格的性能监控
- 保持记录：在 `notes` 字段中记录调整原因

## 故障排查

### 基准测试失败或超时

**症状**: 基准测试运行时间过长或无法完成

**可能原因**:
1. 工作负载过大（如 Fibonacci(35)）
2. 预热迭代过多
3. 测量迭代过多

**解决方法**:
```java
// 在 GraalVMJitBenchmark.java 中调整参数
private static final BenchmarkCase FIBONACCI_HEAVY = new BenchmarkCase(
    "Fibonacci(20) Heavy",
    "bench-fibonacci20-jit.json",
    "main",
    readBenchmarkJsonUnchecked("benchmarks/core/fibonacci_20_core.json"),
    6_765,
    200,    // coldIterations - 减少冷启动次数
    100,    // warmupIterations - 减少预热次数
    2_000   // measureIterations - 减少测量次数
);
```

### 性能波动过大

**症状**: 相同代码在不同运行中性能差异 > 10%

**可能原因**:
1. JVM 预热不足
2. 系统负载波动
3. GC 活动干扰

**解决方法**:
1. 增加预热迭代次数
2. 在专用 CI 节点上运行基准测试
3. 运行多次取中位数

### CI 环境中的性能差异

**症状**: 本地性能正常，CI 中触发回归警告

**可能原因**:
1. CI 环境硬件配置不同
2. 容器资源限制
3. 共享资源竞争

**解决方法**:
1. 建立 CI 环境的专属基线
2. 使用相对性能比较而非绝对值
3. 考虑提高 CI 环境的阈值

## 未来改进

### 计划中的增强功能

1. **自动化阈值检查**:
   - 实现 Java 工具类读取 `performance-thresholds.json`
   - 在基准测试结束后自动比较结果
   - 生成结构化的回归报告

2. **历史趋势分析**:
   - 记录每次 CI 运行的性能数据
   - 绘制性能趋势图表
   - 识别渐进式性能退化

3. **多维度比较**:
   - 跨后端性能比较（Truffle vs Pure Java vs GraalVM JIT）
   - 跨版本性能比较
   - 跨分支性能比较

4. **智能基线更新**:
   - 基于统计分析自动建议基线调整
   - 异常值检测和过滤
   - 自适应阈值调整

## 参考资料

- [Cross-Backend Benchmark Results](./cross-backend-benchmark-results.md)
- [Truffle Performance Comparison](./truffle-performance-comparison.md)
- [GraalVM Documentation](https://www.graalvm.org/latest/reference-manual/)
- [Truffle Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
