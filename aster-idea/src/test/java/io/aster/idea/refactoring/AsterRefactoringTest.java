package io.aster.idea.refactoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster 重构组件单元测试
 */
@DisplayName("Aster 重构组件")
class AsterRefactoringTest {

    @Nested
    @DisplayName("AsterNamesValidator")
    class NamesValidatorTest {

        private AsterNamesValidator validator;

        @BeforeEach
        void setUp() {
            validator = new AsterNamesValidator();
        }

        @Nested
        @DisplayName("isKeyword")
        class IsKeywordTest {

            @Test
            @DisplayName("应识别声明关键字")
            void shouldRecognizeDeclarationKeywords() {
                assertTrue(validator.isKeyword("module", null), "module 应是关键字");
                assertTrue(validator.isKeyword("import", null), "import 应是关键字");
                assertTrue(validator.isKeyword("func", null), "func 应是关键字");
                assertTrue(validator.isKeyword("data", null), "data 应是关键字");
                assertTrue(validator.isKeyword("enum", null), "enum 应是关键字");
                assertTrue(validator.isKeyword("type", null), "type 应是关键字");
            }

            @Test
            @DisplayName("应识别控制流关键字")
            void shouldRecognizeControlFlowKeywords() {
                assertTrue(validator.isKeyword("if", null), "if 应是关键字");
                assertTrue(validator.isKeyword("elif", null), "elif 应是关键字");
                assertTrue(validator.isKeyword("else", null), "else 应是关键字");
                assertTrue(validator.isKeyword("match", null), "match 应是关键字");
                assertTrue(validator.isKeyword("for", null), "for 应是关键字");
                assertTrue(validator.isKeyword("while", null), "while 应是关键字");
            }

            @Test
            @DisplayName("应识别语句关键字")
            void shouldRecognizeStatementKeywords() {
                assertTrue(validator.isKeyword("let", null), "let 应是关键字");
                assertTrue(validator.isKeyword("set", null), "set 应是关键字");
                assertTrue(validator.isKeyword("return", null), "return 应是关键字");
            }

            @Test
            @DisplayName("应识别工作流关键字")
            void shouldRecognizeWorkflowKeywords() {
                assertTrue(validator.isKeyword("workflow", null), "workflow 应是关键字");
                assertTrue(validator.isKeyword("step", null), "step 应是关键字");
                assertTrue(validator.isKeyword("start", null), "start 应是关键字");
                assertTrue(validator.isKeyword("wait", null), "wait 应是关键字");
            }

            @Test
            @DisplayName("应识别值关键字")
            void shouldRecognizeValueKeywords() {
                assertTrue(validator.isKeyword("true", null), "true 应是关键字");
                assertTrue(validator.isKeyword("false", null), "false 应是关键字");
                assertTrue(validator.isKeyword("null", null), "null 应是关键字");
                assertTrue(validator.isKeyword("none", null), "none 应是关键字");
                assertTrue(validator.isKeyword("some", null), "some 应是关键字");
            }

            @Test
            @DisplayName("应识别效果关键字")
            void shouldRecognizeEffectKeywords() {
                assertTrue(validator.isKeyword("io", null), "io 应是关键字");
                assertTrue(validator.isKeyword("cpu", null), "cpu 应是关键字");
                assertTrue(validator.isKeyword("pure", null), "pure 应是关键字");
            }

            @Test
            @DisplayName("应对大小写不敏感")
            void shouldBeCaseInsensitive() {
                assertTrue(validator.isKeyword("IF", null), "IF 应是关键字");
                assertTrue(validator.isKeyword("Let", null), "Let 应是关键字");
                assertTrue(validator.isKeyword("FUNC", null), "FUNC 应是关键字");
            }

            @Test
            @DisplayName("非关键字应返回 false")
            void shouldReturnFalseForNonKeywords() {
                assertFalse(validator.isKeyword("foo", null), "foo 不应是关键字");
                assertFalse(validator.isKeyword("myFunc", null), "myFunc 不应是关键字");
                assertFalse(validator.isKeyword("calculate", null), "calculate 不应是关键字");
            }
        }

