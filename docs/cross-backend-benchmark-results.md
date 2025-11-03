# Cross-Backend Benchmark Results

**Date**: 2025-11-03 14:13 (NZST)
**Environment**: MacOS, Apple Silicon, GraalVM JDK 25 (Oracle GraalVM 25+37.1, JIT runtime enabled)

## Summary

This document contains actual performance measurements for the Aster language execution backends.

### Architecture Clarification

**IMPORTANT**: The Aster language has the following architecture:
- **TypeScript Frontend**: Compiles CNL source code to Core IR (JSON format)
- **Java Backends**: Execute Core IR
  1. **Truffle Backend** - AST interpreter with optional GraalVM JIT compilation
  2. **Pure Java Backend** - Bytecode generation and JVM execution (if implemented)

**TypeScript is NOT an execution backend** - it's purely a compiler. There is no TypeScript interpreter for Core IR.

Therefore, this benchmark compares:
1. **Truffle Backend** (Interpreter mode - ✅ measurements completed)
2. **Pure Java Backend** (Bytecode) - ✅ measurements completed

## Benchmark Suite

Four standard benchmarks running identical algorithms:

### 1. Factorial(10) - Recursive Function Calls

**Algorithm**: Recursive factorial calculation
**Test Case**: factorial(10) = 3,628,800
**Iterations**: 1,000 (interpreter warmup 50; JIT warmup 100 cold + 2,000 stabilization)

| Backend | Average Time | Relative | Status |
|---------|-------------|----------|--------|
| Truffle (Interpreter) | **0.018 ms** | Baseline | ✅ Measured |
| Pure Java (Bytecode) | **0.002369 ms** | 7.6x faster | ✅ Measured |
| Truffle (GraalVM JIT) | **0.020 ms** | 1.1x slower vs interpreter | ✅ Measured |

**Analysis**: Truffle interpreter remains extremely fast for this workload (0.018ms). GraalVM JIT adds only marginal overhead (~0.020ms) because the recursion tree is shallow and already optimized in the interpreter. Pure Java bytecode still delivers the best result at 0.002369ms (~7.6x faster than interpreter), highlighting JVM bytecode efficiency for tight recursive loops.

---

### 2. Fibonacci(20) - Heavy Recursion

**Algorithm**: Naive recursive Fibonacci (exponential complexity)
**Test Case**: fib(20) = 6,765
**Iterations**: 100 (interpreter warmup 50; JIT warmup 100 cold + 2,000 stabilization)

| Backend | Average Time | Relative | Status |
|---------|-------------|----------|--------|
| Truffle (Interpreter) | **23.803 ms** | Baseline | ✅ Measured |
| Pure Java (Bytecode) | **0.051874 ms** | 459x faster | ✅ Measured |
| Truffle (GraalVM JIT) | **27.478 ms** | 1.2x slower vs interpreter | ✅ Measured |

**Analysis**: Heavy recursion stresses the Truffle interpreter at 23.8ms per call, while the GraalVM JIT result (27.5ms) shows no improvement yet—indicating that these kernels require additional profiling data or larger inputs before Graal can optimize them. Pure Java bytecode remains dramatically faster (0.051874ms, ~459x faster than interpreter), confirming JVM bytecode’s superiority for deep recursion.

---

### 3. List.map(2 items) - Higher-Order Functions

**Algorithm**: List.map with doubling lambda over 2-element list
**Test Case**: [1, 2].map(x => x * 2) = [2, 4], length = 2
**Iterations**: 10,000 (interpreter warmup 1,000; JIT warmup 100 cold + 5,000 stabilization)

| Backend | Average Time | Relative | Status |
|---------|-------------|----------|--------|
| Truffle (Interpreter) | **0.006 ms** | Baseline | ✅ Measured |
| Pure Java (Bytecode) | **0.000550 ms** | 10.9x faster | ✅ Measured |
| Truffle (GraalVM JIT) | **0.004672 ms** | 1.3x faster vs interpreter | ✅ Measured |

**Analysis**: Higher-order functions benefit from GraalVM JIT, dropping from 0.006ms to 0.004672ms (~1.3x faster) once the warmup completes. Pure Java bytecode continues to deliver the fastest execution (0.000550ms), but the JIT narrowing gap shows that Graal optimization helps workloads dominated by CallTarget invocation and lambda captures.

---

### 4. Arithmetic - Simple Computation

**Algorithm**: compute(x) = (x * 2) + (x / 3)
**Test Case**: compute(100) = 233
**Iterations**: 10,000 (interpreter warmup 1,000; JIT warmup 100 cold + 5,000 stabilization)

| Backend | Average Time | Relative | Status |
|---------|-------------|----------|--------|
| Truffle (Interpreter) | **0.002 ms** | Baseline | ✅ Measured |
| Pure Java (Bytecode) | **0.000399 ms** | 5x faster | ✅ Measured |
| Truffle (GraalVM JIT) | **0.001866 ms** | 1.1x faster vs interpreter | ✅ Measured |

**Analysis**: Arithmetic-heavy code is already efficient in the interpreter at 0.002ms. GraalVM JIT trims the average to 0.001866ms (~1.1x faster), indicating that simple numeric kernels do benefit slightly from compilation. Pure Java bytecode remains the fastest option (0.000399ms, ~5x faster than interpreter).

---

## Key Findings

### 1. Truffle Interpreter vs GraalVM JIT

