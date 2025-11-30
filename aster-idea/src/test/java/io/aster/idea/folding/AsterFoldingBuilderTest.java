package io.aster.idea.folding;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.DumbAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AsterFoldingBuilder 单元测试
 */
@DisplayName("AsterFoldingBuilder")
class AsterFoldingBuilderTest {

    private AsterFoldingBuilder foldingBuilder;

    @BeforeEach
    void setUp() {
        foldingBuilder = new AsterFoldingBuilder();
    }

    @Nested
    @DisplayName("构造函数")
    class ConstructorTest {

        @Test
        @DisplayName("应成功创建实例")
        void shouldCreateInstance() {
            assertNotNull(foldingBuilder);
        }

        @Test
        @DisplayName("应实现 DumbAware 接口")
        void shouldImplementDumbAware() {
            assertTrue(foldingBuilder instanceof DumbAware,
                "AsterFoldingBuilder 应实现 DumbAware 接口");
        }
    }

    @Nested
    @DisplayName("getPlaceholderText")
    class GetPlaceholderTextTest {

        @Test
        @DisplayName("应返回默认占位符文本")
        void shouldReturnDefaultPlaceholderText() {
            ASTNode mockNode = mock(ASTNode.class);
            String placeholder = foldingBuilder.getPlaceholderText(mockNode);
            assertEquals("...", placeholder, "默认占位符应为 '...'");
        }
    }

    @Nested
    @DisplayName("isCollapsedByDefault")
    class IsCollapsedByDefaultTest {

        @Test
        @DisplayName("默认不应折叠")
        void shouldNotBeCollapsedByDefault() {
            ASTNode mockNode = mock(ASTNode.class);
            boolean isCollapsed = foldingBuilder.isCollapsedByDefault(mockNode);
            assertFalse(isCollapsed, "默认不应折叠");
        }
    }

    @Nested
    @DisplayName("接口实现")
    class InterfaceImplementationTest {

        @Test
        @DisplayName("应继承 FoldingBuilderEx")
        void shouldExtendFoldingBuilderEx() {
            assertTrue(
                com.intellij.lang.folding.FoldingBuilderEx.class.isAssignableFrom(
                    AsterFoldingBuilder.class),
                "应继承 FoldingBuilderEx"
            );
        }

        @Test
        @DisplayName("buildFoldRegions 方法应存在")
        void buildFoldRegionsMethodShouldExist() throws Exception {
            var method = AsterFoldingBuilder.class.getMethod("buildFoldRegions",
                com.intellij.psi.PsiElement.class,
                com.intellij.openapi.editor.Document.class,
                boolean.class);
            assertNotNull(method);
        }
    }
}
