# Truffle 性能基准测试文档

## 概述

本文档描述 Aster Lang Truffle 后端的性能基准测试套件，用于验证 Truffle JIT 编译器的优化效果和检测性能退化。

## 测试方法论

### 三阶段测试

每个基准测试遵循标准的三阶段测试流程：

1. **Warmup 阶段**：重复执行代码，让 Truffle JIT 编译器有机会优化热点路径
2. **测量阶段**：多次迭代执行并测量平均执行时间
3. **验证阶段**：确保性能指标在可接受的阈值范围内

### 测试环境说明

- **JIT 状态**：测试在解释模式下运行（无 GraalVM JIT 编译器）
- **性能基线**：阈值设置考虑了解释模式的额外开销
- **CI/CD 集成**：基准测试作为标准测试套件的一部分，在每次 PR 中运行

## 当前基准测试

### 1. 简单算术计算 (`benchmarkArithmetic`)

**测试目标**：验证 Builtin 函数调用优化

**测试场景**：
```aster
func compute(x: Int): Int {
  return (x * 2) + (x / 3)
}
```

**性能指标**：
- 迭代次数：10,000 次
- 性能阈值：< 1 ms/iteration
- 典型性能：~0.002 ms/iteration

**关键优化点**：
- Builtin 函数内联
- 简单算术运算优化
- 参数传递开销

### 2. 递归阶乘计算 (`benchmarkFactorial`)

**测试目标**：验证递归函数调用优化

**测试场景**：
```aster
func factorial(n: Int): Int {
  if n <= 1 {
    return 1
  } else {
    return n * factorial(n - 1)
  }
}

factorial(10) // = 3,628,800
```

**性能指标**：
- Warmup：100 次迭代
- 测试迭代次数：1,000 次
- 性能阈值：< 10 ms/iteration
- 典型性能：~0.026 ms/iteration

**关键优化点**：
- 递归调用内联
- 条件分支预测
- Frame 栈管理

### 3. 递归斐波那契数列 (`benchmarkFibonacci`)

**测试目标**：验证重复递归调用优化（最密集的递归测试）

**测试场景**：
```aster
func fib(n: Int): Int {
  if n <= 1 {
    return n
  } else {
    return fib(n - 1) + fib(n - 2)
  }
}

fib(15) // = 610
```

**性能指标**：
- Warmup：50 次迭代
- 测试迭代次数：100 次
- 性能阈值：< 50 ms/iteration
- 典型性能：~2.2 ms/iteration

**关键优化点**：
- 多重递归调用优化
- 重复计算处理
- 函数调用缓存

**注意事项**：
- fib(15) 产生 1,219 次函数调用
- 指数级递归复杂度 O(2^n)
- 对 JIT 编译器压力测试最大

## 规划中的基准测试（待实现）

以下基准测试已编写但暂时禁用，等待 Core IR JSON 格式完善：

### 4. Lambda 高阶函数 (`benchmarkLambdaCall`) [禁用]

**测试目标**：Lambda 创建和调用开销

**状态**：等待修复 Lambda JSON 格式（需要 `ret`, `captures` 字段）

**性能目标**：< 5 ms/iteration (1000 iterations)

### 5. 闭包捕获 (`benchmarkClosureCapture`) [禁用]

**测试目标**：闭包变量捕获和访问性能

**状态**：等待修复 Lambda JSON 格式

**性能目标**：< 15 ms/iteration (500 iterations)

### 6. 模式匹配 (`benchmarkPatternMatching`) [禁用]

**测试目标**：Match 表达式性能

**状态**：等待修复 Match JSON 格式（使用 `expr` 而非 `scrutinee`，Case body 不应包装在 Block 中）

**性能目标**：< 2 ms/iteration (5000 iterations)

## 运行基准测试

### 单独运行基准测试

```bash
./gradlew :aster-truffle:test --tests "BenchmarkTest"
```

### 运行特定基准

```bash
./gradlew :aster-truffle:test --tests "BenchmarkTest.benchmarkFactorial"
```

### 完整测试套件（包括基准测试）

```bash
./gradlew :aster-truffle:test
```

## 性能分析

### 当前性能特征

1. **简单操作非常快速**：算术计算在 0.002 ms/iteration
2. **递归性能良好**：阶乘 10 在 0.026 ms，fib(15) 在 2.2 ms
3. **解释模式限制**：所有测试在无 GraalVM JIT 的环境下运行

### 性能优化建议

1. **启用 GraalVM**：使用 GraalVM JDK 可大幅提升性能（预计 10-100x）
2. **Frame 优化**：已实现 Frame slot tracking 优化变量访问
3. **DSL 类型特化**：已为 NameNode/LetNode/SetNode 启用 Truffle DSL

### 性能退化检测

基准测试在 CI 中运行，如果性能超过阈值会导致测试失败：

- ✅ `benchmarkArithmetic`: 0.002 ms < 1.0 ms (通过)
- ✅ `benchmarkFactorial`: 0.027 ms < 10.0 ms (通过)
- ✅ `benchmarkFibonacci`: 2.21 ms < 50.0 ms (通过)

## 与其他后端的性能对比

| 后端 | 阶乘(10) | 斐波那契(15) | 算术 | 备注 |
|------|----------|--------------|------|------|
| Truffle (解释) | 0.026 ms | 2.2 ms | 0.002 ms | 当前实现 |
| TypeScript | TBD | TBD | TBD | 待测量 |
| Java | TBD | TBD | TBD | 待测量 |
| Truffle (GraalVM) | 预计 0.003 ms | 预计 0.2 ms | 预计 0.0002 ms | 理论值 |

## 基准测试代码位置

- **测试文件**：`aster-truffle/src/test/java/aster/truffle/BenchmarkTest.java`
- **文档**：`docs/truffle-performance-benchmarks.md`
- **CI 集成**：`package.json` - `npm run ci` 包含 `npm run truffle:test`

## 未来工作

### 短期目标

1. ✅ 建立基础基准测试（已完成）
2. ⏳ 修复 Lambda/Closure/Match 基准测试的 JSON 格式
3. ⏳ 添加更多场景覆盖：
   - 数据结构操作（Map/List 访问）
   - 字符串操作
   - 异常处理性能

### 长期目标

1. 启用 GraalVM 原生编译的基准测试
2. 建立性能回归追踪系统
3. 与其他语言/后端的性能对比
4. 优化热点路径（基于 profiler 数据）

## 相关文档

- [Truffle Backend Limitations](truffle-backend-limitations.md) - Truffle 后端功能限制
- [Testing Guide](testing.md) - 完整测试指南

## 更新日志

- **2025-11-03**: 初始版本，包含 3 个活跃基准测试
  - 简单算术计算
  - 递归阶乘
  - 递归斐波那契
- **2025-11-03**: 添加 3 个禁用的基准测试（待 JSON 格式修复）
  - Lambda 调用
  - 闭包捕获
  - 模式匹配
