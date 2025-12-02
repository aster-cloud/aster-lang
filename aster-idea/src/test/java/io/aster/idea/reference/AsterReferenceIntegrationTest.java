package io.aster.idea.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Aster 引用解析行为测试
 * <p>
 * 使用 Mockito 验证 AsterReference 的行为
 * <p>
 * 注意：完整的 IntelliJ 测试框架（@TestApplication）需要更复杂的环境配置，
 * 包括完整的 plugin.xml 加载和服务初始化。这些测试使用 Mockito 作为替代方案。
 */
@DisplayName("Aster 引用解析行为测试")
class AsterReferenceIntegrationTest {

    @Nested
    @DisplayName("AsterReference 构造与基本属性")
    class ReferenceConstructionTest {

        @Test
        @DisplayName("应正确创建无限定前缀的引用")
        void shouldCreateReferenceWithoutQualifiedPrefix() throws Exception {
            // 使用 Mockito 创建 mock PsiElement
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("testName");
            var range = new TextRange(0, 8);

            var reference = new AsterReference(mockElement, range);

            assertNotNull(reference);
            assertEquals(mockElement, reference.getElement());
            assertEquals(range, reference.getRangeInElement());
        }

        @Test
        @DisplayName("应正确创建带限定前缀的引用")
        void shouldCreateReferenceWithQualifiedPrefix() throws Exception {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("symbol");
            var range = new TextRange(0, 6);

            var reference = new AsterReference(mockElement, range, "math");

            assertNotNull(reference);

            // 验证 qualifiedPrefix 字段
            var field = AsterReference.class.getDeclaredField("qualifiedPrefix");
            field.setAccessible(true);
            assertEquals("math", field.get(reference));
        }

        @Test
        @DisplayName("应正确提取引用名称")
        void shouldExtractReferenceName() throws Exception {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("myFunction");
            var range = new TextRange(0, 10);

            var reference = new AsterReference(mockElement, range);

            var field = AsterReference.class.getDeclaredField("referenceName");
            field.setAccessible(true);
            assertEquals("myFunction", field.get(reference));
        }

        @Test
        @DisplayName("应正确处理部分范围的引用名称")
        void shouldExtractPartialReferenceName() throws Exception {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("prefix.symbol");
            var range = new TextRange(7, 13); // "symbol" 部分

            var reference = new AsterReference(mockElement, range);

            var field = AsterReference.class.getDeclaredField("referenceName");
            field.setAccessible(true);
            assertEquals("symbol", field.get(reference));
        }
    }

    @Nested
    @DisplayName("getVariants 行为")
    class GetVariantsTest {

        @Test
        @DisplayName("getVariants 应返回空数组（由 CompletionContributor 提供）")
        void getVariantsShouldReturnEmptyArray() {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("test");
            var reference = new AsterReference(mockElement, new TextRange(0, 4));

            Object[] variants = reference.getVariants();

            assertNotNull(variants);
            assertEquals(0, variants.length, "变体应由 CompletionContributor 提供，此处返回空");
        }
    }

    @Nested
    @DisplayName("限定引用解析行为")
    class QualifiedReferenceResolutionTest {

        @Test
        @DisplayName("限定引用解析失败时应返回空数组")
        void qualifiedReferenceShouldReturnEmptyOnFailure() {
            // 创建一个限定引用（带 qualifiedPrefix）
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("add");
            when(mockElement.getContainingFile()).thenReturn(null); // 无文件上下文

            var reference = new AsterReference(mockElement, new TextRange(0, 3), "math");

            // 由于没有有效的文件上下文，multiResolve 应返回空数组
            ResolveResult[] results = reference.multiResolve(false);

            assertNotNull(results);
            assertEquals(0, results.length, "无法解析时应返回空数组");
        }

        @Test
        @DisplayName("无限定前缀时也应在无上下文时返回空数组")
        void unqualifiedReferenceShouldReturnEmptyOnNoContext() {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("localVar");
            when(mockElement.getContainingFile()).thenReturn(null);

            var reference = new AsterReference(mockElement, new TextRange(0, 8));

            ResolveResult[] results = reference.multiResolve(false);

            assertNotNull(results);
            assertEquals(0, results.length);
        }
    }

    @Nested
    @DisplayName("resolve 与 multiResolve 关系")
    class ResolveRelationTest {

        @Test
        @DisplayName("resolve 应在 multiResolve 返回空时返回 null")
        void resolveShouldReturnNullWhenMultiResolveEmpty() {
            PsiElement mockElement = mock(PsiElement.class);
            when(mockElement.getText()).thenReturn("unknown");
            when(mockElement.getContainingFile()).thenReturn(null);

            var reference = new AsterReference(mockElement, new TextRange(0, 7));

            PsiElement resolved = reference.resolve();

            assertNull(resolved, "无法解析时 resolve 应返回 null");
        }
    }

}
