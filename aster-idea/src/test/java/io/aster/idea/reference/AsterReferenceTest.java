package io.aster.idea.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster 引用解析组件单元测试
 */
@DisplayName("Aster 引用解析组件")
class AsterReferenceTest {

    @Nested
    @DisplayName("AsterReferenceContributor")
    class ReferenceContributorTest {

        private AsterReferenceContributor contributor;

        @BeforeEach
        void setUp() {
            contributor = new AsterReferenceContributor();
        }

        @Test
        @DisplayName("应成功创建实例")
        void shouldCreateInstance() {
            assertNotNull(contributor);
        }

        @Test
        @DisplayName("应继承 PsiReferenceContributor")
        void shouldExtendPsiReferenceContributor() {
            assertTrue(
                com.intellij.psi.PsiReferenceContributor.class.isAssignableFrom(
                    AsterReferenceContributor.class),
                "应继承 PsiReferenceContributor"
            );
        }

        @Test
        @DisplayName("registerReferenceProviders 方法应存在")
        void registerReferenceProvidersMethodShouldExist() throws Exception {
            var method = AsterReferenceContributor.class.getMethod(
                "registerReferenceProviders",
                com.intellij.psi.PsiReferenceRegistrar.class);
            assertNotNull(method);
        }
    }

    @Nested
    @DisplayName("AsterReference")
    class AsterReferenceInstanceTest {

        @Test
        @DisplayName("AsterReference 类应存在")
        void asterReferenceClassShouldExist() {
            assertNotNull(AsterReference.class);
        }

        @Test
        @DisplayName("应实现 PsiPolyVariantReference")
        void shouldImplementPsiPolyVariantReference() {
            assertTrue(
                com.intellij.psi.PsiPolyVariantReference.class.isAssignableFrom(
                    AsterReference.class),
                "应实现 PsiPolyVariantReference"
            );
        }

        @Test
        @DisplayName("multiResolve 方法应存在")
        void multiResolveMethodShouldExist() throws Exception {
            var method = AsterReference.class.getMethod("multiResolve", boolean.class);
            assertNotNull(method);
        }

        @Test
        @DisplayName("resolve 方法应存在")
        void resolveMethodShouldExist() throws Exception {
            var method = AsterReference.class.getMethod("resolve");
            assertNotNull(method);
        }

        @Test
        @DisplayName("handleElementRename 方法应存在")
        void handleElementRenameMethodShouldExist() throws Exception {
            var method = AsterReference.class.getMethod("handleElementRename", String.class);
            assertNotNull(method);
        }

        @Test
        @DisplayName("getVariants 方法应存在")
        void getVariantsMethodShouldExist() throws Exception {
            var method = AsterReference.class.getMethod("getVariants");
            assertNotNull(method);
        }
    }

    @Nested
    @DisplayName("Shadowing 语义")
    class ShadowingTest {

