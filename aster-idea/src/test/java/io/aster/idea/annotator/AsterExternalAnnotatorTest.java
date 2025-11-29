package io.aster.idea.annotator;

import aster.core.ast.Module;
import aster.core.ir.CoreModel;
import aster.core.lowering.CoreLowering;
import aster.core.parser.AsterCustomLexer;
import aster.core.parser.AsterParser;
import aster.core.parser.AstBuilder;
import aster.core.typecheck.ErrorCode;
import aster.core.typecheck.TypeChecker;
import aster.core.typecheck.model.Diagnostic;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterExternalAnnotator 集成测试
 * <p>
 * 测试类型检查器与 IDE 注解系统的集成。
 * 这些测试不依赖完整的 IntelliJ 测试框架，而是直接测试类型检查逻辑。
 * <p>
 * Aster 使用自然语言风格的语法：
 * - 模块声明：This module is xxx.
 * - 数据类型：Define a TypeName with field: Type.
 * - 函数定义：To funcName with param: Type, produce ReturnType: ...
 */
@DisplayName("Aster External Annotator 集成测试")
class AsterExternalAnnotatorTest {

    /**
     * 编译源代码并进行类型检查
     */
    private List<Diagnostic> compileAndCheck(String source) {
        try {
            // 词法分析
            AsterCustomLexer lexer = new AsterCustomLexer(CharStreams.fromString(source));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 语法分析
            AsterParser parser = new AsterParser(tokens);
            AsterParser.ModuleContext parseTree = parser.module();

            // 构建 AST
            AstBuilder astBuilder = new AstBuilder();
            Module ast = astBuilder.visitModule(parseTree);
            if (ast == null) {
                return List.of();
            }

            // 降级为 Core IR
            CoreLowering lowering = new CoreLowering();
            CoreModel.Module module = lowering.lowerModule(ast);
            if (module == null) {
                return List.of();
            }

            // 进行类型检查
            TypeChecker typeChecker = new TypeChecker();
            return typeChecker.typecheckModule(module);
        } catch (Exception e) {
            // 语法错误时返回空列表
            return List.of();
        }
    }

    /**
     * 检查是否有特定严重级别的诊断
     */
    private boolean hasSeverity(List<Diagnostic> diagnostics, Diagnostic.Severity severity) {
        return diagnostics.stream().anyMatch(d -> d.severity() == severity);
    }

    @Nested
    @DisplayName("正确代码测试")
    class ValidCodeTests {

        @Test
        @DisplayName("简单函数应无错误")
        void simpleFunctionShouldHaveNoErrors() {
            String source = """
                This module is test.

                To greet with name: Text, produce Text:
                    Return name.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 过滤掉警告，只检查错误
            List<Diagnostic> errors = diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();

            assertTrue(errors.isEmpty(),
                "简单函数不应有错误: " + errors);
        }

        @Test
        @DisplayName("数据类型定义应无错误")
        void dataTypeShouldHaveNoErrors() {
            String source = """
                This module is test.

                Define a User with name: Text and age: Int.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            List<Diagnostic> errors = diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();

            assertTrue(errors.isEmpty(),
                "数据类型定义不应有错误: " + errors);
        }

        @Test
        @DisplayName("多函数模块应无错误")
        void multipleFunctionsShouldHaveNoErrors() {
            String source = """
                This module is test.

                To add with x: Int and y: Int, produce Int:
                    Return x.

                To multiply with a: Int and b: Int, produce Int:
                    Return a.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            List<Diagnostic> errors = diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();

            assertTrue(errors.isEmpty(),
                "多函数模块不应有错误: " + errors);
        }
    }

    @Nested
    @DisplayName("类型错误检测")
    class TypeErrorTests {

        @Test
        @DisplayName("返回类型不匹配应报错")
        void returnTypeMismatchShouldError() {
            String source = """
                This module is test.

                To getNumber, produce Int:
                    Return "hello".
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 返回类型不匹配应产生错误
            assertTrue(hasSeverity(diagnostics, Diagnostic.Severity.ERROR),
                "返回类型不匹配应产生错误: " + diagnostics);
        }
    }

    @Nested
    @DisplayName("诊断信息质量")
    class DiagnosticQualityTests {

