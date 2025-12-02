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
 * 测试 Java 编译器后端的 Canonicalizer 集成
 */
class JavaCanonicalizeTest {

    private JavaCompilerBackend backend;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        backend = new JavaCompilerBackend();
    }

    @Test
    void testCanonicalizeStageIsAvailable() {
        assertTrue(backend.isStageAvailable("canonicalize"),
            "canonicalize 阶段应该可用");
    }

    @Test
    void testCanonicalizeBasicInput() throws Exception {
        // 创建测试输入文件
        Path inputFile = tempDir.resolve("test.aster");
        String input = "This Module Is app.\n// Comment\nTo greet: Return \"Hello\".";
        Files.writeString(inputFile, input);

        // 执行规范化
        var result = backend.execute("canonicalize", List.of(inputFile.toString()));

        // 验证结果
        assertEquals(0, result.exitCode(), "应该成功执行");
        assertFalse(result.stdout().isEmpty(), "应该有输出");
        assertTrue(result.stdout().contains("this module is"), "应该规范化关键字大小写");
        assertFalse(result.stdout().contains("// Comment"), "应该移除注释");
    }

    @Test
    void testCanonicalizeWithArticleRemoval() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "define the function";
        Files.writeString(inputFile, input);

        var result = backend.execute("canonicalize", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        assertEquals("define function", result.stdout());
    }

    @Test
    void testCanonicalizePreservesStringContent() throws Exception {
        Path inputFile = tempDir.resolve("test.aster");
        String input = "print \"the quick brown fox\"";
        Files.writeString(inputFile, input);

        var result = backend.execute("canonicalize", List.of(inputFile.toString()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("\"the quick brown fox\""),
            "字符串内的冠词应该保留");
    }

    @Test
    void testCanonicalizeErrorHandling() {
        // 测试文件不存在的情况
        var result = backend.execute("canonicalize", List.of("/nonexistent/file.aster"));

        assertEquals(1, result.exitCode(), "应该返回错误码");
        assertTrue(result.stderr().contains("读取文件失败"), "应该有错误信息");
    }

    @Test
    void testCanonicalizeRequiresInputFile() {
        var result = backend.execute("canonicalize", List.of());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("需要提供输入文件"));
    }
}