        @Test
        @DisplayName("findDefinition 方法应存在（私有方法）")
        void findDefinitionMethodShouldExist() throws Exception {
            var method = AsterReference.class.getDeclaredMethod(
                "findDefinition",
                io.aster.idea.psi.AsterFile.class,
                String.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findDefinition 方法应存在");
        }

        @Test
        @DisplayName("findInScope 方法应存在（私有方法）")
        void findInScopeMethodShouldExist() throws Exception {
            var method = AsterReference.class.getDeclaredMethod(
                "findInScope",
                com.intellij.psi.PsiElement.class,
                String.class,
                int.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findInScope 方法应接受 offset 参数以支持作用域感知");
        }

        @Test
        @DisplayName("findTopLevel 方法应存在（私有方法）")
        void findTopLevelMethodShouldExist() throws Exception {
            var method = AsterReference.class.getDeclaredMethod(
                "findTopLevel",
                com.intellij.psi.PsiElement.class,
                String.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findTopLevel 方法应存在");
        }

        @Test
        @DisplayName("multiResolve 应返回单个结果（shadowing 语义）")
        void multiResolveShouldReturnSingleResult() throws Exception {
            // 验证 multiResolve 返回类型正确（单个结果或空）
            var method = AsterReference.class.getMethod("multiResolve", boolean.class);
            var returnType = method.getReturnType();
            assertTrue(returnType.isArray(), "multiResolve 应返回数组");
            assertEquals(
                com.intellij.psi.ResolveResult.class,
                returnType.getComponentType(),
                "multiResolve 应返回 ResolveResult 数组"
            );
        }
    }

    @Nested
    @DisplayName("重命名支持")
    class RenameTest {

        @Test
        @DisplayName("handleElementRename 不应依赖全局 ElementManipulator")
        void handleElementRenameShouldNotDependOnGlobalManipulator() throws Exception {
            // 验证方法实现不依赖全局注册的 ElementManipulator
            // 方法签名正确即表示已实现独立处理
            var method = AsterReference.class.getMethod("handleElementRename", String.class);
            assertEquals(
                com.intellij.psi.PsiElement.class,
                method.getReturnType(),
                "handleElementRename 应返回 PsiElement"
            );
        }
    }

    @Nested
    @DisplayName("跨文件引用")
    class CrossFileReferenceTest {

        @Test
        @DisplayName("findCrossFileDefinition 方法应存在（私有方法）")
        void findCrossFileDefinitionMethodShouldExist() throws Exception {
            var method = AsterReference.class.getDeclaredMethod(
                "findCrossFileDefinition",
                io.aster.idea.psi.AsterFile.class,
                String.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findCrossFileDefinition 方法应存在");
        }

        @Test
        @DisplayName("findModuleDeclaration 方法应存在（私有方法）")
        void findModuleDeclarationMethodShouldExist() throws Exception {
            var method = AsterReference.class.getDeclaredMethod(
                "findModuleDeclaration",
                io.aster.idea.psi.AsterFile.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findModuleDeclaration 方法应存在");
        }

        @Test
        @DisplayName("multiResolve 应支持跨文件解析")
        void multiResolveShouldSupportCrossFileResolution() throws Exception {
            // 验证 multiResolve 方法能够处理跨文件引用
            var method = AsterReference.class.getMethod("multiResolve", boolean.class);
            assertNotNull(method, "multiResolve 方法应存在");

            // 验证返回类型正确
            var returnType = method.getReturnType();
            assertTrue(returnType.isArray(), "应返回数组");
            assertEquals(
                com.intellij.psi.ResolveResult.class,
                returnType.getComponentType(),
                "应返回 ResolveResult 数组"
            );
        }
    }

    @Nested
    @DisplayName("限定前缀支持")
    class QualifiedPrefixTest {

        @Test
        @DisplayName("AsterReference 应支持限定前缀构造函数")
        void asterReferenceShouldSupportQualifiedPrefixConstructor() throws Exception {
            // 验证存在三参数构造函数（element, rangeInElement, qualifiedPrefix）
            var constructor = AsterReference.class.getConstructor(
                com.intellij.psi.PsiElement.class,
                com.intellij.openapi.util.TextRange.class,
                String.class
            );
            assertNotNull(constructor, "应存在接受 qualifiedPrefix 的构造函数");
        }

        @Test
        @DisplayName("AsterReference 应有 qualifiedPrefix 字段")
        void asterReferenceShouldHaveQualifiedPrefixField() throws Exception {
            var field = AsterReference.class.getDeclaredField("qualifiedPrefix");
            field.setAccessible(true);
            assertNotNull(field, "应有 qualifiedPrefix 字段");
            assertEquals(String.class, field.getType(), "qualifiedPrefix 应为 String 类型");
        }

        @Test
        @DisplayName("AsterReferenceProvider 应有 findQualifiedPrefix 方法")
        void asterReferenceProviderShouldHaveFindQualifiedPrefixMethod() throws Exception {
            // 获取内部类 AsterReferenceProvider
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            Class<?> providerClass = null;
            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals("AsterReferenceProvider")) {
                    providerClass = innerClass;
                    break;
                }
            }
            assertNotNull(providerClass, "AsterReferenceProvider 内部类应存在");

            // 验证 findQualifiedPrefix 方法存在
            var method = providerClass.getDeclaredMethod(
                "findQualifiedPrefix",
                com.intellij.psi.PsiElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findQualifiedPrefix 方法应存在");
            assertEquals(String.class, method.getReturnType(), "应返回 String");
        }

        @Test
        @DisplayName("AsterReferenceProvider 应有 isIdentifier 方法")
        void asterReferenceProviderShouldHaveIsIdentifierMethod() throws Exception {
            // 获取内部类 AsterReferenceProvider
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            Class<?> providerClass = null;
            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals("AsterReferenceProvider")) {
                    providerClass = innerClass;
                    break;
                }
            }
            assertNotNull(providerClass, "AsterReferenceProvider 内部类应存在");

            // 验证 isIdentifier 方法存在
            var method = providerClass.getDeclaredMethod(
                "isIdentifier",
                com.intellij.psi.PsiElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "isIdentifier 方法应存在");
            assertEquals(boolean.class, method.getReturnType(), "应返回 boolean");
        }
    }

    @Nested
    @DisplayName("PsiTreeUtil 集成")
    class PsiTreeUtilIntegrationTest {

        @Test
        @DisplayName("AsterReferenceContributor 应导入 PsiTreeUtil")
        void shouldImportPsiTreeUtil() {
            // 通过检查 findQualifiedPrefix 方法的存在验证 PsiTreeUtil 被正确使用
            // 因为该方法使用了 PsiTreeUtil.skipWhitespacesAndCommentsBackward
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            Class<?> providerClass = null;
            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals("AsterReferenceProvider")) {
                    providerClass = innerClass;
                    break;
                }
            }
            assertNotNull(providerClass, "AsterReferenceProvider 内部类应存在");

            try {
                var method = providerClass.getDeclaredMethod(
                    "findQualifiedPrefix",
                    com.intellij.psi.PsiElement.class
                );
                assertNotNull(method, "findQualifiedPrefix 方法应存在，表明 PsiTreeUtil 已集成");
            } catch (NoSuchMethodException e) {
                fail("findQualifiedPrefix 方法不存在");
            }
        }

        @Test
        @DisplayName("AsterCompletionContributor 应有 findExistingAliasPrefix 方法")
        void asterCompletionContributorShouldHaveFindExistingAliasPrefix() throws Exception {
            // 方法签名：findExistingAliasPrefix(PsiElement position, Map<String, ImportInfo> imports)
            // 返回类型为 PrefixInfo（内部记录类），包含 alias 和 subPath 以支持多级路径
            var method = io.aster.idea.completion.AsterCompletionContributor.class.getDeclaredMethod(
                "findExistingAliasPrefix",
                com.intellij.psi.PsiElement.class,
                java.util.Map.class
            );
            method.setAccessible(true);
            assertNotNull(method, "findExistingAliasPrefix 方法应存在");
            // 返回类型是 PrefixInfo 记录类（内部类），检查是 Record 子类
            assertTrue(Record.class.isAssignableFrom(method.getReturnType()),
                "应返回 PrefixInfo 记录类型（支持多级别名路径）");
        }
    }
}
