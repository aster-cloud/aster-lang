package aster.truffle;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能基准测试 - 测试 CPU 密集型计算场景
 *
 * 目标：验证 Truffle JIT 编译优化效果
 * - 递归深度测试
 * - 循环密集计算
 * - 函数调用开销
 */
public class BenchmarkTest {

  /**
   * 基准测试：阶乘计算（递归）
   * 测试场景：递归函数调用优化
   */
  @Test
  public void benchmarkFactorial() throws IOException {
    String json = """
        {
          "name": "bench.factorial",
          "decls": [
            {
              "kind": "Func",
              "name": "factorial",
              "params": [{"name": "n", "type": {"kind": "TypeName", "name": "Int"}}],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "If",
                  "cond": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "lte"},
                    "args": [
                      {"kind": "Name", "name": "n"},
                      {"kind": "Int", "value": 1}
                    ]
                  },
                  "thenBlock": {
                    "kind": "Block",
                    "statements": [{"kind": "Return", "expr": {"kind": "Int", "value": 1}}]
                  },
                  "elseBlock": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "mul"},
                        "args": [
                          {"kind": "Name", "name": "n"},
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "factorial"},
                            "args": [{
                              "kind": "Call",
                              "target": {"kind": "Name", "name": "sub"},
                              "args": [
                                {"kind": "Name", "name": "n"},
                                {"kind": "Int", "value": 1}
                              ]
                            }]
                          }
                        ]
                      }
                    }]
                  }
                }]
              }
            },
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "factorial"},
                    "args": [{"kind": "Int", "value": 10}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-factorial.json").build();

      // Warmup: 让 JIT 编译器优化代码
      for (int i = 0; i < 100; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 1000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(3628800, result.asInt(), "10! should be 3628800");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Factorial benchmark: %.3f ms per iteration (1000 iterations)%n", avgMs);

      // 验证性能：平均每次迭代应在合理范围内（宽松阈值，因为是解释执行）
      assertTrue(avgMs < 10.0, "Performance regression: " + avgMs + " ms > 10 ms");
    }
  }

  /**
   * 基准测试：斐波那契数列（递归）
   * 测试场景：重复递归调用优化
   */
  @Test
  public void benchmarkFibonacci() throws IOException {
    String json = """
        {
          "name": "bench.fibonacci",
          "decls": [
            {
              "kind": "Func",
              "name": "fib",
              "params": [{"name": "n", "type": {"kind": "TypeName", "name": "Int"}}],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "If",
                  "cond": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "lte"},
                    "args": [
                      {"kind": "Name", "name": "n"},
                      {"kind": "Int", "value": 1}
                    ]
                  },
                  "thenBlock": {
                    "kind": "Block",
                    "statements": [{"kind": "Return", "expr": {"kind": "Name", "name": "n"}}]
                  },
                  "elseBlock": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "add"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "fib"},
                            "args": [{
                              "kind": "Call",
                              "target": {"kind": "Name", "name": "sub"},
                              "args": [
                                {"kind": "Name", "name": "n"},
                                {"kind": "Int", "value": 1}
                              ]
                            }]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "fib"},
                            "args": [{
                              "kind": "Call",
                              "target": {"kind": "Name", "name": "sub"},
                              "args": [
                                {"kind": "Name", "name": "n"},
                                {"kind": "Int", "value": 2}
                              ]
                            }]
                          }
                        ]
                      }
                    }]
                  }
                }]
              }
            },
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "fib"},
                    "args": [{"kind": "Int", "value": 15}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-fib.json").build();

      // Warmup
      for (int i = 0; i < 50; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 100;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(610, result.asInt(), "fib(15) should be 610");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Fibonacci benchmark: %.3f ms per iteration (100 iterations)%n", avgMs);

      // 斐波那契递归更密集，阈值更宽松
      assertTrue(avgMs < 50.0, "Performance regression: " + avgMs + " ms > 50 ms");
    }
  }

  /**
   * 基准测试：简单算术计算
   * 测试场景：Builtin 函数调用优化
   */
  @Test
  public void benchmarkArithmetic() throws IOException {
    String json = """
        {
          "name": "bench.arithmetic",
          "decls": [
            {
              "kind": "Func",
              "name": "compute",
              "params": [{"name": "x", "type": {"kind": "TypeName", "name": "Int"}}],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "add"},
                    "args": [
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "mul"},
                        "args": [
                          {"kind": "Name", "name": "x"},
                          {"kind": "Int", "value": 2}
                        ]
                      },
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "div"},
                        "args": [
                          {"kind": "Name", "name": "x"},
                          {"kind": "Int", "value": 3}
                        ]
                      }
                    ]
                  }
                }]
              }
            },
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "compute"},
                    "args": [{"kind": "Int", "value": 100}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-arithmetic.json").build();

      // Warmup
      for (int i = 0; i < 1000; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 10000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(233, result.asInt(), "compute(100) should be 233");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Arithmetic benchmark: %.3f ms per iteration (10000 iterations)%n", avgMs);

      // 简单算术应该很快
      assertTrue(avgMs < 1.0, "Performance regression: " + avgMs + " ms > 1 ms");
    }
  }
}
