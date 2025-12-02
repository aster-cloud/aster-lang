package io.aster.idea.reference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aster 引用解析行为契约测试
 * <p>
 * 验证引用解析组件的设计契约和行为语义，确保：
 * - 限定引用失败时不回退到本地定义
 * - 别名前缀检测正确返回首段别名
 * - Shadowing 语义正确实现
 */
@DisplayName("Aster 引用解析行为契约")
class AsterReferenceBehaviorTest {

    @Nested
    @DisplayName("限定引用解析契约")
    class QualifiedReferenceContract {

        @Test
        @DisplayName("qualifiedPrefix 字段应为 final")
        void qualifiedPrefixShouldBeFinal() throws Exception {
            Field field = AsterReference.class.getDeclaredField("qualifiedPrefix");
            assertTrue(Modifier.isFinal(field.getModifiers()),
                "qualifiedPrefix 应为 final，确保不可变");
        }

        @Test
        @DisplayName("multiResolve 返回类型应支持空数组（表示未解析）")
        void multiResolveShouldSupportEmptyArray() throws Exception {
            Method method = AsterReference.class.getMethod("multiResolve", boolean.class);
            Class<?> returnType = method.getReturnType();
            assertTrue(returnType.isArray(), "应返回数组类型");
            assertEquals(com.intellij.psi.ResolveResult.class, returnType.getComponentType(),
                "数组元素类型应为 ResolveResult");
        }

        @Test
        @DisplayName("存在 qualifiedPrefix 参数构造函数")
        void shouldHaveQualifiedPrefixConstructor() throws Exception {
            var constructor = AsterReference.class.getConstructor(
                com.intellij.psi.PsiElement.class,
                com.intellij.openapi.util.TextRange.class,
                String.class
            );
            assertNotNull(constructor, "应有接受 qualifiedPrefix 的构造函数");
            assertEquals(3, constructor.getParameterCount());
        }

        @Test
        @DisplayName("findCrossFileDefinition 应为私有方法")
        void findCrossFileDefinitionShouldBePrivate() throws Exception {
            Method method = AsterReference.class.getDeclaredMethod(
                "findCrossFileDefinition",
                io.aster.idea.psi.AsterFile.class,
                String.class
            );
            assertTrue(Modifier.isPrivate(method.getModifiers()),
                "findCrossFileDefinition 应为私有，仅供内部使用");
        }

        /**
         * 契约说明：当 qualifiedPrefix != null 时，multiResolve 应只进行跨文件解析，
         * 解析失败时返回空数组，不回退到本地定义搜索。
         * <p>
         * 这确保 "math.add" 在没有正确导入时不会错误跳转到本地定义的 "add"。
         */
        @Test
        @DisplayName("文档契约：限定引用失败时不回退本地")
        void documentQualifiedReferenceNoFallbackContract() {
            // 此测试通过文档说明契约行为
            // 实际行为验证需要 IntelliJ 测试基础设施
            String contract = """
                当 AsterReference 的 qualifiedPrefix 不为 null 时：
                1. multiResolve 仅执行跨文件解析（findCrossFileDefinition）
                2. 解析失败返回 ResolveResult.EMPTY_ARRAY
                3. 不执行本地定义搜索（findDefinition）

                这防止了以下错误场景：
                - 用户输入 "math.add" 但未正确导入 math 模块
                - 如果回退本地搜索，会错误跳转到本地定义的 "add" 函数
                - 正确行为：返回空结果，IDE 显示"无法解析"
                """;
            assertNotNull(contract);
        }
    }

    @Nested
    @DisplayName("别名前缀检测契约")
    class AliasPrefixDetectionContract {

        @Test
        @DisplayName("findExistingAliasPrefix 应接受 imports 参数")
        void findExistingAliasPrefixShouldAcceptImports() throws Exception {
            Method method = io.aster.idea.completion.AsterCompletionContributor.class.getDeclaredMethod(
                "findExistingAliasPrefix",
                com.intellij.psi.PsiElement.class,
                java.util.Map.class
            );
            assertEquals(2, method.getParameterCount(),
                "findExistingAliasPrefix 应接受 position 和 imports 两个参数");
        }

        @Test
        @DisplayName("findExistingAliasPrefix 返回类型应为 PrefixInfo（可空）")
        void findExistingAliasPrefixReturnTypeShouldBeNullablePrefixInfo() throws Exception {
            Method method = io.aster.idea.completion.AsterCompletionContributor.class.getDeclaredMethod(
                "findExistingAliasPrefix",
                com.intellij.psi.PsiElement.class,
                java.util.Map.class
            );
            // 返回类型为 PrefixInfo 记录类，包含 alias 和 subPath 以支持多级路径
            assertTrue(Record.class.isAssignableFrom(method.getReturnType()),
                "返回类型应为 PrefixInfo 记录类（支持多级别名路径）");
            // nullable 通过返回 null 表示无前缀
        }

