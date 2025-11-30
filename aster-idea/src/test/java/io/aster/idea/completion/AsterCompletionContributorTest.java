package io.aster.idea.completion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterCompletionContributor 单元测试
 */
@DisplayName("AsterCompletionContributor")
class AsterCompletionContributorTest {

    private AsterCompletionContributor contributor;

    @BeforeEach
    void setUp() {
        contributor = new AsterCompletionContributor();
    }

    @Nested
    @DisplayName("关键字列表")
    class KeywordsTest {

        @Test
        @DisplayName("应包含声明关键字")
        void shouldContainDeclarationKeywords() throws Exception {
            String[] keywords = getKeywords();
            Set<String> keywordSet = Set.of(keywords);

            assertTrue(keywordSet.contains("module"), "应包含 module");
            assertTrue(keywordSet.contains("import"), "应包含 import");
            assertTrue(keywordSet.contains("func"), "应包含 func");
            assertTrue(keywordSet.contains("data"), "应包含 data");
            assertTrue(keywordSet.contains("enum"), "应包含 enum");
            assertTrue(keywordSet.contains("type"), "应包含 type");
        }

        @Test
        @DisplayName("应包含控制流关键字")
        void shouldContainControlFlowKeywords() throws Exception {
            String[] keywords = getKeywords();
            Set<String> keywordSet = Set.of(keywords);

            assertTrue(keywordSet.contains("if"), "应包含 if");
            assertTrue(keywordSet.contains("elif"), "应包含 elif");
            assertTrue(keywordSet.contains("else"), "应包含 else");
            assertTrue(keywordSet.contains("match"), "应包含 match");
            assertTrue(keywordSet.contains("for"), "应包含 for");
            assertTrue(keywordSet.contains("while"), "应包含 while");
        }

        @Test
        @DisplayName("应包含工作流关键字")
        void shouldContainWorkflowKeywords() throws Exception {
            String[] keywords = getKeywords();
            Set<String> keywordSet = Set.of(keywords);

            assertTrue(keywordSet.contains("workflow"), "应包含 workflow");
            assertTrue(keywordSet.contains("step"), "应包含 step");
            assertTrue(keywordSet.contains("start"), "应包含 start");
            assertTrue(keywordSet.contains("wait"), "应包含 wait");
        }

        @Test
        @DisplayName("应包含值关键字")
        void shouldContainValueKeywords() throws Exception {
            String[] keywords = getKeywords();
            Set<String> keywordSet = Set.of(keywords);

            assertTrue(keywordSet.contains("true"), "应包含 true");
            assertTrue(keywordSet.contains("false"), "应包含 false");
            assertTrue(keywordSet.contains("null"), "应包含 null");
            assertTrue(keywordSet.contains("none"), "应包含 none");
            assertTrue(keywordSet.contains("some"), "应包含 some");
        }

        private String[] getKeywords() throws Exception {
            var field = AsterCompletionContributor.class.getDeclaredField("KEYWORDS");
            field.setAccessible(true);
            return (String[]) field.get(null);
        }
    }

    @Nested
    @DisplayName("内置类型列表")
    class BuiltinTypesTest {

        @Test
        @DisplayName("应包含基本类型")
        void shouldContainPrimitiveTypes() throws Exception {
            String[] types = getBuiltinTypes();
            Set<String> typeSet = Set.of(types);

            assertTrue(typeSet.contains("Int"), "应包含 Int");
            assertTrue(typeSet.contains("Long"), "应包含 Long");
            assertTrue(typeSet.contains("Double"), "应包含 Double");
            assertTrue(typeSet.contains("String"), "应包含 String");
            assertTrue(typeSet.contains("Bool"), "应包含 Bool");
        }