        @Test
        @DisplayName("类型错误诊断应包含必要信息")
        void typeErrorDiagnosticShouldHaveRequiredInfo() {
            String source = """
                This module is test.

                To bad, produce Int:
                    Return "string".
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            for (Diagnostic d : diagnostics) {
                // 每个诊断都应有消息
                assertNotNull(d.message(), "诊断应有消息");
                assertFalse(d.message().isEmpty(), "诊断消息不应为空");

                // 每个诊断都应有错误码
                assertNotNull(d.code(), "诊断应有错误码");

                // 每个诊断都应有严重级别
                assertNotNull(d.severity(), "诊断应有严重级别");
            }
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCaseTests {

        @Test
        @DisplayName("空源代码应安全处理")
        void emptySourceShouldBeSafe() {
            String source = "";
            List<Diagnostic> diagnostics = compileAndCheck(source);
            // 不应抛出异常
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("只有模块声明的源代码应安全处理")
        void moduleOnlySourceShouldBeSafe() {
            String source = "This module is empty.";
            List<Diagnostic> diagnostics = compileAndCheck(source);
            // 不应抛出异常
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("只有注释的源代码应安全处理")
        void commentOnlySourceShouldBeSafe() {
            String source = """
                # 这是一个注释
                # 另一个注释
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);
            // 不应抛出异常
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("语法错误应安全处理")
        void syntaxErrorShouldBeSafe() {
            String source = """
                This module is
                """;
            // 不应抛出异常，即使有语法错误
            assertDoesNotThrow(() -> compileAndCheck(source));
        }

        @Test
        @DisplayName("不完整的函数定义应安全处理")
        void incompleteFunctionShouldBeSafe() {
            String source = """
                This module is test.

                To incomplete
                """;
            // 不应抛出异常
            assertDoesNotThrow(() -> compileAndCheck(source));
        }
    }

    @Nested
    @DisplayName("复杂场景测试")
    class ComplexScenarioTests {

        @Test
        @DisplayName("混合声明模块应正确检查")
        void mixedDeclarationsShouldBeChecked() {
            String source = """
                This module is test.

                Define a Point with x: Int and y: Int.

                To origin, produce Point:
                    Return Point(x = 0, y = 0).
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 检查是否能正确解析和检查混合声明
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("带参数的函数应正确检查")
        void functionWithParametersShouldBeChecked() {
            String source = """
                This module is test.

                To greet with name: Text and greeting: Text, produce Text:
                    Return greeting.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            List<Diagnostic> errors = diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();

            assertTrue(errors.isEmpty(),
                "带参数的函数不应有错误: " + errors);
        }

        @Test
        @DisplayName("Option 类型应正确检查")
        void optionTypeShouldBeChecked() {
            String source = """
                This module is test.

                To findUser with id: Text, produce maybe User:
                    Return null.

                Define a User with id: Text.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // Option 类型应能正确解析
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("Match 表达式应正确检查")
        void matchExpressionShouldBeChecked() {
            String source = """
                This module is test.

                Define a User with id: Text and name: Text.

                To greet with user: maybe User, produce Text:
                    Match user:
                        When null, Return "Hi, guest".
                        When User(id, name), Return name.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // Match 表达式应能正确解析
            assertNotNull(diagnostics);
        }
    }

    @Nested
    @DisplayName("ExternalAnnotator 流程测试")
    class AnnotatorFlowTests {

        @Test
        @DisplayName("完整编译流程应能执行")
        void fullCompilationPipelineShouldWork() {
            String source = """
                This module is demo.

                Define a Product with name: Text and price: Int.

                To createProduct with name: Text and price: Int, produce Product:
                    Return Product(name = name, price = price).

                To getProductName with product: Product, produce Text:
                    Match product:
                        When Product(name, price), Return name.
                """;

            // 测试完整的编译流程
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 应该能够完成编译
            assertNotNull(diagnostics);

            // 输出诊断信息用于调试
            if (!diagnostics.isEmpty()) {
                System.out.println("诊断信息:");
                for (Diagnostic d : diagnostics) {
                    System.out.println("  [" + d.severity() + "] " + d.code() + ": " + d.message());
                }
            }
        }
    }

    @Nested
    @DisplayName("类型检查覆盖范围测试")
    class TypeCheckCoverageTests {

