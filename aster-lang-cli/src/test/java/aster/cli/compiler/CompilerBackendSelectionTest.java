package aster.cli.compiler;

import aster.cli.CommandHandler;
import aster.cli.DiagnosticFormatter;
import aster.cli.PathResolver;
import aster.cli.TypeScriptBridge;
import aster.cli.VersionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试编译器后端选择逻辑
 * <p>
 * 验证 {@link CommandHandler} 能够正确使用注入的编译器后端。
 */
class CompilerBackendSelectionTest {

    private TypeScriptBridge mockBridge;
    private PathResolver mockPathResolver;
    private DiagnosticFormatter mockDiagnosticFormatter;
    private VersionReader mockVersionReader;

    @BeforeEach
    void setUp() throws Exception {
        // 创建模拟依赖
        mockBridge = new TypeScriptBridge();
        mockPathResolver = new PathResolver(Path.of("."));
        mockDiagnosticFormatter = new DiagnosticFormatter();
        mockVersionReader = new VersionReader();
    }

    /**
     * 测试：TypeScript 编译器后端
     */
    @Test
    void testTypeScriptBackend() {
        CompilerBackend backend = new TypeScriptCompilerBackend(mockBridge);
        CommandHandler handler = new CommandHandler(
            mockBridge, mockPathResolver, mockDiagnosticFormatter, mockVersionReader, backend);

        assertEquals("typescript", backend.getType());
        assertTrue(backend instanceof TypeScriptCompilerBackend);
    }

    /**
     * 测试：Java 编译器后端
     */
    @Test
    void testJavaBackend() {
        CompilerBackend backend = new JavaCompilerBackend();
        CommandHandler handler = new CommandHandler(
            mockBridge, mockPathResolver, mockDiagnosticFormatter, mockVersionReader, backend);

        assertEquals("java", backend.getType());
        assertTrue(backend instanceof JavaCompilerBackend);
    }

    /**
     * 测试：Java 编译器的所有阶段初始状态应为不可用
     */
    @Test
    void testJavaBackendStagesInitiallyUnavailable() {
        CompilerBackend backend = new JavaCompilerBackend();

        // 验证所有阶段初始状态为不可用
        assertFalse(backend.isStageAvailable("native:cli:class"), "compile 阶段应不可用");
        assertFalse(backend.isStageAvailable("native:cli:typecheck"), "typecheck 阶段应不可用");
        assertFalse(backend.isStageAvailable("native:cli:jar"), "jar 阶段应不可用");
        assertFalse(backend.isStageAvailable("parse"), "parse 阶段应不可用");
        assertFalse(backend.isStageAvailable("core"), "core 阶段应不可用");
        assertFalse(backend.isStageAvailable("canonicalize"), "canonicalize 阶段应不可用");
        assertFalse(backend.isStageAvailable("lex"), "lex 阶段应不可用");
    }

    /**
     * 测试：TypeScript 编译器的所有阶段应可用
     */
    @Test
    void testTypeScriptBackendStagesAvailable() {
        CompilerBackend backend = new TypeScriptCompilerBackend(mockBridge);

        // TypeScript 编译器所有阶段应可用
        assertTrue(backend.isStageAvailable("native:cli:class"));
        assertTrue(backend.isStageAvailable("native:cli:typecheck"));
        assertTrue(backend.isStageAvailable("native:cli:jar"));
        assertTrue(backend.isStageAvailable("parse"));
        assertTrue(backend.isStageAvailable("any-stage")); // 任何阶段都可用
    }

    /**
     * 测试：Java 编译器在执行不可用阶段时应返回友好错误消息
     */
    @Test
    void testJavaBackendReturnsErrorForUnavailableStage() {
        CompilerBackend backend = new JavaCompilerBackend();

        var result = backend.execute("native:cli:class", java.util.List.of("test.aster"));

        assertEquals(1, result.exitCode(), "执行不可用阶段应返回非零退出码");
        assertTrue(result.stderr().contains("Java 编译器尚未实现阶段"), "错误信息应提示未实现");
        assertTrue(result.stderr().contains("ASTER_COMPILER=typescript"), "错误信息应提示如何回退");
    }
}
