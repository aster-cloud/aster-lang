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
 * - Lambda/闭包性能
 * - 数据结构访问性能
 * - 模式匹配性能
 *
 * 测试方法论：
 * 1. Warmup 阶段：让 JIT 编译器优化代码
 * 2. 测量阶段：多次迭代测量平均性能
 * 3. 阈值验证：确保性能在可接受范围内
 *
 * 性能指标：
 * - 简单算术: < 1 ms/iteration (10000 iterations)
 * - 递归阶乘: < 10 ms/iteration (1000 iterations)
 * - 递归斐波那契: < 50 ms/iteration (100 iterations)
 * - Lambda 调用: < 5 ms/iteration (1000 iterations)
 * - 闭包捕获: < 15 ms/iteration (500 iterations)
 * - 模式匹配: < 2 ms/iteration (5000 iterations)
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

  /**
   * 基准测试：Lambda 高阶函数
   * 测试场景：Lambda 创建和调用开销
   *
   * 注意：Lambda benchmark 暂时禁用，等待 Core IR JSON 格式完善
   * TODO: 修复 Lambda JSON 格式（需要 ret, captures 字段）
   */
  // @Test
  public void benchmarkLambdaCall() throws IOException {
    String json = """
        {
          "name": "bench.lambda",
          "decls": [
            {
              "kind": "Func",
              "name": "apply",
              "params": [
                {"name": "f"},
                {"name": "x", "type": {"kind": "TypeName", "name": "Int"}}
              ],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "f"},
                    "args": [{"kind": "Name", "name": "x"}]
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
                "statements": [
                  {
                    "kind": "Let",
                    "name": "double",
                    "value": {
                      "kind": "Lambda",
                      "params": [{"name": "n"}],
                      "body": {
                        "kind": "Block",
                        "statements": [{
                          "kind": "Return",
                          "expr": {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "mul"},
                            "args": [
                              {"kind": "Name", "name": "n"},
                              {"kind": "Int", "value": 2}
                            ]
                          }
                        }]
                      }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "apply"},
                      "args": [
                        {"kind": "Name", "name": "double"},
                        {"kind": "Int", "value": 21}
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-lambda.json").build();

      // Warmup
      for (int i = 0; i < 200; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 1000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(42, result.asInt(), "apply(double, 21) should be 42");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Lambda call benchmark: %.3f ms per iteration (1000 iterations)%n", avgMs);

      // Lambda 调用开销应该合理
      assertTrue(avgMs < 5.0, "Performance regression: " + avgMs + " ms > 5 ms");
    }
  }

  /**
   * 基准测试：闭包捕获
   * 测试场景：闭包变量捕获和访问性能
   *
   * 注意：Closure benchmark 暂时禁用，等待 Core IR JSON 格式完善
   * TODO: 修复 Lambda JSON 格式（需要 ret, captures 字段）
   */
  // @Test
  public void benchmarkClosureCapture() throws IOException {
    String json = """
        {
          "name": "bench.closure",
          "decls": [
            {
              "kind": "Func",
              "name": "makeAdder",
              "params": [{"name": "x", "type": {"kind": "TypeName", "name": "Int"}}],
              "ret": {"kind": "TypeName", "name": "Any"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Lambda",
                    "params": [{"name": "y"}],
                    "body": {
                      "kind": "Block",
                      "statements": [{
                        "kind": "Return",
                        "expr": {
                          "kind": "Call",
                          "target": {"kind": "Name", "name": "add"},
                          "args": [
                            {"kind": "Name", "name": "x"},
                            {"kind": "Name", "name": "y"}
                          ]
                        }
                      }]
                    }
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
                "statements": [
                  {
                    "kind": "Let",
                    "name": "add10",
                    "value": {
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "makeAdder"},
                      "args": [{"kind": "Int", "value": 10}]
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "add10"},
                      "args": [{"kind": "Int", "value": 32}]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-closure.json").build();

      // Warmup
      for (int i = 0; i < 100; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 500;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(42, result.asInt(), "add10(32) should be 42");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Closure capture benchmark: %.3f ms per iteration (500 iterations)%n", avgMs);

      // 闭包捕获有额外开销，但应该可控
      assertTrue(avgMs < 15.0, "Performance regression: " + avgMs + " ms > 15 ms");
    }
  }

  /**
   * 基准测试：模式匹配
   * 测试场景：Match 表达式性能
   *
   * 注意：Match benchmark 暂时禁用，等待 Core IR JSON 格式完善
   * TODO: 修复 Match JSON 格式（使用 expr 而非 scrutinee，Case body 不应包装在 Block 中）
   */
  // @Test
  public void benchmarkPatternMatching() throws IOException {
    String json = """
        {
          "name": "bench.match",
          "decls": [
            {
              "kind": "Func",
              "name": "classify",
              "params": [{"name": "n", "type": {"kind": "TypeName", "name": "Int"}}],
              "ret": {"kind": "TypeName", "name": "Int"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Match",
                  "scrutinee": {"kind": "Name", "name": "n"},
                  "cases": [
                    {
                      "pattern": {"kind": "PatInt", "value": 0},
                      "body": {
                        "kind": "Block",
                        "statements": [{"kind": "Return", "expr": {"kind": "Int", "value": 100}}]
                      }
                    },
                    {
                      "pattern": {"kind": "PatInt", "value": 1},
                      "body": {
                        "kind": "Block",
                        "statements": [{"kind": "Return", "expr": {"kind": "Int", "value": 101}}]
                      }
                    },
                    {
                      "pattern": {"kind": "PatName", "name": "_"},
                      "body": {
                        "kind": "Block",
                        "statements": [{
                          "kind": "Return",
                          "expr": {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "add"},
                            "args": [
                              {"kind": "Name", "name": "n"},
                              {"kind": "Int", "value": 100}
                            ]
                          }
                        }]
                      }
                    }
                  ]
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
                    "target": {"kind": "Name", "name": "classify"},
                    "args": [{"kind": "Int", "value": 5}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-match.json").build();

      // Warmup
      for (int i = 0; i < 500; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 5000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(105, result.asInt(), "classify(5) should be 105");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Pattern matching benchmark: %.3f ms per iteration (5000 iterations)%n", avgMs);

      // 模式匹配应该比较快
      assertTrue(avgMs < 2.0, "Performance regression: " + avgMs + " ms > 2 ms");
    }
  }

  /**
   * 基准测试：Text.concat (字符串拼接)
   * 测试场景：验证 executeString() 快速路径性能
   * Phase 2B: Text builtin 内联优化
   */
  @Test
  public void benchmarkTextConcat() throws IOException {
    String json = """
        {
          "name": "bench.textconcat",
          "decls": [
            {
              "kind": "Func",
              "name": "concat10",
              "params": [{"name": "x", "type": {"kind": "TypeName", "name": "String"}}],
              "ret": {"kind": "TypeName", "name": "String"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "Text.concat"},
                    "args": [
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "Text.concat"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.concat"},
                            "args": [
                              {
                                "kind": "Call",
                                "target": {"kind": "Name", "name": "Text.concat"},
                                "args": [
                                  {"kind": "Name", "name": "x"},
                                  {"kind": "Name", "name": "x"}
                                ]
                              },
                              {
                                "kind": "Call",
                                "target": {"kind": "Name", "name": "Text.concat"},
                                "args": [
                                  {"kind": "Name", "name": "x"},
                                  {"kind": "Name", "name": "x"}
                                ]
                              }
                            ]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.concat"},
                            "args": [
                              {
                                "kind": "Call",
                                "target": {"kind": "Name", "name": "Text.concat"},
                                "args": [
                                  {"kind": "Name", "name": "x"},
                                  {"kind": "Name", "name": "x"}
                                ]
                              },
                              {
                                "kind": "Call",
                                "target": {"kind": "Name", "name": "Text.concat"},
                                "args": [
                                  {"kind": "Name", "name": "x"},
                                  {"kind": "Name", "name": "x"}
                                ]
                              }
                            ]
                          }
                        ]
                      },
                      {"kind": "Name", "name": "x"}
                    ]
                  }
                }]
              }
            },
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": {"kind": "TypeName", "name": "String"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "concat10"},
                    "args": [{"kind": "String", "value": "test"}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-textconcat.json").build();

      // Warmup
      for (int i = 0; i < 1000; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 10000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals("testtesttesttesttesttesttesttesttest", result.asString(),
                     "concat10(\"test\") should concatenate 8 times");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Text.concat benchmark: %.3f ms per iteration (10000 iterations)%n", avgMs);

      // Text.concat 应该很快（executeString 快速路径）
      assertTrue(avgMs < 1.0, "Performance regression: " + avgMs + " ms > 1 ms");
    }
  }

  /**
   * 基准测试：Text.length (字符串长度)
   * 测试场景：验证 executeString() 快速路径性能
   * Phase 2B: Text builtin 内联优化
   */
  @Test
  public void benchmarkTextLength() throws IOException {
    String json = """
        {
          "name": "bench.textlength",
          "decls": [
            {
              "kind": "Func",
              "name": "lengths",
              "params": [{"name": "s", "type": {"kind": "TypeName", "name": "String"}}],
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
                        "target": {"kind": "Name", "name": "add"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.length"},
                            "args": [{"kind": "Name", "name": "s"}]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.length"},
                            "args": [{"kind": "Name", "name": "s"}]
                          }
                        ]
                      },
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "add"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.length"},
                            "args": [{"kind": "Name", "name": "s"}]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "Text.length"},
                            "args": [{"kind": "Name", "name": "s"}]
                          }
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
                    "target": {"kind": "Name", "name": "lengths"},
                    "args": [{"kind": "String", "value": "hello world"}]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-textlength.json").build();

      // Warmup
      for (int i = 0; i < 1000; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 10000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(44, result.asInt(), "lengths(\"hello world\") should be 11*4=44");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("Text.length benchmark: %.3f ms per iteration (10000 iterations)%n", avgMs);

      // Text.length 应该非常快（executeString 快速路径 + 简单操作）
      assertTrue(avgMs < 0.5, "Performance regression: " + avgMs + " ms > 0.5 ms");
    }
  }

  /**
   * List.length 性能基准测试
   * 测试场景：4次 List.length 调用 + 算术运算
   * 关键性能对比：List.length (executeGeneric + instanceof) vs Text.length (executeString)
   * 目标：量化 instanceof 检查的性能开销
   */
  @Test
  public void benchmarkListLength() throws IOException {
    String json = """
        {
          "name": "bench.listlength",
          "decls": [
            {
              "kind": "Func",
              "name": "lengths",
              "params": [{"name": "lst", "type": {"kind": "TypeName", "name": "List"}}],
              "ret": {"kind": "TypeName", "name": "Int"},
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
                        "target": {"kind": "Name", "name": "add"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.length"},
                            "args": [{"kind": "Name", "name": "lst"}]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.length"},
                            "args": [{"kind": "Name", "name": "lst"}]
                          }
                        ]
                      },
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "add"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.length"},
                            "args": [{"kind": "Name", "name": "lst"}]
                          },
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.length"},
                            "args": [{"kind": "Name", "name": "lst"}]
                          }
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
                    "target": {"kind": "Name", "name": "lengths"},
                    "args": [{
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "List.append"},
                      "args": [
                        {
                          "kind": "Call",
                          "target": {"kind": "Name", "name": "List.append"},
                          "args": [
                            {
                              "kind": "Call",
                              "target": {"kind": "Name", "name": "List.append"},
                              "args": [
                                {
                                  "kind": "Call",
                                  "target": {"kind": "Name", "name": "List.append"},
                                  "args": [
                                    {
                                      "kind": "Call",
                                      "target": {"kind": "Name", "name": "List.append"},
                                      "args": [
                                        {
                                          "kind": "Call",
                                          "target": {"kind": "Name", "name": "List.empty"},
                                          "args": []
                                        },
                                        {"kind": "Int", "value": 1}
                                      ]
                                    },
                                    {"kind": "Int", "value": 2}
                                  ]
                                },
                                {"kind": "Int", "value": 3}
                              ]
                            },
                            {"kind": "Int", "value": 4}
                          ]
                        },
                        {"kind": "Int", "value": 5}
                      ]
                    }]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-listlength.json").build();

      // Warmup: 1000次迭代
      for (int i = 0; i < 1000; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 10000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(20, result.asInt(), "lengths([1,2,3,4,5]) should be 5*4=20");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("List.length benchmark: %.3f ms per iteration (10000 iterations)%n", avgMs);

      // List.length 应该快速（executeGeneric + instanceof 模式）
      // 允许比 Text.length 略慢（预期 5-20% 慢），因为 instanceof 检查开销
      assertTrue(avgMs < 1.0, "Performance regression: " + avgMs + " ms > 1.0 ms");
    }
  }

  /**
   * List.append 性能基准测试
   * 测试场景：连续 3 次 List.append 调用，测试对象分配性能影响
   * 关键风险：new ArrayList() 对象分配可能导致性能低于 0.01 ms 阈值
   * 目标：验证 executeGeneric + instanceof + 对象分配 模式的性能可行性
   */
  @Test
  public void benchmarkListAppend() throws IOException {
    String json = """
        {
          "name": "bench.listappend",
          "decls": [
            {
              "kind": "Func",
              "name": "appendMultiple",
              "params": [{"name": "base", "type": {"kind": "TypeName", "name": "List"}}],
              "ret": {"kind": "TypeName", "name": "List"},
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [{
                  "kind": "Return",
                  "expr": {
                    "kind": "Call",
                    "target": {"kind": "Name", "name": "List.append"},
                    "args": [
                      {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "List.append"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.append"},
                            "args": [
                              {"kind": "Name", "name": "base"},
                              {"kind": "Int", "value": 6}
                            ]
                          },
                          {"kind": "Int", "value": 7}
                        ]
                      },
                      {"kind": "Int", "value": 8}
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
                    "target": {"kind": "Name", "name": "List.length"},
                    "args": [{
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "appendMultiple"},
                      "args": [{
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "List.append"},
                        "args": [
                          {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "List.append"},
                            "args": [
                              {
                                "kind": "Call",
                                "target": {"kind": "Name", "name": "List.append"},
                                "args": [
                                  {
                                    "kind": "Call",
                                    "target": {"kind": "Name", "name": "List.append"},
                                    "args": [
                                      {
                                        "kind": "Call",
                                        "target": {"kind": "Name", "name": "List.append"},
                                        "args": [
                                          {
                                            "kind": "Call",
                                            "target": {"kind": "Name", "name": "List.empty"},
                                            "args": []
                                          },
                                          {"kind": "Int", "value": 1}
                                        ]
                                      },
                                      {"kind": "Int", "value": 2}
                                    ]
                                  },
                                  {"kind": "Int", "value": 3}
                                ]
                              },
                              {"kind": "Int", "value": 4}
                            ]
                          },
                          {"kind": "Int", "value": 5}
                        ]
                      }]
                    }]
                  }
                }]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "bench-listappend.json").build();

      // Warmup: 1000次迭代
      for (int i = 0; i < 1000; i++) {
        context.eval(source);
      }

      // 实际测试
      long start = System.nanoTime();
      int iterations = 10000;
      for (int i = 0; i < iterations; i++) {
        Value result = context.eval(source);
        assertEquals(8, result.asInt(), "appendMultiple([1,2,3,4,5]) should produce list of length 8");
      }
      long end = System.nanoTime();

      double avgMs = (end - start) / 1_000_000.0 / iterations;
      System.out.printf("List.append benchmark: %.3f ms per iteration (10000 iterations)%n", avgMs);

      // List.append 应该快速（但涉及对象分配：new ArrayList()）
      // 性能阈值：< 0.01 ms（比 List.length 的 1.0 ms 严格 100 倍）
      // 如果超过阈值，说明对象分配开销过大，触发 Batch 3 退出条件
      assertTrue(avgMs < 0.01, "Performance regression: " + avgMs + " ms >= 0.01 ms (object allocation overhead too high)");
    }
  }
}