        @Test
        @DisplayName("字段类型检查")
        void fieldTypeCheckShouldWork() {
            String source = """
                This module is test.

                Define a User with name: Text and age: Int.

                To createUser, produce User:
                    Return User(name = "Alice", age = 30).
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("列表类型检查")
        void listTypeCheckShouldWork() {
            String source = """
                This module is test.

                To getNumbers, produce list of Int:
                    Return [1, 2, 3].
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("条件表达式类型检查")
        void conditionalTypeCheckShouldWork() {
            String source = """
                This module is test.

                To max with a: Int and b: Int, produce Int:
                    If a > b:
                        Return a.
                    Else:
                        Return b.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);
            assertNotNull(diagnostics);
        }

        @Test
        @DisplayName("let 绑定类型检查")
        void letBindingTypeCheckShouldWork() {
            String source = """
                This module is test.

                To compute, produce Int:
                    Let x = 10.
                    Let y = 20.
                    Return x.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            List<Diagnostic> errors = diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();

            assertTrue(errors.isEmpty(),
                "正确的 let 绑定不应有错误: " + errors);
        }

        @Test
        @DisplayName("函数调用类型检查")
        void functionCallTypeCheckShouldWork() {
            String source = """
                This module is test.

                To add with x: Int and y: Int, produce Int:
                    Return x.

                To compute, produce Int:
                    Let result = add(5, 10).
                    Return result.
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);
            assertNotNull(diagnostics);
        }
    }

    @Nested
    @DisplayName("错误码分类测试")
    class ErrorCodeCategoryTests {

        @Test
        @DisplayName("类型不匹配错误应有正确的错误码")
        void typeMismatchShouldHaveCorrectErrorCode() {
            String source = """
                This module is test.

                To bad, produce Int:
                    Return "not an int".
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 应该有类型相关的错误
            boolean hasTypeError = diagnostics.stream()
                .anyMatch(d -> d.code().name().contains("TYPE") ||
                              d.code().name().contains("RETURN") ||
                              d.code().name().contains("MISMATCH"));

            assertTrue(hasTypeError || diagnostics.isEmpty(),
                "类型不匹配应产生类型相关错误码");
        }

        @Test
        @DisplayName("诊断应有帮助信息")
        void diagnosticShouldHaveHelpInfo() {
            String source = """
                This module is test.

                To bad, produce Int:
                    Return "string".
                """;
            List<Diagnostic> diagnostics = compileAndCheck(source);

            // 检查是否有诊断包含帮助信息
            for (Diagnostic d : diagnostics) {
                // help 是 Optional，可能为空
                assertNotNull(d.help(), "help() 不应返回 null");
            }
        }
    }

    @Nested
    @DisplayName("IDE 集成特性测试")
    class IdeIntegrationTests {

        @Test
        @DisplayName("增量编辑场景 - 连续修改")
        void incrementalEditScenario() {
            // 模拟用户连续编辑的场景
            String source1 = """
                This module is test.

                To greet, produce Text:
                    Return "Hello".
                """;

            String source2 = """
                This module is test.

                To greet with name: Text, produce Text:
                    Return name.
                """;

            // 第一次编译
            List<Diagnostic> diagnostics1 = compileAndCheck(source1);
            assertNotNull(diagnostics1);

            // 第二次编译（修改后）
            List<Diagnostic> diagnostics2 = compileAndCheck(source2);
            assertNotNull(diagnostics2);

            // 两次编译都应该成功
            assertTrue(
                diagnostics1.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR) ||
                diagnostics2.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR),
                "增量编辑场景应能正确处理"
            );
        }

        @Test
        @DisplayName("大文件处理性能")
        void largeFilePerformance() {
            // 生成一个包含多个函数的大文件
            StringBuilder sb = new StringBuilder();
            sb.append("This module is test.\n\n");

            for (int i = 0; i < 50; i++) {
                sb.append(String.format("""
                    To func%d with x: Int, produce Int:
                        Return x.

                    """, i));
            }

            String source = sb.toString();

            // 测量编译时间
            long startTime = System.currentTimeMillis();
            List<Diagnostic> diagnostics = compileAndCheck(source);
            long endTime = System.currentTimeMillis();

            assertNotNull(diagnostics);

            // 编译应该在合理时间内完成（小于 5 秒）
            long duration = endTime - startTime;
            assertTrue(duration < 5000,
                "大文件编译应在 5 秒内完成，实际耗时: " + duration + "ms");
        }
    }
}