        @Nested
        @DisplayName("isIdentifier")
        class IsIdentifierTest {

            @Test
            @DisplayName("应接受有效标识符")
            void shouldAcceptValidIdentifiers() {
                assertTrue(validator.isIdentifier("foo", null), "foo 应是有效标识符");
                assertTrue(validator.isIdentifier("myFunc", null), "myFunc 应是有效标识符");
                assertTrue(validator.isIdentifier("_private", null), "_private 应是有效标识符");
                assertTrue(validator.isIdentifier("value123", null), "value123 应是有效标识符");
                assertTrue(validator.isIdentifier("CamelCase", null), "CamelCase 应是有效标识符");
            }

            @Test
            @DisplayName("应拒绝空字符串")
            void shouldRejectEmptyString() {
                assertFalse(validator.isIdentifier("", null), "空字符串不应是有效标识符");
            }

            @Test
            @DisplayName("应拒绝关键字作为标识符")
            void shouldRejectKeywordsAsIdentifiers() {
                assertFalse(validator.isIdentifier("if", null), "if 不应是有效标识符");
                assertFalse(validator.isIdentifier("let", null), "let 不应是有效标识符");
                assertFalse(validator.isIdentifier("func", null), "func 不应是有效标识符");
            }

            @Test
            @DisplayName("应拒绝以数字开头的标识符")
            void shouldRejectIdentifiersStartingWithDigit() {
                assertFalse(validator.isIdentifier("123foo", null), "123foo 不应是有效标识符");
                assertFalse(validator.isIdentifier("1value", null), "1value 不应是有效标识符");
            }

            @Test
            @DisplayName("应拒绝包含特殊字符的标识符")
            void shouldRejectIdentifiersWithSpecialCharacters() {
                assertFalse(validator.isIdentifier("foo-bar", null), "foo-bar 不应是有效标识符");
                assertFalse(validator.isIdentifier("foo.bar", null), "foo.bar 不应是有效标识符");
                assertFalse(validator.isIdentifier("foo bar", null), "foo bar 不应是有效标识符");
                assertFalse(validator.isIdentifier("foo@bar", null), "foo@bar 不应是有效标识符");
            }
        }

        @Nested
        @DisplayName("接口实现")
        class InterfaceImplementationTest {

            @Test
            @DisplayName("应实现 NamesValidator 接口")
            void shouldImplementNamesValidator() {
                assertTrue(
                    com.intellij.lang.refactoring.NamesValidator.class.isAssignableFrom(
                        AsterNamesValidator.class),
                    "应实现 NamesValidator 接口"
                );
            }
        }
    }

    @Nested
    @DisplayName("AsterElementManipulator")
    class ElementManipulatorTest {

        private AsterElementManipulator manipulator;

        @BeforeEach
        void setUp() {
            manipulator = new AsterElementManipulator();
        }

        @Test
        @DisplayName("应成功创建实例")
        void shouldCreateInstance() {
            assertNotNull(manipulator);
        }

        @Test
        @DisplayName("应实现 ElementManipulator 接口")
        void shouldImplementElementManipulator() {
            assertTrue(
                com.intellij.psi.ElementManipulator.class.isAssignableFrom(
                    AsterElementManipulator.class),
                "应实现 ElementManipulator 接口"
            );
        }

        @Test
        @DisplayName("handleContentChange 方法应存在")
        void handleContentChangeMethodShouldExist() throws Exception {
            var method = AsterElementManipulator.class.getMethod("handleContentChange",
                com.intellij.psi.PsiElement.class,
                com.intellij.openapi.util.TextRange.class,
                String.class);
            assertNotNull(method);
        }

        @Test
        @DisplayName("getRangeInElement 方法应存在")
        void getRangeInElementMethodShouldExist() throws Exception {
            var method = AsterElementManipulator.class.getMethod("getRangeInElement",
                com.intellij.psi.PsiElement.class);
            assertNotNull(method);
        }
    }
}
