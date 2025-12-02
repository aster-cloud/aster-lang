package io.aster.idea.reference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterModuleResolver 单元测试
 * <p>
 * 使用 JUnit 5 测试跨文件模块解析功能
 */
@DisplayName("AsterModuleResolver")
class AsterModuleResolverTest {

    @Nested
    @DisplayName("类结构")
    class ClassStructureTest {

        @Test
        @DisplayName("类应存在")
        void classShouldExist() {
            assertNotNull(AsterModuleResolver.class);
        }

        @Test
        @DisplayName("应有构造函数接受 Project 参数")
        void shouldHaveProjectConstructor() throws Exception {
            var constructor = AsterModuleResolver.class.getConstructor(
                com.intellij.openapi.project.Project.class
            );
            assertNotNull(constructor);
        }

        @Test
        @DisplayName("应有 getInstance 静态方法")
        void shouldHaveGetInstanceMethod() throws Exception {
            Method method = AsterModuleResolver.class.getMethod(
                "getInstance",
                com.intellij.openapi.project.Project.class
            );
            assertNotNull(method, "getInstance 方法应存在");
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()),
                "getInstance 应是静态方法");
            assertEquals(AsterModuleResolver.class, method.getReturnType(),
                "getInstance 应返回 AsterModuleResolver");
        }

        @Test
        @DisplayName("应有 MODULE_CACHE_KEY 静态字段")
        void shouldHaveModuleCacheKey() throws Exception {
            var field = AsterModuleResolver.class.getDeclaredField("MODULE_CACHE_KEY");
            field.setAccessible(true);
            assertNotNull(field, "MODULE_CACHE_KEY 字段应存在");
            assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
                "MODULE_CACHE_KEY 应是静态字段");
        }

        @Test
        @DisplayName("应有 IMPORT_CACHE_KEY 静态字段")
        void shouldHaveImportCacheKey() throws Exception {
            var field = AsterModuleResolver.class.getDeclaredField("IMPORT_CACHE_KEY");
            field.setAccessible(true);
            assertNotNull(field, "IMPORT_CACHE_KEY 字段应存在");
            assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
                "IMPORT_CACHE_KEY 应是静态字段");
        }
    }

    @Nested
    @DisplayName("resolveModule 方法")
    class ResolveModuleTest {

        @Test
        @DisplayName("方法应存在")
        void methodShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getMethod(
                "resolveModule",
                String.class
            );
            assertNotNull(method);
            assertEquals(
                io.aster.idea.psi.AsterFile.class,
                method.getReturnType(),
                "应返回 AsterFile"
            );
        }
    }

    @Nested
    @DisplayName("findExportedSymbol 方法")
    class FindExportedSymbolTest {

        @Test
        @DisplayName("方法应存在")
        void methodShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getMethod(
                "findExportedSymbol",
                io.aster.idea.psi.AsterFile.class,
                String.class
            );
            assertNotNull(method);
            assertEquals(
                io.aster.idea.psi.AsterNamedElement.class,
                method.getReturnType(),
                "应返回 AsterNamedElement"
            );
        }
    }

    @Nested
    @DisplayName("getExportedSymbols 方法")
    class GetExportedSymbolsTest {

        @Test
        @DisplayName("方法应存在")
        void methodShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getMethod(
                "getExportedSymbols",
                io.aster.idea.psi.AsterFile.class
            );
            assertNotNull(method);
            assertEquals(
                java.util.List.class,
                method.getReturnType(),
                "应返回 List"
            );
        }
    }

    @Nested
    @DisplayName("collectImports 方法")
    class CollectImportsTest {

        @Test
        @DisplayName("方法应存在")
        void methodShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getMethod(
                "collectImports",
                io.aster.idea.psi.AsterFile.class
            );
            assertNotNull(method);
            assertEquals(
                java.util.Map.class,
                method.getReturnType(),
                "应返回 Map"
            );
        }
    }

    @Nested
    @DisplayName("ImportInfo 内部类")
    class ImportInfoTest {

        @Test
        @DisplayName("类应存在")
        void classShouldExist() {
            assertNotNull(AsterModuleResolver.ImportInfo.class);
        }

        @Test
        @DisplayName("应有 modulePath 字段")
        void shouldHaveModulePathField() throws Exception {
            var field = AsterModuleResolver.ImportInfo.class.getField("modulePath");
            assertNotNull(field);
            assertEquals(String.class, field.getType());
        }

        @Test
        @DisplayName("应有 alias 字段")
        void shouldHaveAliasField() throws Exception {
            var field = AsterModuleResolver.ImportInfo.class.getField("alias");
            assertNotNull(field);
            assertEquals(String.class, field.getType());
        }

        @Test
        @DisplayName("matches 方法应存在")
        void matchesMethodShouldExist() throws Exception {
            Method method = AsterModuleResolver.ImportInfo.class.getMethod(
                "matches",
                String.class
            );
            assertNotNull(method);
            assertEquals(boolean.class, method.getReturnType());
        }

        @Test
        @DisplayName("resolveFullPath 方法应存在")
        void resolveFullPathMethodShouldExist() throws Exception {
            Method method = AsterModuleResolver.ImportInfo.class.getMethod(
                "resolveFullPath",
                String.class
            );
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        @DisplayName("matches 应正确匹配别名")
        void matchesShouldMatchAlias() {
            var info = new AsterModuleResolver.ImportInfo("aster.math", "math");
            assertTrue(info.matches("math"), "应匹配别名");
            assertTrue(info.matches("math.add"), "应匹配别名前缀");
            assertFalse(info.matches("other"), "不应匹配其他名称");
            assertFalse(info.matches("mathematics"), "不应匹配前缀相似的名称");
        }

        @Test
        @DisplayName("matches 应处理边界情况")
        void matchesShouldHandleEdgeCases() {
            var info = new AsterModuleResolver.ImportInfo("aster.math", "M");

            // 单字符别名
            assertTrue(info.matches("M"), "应匹配单字符别名");
            assertTrue(info.matches("M.add"), "应匹配单字符别名加符号");
            assertFalse(info.matches("Math"), "不应匹配仅前缀相同的名称");

            // 长路径
            var longPath = new AsterModuleResolver.ImportInfo("aster.core.io.network", "net");
            assertTrue(longPath.matches("net"), "应匹配短别名");
            assertTrue(longPath.matches("net.fetch"), "应匹配短别名加符号");
        }

        @Test
        @DisplayName("matches 应处理多级限定名")
        void matchesShouldHandleDeepQualifiedNames() {
            var info = new AsterModuleResolver.ImportInfo("aster.math", "math");

            assertTrue(info.matches("math.core.add"), "应匹配多级限定名");
            assertTrue(info.matches("math.a.b.c"), "应匹配深层嵌套");
        }

        @Test
        @DisplayName("resolveFullPath 应正确解析路径")
        void resolveFullPathShouldResolvePath() {
            var info = new AsterModuleResolver.ImportInfo("aster.math", "math");

            assertEquals("aster.math", info.resolveFullPath("math"),
                "应解析别名到模块路径");
            assertEquals("aster.math.add", info.resolveFullPath("math.add"),
                "应解析限定名到完整路径");
            assertEquals("other", info.resolveFullPath("other"),
                "不匹配的名称应原样返回");
        }

        @Test
        @DisplayName("resolveFullPath 应处理多级限定名")
        void resolveFullPathShouldHandleDeepPaths() {
            var info = new AsterModuleResolver.ImportInfo("aster.math", "math");

            assertEquals("aster.math.core.add", info.resolveFullPath("math.core.add"),
                "应正确拼接多级路径");
        }

        @Test
        @DisplayName("不同别名的独立性")
        void differentAliasesShouldBeIndependent() {
            var info1 = new AsterModuleResolver.ImportInfo("aster.math", "M");
            var info2 = new AsterModuleResolver.ImportInfo("aster.io", "IO");

            // info1 只匹配 M
            assertTrue(info1.matches("M"), "info1 应匹配 M");
            assertFalse(info1.matches("IO"), "info1 不应匹配 IO");

            // info2 只匹配 IO
            assertTrue(info2.matches("IO"), "info2 应匹配 IO");
            assertFalse(info2.matches("M"), "info2 不应匹配 M");
        }
    }

    @Nested
    @DisplayName("私有辅助方法")
    class PrivateHelperMethodsTest {

        @Test
        @DisplayName("matchesModulePath 方法应存在")
        void matchesModulePathShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getDeclaredMethod(
                "matchesModulePath",
                com.intellij.openapi.vfs.VirtualFile.class,
                String.class
            );
            method.setAccessible(true);
            assertNotNull(method);
        }

        @Test
        @DisplayName("isExportableSymbol 方法应存在")
        void isExportableSymbolShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getDeclaredMethod(
                "isExportableSymbol",
                io.aster.idea.psi.AsterNamedElement.class
            );
            method.setAccessible(true);
            assertNotNull(method);
        }

        @Test
        @DisplayName("parseImportDecl 方法应存在")
        void parseImportDeclShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getDeclaredMethod(
                "parseImportDecl",
                io.aster.idea.psi.impl.AsterImportDeclImpl.class
            );
            method.setAccessible(true);
            assertNotNull(method);
        }

        @Test
        @DisplayName("extractModulePath 方法应存在")
        void extractModulePathShouldExist() throws Exception {
            Method method = AsterModuleResolver.class.getDeclaredMethod(
                "extractModulePath",
                String.class
            );
            method.setAccessible(true);
            assertNotNull(method);
        }
    }

    @Nested
    @DisplayName("缓存结构测试")
    class CacheStructureTest {

        @Test
        @DisplayName("CacheEntry 内部类应存在")
        void cacheEntryShouldExist() throws Exception {
            // 通过反射访问内部类
            var cacheEntryClass = Class.forName(
                "io.aster.idea.reference.AsterModuleResolver$CacheEntry"
            );
            assertNotNull(cacheEntryClass, "CacheEntry 类应存在");
        }

        @Test
        @DisplayName("CacheEntry 应有 isValid 方法")
        void cacheEntryShouldHaveIsValidMethod() throws Exception {
            var cacheEntryClass = Class.forName(
                "io.aster.idea.reference.AsterModuleResolver$CacheEntry"
            );
            var isValidMethod = cacheEntryClass.getDeclaredMethod(
                "isValid",
                long.class,
                String.class
            );
            assertNotNull(isValidMethod, "isValid 方法应存在");
        }

        @Test
        @DisplayName("模块文件缓存应存在")
        void moduleFileCacheShouldExist() throws Exception {
            var field = AsterModuleResolver.class.getDeclaredField("moduleFileCache");
            field.setAccessible(true);
            assertNotNull(field, "moduleFileCache 字段应存在");
            assertTrue(java.util.Map.class.isAssignableFrom(field.getType()),
                "应实现 Map 接口");
        }

        @Test
        @DisplayName("导入缓存应存在")
        void importCacheShouldExist() throws Exception {
            var field = AsterModuleResolver.class.getDeclaredField("importCache");
            field.setAccessible(true);
            assertNotNull(field, "importCache 字段应存在");
        }
    }
}
