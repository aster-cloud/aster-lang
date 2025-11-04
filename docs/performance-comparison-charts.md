# Performance Comparison Charts

本文档提供 Aster 语言三个后端实现的性能对比可视化图表。

## 概览

基于实际基准测试结果（2025-11-03），对比三个后端的执行性能：
- **GraalVM JIT**: 优化的 JIT 编译执行
- **Truffle Interpreter**: 未优化的 AST 解释执行（估算值）
- **Pure Java**: 直接字节码生成（估算值）

## 1. 执行时间对比

### Fibonacci(20) - 递归算法

```
执行时间 (ms/迭代，越低越好)
└───────────────────────────────────────────────┐
    Pure Java          ████  15 ms (估算)
    GraalVM JIT        █████████  26.25 ms
    Truffle Interp.    ███████████████████████████████████████████ 150 ms (估算)
                       0        50       100      150      200
```

**性能比较**:
- GraalVM JIT vs Truffle: **5.7x faster**
- Pure Java vs GraalVM JIT: **1.75x faster**
- Pure Java vs Truffle: **10x faster**

### Factorial(12) - 迭代算法

```
执行时间 (ms/迭代，越低越好)
└─────────────────────────────────────────────┐
    Pure Java          █  2 ms (估算)
    GraalVM JIT        ██  5.0 ms
    Truffle Interp.    ████████████  30 ms (估算)
                       0    10   20   30   40
```

**性能比较**:
- GraalVM JIT vs Truffle: **6x faster**
- Pure Java vs GraalVM JIT: **2.5x faster**
- Pure Java vs Truffle: **15x faster**

### QuickSort(100) - 排序算法

```
执行时间 (ms/迭代，越低越好)
└───────────────────────────────────────────────┐
    Pure Java          ████  50 ms (估算)
    GraalVM JIT        ██████████  99.37 ms
    Truffle Interp.    ██████████████████████████  500 ms (估算)
                       0       200      400      600
```

**性能比较**:
- GraalVM JIT vs Truffle: **5x faster**
- Pure Java vs GraalVM JIT: **2x faster**
- Pure Java vs Truffle: **10x faster**

### Binary Tree Traversal(15) - 树遍历

```
执行时间 (ms/迭代，越低越好)
└──────────────────────────────────────────────┐
    Pure Java          █  0.05 ms (估算)
    GraalVM JIT        █  0.074 ms
    Truffle Interp.    ██████████  0.5 ms (估算)
                       0    0.2   0.4   0.6
```

**性能比较**:
- GraalVM JIT vs Truffle: **6.8x faster**
- Pure Java vs GraalVM JIT: **1.5x faster**
- Pure Java vs Truffle: **10x faster**

### String Operations - 字符串处理

```
执行时间 (ms/迭代，越低越好)
└───────────────────────────────────────────────┐
    Pure Java          █  0.2 ms (估算)
    GraalVM JIT        █  0.288 ms
    Truffle Interp.    ██████████  2.0 ms (估算)
                       0     1     2     3
```

**性能比较**:
- GraalVM JIT vs Truffle: **7x faster**
- Pure Java vs GraalVM JIT: **1.4x faster**
- Pure Java vs Truffle: **10x faster**

## 2. 综合性能对比表

### 绝对性能（ms/迭代）

| 基准测试 | Pure Java | GraalVM JIT | Truffle Interp. |
|---------|-----------|-------------|-----------------|
| Fibonacci(20) | 15 (估) | **26.25** | 150 (估) |
| Factorial(12) | 2 (估) | **5.0** | 30 (估) |
| QuickSort(100) | 50 (估) | **99.37** | 500 (估) |
| BinaryTree(15) | 0.05 (估) | **0.074** | 0.5 (估) |
| StringOps | 0.2 (估) | **0.288** | 2.0 (估) |

**注**: 粗体为实际测量值，其他为估算值

### 相对性能（以 Truffle 解释器为基准 = 1x）

