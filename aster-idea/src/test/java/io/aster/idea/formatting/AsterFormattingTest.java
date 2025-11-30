package io.aster.idea.formatting;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.lang.ASTNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Aster 代码格式化组件单元测试
 */
@DisplayName("Aster 代码格式化组件")
class AsterFormattingTest {

    @Nested
    @DisplayName("AsterFormattingModelBuilder")
    class FormattingModelBuilderTest {

        private AsterFormattingModelBuilder builder;

        @BeforeEach
        void setUp() {
            builder = new AsterFormattingModelBuilder();
        }

        @Test
        @DisplayName("应成功创建实例")
        void shouldCreateInstance() {
            assertNotNull(builder);
        }

        @Test
        @DisplayName("应实现 FormattingModelBuilder 接口")
        void shouldImplementFormattingModelBuilder() {
            assertTrue(builder instanceof FormattingModelBuilder,
                "应实现 FormattingModelBuilder 接口");
        }

        @Test
        @DisplayName("createModel 方法应存在")
        void createModelMethodShouldExist() throws Exception {
            var method = AsterFormattingModelBuilder.class.getMethod(
                "createModel",
                com.intellij.formatting.FormattingContext.class);
            assertNotNull(method);
        }
    }

    @Nested
    @DisplayName("AsterBlock")
    class AsterBlockTest {

        @Test
        @DisplayName("AsterBlock 类应存在")
        void asterBlockClassShouldExist() {
            assertNotNull(AsterBlock.class);
        }

        @Test
        @DisplayName("应实现 Block 接口")
        void shouldImplementBlockInterface() {
            assertTrue(
                Block.class.isAssignableFrom(AsterBlock.class),
                "应实现 Block 接口"
            );
        }

        @Test
        @DisplayName("getTextRange 方法应存在")
        void getTextRangeMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("getTextRange");
            assertNotNull(method);
        }

        @Test
        @DisplayName("getSubBlocks 方法应存在")
        void getSubBlocksMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("getSubBlocks");
            assertNotNull(method);
        }

        @Test
        @DisplayName("getIndent 方法应存在")
        void getIndentMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("getIndent");
            assertNotNull(method);
        }

        @Test
        @DisplayName("getSpacing 方法应存在")
        void getSpacingMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("getSpacing",
                Block.class, Block.class);
            assertNotNull(method);
        }

        @Test
        @DisplayName("isLeaf 方法应存在")
        void isLeafMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("isLeaf");
            assertNotNull(method);
        }

        @Test
        @DisplayName("isIncomplete 方法应存在")
        void isIncompleteMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("isIncomplete");
            assertNotNull(method);
        }

        @Test
        @DisplayName("getChildAttributes 方法应存在")
        void getChildAttributesMethodShouldExist() throws Exception {
            var method = AsterBlock.class.getMethod("getChildAttributes", int.class);
            assertNotNull(method);
        }
    }

    @Nested
    @DisplayName("格式化规则")
    class FormattingRulesTest {

        @Test
        @DisplayName("AsterBlock 应有 getNode 方法")
        void shouldHaveGetNodeMethod() throws Exception {
            var method = AsterBlock.class.getMethod("getNode");
            assertNotNull(method);
            assertEquals(ASTNode.class, method.getReturnType());
        }

        @Test
        @DisplayName("AsterBlock 应有 getWrap 方法")
        void shouldHaveGetWrapMethod() throws Exception {
            var method = AsterBlock.class.getMethod("getWrap");
            assertNotNull(method);
        }

        @Test
        @DisplayName("AsterBlock 应有 getAlignment 方法")
        void shouldHaveGetAlignmentMethod() throws Exception {
            var method = AsterBlock.class.getMethod("getAlignment");
            assertNotNull(method);
        }
    }
}
