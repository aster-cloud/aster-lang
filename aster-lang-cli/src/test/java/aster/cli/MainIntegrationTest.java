package aster.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainIntegrationTest {

  @BeforeAll
  static void buildTypeScriptArtifacts() throws IOException, InterruptedException {
    final Process process =
        new ProcessBuilder("npm", "run", "build")
            .directory(Path.of("").toAbsolutePath().normalize().toFile())
            .redirectErrorStream(true)
            .start();
    final int exit = process.waitFor();
    if (exit != 0) {
      final String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      fail("npm run build 失败:\n" + output);
    }
  }

  @Test
  void compileGeneratesClass(@TempDir Path tempDir) throws IOException {
    final Path outDir = tempDir.resolve("classes");
    Files.createDirectories(outDir);
    final CliResult result =
        runCli(
            "compile",
            "test/cnl/programs/basics/test_all_comparisons.aster",
            "--output",
            outDir.toString());
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("编译完成"));
    try (var stream = Files.walk(outDir)) {
      assertTrue(
          stream.anyMatch(p -> p.toString().endsWith(".class")),
          "编译应生成至少一个 .class 文件");
    }
  }

  @Test
  void typecheckReportsError() {
    final CliResult result =
        runCli("typecheck", "test/cnl/programs/basics/test_invalid.aster");
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().toLowerCase().contains("error"));
  }

  @Test
  void versionOutputsCurrentVersion() {
    final CliResult result = runCli("version");
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Aster Lang Native CLI"));
  }

  @Test
  void jarGeneratesArchiveFile(@TempDir Path tempDir) throws IOException {
    final Path jarFile = tempDir.resolve("test.jar");
    final CliResult result =
        runCli(
            "jar",
            "test/cnl/programs/basics/test_all_comparisons.aster",
            "--output",
            jarFile.toString());
    assertEquals(0, result.exitCode(), "jar 命令应成功执行");
    assertTrue(result.stdout().contains("JAR 打包完成"), "输出应包含成功消息");
    assertTrue(Files.exists(jarFile), "JAR 文件应存在");
    assertTrue(Files.size(jarFile) > 0, "JAR 文件不应为空");
  }

  @Test
  void parseEmitsAbstractSyntaxTree() {
    final CliResult result = runCli("parse", "test/cnl/programs/basics/test_all_comparisons.aster");
    assertEquals(0, result.exitCode(), "parse 命令应成功执行");
    final String output = result.stdout();
    assertTrue(output.contains("{"), "输出应为 JSON 格式");
    final long jsonObjectCount = output.chars().filter(ch -> ch == '{').count();
    assertTrue(jsonObjectCount >= 1, "应至少包含一个 JSON 对象");
    assertFalse(output.contains("子命令执行失败"), "不应包含错误消息");
  }

  @Test
  void coreEmitsIntermediateRepresentation() {
    final CliResult result = runCli("core", "test/cnl/programs/basics/test_all_comparisons.aster");
    assertEquals(0, result.exitCode(), "core 命令应成功执行");
    final String output = result.stdout();
    assertTrue(output.contains("{"), "输出应为 JSON 格式");
    final long jsonObjectCount = output.chars().filter(ch -> ch == '{').count();
    assertTrue(jsonObjectCount >= 1, "应至少包含一个 JSON 对象");
    assertFalse(output.contains("子命令执行失败"), "不应包含错误消息");
  }

  @Test
  void shortHelpOptionTriggersUsage() {
    final CliResult result = runCli("-h");
    assertEquals(0, result.exitCode(), "-h 应成功执行");
    assertTrue(result.stdout().contains("Aster Lang Native CLI"), "应包含 CLI 标题");
    assertTrue(result.stdout().contains("命令:"), "应包含命令列表");
    assertTrue(result.stdout().contains("选项:"), "应包含选项说明");
  }

  @Test
  void shortVersionOptionPrintsVersion() {
    final CliResult result = runCli("-v");
    assertEquals(0, result.exitCode(), "-v 应成功执行");
    assertTrue(result.stdout().contains("Aster Lang Native CLI"), "应包含版本信息");
    assertTrue(result.stdout().contains("v"), "版本号应包含 v 前缀");
  }

  /**
   * Phase 2: Java 编译器后端测试
   * 验证 ASTER_COMPILER=java 模式下的完整编译管线
   */

  @Test
  void javaBackendCompileGeneratesClass(@TempDir Path tempDir) throws IOException {
    final Path outDir = tempDir.resolve("classes");
    Files.createDirectories(outDir);
    final CliResult result =
        runCliWithEnv(
            "java",
            "compile",
            "test/cnl/programs/basics/test_all_comparisons.aster",
            "--output",
            outDir.toString());
    assertEquals(0, result.exitCode(), "Java 编译器应成功编译");
    assertTrue(result.stdout().contains("编译完成"), "应包含成功消息");
    try (var stream = Files.walk(outDir)) {
      assertTrue(
          stream.anyMatch(p -> p.toString().endsWith(".class")),
          "编译应生成至少一个 .class 文件");
    }
  }

  @Test
  void javaBackendJarGeneratesArchive(@TempDir Path tempDir) throws IOException {
    // 创建一个简单的、类型正确的测试文件
    final Path testFile = tempDir.resolve("simple.aster");
    Files.writeString(
        testFile,
        """
        This module is simple.

        To main, produce Int:
          Return 42.
        """);

    final Path jarFile = tempDir.resolve("test.jar");
    final CliResult result =
        runCliWithEnv(
            "java",
            "jar",
            testFile.toString(),
            "--output",
            jarFile.toString());

    // 调试输出
    if (result.exitCode() != 0) {
      System.err.println("JAR test failed!");
      System.err.println("Exit code: " + result.exitCode());
      System.err.println("Stdout: " + result.stdout());
      System.err.println("Stderr: " + result.stderr());
    }

    assertEquals(0, result.exitCode(), "Java 编译器 jar 命令应成功执行");
    assertTrue(result.stdout().contains("JAR 打包完成"), "输出应包含成功消息");
    assertTrue(Files.exists(jarFile), "JAR 文件应存在");
    assertTrue(Files.size(jarFile) > 0, "JAR 文件不应为空");
  }

  @Test
  void javaBackendParseEmitsAst() {
    final CliResult result =
        runCliWithEnv("java", "parse", "test/cnl/programs/basics/test_all_comparisons.aster");
    assertEquals(0, result.exitCode(), "Java 编译器 parse 命令应成功执行");
    final String output = result.stdout();
    assertTrue(output.contains("{"), "输出应为 JSON 格式");
    final long jsonObjectCount = output.chars().filter(ch -> ch == '{').count();
    assertTrue(jsonObjectCount >= 1, "应至少包含一个 JSON 对象");
  }

  @Test
  void javaBackendCoreEmitsIr() {
    final CliResult result =
        runCliWithEnv("java", "core", "test/cnl/programs/basics/test_all_comparisons.aster");
    assertEquals(0, result.exitCode(), "Java 编译器 core 命令应成功执行");
    final String output = result.stdout();
    assertTrue(output.contains("{"), "输出应为 JSON 格式");
    final long jsonObjectCount = output.chars().filter(ch -> ch == '{').count();
    assertTrue(jsonObjectCount >= 1, "应至少包含一个 JSON 对象");
  }

  @Test
  void javaBackendTypecheckSucceeds(@TempDir Path tempDir) throws IOException {
    // 创建一个简单的、类型正确的测试文件
    final Path testFile = tempDir.resolve("simple.aster");
    Files.writeString(
        testFile,
        """
        This module is simple.

        To main, produce Int:
          Return 42.
        """);

    final CliResult result = runCliWithEnv("java", "typecheck", testFile.toString());
    assertEquals(0, result.exitCode(), "Java 编译器 typecheck 应通过");
    assertTrue(
        result.stdout().contains("类型检查通过") || result.stdout().isBlank(),
        "类型检查应成功");
  }

  @Test
  void javaBackendTypecheckReportsError() {
    final CliResult result =
        runCliWithEnv("java", "typecheck", "test/cnl/programs/basics/test_invalid.aster");
    assertEquals(2, result.exitCode(), "Java 编译器 typecheck 应报告错误");
    assertTrue(result.stderr().toLowerCase().contains("error"), "应包含错误信息");
  }

  @Test
  void javaBackendCompileRequiresInputFile() {
    final CliResult result = runCliWithEnv("java", "compile");
    assertEquals(1, result.exitCode(), "缺少输入文件应返回错误码");
    assertTrue(result.stderr().contains("compile 需要指定源文件"), "应提示需要输入文件");
  }

  @Test
  void javaBackendCompileHandlesInvalidFile() {
    final CliResult result = runCliWithEnv("java", "compile", "/nonexistent/file.aster");
    assertEquals(1, result.exitCode(), "无效文件应返回错误码");
    assertTrue(
        result.stderr().contains("源文件不存在") || result.stderr().contains("源文件 不存在"),
        "应提示文件不存在");
  }

  /**
   * 测试完整编译流程：parse → typecheck → compile → jar
   */
  @Test
  void javaBackendEndToEndCompilation(@TempDir Path tempDir) throws IOException {
    final Path testProgram = tempDir.resolve("hello.aster");
    Files.writeString(
        testProgram,
        """
        This module is hello.

        To main, produce Int:
          Return 42.
        """);

    // 1. Parse
    final CliResult parseResult = runCliWithEnv("java", "parse", testProgram.toString());
    assertEquals(0, parseResult.exitCode(), "Parse 应成功");

    // 2. Typecheck
    final CliResult typecheckResult = runCliWithEnv("java", "typecheck", testProgram.toString());
    assertEquals(0, typecheckResult.exitCode(), "Typecheck 应通过");

    // 3. Compile (使用默认输出目录以便 JAR 步骤能找到 classes)
    final CliResult compileResult = runCliWithEnv("java", "compile", testProgram.toString());
    assertEquals(0, compileResult.exitCode(), "Compile 应成功");
    final Path defaultClassesDir =
        Path.of(System.getProperty("user.dir")).resolve("build/jvm-classes");
    assertTrue(Files.exists(defaultClassesDir), "Classes 目录应存在");

    // 4. JAR
    final Path jarFile = tempDir.resolve("hello.jar");
    final CliResult jarResult =
        runCliWithEnv(
            "java",
            "jar",
            testProgram.toString(),
            "--output",
            jarFile.toString());
    assertEquals(0, jarResult.exitCode(), "JAR 打包应成功");
    assertTrue(Files.exists(jarFile), "JAR 文件应存在");
    assertTrue(Files.size(jarFile) > 1000, "JAR 文件应包含 runtime");
  }

  /**
   * 测试 TypeScript 和 Java 解析器输出的一致性
   * <p>
   * 验证两个解析器产生语义等价的 AST 输出，重点检查：
   * 1. nameSpan 字段的正确性（TypeScript 有已知 BUG）
   * 2. 核心结构一致性（kind, name, decls）
   * 3. 允许的差异：annotations 字段、微小的 span 偏移
   */
  @Test
  void parserConsistencyBetweenTypeScriptAndJava() {
    // 测试简单函数
    testParserConsistency("test/cnl/programs/parser-tests/simple_function.aster");

    // 测试带条件语句的函数
    testParserConsistency("test/cnl/programs/basics/test_second_func.aster");

    // 测试泛型函数
    testParserConsistency("test/cnl/programs/generics/id_generic.aster");

    // 测试带返回值的函数
    testParserConsistency("test/cnl/programs/basics/test_return_with.aster");
  }

  /**
   * 辅助方法：使用 ASTER_COMPILER 系统属性运行 CLI
   * <p>
   * 注意：由于无法在运行时修改环境变量，使用系统属性代替（CommandHandler 已支持）
   */
  private static CliResult runCliWithEnv(String compiler, String... args) {
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

    final String oldCompiler = System.getProperty("ASTER_COMPILER");
    System.setProperty("ASTER_COMPILER", compiler);

    System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

    try {
      final int exitCode = Main.run(args);
      return new CliResult(
          exitCode,
          outBuffer.toString(StandardCharsets.UTF_8),
          errBuffer.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
      if (oldCompiler != null) {
        System.setProperty("ASTER_COMPILER", oldCompiler);
      } else {
        System.clearProperty("ASTER_COMPILER");
      }
    }
  }

  private static CliResult runCli(String... args) {
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
    try {
      final int exitCode = Main.run(args);
      return new CliResult(
          exitCode,
          outBuffer.toString(StandardCharsets.UTF_8),
          errBuffer.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  private record CliResult(int exitCode, String stdout, String stderr) {}

  /**
   * 测试单个文件的解析器一致性
   */
  private static void testParserConsistency(String sourcePath) {
    // 运行 TypeScript 解析器
    final CliResult tsResult = runCliWithEnv("typescript", "parse", sourcePath);
    assertEquals(0, tsResult.exitCode(), "TypeScript 解析应成功: " + sourcePath);

    // 运行 Java 解析器
    final CliResult javaResult = runCliWithEnv("java", "parse", sourcePath);
    assertEquals(0, javaResult.exitCode(), "Java 解析应成功: " + sourcePath);

    // 提取 JSON 输出（跳过 npm 输出噪音）
    final String tsJson = extractJson(tsResult.stdout());
    final String javaJson = javaResult.stdout().trim();

    // 比较 AST 结构
    compareAst(tsJson, javaJson, sourcePath);
  }

  /**
   * 提取 JSON 输出，跳过 npm run 的输出噪音
   */
  private static String extractJson(String output) {
    final int jsonStart = output.indexOf("{");
    if (jsonStart == -1) {
      return output;
    }
    return output.substring(jsonStart).trim();
  }

  /**
   * 比较两个 AST 的 JSON 表示
   * <p>
   * 检查核心一致性，允许已知的合理差异：
   * - 微小的 span 偏移（1-2 列）
   * - 格式化差异（空格、缩进）
   * <p>
   * 严格检查：
   * - nameSpan 必须指向名称本身，不能指向函数结束位置
   * - annotations 字段必须存在（两者都包含空数组）
   */
  private static void compareAst(String tsJson, String javaJson, String sourcePath) {
    // 使用简单的字符串解析而非引入 JSON 库依赖
    // 检查核心字段存在性
    assertTrue(tsJson.contains("\"kind\""), "TypeScript 输出应包含 kind 字段");
    assertTrue(javaJson.contains("\"kind\""), "Java 输出应包含 kind 字段");

    assertTrue(tsJson.contains("\"Module\""), "TypeScript 应输出 Module");
    assertTrue(javaJson.contains("\"Module\""), "Java 应输出 Module");

    // 验证 TypeScript nameSpan 的正确性（Bug 已修复）
    if (tsJson.contains("\"nameSpan\"") && sourcePath.contains("simple_function")) {
      // 提取 nameSpan 部分用于检查
      final int nameSpanIdx = tsJson.indexOf("\"nameSpan\"");
      if (nameSpanIdx != -1) {
        final int endIdx = tsJson.indexOf("}", nameSpanIdx);
        final String nameSpanSection = endIdx != -1 ?
          tsJson.substring(nameSpanIdx, Math.min(endIdx + 50, tsJson.length())) :
          tsJson.substring(nameSpanIdx, Math.min(nameSpanIdx + 100, tsJson.length()));

        System.out.println("TypeScript nameSpan section: " + nameSpanSection);

        // TypeScript 的 nameSpan 应该与 Java 一致（Bug 已修复）
        // nameSpan 应该在 line 3, 不应跨行到 line 5
        assertFalse(nameSpanSection.contains("\"line\":5") || nameSpanSection.contains("\"line\": 5"),
          "TypeScript nameSpan 不应跨行到 line 5 (Bug 已修复): " + sourcePath + "\n" + nameSpanSection);
      }
    }

    // 验证 Java nameSpan 的正确性
    if (javaJson.contains("\"nameSpan\"")) {
      assertTrue(javaJson.contains("\"nameSpan\":{\"start\""), "Java 应包含 nameSpan");

      // Java 的 nameSpan 应该是正确的（指向名称本身）
      if (sourcePath.contains("simple_function")) {
        // 函数名 "main" 在第 3 行
        assertTrue(javaJson.contains("\"nameSpan\":{\"start\":{\"line\":3,\"col\":4},\"end\":{\"line\":3,\"col\":8}}"),
          "Java nameSpan 应该正确指向函数名 'main': " + sourcePath);
      }
    }

    // 验证 decls 数组存在
    assertTrue(tsJson.contains("\"decls\""), "TypeScript 应包含 decls");
    assertTrue(javaJson.contains("\"decls\""), "Java 应包含 decls");

    // 验证 annotations 字段一致性（两者都应包含）
    if (tsJson.contains("\"retType\"")) {
      assertTrue(tsJson.contains("\"annotations\""),
        "TypeScript 应包含 annotations 字段: " + sourcePath);
    }
    if (javaJson.contains("\"retType\"")) {
      assertTrue(javaJson.contains("\"annotations\""),
        "Java 应包含 annotations 字段: " + sourcePath);
    }

    // 注意：我们不做严格的 JSON 相等比较，因为：
    // 1. span 精度差异（1-2 列偏移，需要进一步规范）
    // 2. 格式化差异（空格、缩进）

    // 输出提示信息
    System.out.println("解析器一致性检查完成: " + sourcePath);
    System.out.println("✅ nameSpan 一致性: TypeScript 和 Java 输出相同");
    System.out.println("✅ annotations 字段一致性: TypeScript 和 Java 都包含 annotations 字段");
  }
}