| 基准测试 | Pure Java | GraalVM JIT | Truffle Interp. |
|---------|-----------|-------------|-----------------|
| Fibonacci(20) | 10x | 5.7x | 1x |
| Factorial(12) | 15x | 6x | 1x |
| QuickSort(100) | 10x | 5x | 1x |
| BinaryTree(15) | 10x | 6.8x | 1x |
| StringOps | 10x | 7x | 1x |
| **平均** | **11x** | **6.1x** | **1x** |

## 3. 后端特性对比

### 启动时间 vs 稳态性能

```
性能提升 (以 Truffle 为基准)
└─────────────────────────────────────────────┐
Pure Java           ████████████████████ 11x
                    ↑ 立即达到最大性能

GraalVM JIT         ██████████████ 6.1x
                    ↑ 需要预热达到峰值性能
                    │ 预热: 2000-5000 次迭代

Truffle Interp.     ██ 1x
                    ↑ 固定性能，无优化
                    0   5x  10x  15x  20x
```

### 内存占用对比（相对值）

| 后端 | 堆内存占用 | JIT 编译缓存 | 总内存 |
|-----|----------|-------------|--------|
| Pure Java | 中 | 无 | **中** |
| GraalVM JIT | 中 | 高 | **高** |
| Truffle Interpreter | 低 | 无 | **低** |
| Native Image (可选) | 极低 | 无 | **极低** |

### 开发体验对比

| 特性 | Pure Java | GraalVM JIT | Truffle Interp. |
|-----|----------|-------------|-----------------|
| 调试友好度 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 错误信息质量 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 热重载支持 | ❌ | ⚠️ 部分 | ✅ 完整 |
| 性能剖析 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 代码覆盖率 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

## 4. 使用场景推荐

### 场景决策树

```
┌─ 需要极致性能？
│   └─ 是 → 预算足够？
│           ├─ 是 → 使用 Pure Java (最快)
│           └─ 否 → 使用 GraalVM JIT (次快，但需预热)
│
├─ 长时间运行的服务？
│   └─ 是 → 使用 GraalVM JIT (稳态性能接近 Pure Java)
│
├─ 开发和调试阶段？
│   └─ 是 → 使用 Truffle Interpreter (最佳开发体验)
│
└─ 快速原型或脚本？
    └─ 是 → 使用 Truffle Interpreter (启动快，无预热)
```

### 详细推荐

#### 使用 Pure Java 字节码的场景

✅ **适合**:
- CLI 工具（短期执行，<1 秒）
- 批处理任务（执行时间 <10 秒）
- 性能关键的微服务
- 对启动时间敏感的应用

❌ **不适合**:
- 频繁迭代的开发阶段
- 需要动态语言特性的应用
- 对内存占用敏感的环境

#### 使用 GraalVM JIT 的场景

✅ **适合**:
- 长期运行的服务器应用（>1 分钟）
- Web 应用后端
- 数据处理管道（运行时间 >30 秒）
- 需要平衡性能与开发体验

❌ **不适合**:
- 极短期任务（<5 秒）
- 内存受限环境（<512MB 堆）
- 无法接受预热开销的场景

#### 使用 Truffle 解释器的场景

✅ **适合**:
- 开发和调试阶段
- 单元测试和集成测试
- 快速原型开发
- 教学和演示
- 脚本和交互式 REPL

❌ **不适合**:
- 生产环境（性能要求高）
- 长时间运行的计算密集型任务
- 对响应时间敏感的实时系统

## 5. 性能调优指南

### GraalVM JIT 优化技巧

#### 1. 充分预热

```java
// 不良实践：预热不足
for (int i = 0; i < 100; i++) {
    compute();  // JIT 编译器可能还未触发
}
measure();

// 最佳实践：三阶段预热
coldRun(200);       // 阶段 1: 触发编译
warmup(2000);       // 阶段 2: 等待优化稳定
measure(1000);      // 阶段 3: 真实性能测量
```

#### 2. 避免类型不稳定

