package aster.emitter;

import aster.core.ir.CoreModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 纯 Java 后端基准测试，对比 Truffle 结果使用相同的 Core IR。
 */
public class PureJavaBenchmark {

  private record BenchmarkCase(
      String displayName,
      String json,
      int warmupIterations,
      int measurementIterations,
      int expectedResult
  ) {}

  private record BenchmarkResult(String name, double avgMs, int iterations) {}

  private static final List<BenchmarkResult> SUMMARY_RESULTS = new ArrayList<>();

  private static final String FACTORIAL_JSON = """
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

  private static final String FIBONACCI_JSON = """
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
                      "target": {"kind": "Name", "name": "+"},
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
                  "args": [{"kind": "Int", "value": 20}]
                }
              }]
            }
          }
        ]
      }
      """;

  private static final String LIST_MAP_JSON = """
      {
        "name": "bench.list.map",
        "decls": [{
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
                "expr": {
                  "kind": "Lambda",
                  "params": [{"name": "x", "type": {"kind": "TypeName", "name": "Int"}}],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "captures": [],
                  "body": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "mul"},
                        "args": [
                          {"kind": "Name", "name": "x"},
                          {"kind": "Int", "value": 2}
                        ]
                      }
                    }]
                  }
                }
              },
              {
                "kind": "Let",
                "name": "numbers",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.empty"},
                  "args": []
                }
              },
              {
                "kind": "Let",
                "name": "numbers1",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.append"},
                  "args": [
                    {"kind": "Name", "name": "numbers"},
                    {"kind": "Int", "value": 1}
                  ]
                }
              },
              {
                "kind": "Let",
                "name": "numbers2",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.append"},
                  "args": [
                    {"kind": "Name", "name": "numbers1"},
                    {"kind": "Int", "value": 2}
                  ]
                }
              },
              {
                "kind": "Let",
                "name": "result",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.map"},
                  "args": [
                    {"kind": "Name", "name": "numbers2"},
                    {"kind": "Name", "name": "double"}
                  ]
                }
              },
              {
                "kind": "Return",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.length"},
                  "args": [{"kind": "Name", "name": "result"}]
                }
              }
            ]
          }
        }]
      }
      """;

  private static final String LIST_FILTER_JSON = """
      {
        "name": "bench.list.filter",
        "decls": [{
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
                "name": "isGreaterThanOne",
                "expr": {
                  "kind": "Lambda",
                  "params": [{"name": "x", "type": {"kind": "TypeName", "name": "Int"}}],
                  "ret": {"kind": "TypeName", "name": "Bool"},
                  "captures": [],
                  "body": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": ">"},
                        "args": [
                          {"kind": "Name", "name": "x"},
                          {"kind": "Int", "value": 1}
                        ]
                      }
                    }]
                  }
                }
              },
              {
                "kind": "Let",
                "name": "numbers",
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
                            {"kind": "Call", "target": {"kind": "Name", "name": "List.empty"}, "args": []},
                            {"kind": "Int", "value": 1}
                          ]
                        },
                        {"kind": "Int", "value": 2}
                      ]
                    },
                    {"kind": "Int", "value": 3}
                  ]
                }
              },
              {
                "kind": "Return",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "List.length"},
                  "args": [
                    {
                      "kind": "Call",
                      "target": {"kind": "Name", "name": "List.filter"},
                      "args": [
                        {"kind": "Name", "name": "numbers"},
                        {"kind": "Name", "name": "isGreaterThanOne"}
                      ]
                    }
                  ]
                }
              }
            ]
          }
        }]
      }
      """;

  private static final String LIST_REDUCE_JSON = """
      {
        "name": "bench.list.reduce",
        "decls": [{
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
                "name": "numbers",
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
                            {"kind": "Call", "target": {"kind": "Name", "name": "List.empty"}, "args": []},
                            {"kind": "Int", "value": 1}
                          ]
                        },
                        {"kind": "Int", "value": 2}
                      ]
                    },
                    {"kind": "Int", "value": 3}
                  ]
                }
              },
              {
                "kind": "Let",
                "name": "sum",
                "expr": {
                  "kind": "Lambda",
                  "params": [
                    {"name": "acc", "type": {"kind": "TypeName", "name": "Int"}},
                    {"name": "x", "type": {"kind": "TypeName", "name": "Int"}}
                  ],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "captures": [],
                  "body": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "+"},
                        "args": [
                          {"kind": "Name", "name": "acc"},
                          {"kind": "Name", "name": "x"}
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
                  "target": {"kind": "Name", "name": "List.reduce"},
                  "args": [
                    {"kind": "Name", "name": "numbers"},
                    {"kind": "Int", "value": 0},
                    {"kind": "Name", "name": "sum"}
                  ]
                }
              }
            ]
          }
        }]
      }
      """;

  private static final String RESULT_MAP_OK_JSON = """
      {
        "name": "bench.result_map_ok",
        "decls": [{
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
                "name": "inc",
                "expr": {
                  "kind": "Lambda",
                  "params": [{"name": "x", "type": {"kind": "TypeName", "name": "Int"}}],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "captures": [],
                  "body": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "+"},
                        "args": [
                          {"kind": "Name", "name": "x"},
                          {"kind": "Int", "value": 1}
                        ]
                      }
                    }]
                  }
                }
              },
              {
                "kind": "Let",
                "name": "okInput",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "aster.runtime.StdResult.okInt"},
                  "args": [
                    {"kind": "Int", "value": 5}
                  ]
                }
              },
              {
                "kind": "Let",
                "name": "mapped",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "Result.mapOk"},
                  "args": [
                    {"kind": "Name", "name": "okInput"},
                    {"kind": "Name", "name": "inc"}
                  ]
                }
              },
              {
                "kind": "Return",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "Result.unwrap"},
                  "args": [
                    {"kind": "Name", "name": "mapped"}
                  ]
                }
              }
            ]
          }
        }]
      }
      """;

  private static final String RESULT_MAP_ERR_JSON = """
      {
        "name": "bench.result_map_err",
        "decls": [{
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
                "name": "errLen",
                "expr": {
                  "kind": "Lambda",
                  "params": [{"name": "msg", "type": {"kind": "TypeName", "name": "Text"}}],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "captures": [],
                  "body": {
                    "kind": "Block",
                    "statements": [{
                      "kind": "Return",
                      "expr": {
                        "kind": "Call",
                        "target": {"kind": "Name", "name": "Text.length"},
                        "args": [{"kind": "Name", "name": "msg"}]
                      }
                    }]
                  }
                }
              },
              {
                "kind": "Let",
                "name": "errInput",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "aster.runtime.StdResult.errText"},
                  "args": [
                    {"kind": "String", "value": "oops"}
                  ]
                }
              },
              {
                "kind": "Let",
                "name": "mappedErr",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "Result.mapErr"},
                  "args": [
                    {"kind": "Name", "name": "errInput"},
                    {"kind": "Name", "name": "errLen"}
                  ]
                }
              },
              {
                "kind": "Return",
                "expr": {
                  "kind": "Call",
                  "target": {"kind": "Name", "name": "Result.unwrapErr"},
                  "args": [
                    {"kind": "Name", "name": "mappedErr"}
                  ]
                }
              }
            ]
          }
        }]
      }
      """;

  private static final String ARITHMETIC_JSON = """
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
                  "target": {"kind": "Name", "name": "+"},
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

  @Test
  public void listFilterKeepsEvenValues() throws Exception {
    Object raw = runModule(LIST_FILTER_JSON);
    assertTrue(raw instanceof Number, "List.filter 测试应返回数字结果");
    assertEquals(2, ((Number) raw).intValue(), "List.filter 结果数量不符");
  }

  @Test
  public void listReduceComputesSum() throws Exception {
    Object raw = runModule(LIST_REDUCE_JSON);
    assertTrue(raw instanceof Number, "List.reduce 测试应返回数字结果");
    assertEquals(6, ((Number) raw).intValue(), "List.reduce 累加结果不符");
  }

  @Test
  public void resultMapOkTransformsValue() throws Exception {
    Object raw = runModule(RESULT_MAP_OK_JSON);
    assertTrue(raw instanceof Number, "Result.mapOk 测试应返回数字结果");
    assertEquals(6, ((Number) raw).intValue(), "Result.mapOk 未正确映射 Ok 值");
  }

  @Test
  public void resultMapErrTransformsError() throws Exception {
    Object raw = runModule(RESULT_MAP_ERR_JSON);
    assertTrue(raw instanceof Number, "Result.mapErr 测试应返回数字结果");
    assertEquals(4, ((Number) raw).intValue(), "Result.mapErr 未正确映射 Err 错误");
  }

  @Test
  public void runPureJavaBenchmarks() throws Exception {
    // TODO: Add more complex benchmarks (QuickSort, BinaryTree, StringOps) once bytecode emission is fixed
    SUMMARY_RESULTS.clear();

    List<BenchmarkCase> cases = List.of(
        new BenchmarkCase("Factorial(10) Benchmark (Pure Java)", FACTORIAL_JSON, 100, 1000, 3_628_800),
        new BenchmarkCase("Fibonacci(20) Benchmark (Pure Java)", FIBONACCI_JSON, 50, 100, 6_765),
        new BenchmarkCase("List.map (2 items) Benchmark (Pure Java)", LIST_MAP_JSON, 1_000, 10_000, 2),
        new BenchmarkCase("Arithmetic Benchmark (Pure Java)", ARITHMETIC_JSON, 1_000, 10_000, 233)
    );

    System.out.println("\n================ Pure Java Backend Benchmarks ================");
    List<BenchmarkResult> results = new ArrayList<>();

    for (BenchmarkCase benchCase : cases) {
      System.out.printf("\n=== %s ===%n", benchCase.displayName());
      BenchmarkResult result = executeBenchmark(benchCase);
      results.add(result);
      SUMMARY_RESULTS.add(result);
    }

    System.out.println("\nSummary (Pure Java backend):");
    for (BenchmarkResult result : results) {
      System.out.printf("  %-40s : %.6f ms/iteration (%d iterations)%n",
          result.name(), result.avgMs(), result.iterations());
    }
  }

  private BenchmarkResult executeBenchmark(BenchmarkCase benchCase) throws Exception {
    CoreModel.Module module = parseModule(benchCase.json());
    String moduleName = (module.name == null || module.name.isEmpty()) ? "app" : module.name;

    Path outputDir = Files.createTempDirectory("pure-java-bench-");
    try {
      Main.CompileResult compileResult = Main.compile(module, outputDir, buildFuncHints(module));
      assertTrue(compileResult.success, "编译失败: " + String.join("\n", compileResult.errors));

      try (URLClassLoader loader = new URLClassLoader(new URL[]{outputDir.toUri().toURL()}, ClassLoader.getSystemClassLoader())) {
        Class<?> mainClass = Class.forName(moduleName + ".main_fn", true, loader);
        Method method = mainClass.getMethod("main");

        System.out.println("Warming up...");
        for (int i = 0; i < benchCase.warmupIterations(); i++) {
          invokeAndCheck(method, benchCase.expectedResult(), benchCase.displayName());
        }

        System.out.println("Measuring...");
        long start = System.nanoTime();
        for (int i = 0; i < benchCase.measurementIterations(); i++) {
          invokeAndCheck(method, benchCase.expectedResult(), benchCase.displayName());
        }
        long end = System.nanoTime();

        double avgMs = (end - start) / 1_000_000.0 / benchCase.measurementIterations();
        System.out.printf("Pure Java: %.6f ms/iteration (%d iterations)%n", avgMs, benchCase.measurementIterations());
        return new BenchmarkResult(benchCase.displayName(), avgMs, benchCase.measurementIterations());
      }
    } finally {
      // deleteDirectory(outputDir);
    }
  }

  private Object runModule(String json) throws Exception {
    CoreModel.Module module = parseModule(json);
    String moduleName = (module.name == null || module.name.isEmpty()) ? "app" : module.name;

    Path outputDir = Files.createTempDirectory("pure-java-run-");
    try {
      Main.CompileResult compileResult = Main.compile(module, outputDir, buildFuncHints(module));
      assertTrue(compileResult.success, "编译失败: " + String.join("\n", compileResult.errors));

      try (URLClassLoader loader = new URLClassLoader(new URL[]{outputDir.toUri().toURL()}, ClassLoader.getSystemClassLoader())) {
        Class<?> mainClass = Class.forName(moduleName + ".main_fn", true, loader);
        Method method = mainClass.getMethod("main");
        return method.invoke(null);
      }
    } finally {
      // deleteDirectory(outputDir);
    }
  }

  private static CoreModel.Module parseModule(String json) throws IOException {
    byte[] data = json.getBytes(StandardCharsets.UTF_8);
    try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
      return ModuleLoader.loadModule(in);
    }
  }

  private static int invokeAndCheck(Method method, int expected, String label) throws Exception {
    Object raw = method.invoke(null);
    assertTrue(raw instanceof Number, label + " 返回值必须是数字");
    int value = ((Number) raw).intValue();
    assertEquals(expected, value, label + " 结果不符合预期");
    return value;
  }

  private static void deleteDirectory(Path dir) {
    if (dir == null || !Files.exists(dir)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(dir)) {
      stream.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException __) {
          // 基准测试临时文件清理失败时忽略
        }
      });
    } catch (IOException __) {
      // 忽略清理异常
    }
  }

  private static String readBenchmarkJson(String relativePath) throws IOException {
    Path current = Path.of("").toAbsolutePath();
    for (int i = 0; i < 8; i++) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return Files.readString(candidate);
      }
      if (current.getParent() == null) {
        break;
      }
      current = current.getParent();
    }
    throw new IOException("无法找到基准测试 Core IR 文件: " + relativePath);
  }

  private static Map<String, Map<String, Character>> buildFuncHints(CoreModel.Module module) {
    if (module == null || module.name == null) {
      return Map.of();
    }
    if (!"bench.quicksort".equals(module.name)) {
      return Map.of();
    }

    Map<String, Map<String, Character>> hints = new HashMap<>();

    Map<String, Character> quicksort = new HashMap<>();
    quicksort.put("length", 'I');
    quicksort.put("pivot", 'I');
    quicksort.put("size", 'I');
    hints.put("bench.quicksort.quicksort", quicksort);

    Map<String, Character> partition = new HashMap<>();
    partition.put("elem", 'I');
    partition.put("index", 'I');
    partition.put("size", 'I');
    hints.put("bench.quicksort.partition", partition);

    Map<String, Character> buildList = new HashMap<>();
    buildList.put("limit", 'I');
    buildList.put("index", 'I');
    buildList.put("squared", 'I');
    buildList.put("scaled", 'I');
    buildList.put("value", 'I');
    hints.put("bench.quicksort.buildList", buildList);

    Map<String, Character> dropFrom = new HashMap<>();
    dropFrom.put("total", 'I');
    dropFrom.put("current", 'I');
    hints.put("bench.quicksort.dropFrom", dropFrom);

    return hints;
  }

  @AfterAll
  static void writePureJavaMeasurements() throws IOException {
    if (SUMMARY_RESULTS.isEmpty()) {
      return;
    }
    Path outputDir = Path.of("build", "benchmarks");
    Files.createDirectories(outputDir);
    Path output = outputDir.resolve("pure-java.json");

    StringBuilder sb = new StringBuilder();
    sb.append("{\"backend\":\"pure-java\",\"results\":[");
    for (int i = 0; i < SUMMARY_RESULTS.size(); i++) {
      BenchmarkResult result = SUMMARY_RESULTS.get(i);
      if (i > 0) {
        sb.append(',');
      }
      String name = result.name().replace("\"", "\\\"");
      sb.append(String.format(Locale.ROOT,
          "{\"benchmark\":\"%s\",\"avg_ms\":%.6f,\"iterations\":%d}",
          name, result.avgMs(), result.iterations()));
    }
    sb.append("]}\n");
    Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
  }
}