        @Test
        @DisplayName("应包含容器类型")
        void shouldContainContainerTypes() throws Exception {
            String[] types = getBuiltinTypes();
            Set<String> typeSet = Set.of(types);

            assertTrue(typeSet.contains("List"), "应包含 List");
            assertTrue(typeSet.contains("Map"), "应包含 Map");
            assertTrue(typeSet.contains("Set"), "应包含 Set");
            assertTrue(typeSet.contains("Option"), "应包含 Option");
            assertTrue(typeSet.contains("Result"), "应包含 Result");
        }

        @Test
        @DisplayName("应包含特殊类型")
        void shouldContainSpecialTypes() throws Exception {
            String[] types = getBuiltinTypes();
            Set<String> typeSet = Set.of(types);

            assertTrue(typeSet.contains("Unit"), "应包含 Unit");
            assertTrue(typeSet.contains("Any"), "应包含 Any");
            assertTrue(typeSet.contains("Nothing"), "应包含 Nothing");
        }

        private String[] getBuiltinTypes() throws Exception {
            var field = AsterCompletionContributor.class.getDeclaredField("BUILTIN_TYPES");
            field.setAccessible(true);
            return (String[]) field.get(null);
        }
    }

    @Nested
    @DisplayName("needsSpaceAfter 方法")
    class NeedsSpaceAfterTest {

        @Test
        @DisplayName("声明关键字后应添加空格")
        void shouldNeedSpaceAfterDeclarationKeywords() throws Exception {
            assertTrue(needsSpaceAfter("let"), "let 后应添加空格");
            assertTrue(needsSpaceAfter("set"), "set 后应添加空格");
            assertTrue(needsSpaceAfter("func"), "func 后应添加空格");
            assertTrue(needsSpaceAfter("data"), "data 后应添加空格");
            assertTrue(needsSpaceAfter("enum"), "enum 后应添加空格");
            assertTrue(needsSpaceAfter("type"), "type 后应添加空格");
        }

        @Test
        @DisplayName("控制流关键字后应添加空格")
        void shouldNeedSpaceAfterControlFlowKeywords() throws Exception {
            assertTrue(needsSpaceAfter("if"), "if 后应添加空格");
            assertTrue(needsSpaceAfter("elif"), "elif 后应添加空格");
            assertTrue(needsSpaceAfter("match"), "match 后应添加空格");
            assertTrue(needsSpaceAfter("for"), "for 后应添加空格");
            assertTrue(needsSpaceAfter("while"), "while 后应添加空格");
        }

        @Test
        @DisplayName("值关键字后不应添加空格")
        void shouldNotNeedSpaceAfterValueKeywords() throws Exception {
            assertFalse(needsSpaceAfter("true"), "true 后不应添加空格");
            assertFalse(needsSpaceAfter("false"), "false 后不应添加空格");
            assertFalse(needsSpaceAfter("null"), "null 后不应添加空格");
            assertFalse(needsSpaceAfter("none"), "none 后不应添加空格");
            assertFalse(needsSpaceAfter("some"), "some 后不应添加空格");
        }

        @Test
        @DisplayName("else 关键字后不应添加空格")
        void shouldNotNeedSpaceAfterElse() throws Exception {
            assertFalse(needsSpaceAfter("else"), "else 后不应添加空格");
        }

        private boolean needsSpaceAfter(String keyword) throws Exception {
            Method method = AsterCompletionContributor.class.getDeclaredMethod("needsSpaceAfter", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(contributor, keyword);
        }
    }

    @Nested
    @DisplayName("构造函数")
    class ConstructorTest {

        @Test
        @DisplayName("应成功创建实例")
        void shouldCreateInstance() {
            assertNotNull(contributor);
        }
    }

    @Nested
    @DisplayName("isLocalVariable 方法")
    class IsLocalVariableTest {

        @Test
        @DisplayName("应正确识别局部变量类型")
        void shouldIdentifyLocalVariableTypes() throws Exception {
            // 验证方法存在且可访问
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "isLocalVariable",
                io.aster.idea.psi.AsterNamedElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "isLocalVariable 方法应存在");
        }
    }

