package aster.emitter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden 测试：确保针对典型输入生成的字节码保持稳定，为后续重构提供安全网。
 */
class GoldenClassTest {

  private Path tempOutput;

  @BeforeEach
  void setUp() throws Exception {
    tempOutput = Files.createTempDirectory("aster-golden");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (tempOutput != null) {
      try (var paths = Files.walk(tempOutput)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception ignored) {
          }
        });
      }
    }
  }

  @Test
  void testGreetFunction() throws Exception {
    runGoldenTest("greet", "test/e2e/golden/core/expected_greet_core.json");
  }

  @Test
  void testDataStructure() throws Exception {
    runGoldenTest("data", "test/e2e/golden/core/expected_stdlib_collections_core.json");
  }

  @Test
  void testComplexLogic() throws Exception {
    runGoldenTest("complex", "test/cnl/programs/core-reference/nested_match_core.json");
  }

  @Test
  void testLambdaClosure() throws Exception {
    runGoldenTest("lambda_closure", "test/cnl/programs/core-reference/lambda_closure_core.json");
  }

  @Test
  void testResultTryCatch() throws Exception {
    runGoldenTest("result_trycatch", "test/cnl/programs/core-reference/result_trycatch_core.json");
  }

  @Test
  void testArithmeticExpr() throws Exception {
    runGoldenTest("arithmetic_expr", "test/cnl/programs/core-reference/arithmetic_expr_core.json");
  }

  @Test
  void testNestedCall() throws Exception {
    runGoldenTest("nested_call", "test/cnl/programs/core-reference/nested_call_core.json");
  }

  @Test
  void testMatchCtor() throws Exception {
    runGoldenTest("match_ctor", "test/cnl/programs/core-reference/match_ctor_core.json");
  }

  @Test
  void testSetStatement() throws Exception {
    runGoldenTest("set_statement", "test/cnl/programs/core-reference/set_statement_core.json");
  }

  @Test
  void testAwaitExpression() throws Exception {
    runGoldenTest("await_expression", "test/cnl/programs/core-reference/await_expr_core.json");
  }

  @Test
  void testWorkflowLinear() throws Exception {
    runGoldenTest("workflow_linear", "test/e2e/golden/core/expected_workflow-linear_core.json");
  }

  /**
   * 统一执行流程：读取基线、重新编译并比较结构。
   */
  private void runGoldenTest(String scenario, String inputRelativePath) throws Exception {
    Map<String, byte[]> expected = readGoldenClasses(scenario);
    Map<String, byte[]> actual = emitClasses(resolveInput(inputRelativePath));
    assertEquals(expected.keySet(), actual.keySet(), "生成的类文件集合不一致，场景=" + scenario);
    for (Map.Entry<String, byte[]> entry : expected.entrySet()) {
      String relativePath = entry.getKey();
      byte[] expectedBytes = entry.getValue();
      byte[] actualBytes = actual.get(relativePath);
      BytecodeComparator.assertEquals(expectedBytes, actualBytes);
    }
  }

  /**
   * 从资源目录中加载 golden .class 文件。
   */
  private Map<String, byte[]> readGoldenClasses(String scenario) throws Exception {
    var resource = getClass().getClassLoader().getResource("golden-classes/" + scenario);
    if (resource == null) throw new IllegalStateException("缺少场景 '" + scenario + "' 的 golden 基线");
    Path base = Path.of(resource.toURI());
    try (var stream = Files.walk(base)) {
      return stream
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".class"))
        .collect(Collectors.toMap(
          path -> base.relativize(path).toString().replace('\\', '/'),
          path -> {
            try {
              return Files.readAllBytes(path);
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          },
          (left, right) -> left,
          LinkedHashMap::new
        ));
    }
  }

  /**
   * 调用现有 Main 入口重新生成字节码，并按相对路径读取。
   */
  private Map<String, byte[]> emitClasses(Path inputPath) throws Exception {
    byte[] payload = Files.readAllBytes(inputPath);
    InputStream original = System.in;
    try {
      System.setIn(new ByteArrayInputStream(payload));
      Main.main(new String[]{tempOutput.toString()});
    } finally {
      System.setIn(original);
    }

    try (var stream = Files.walk(tempOutput)) {
      return stream
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".class"))
        .collect(Collectors.toMap(
          path -> tempOutput.relativize(path).toString().replace('\\', '/'),
          path -> {
            try {
              return Files.readAllBytes(path);
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          },
          (left, right) -> left,
          LinkedHashMap::new
        ));
    }
  }

  /**
   * 在模块/项目层级向上查找输入文件，兼容 Gradle 测试的工作目录差异。
   */
  private Path resolveInput(String relative) throws Exception {
    Path cursor = Paths.get("").toAbsolutePath();
    while (cursor != null) {
      Path candidate = cursor.resolve(relative);
      if (Files.exists(candidate)) return candidate;
      cursor = cursor.getParent();
    }
    throw new IllegalStateException("无法定位输入文件: " + relative);
  }
}
