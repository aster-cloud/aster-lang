package aster.cli.compiler;

import aster.cli.TypeScriptBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Java 编译器后端的 Lexer 集成
 */
class JavaLexerTest {

    private JavaCompilerBackend backend;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        backend = new JavaCompilerBackend();
    }

    @Test
    void testLexStageIsAvailable() {
        assertTrue(backend.isStageAvailable("lex"),
            "lex 阶段应该可用");
    }

    @Test
    void testLexBasicInput() throws Exception {
        // 创建测试输入文件
        Path inputFile = tempDir.resolve("test.aster");
        String input = "x + 1";
        Files.writeString(inputFile, input);

        // 执行词法分析
        var result = backend.execute("lex", List.of(inputFile.toString()));

        // 验证结果
        assertEquals(0, result.exitCode(), "应该成功执行");
        assertFalse(result.stdout().isEmpty(), "应该有输出");
        assertTrue(result.stdout().contains("\"kind\":\"IDENT\""), "应该包含 IDENT token");
        assertTrue(result.stdout().contains("\"kind\":\"PLUS\""), "应该包含 PLUS token");
        assertTrue(result.stdout().contains("\"kind\":\"INT\""), "应该包含 INT token");
    }

    @Test
    void testLexWithIndentation() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "line1\n  line2";
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"kind\":\"INDENT\""), "应该包含 INDENT token");
        assertTrue(result.stdout().contains("\"kind\":\"DEDENT\""), "应该包含 DEDENT token");
    }

    @Test
    void testLexWithComments() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "# comment\ncode";
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"kind\":\"COMMENT\""), "应该包含 COMMENT token");
        assertTrue(result.stdout().contains("\"channel\":\"trivia\""), "注释应该在 trivia 通道");
    }

    @Test
    void testLexWithString() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "\"Hello, world!\"";
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"kind\":\"STRING\""), "应该包含 STRING token");
        assertTrue(result.stdout().contains("Hello, world!"), "应该包含字符串内容");
    }

    @Test
    void testLexErrorHandling_InvalidCharacter() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "x + $"; // $ 不是合法字符
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(1, result.exitCode(), "应该返回错误码");
        assertTrue(result.stderr().contains("词法分析失败"), "应该有错误信息");
    }

    @Test
    void testLexErrorHandling_InvalidIndentation() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "line1\n line2"; // 1 space (奇数)
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(1, result.exitCode(), "应该返回错误码");
        assertTrue(result.stderr().contains("Invalid indentation"), "应该有缩进错误信息");
    }

    @Test
    void testLexRequiresInputFile() {
        var result = backend.execute("lex", List.of());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("需要提供输入文件"));
    }

    @Test
    void testLexFileNotFound() {
        var result = backend.execute("lex", List.of("/nonexistent/file.aster"));

        assertEquals(1, result.exitCode(), "应该返回错误码");
        assertTrue(result.stderr().contains("读取文件失败"), "应该有错误信息");
    }

    @Test
    void testLexJSONOutput() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "42";
        Files.writeString(inputFile, input);

        var result = backend.execute("lex", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        String output = result.stdout();

        // 验证 JSON 格式
        assertTrue(output.startsWith("["), "输出应该是 JSON 数组");
        assertTrue(output.endsWith("]\n"), "输出应该以 ] 结尾");
        assertTrue(output.contains("\"kind\""), "应该包含 kind 字段");
        assertTrue(output.contains("\"value\""), "应该包含 value 字段");
        assertTrue(output.contains("\"start\""), "应该包含 start 字段");
        assertTrue(output.contains("\"end\""), "应该包含 end 字段");
    }
}