```java
// 不良实践：类型频繁变化
Object value = computeValue();
if (condition) {
    value = 42;        // Integer
} else {
    value = "hello";   // String - 导致反优化！
}

// 最佳实践：类型稳定
int intValue = computeIntValue();
String stringValue = computeStringValue();
```

#### 3. 使用 Truffle DSL

```java
// 手动类型检查 - 性能较差
public Object execute(Object arg) {
    if (arg instanceof Integer) {
        return (Integer) arg + 1;
    } else if (arg instanceof Double) {
        return (Double) arg + 1.0;
    }
    throw new IllegalArgumentException();
}

// Truffle DSL - JIT 友好
@Specialization
public int executeInt(int arg) {
    return arg + 1;
}

@Specialization
public double executeDouble(double arg) {
    return arg + 1.0;
}
```

### Pure Java 优化技巧

#### 1. 使用原始类型

```java
// 不良实践：装箱类型
List<Integer> list = new ArrayList<>();
for (Integer i : list) {
    sum += i;  // 频繁拆箱
}

// 最佳实践：原始数组
int[] array = new int[size];
for (int i = 0; i < array.length; i++) {
    sum += array[i];  // 无拆箱开销
}
```

#### 2. 减少对象分配

```java
// 不良实践：每次调用创建新对象
public Result compute() {
    return new Result(value);  // 频繁 GC
}

// 最佳实践：复用或内联
private final Result result = new Result();
public Result compute() {
    result.setValue(value);
    return result;
}
```

## 6. 基准测试方法论

### 测试环境规范

为确保结果可重现，基准测试应在以下环境中运行：

```
硬件:
  CPU: 至少 4 核
  内存: 至少 8GB
  存储: SSD

软件:
  OS: macOS / Linux (推荐)
  JDK: Oracle GraalVM 25+37.1
  Gradle: 9.0+

运行条件:
  - 关闭其他高负载应用
  - 固定 CPU 频率（禁用 Turbo Boost）
  - 使用专用 CI 节点
```

### 统计显著性

单次运行不足以得出结论，应：

1. **多次运行**: 至少 5 次独立运行
2. **计算统计量**: 中位数、平均值、标准差
3. **异常值过滤**: 排除明显的系统干扰
4. **置信区间**: 计算 95% 置信区间

示例：
```
Fibonacci(20) - 5 次运行结果:
  26.1 ms, 26.3 ms, 26.5 ms, 26.0 ms, 26.4 ms

统计:
  中位数: 26.3 ms
  平均值: 26.26 ms ± 0.19 ms
  标准差: 0.19 ms
  变异系数: 0.7% (良好，<5%)
```

## 7. 未来性能改进计划

### 短期目标（1-3 个月）

1. **Native Image 支持**
   - 编译为原生可执行文件
   - 预期启动时间：<50ms
   - 预期内存占用：<50MB

2. **AOT 编译选项**
   - 预编译热点函数
   - 减少预热时间 50%

3. **内联优化**
   - 标准库函数内联
   - 预期性能提升：10-20%

### 长期目标（6-12 个月）

1. **SIMD 向量化**
   - 利用 CPU SIMD 指令
   - 数组操作加速 2-4x

2. **Profile-Guided Optimization (PGO)**
   - 基于实际运行剖析优化
   - 预期性能提升：20-30%

3. **并行执行引擎**
   - 自动并行化纯函数
   - 多核利用率提升

## 参考资料

- [Performance Regression Monitoring](./performance-regression-monitoring.md)
- [GraalVM Setup Guide](./graalvm-setup-guide.md)
- [Truffle Performance Comparison](./truffle-performance-comparison.md)
- [Cross-Backend Benchmark Results](./cross-backend-benchmark-results.md)

## 数据来源

- GraalVM JIT 数据：实际测量（2025-11-03）
- Pure Java 数据：估算值，基于历史经验
- Truffle Interpreter 数据：估算值，基于 GraalVM 文档和项目经验

**注意**: 估算值将在后续实际测量后更新。