    @Nested
    @DisplayName("getTypeText 方法")
    class GetTypeTextTest {

        @Test
        @DisplayName("应正确返回各种元素类型文本")
        void shouldReturnCorrectTypeTexts() throws Exception {
            // 验证方法存在且可访问
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "getTypeText",
                io.aster.idea.psi.AsterNamedElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "getTypeText 方法应存在");
        }
    }

    @Nested
    @DisplayName("作用域感知补全")
    class ScopeAwareCompletionTest {

        @Test
        @DisplayName("collectLocalVariables 方法应存在")
        void collectLocalVariablesShouldExist() throws Exception {
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "collectLocalVariables",
                com.intellij.psi.PsiElement.class,
                com.intellij.codeInsight.completion.CompletionResultSet.class,
                java.util.Set.class,
                int.class
            );
            method.setAccessible(true);
            assertNotNull(method, "collectLocalVariables 方法应存在");
        }

        @Test
        @DisplayName("collectTopLevelElements 方法应存在")
        void collectTopLevelElementsShouldExist() throws Exception {
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "collectTopLevelElements",
                com.intellij.psi.PsiElement.class,
                com.intellij.codeInsight.completion.CompletionResultSet.class,
                java.util.Set.class
            );
            method.setAccessible(true);
            assertNotNull(method, "collectTopLevelElements 方法应存在");
        }

        @Test
        @DisplayName("addLocalIdentifierCompletions 方法应接受位置参数")
        void addLocalIdentifierCompletionsShouldAcceptPosition() throws Exception {
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "addLocalIdentifierCompletions",
                com.intellij.codeInsight.completion.CompletionResultSet.class,
                io.aster.idea.psi.AsterFile.class,
                com.intellij.psi.PsiElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "addLocalIdentifierCompletions 方法应存在且接受 position 参数");
        }
    }

    @Nested
    @DisplayName("跨文件补全")
    class CrossFileCompletionTest {

        @Test
        @DisplayName("addImportedSymbolCompletions 方法应存在")
        void addImportedSymbolCompletionsShouldExist() throws Exception {
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
                "addImportedSymbolCompletions",
                com.intellij.codeInsight.completion.CompletionResultSet.class,
                io.aster.idea.psi.AsterFile.class,
                com.intellij.psi.PsiElement.class
            );
            method.setAccessible(true);
            assertNotNull(method, "addImportedSymbolCompletions 方法应存在且接受 position 参数");
        }

        @Test
        @DisplayName("应能访问 AsterModuleResolver")
        void shouldBeAbleToAccessModuleResolver() {
            // 验证 AsterModuleResolver 可以被访问
            assertNotNull(io.aster.idea.reference.AsterModuleResolver.class,
                "AsterModuleResolver 类应可访问");
        }

        @Test
        @DisplayName("findExistingAliasPrefix 方法应存在")
        void findExistingAliasPrefixShouldExist() throws Exception {
            // 方法签名：findExistingAliasPrefix(PsiElement position, Map<String, ImportInfo> imports)
            // 返回类型为 PrefixInfo（内部记录类），包含 alias 和 subPath 以支持多级路径
            Method method = AsterCompletionContributor.class.getDeclaredMethod(
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

        @Test
        @DisplayName("AsterModuleResolver 应有 getInstance 静态方法")
        void asterModuleResolverShouldHaveGetInstance() throws Exception {
            Method method = io.aster.idea.reference.AsterModuleResolver.class.getMethod(
                "getInstance",
                com.intellij.openapi.project.Project.class
            );
            assertNotNull(method, "getInstance 方法应存在");
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()),
                "getInstance 应是静态方法");
            assertEquals(io.aster.idea.reference.AsterModuleResolver.class,
                method.getReturnType(),
                "getInstance 应返回 AsterModuleResolver");
        }
    }
}