| Benchmark | Truffle (Interpreter) | Truffle (GraalVM JIT) | Delta vs Interpreter |
|-----------|-----------------------|-----------------------|----------------------|
| Factorial(10) | 0.018 ms | 0.020 ms | **+11% (slower)** |
| Fibonacci(20) | 23.803 ms | 27.478 ms | **+15% (slower)** |
| List.map (2 items) | 0.006 ms | 0.004672 ms | **−22% (faster)** |
| Arithmetic | 0.002 ms | 0.001866 ms | **−7% (faster)** |

**Insights**:
- Interpreter mode already extracts most performance from small recursive kernels; GraalVM needs larger or longer-running workloads to justify compilation.
- Higher-order and arithmetic workloads do benefit once the JIT completes a longer stabilization cycle (1.1–1.3x faster).
- Retaining multi-phase warmups (100 cold + 2K/5K stabilization iterations) is mandatory to trigger GraalVM optimizations.

### 2. Truffle Interpreter Performance vs Original Estimates

| Benchmark | Estimated | Actual | Improvement |
|-----------|-----------|--------|-------------|
| Factorial | 15 ms | 0.018 ms | **833x better** |
| Fibonacci | 100 ms | 23.8 ms | **4.2x better** |
| List.map | 0.05 ms | 0.006 ms | **8.3x better** |
| Arithmetic | 0.5 ms | 0.002 ms | **250x better** |

Even before JIT, the interpreter significantly outperforms early projections thanks to efficient node implementations and low dispatch overhead.

### 3. Pure Java Bytecode vs Truffle Backends

| Benchmark | Truffle (Interpreter) | Truffle (GraalVM JIT) | Pure Java (Bytecode) | Fastest |
|-----------|-----------------------|-----------------------|----------------------|---------|
| Factorial(10) | 0.018 ms | 0.020 ms | **0.002369 ms** | Pure Java |
| Fibonacci(20) | 23.803 ms | 27.478 ms | **0.051874 ms** | Pure Java |
| List.map (2 items) | 0.006 ms | 0.004672 ms | **0.000550 ms** | Pure Java |
| Arithmetic | 0.002 ms | 0.001866 ms | **0.000399 ms** | Pure Java |

**Observations**:
- Pure Java bytecode remains the fastest backend across all workloads (5x–459x faster), especially on recursion-heavy benchmarks.
- GraalVM JIT narrows the gap for lambda-heavy and arithmetic workloads but still trails bytecode.
- Focusing on broader stdlib coverage for the bytecode backend will unlock more realistic end-to-end comparisons.

### 4. Architecture Understanding

- **Truffle**: Primary execution backend with interpreter + JIT modes (both measured)
- **Pure Java Bytecode**: Generates JVM classes; currently limited stdlib coverage
- **TypeScript**: Compiler frontend only; not part of runtime comparisons

---

## Next Steps

### 1. Broaden Pure Java Backend Coverage 🔧
- Implement `List.filter`, `List.reduce`, `Result.mapOk`, `Result.mapErr`, and related helpers.
- Add bytecode emission + runtime tests to keep parity with interpreter semantics.

### 2. Tune GraalVM JIT Benchmarks 🧪
- Experiment with heavier workloads (larger inputs or batched invocations) to accumulate more profiling data for GraalVM.
- Capture compilation logs (e.g., via `--engine.TraceCompilation`) to verify when optimization kicks in.

### 3. CI Integration 📈
- Surface interpreter, bytecode, and JIT benchmarks as informational checks (non-blocking by default).
- Track deltas over time to spot performance regressions once broader coverage is available.

---

## Test Environment Details

### Hardware
- **Platform**: macOS (Darwin 25.0.0)
- **CPU**: Apple Silicon (M-series)
- **Memory**: Not measured

### Software
- **JDK**: Oracle GraalVM JDK 25 (25+37.1, JVMCI enabled)
- **Truffle Modules**: `truffle-api`, `truffle-runtime`, `truffle-compiler` 25.0.0
- **Build Tool**: Gradle 9.0.0

### Truffle Configuration
- **Context**: Polyglot API with `allowAllAccess(true)`
- **Warnings**: `engine.WarnInterpreterOnly=false` (suppressed after runtime enabled)
- **Warmup Strategy**: Multi-phase (cold trigger + stabilization + measurement)

---

## Measurement Methodology

- **Timing**: `System.nanoTime()`, averaged per iteration.
- **Interpreter Baseline Warmup**: 50 iterations (Factorial/Fibonacci) or 1,000 iterations (List.map/Arithmetic).
- **JIT Warmup**:
  - Phase 1: 100 cold executions to trigger compilation.
  - Phase 2: 2,000 (Factorial/Fibonacci) or 5,000 (List.map/Arithmetic) stabilization iterations.
  - Phase 3: Measurement loops matching interpreter counts (1,000 / 100 / 10,000 / 10,000).
- **Validation**: Each iteration asserts expected results; runs are isolated per benchmark.

---

## Conclusion

- The Truffle interpreter continues to deliver outstanding baseline performance, far exceeding early estimates.
- GraalVM JIT currently yields modest gains (up to ~22%) on small kernels; larger or longer-running workloads are needed to showcase its full potential on Aster.
- Pure Java bytecode remains the throughput leader, underscoring the value of extending backend coverage and keeping both implementations in sync.

**Recommendations**:
1. Prioritize stdlib parity for the Pure Java backend so more programs can be exercised across all three execution modes.
2. Iterate on JIT benchmarks with heavier workloads and tracing to confirm optimization thresholds.
3. Integrate the benchmark suites into CI as informational jobs to catch regressions early.

---

**Document Version**: 1.1  
**Last Updated**: 2025-11-03  
**Author**: Codex (GraalVM benchmark run)  
**Review Status**: Draft (JIT measurements added; awaiting backend coverage expansion)