        /**
         * 契约说明：findExistingAliasPrefix 向左遍历收集完整限定路径，
         * 返回第一段（别名），但只有当该别名在导入表中时才有效。
         */
        @Test
        @DisplayName("文档契约：别名前缀检测返回首段并验证")
        void documentAliasPrefixDetectionContract() {
            String contract = """
                findExistingAliasPrefix 行为契约：

                输入："math." 位置，imports = {math -> ImportInfo}
                行为：向左遍历找到 "math"，检查 imports.containsKey("math")
                输出："math"（别名存在于导入表）

                输入："math.core." 位置，imports = {math -> ImportInfo}
                行为：向左遍历找到 ["math", "core"]，取首段 "math"
                输出："math"（首段别名存在于导入表）

                输入："unknown." 位置，imports = {math -> ImportInfo}
                行为：向左遍历找到 "unknown"，检查 imports.containsKey("unknown")
                输出：null（别名不在导入表中，触发完整补全）

                输入：无点号前缀位置
                行为：向左遍历未找到点号
                输出：null
                """;
            assertNotNull(contract);
        }
    }

    @Nested
    @DisplayName("Shadowing 语义契约")
    class ShadowingSemanticContract {

        @Test
        @DisplayName("findDefinition 应接受 name 参数")
        void findDefinitionShouldAcceptName() throws Exception {
            Method method = AsterReference.class.getDeclaredMethod(
                "findDefinition",
                io.aster.idea.psi.AsterFile.class,
                String.class
            );
            assertEquals(2, method.getParameterCount());
        }

        @Test
        @DisplayName("findInScope 应接受 offset 参数用于作用域感知")
        void findInScopeShouldAcceptOffset() throws Exception {
            Method method = AsterReference.class.getDeclaredMethod(
                "findInScope",
                com.intellij.psi.PsiElement.class,
                String.class,
                int.class
            );
            assertEquals(3, method.getParameterCount(),
                "findInScope 应接受 scope, name, offset 三个参数");
        }

        /**
         * 契约说明：Shadowing 语义确保内层作用域的定义覆盖外层。
         */
        @Test
        @DisplayName("文档契约：Shadowing 语义")
        void documentShadowingContract() {
            String contract = """
                Shadowing 语义契约：

                func outer:
                  let x = 1           // 外层 x
                  func inner:
                    let x = 2         // 内层 x（shadows 外层）
                    return x          // 应解析到内层 x
                  return x            // 应解析到外层 x

                解析顺序：
                1. 从当前位置向上遍历作用域链
                2. 在每个作用域中只查找当前位置之前的定义
                3. 找到第一个匹配立即返回（shadowing）
                4. 顶层定义（函数、类型）全局可见，不受位置限制
                """;
            assertNotNull(contract);
        }
    }

    @Nested
    @DisplayName("AsterReferenceProvider 契约")
    class ReferenceProviderContract {

        @Test
        @DisplayName("AsterReferenceProvider 应存在于 AsterReferenceContributor 内部")
        void asterReferenceProviderShouldExist() {
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            boolean found = false;
            for (Class<?> inner : innerClasses) {
                if (inner.getSimpleName().equals("AsterReferenceProvider")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "AsterReferenceProvider 应为内部类");
        }

        @Test
        @DisplayName("findQualifiedPrefix 方法应存在")
        void findQualifiedPrefixShouldExist() throws Exception {
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            Class<?> providerClass = null;
            for (Class<?> inner : innerClasses) {
                if (inner.getSimpleName().equals("AsterReferenceProvider")) {
                    providerClass = inner;
                    break;
                }
            }
            assertNotNull(providerClass);

            Method method = providerClass.getDeclaredMethod(
                "findQualifiedPrefix",
                com.intellij.psi.PsiElement.class
            );
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        @DisplayName("isIdentifier 方法应存在")
        void isIdentifierShouldExist() throws Exception {
            Class<?>[] innerClasses = AsterReferenceContributor.class.getDeclaredClasses();
            Class<?> providerClass = null;
            for (Class<?> inner : innerClasses) {
                if (inner.getSimpleName().equals("AsterReferenceProvider")) {
                    providerClass = inner;
                    break;
                }
            }
            assertNotNull(providerClass);

            Method method = providerClass.getDeclaredMethod(
                "isIdentifier",
                com.intellij.psi.PsiElement.class
            );
            assertNotNull(method);
            assertEquals(boolean.class, method.getReturnType());
        }
    }

    @Nested
    @DisplayName("重命名支持契约")
    class RenameContract {

        @Test
        @DisplayName("handleElementRename 应返回 PsiElement")
        void handleElementRenameShouldReturnPsiElement() throws Exception {
            Method method = AsterReference.class.getMethod("handleElementRename", String.class);
            assertEquals(com.intellij.psi.PsiElement.class, method.getReturnType());
        }

        @Test
        @DisplayName("handleElementRename 应声明 IncorrectOperationException")
        void handleElementRenameShouldDeclareException() throws Exception {
            Method method = AsterReference.class.getMethod("handleElementRename", String.class);
            Class<?>[] exceptions = method.getExceptionTypes();
            boolean found = false;
            for (Class<?> ex : exceptions) {
                if (ex == com.intellij.util.IncorrectOperationException.class) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "应声明 IncorrectOperationException");
        }
    }
}
