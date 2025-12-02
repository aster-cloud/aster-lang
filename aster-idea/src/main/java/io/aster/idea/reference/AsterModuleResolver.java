package io.aster.idea.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import io.aster.idea.psi.AsterFile;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aster 模块解析器（项目级服务）
 * <p>
 * 负责跨文件引用解析：
 * - 模块路径转换（aster.math → 文件路径）
 * - 导出符号查找
 * - 导入别名处理
 * <p>
 * 性能优化：
 * - 项目级单例，避免重复创建
 * - 使用 CachedValuesManager 进行缓存
 * - 基于 PSI 修改计数器的缓存失效
 */
public class AsterModuleResolver {

    private static final String ASTER_EXTENSION = "aster";

    /**
     * 搜索路径模式
     */
    private static final String[] SEARCH_PATTERNS = {
        "src/modules",
        "src",
        "lib",
        "modules",
        ""  // 项目根目录
    };

    /**
     * 导入信息缓存 Key（文件级别）
     */
    private static final Key<CachedValue<Map<String, ImportInfo>>> IMPORT_CACHE_KEY =
        Key.create("aster.import.cache");

    /**
     * 模块解析缓存 Key（项目级别）
     */
    private static final Key<CachedValue<Map<String, VirtualFile>>> MODULE_CACHE_KEY =
        Key.create("aster.module.cache");

    /**
     * 模块文件缓存（向后兼容测试）
     */
    @SuppressWarnings("unused")
    private final Map<String, CacheEntry<VirtualFile>> moduleFileCache = new HashMap<>();

    /**
     * 导入信息缓存（向后兼容测试）
     */
    @SuppressWarnings("unused")
    private final Map<String, CacheEntry<Map<String, ImportInfo>>> importCache = new HashMap<>();

    private final Project project;
    private final PsiManager psiManager;
    private final CachedValuesManager cachedValuesManager;

    /**
     * 获取项目级服务实例
     */
    public static @NotNull AsterModuleResolver getInstance(@NotNull Project project) {
        return project.getService(AsterModuleResolver.class);
    }

    public AsterModuleResolver(@NotNull Project project) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.cachedValuesManager = CachedValuesManager.getManager(project);
    }

    /**
     * 缓存条目（用于向后兼容测试）
     */
    static class CacheEntry<T> {
        final T value;
        final long modificationCount;
        final String projectBasePath;

        CacheEntry(T value, long modificationCount, String projectBasePath) {
            this.value = value;
            this.modificationCount = modificationCount;
            this.projectBasePath = projectBasePath;
        }

        boolean isValid(long currentModCount, String currentProjectPath) {
            return modificationCount == currentModCount &&
                   Objects.equals(projectBasePath, currentProjectPath);
        }
    }

    /**
     * 根据模块路径查找模块文件
     * <p>
     * 使用 CachedValuesManager 进行项目级缓存，当项目结构修改时自动失效
     *
     * @param modulePath 模块路径，如 "aster.math.core"
     * @return 模块文件，如果未找到则返回 null
     */
    public @Nullable AsterFile resolveModule(@NotNull String modulePath) {
        // 使用线程安全的方式获取或创建模块缓存
        // CachedValuesManager.getCachedValue 内部处理并发，避免竞争条件
        Map<String, VirtualFile> moduleCache = cachedValuesManager.getCachedValue(
            project,
            MODULE_CACHE_KEY,
            () -> CachedValueProvider.Result.create(
                new ConcurrentHashMap<>(),
                PsiModificationTracker.getInstance(project)
            ),
            false
        );
        if (moduleCache == null) {
            moduleCache = new ConcurrentHashMap<>();
        }

        // 检查缓存
        VirtualFile cachedFile = moduleCache.get(modulePath);
        if (cachedFile != null && cachedFile.isValid()) {
            PsiFile psiFile = psiManager.findFile(cachedFile);
            if (psiFile instanceof AsterFile asterFile) {
                return asterFile;
            }
        }

        // 缓存未命中，执行查找
        VirtualFile foundFile = findModuleFile(modulePath);
        if (foundFile != null) {
            moduleCache.put(modulePath, foundFile);
            PsiFile psiFile = psiManager.findFile(foundFile);
            if (psiFile instanceof AsterFile asterFile) {
                return asterFile;
            }
        }

        return null;
    }

    /**
     * 实际执行模块文件查找（无缓存）
     * <p>
     * 按照 SEARCH_PATTERNS 定义的优先级查找模块文件：
     * 1. src/modules - 项目源码模块目录（最高优先级）
     * 2. src - 项目源码目录
     * 3. lib - 本地库目录
     * 4. modules - 模块目录
     * 5. 项目根目录
     * 6. .aster/packages - 依赖包目录（最低优先级）
     * <p>
     * 这确保本地源码优先于 vendor 依赖，避免跳转到错误的模块版本
     */
    private @Nullable VirtualFile findModuleFile(@NotNull String modulePath) {
        // 将模块路径转换为文件名
        String[] parts = modulePath.split("\\.");
        String fileName = parts[parts.length - 1] + "." + ASTER_EXTENSION;

        // 在项目中搜索所有匹配的文件
        Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
            fileName,
            GlobalSearchScope.projectScope(project)
        );

        // 收集所有匹配的模块文件
        List<VirtualFile> matchedFiles = new ArrayList<>();
        for (VirtualFile file : files) {
            if (matchesModulePath(file, modulePath)) {
                matchedFiles.add(file);
            }
        }

        // 如果只有一个匹配，直接返回
        if (matchedFiles.size() <= 1) {
            return matchedFiles.isEmpty() ? null : matchedFiles.get(0);
        }

        // 多个匹配时，按搜索路径优先级排序
        VirtualFile projectBaseDir = ProjectUtil.guessProjectDir(project);
        if (projectBaseDir == null) {
            return matchedFiles.get(0);
        }

        String basePath = projectBaseDir.getPath();

        // 按优先级查找
        for (String pattern : SEARCH_PATTERNS) {
            String priorityPrefix = pattern.isEmpty() ? basePath : basePath + "/" + pattern;
            for (VirtualFile file : matchedFiles) {
                if (file.getPath().startsWith(priorityPrefix + "/") ||
                    file.getPath().equals(priorityPrefix)) {
                    return file;
                }
            }
        }

        // 最后检查 .aster/packages（最低优先级）
        String packagesPath = basePath + "/.aster/packages";
        VirtualFile nonPackagesFile = null;
        for (VirtualFile file : matchedFiles) {
            if (!file.getPath().startsWith(packagesPath)) {
                nonPackagesFile = file;
                break;
            }
        }

        // 优先返回非 packages 目录的文件
        return nonPackagesFile != null ? nonPackagesFile : matchedFiles.get(0);
    }

    /**
     * 检查文件路径是否与模块路径匹配
     */
    private boolean matchesModulePath(@NotNull VirtualFile file, @NotNull String modulePath) {
        String[] parts = modulePath.split("\\.");
        VirtualFile current = file.getParent();

        // 从后向前匹配路径组件（跳过文件名本身）
        for (int i = parts.length - 2; i >= 0; i--) {
            if (current == null) {
                return false;
            }
            if (!current.getName().equals(parts[i])) {
                return false;
            }
            current = current.getParent();
        }

        return true;
    }

    /**
     * 在指定模块文件中查找导出的符号
     *
     * @param moduleFile 模块文件
     * @param symbolName 符号名称
     * @return 找到的符号，如果未找到则返回 null
     */
    public @Nullable AsterNamedElement findExportedSymbol(
            @NotNull AsterFile moduleFile,
            @NotNull String symbolName) {
        for (PsiElement child : moduleFile.getChildren()) {
            AsterNamedElement found = findSymbolRecursive(child, symbolName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 递归查找符号
     */
    private @Nullable AsterNamedElement findSymbolRecursive(
            @NotNull PsiElement element,
            @NotNull String name) {
        if (element instanceof AsterNamedElement namedElement) {
            if (isExportableSymbol(namedElement) && name.equals(namedElement.getName())) {
                return namedElement;
            }
        }

        for (PsiElement child : element.getChildren()) {
            AsterNamedElement found = findSymbolRecursive(child, name);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * 判断符号是否可导出（顶层定义）
     * <p>
     * 可导出符号必须满足两个条件：
     * 1. 是支持的声明类型（函数、数据、枚举、类型别名、workflow）
     * 2. 位于顶层（父节点是文件或模块声明，而非嵌套在函数/workflow 内部）
     */
    private boolean isExportableSymbol(@NotNull AsterNamedElement element) {
        // 检查类型
        boolean isExportableType = element instanceof AsterFuncDeclImpl ||
                                   element instanceof AsterDataDeclImpl ||
                                   element instanceof AsterEnumDeclImpl ||
                                   element instanceof AsterTypeAliasDeclImpl ||
                                   element instanceof AsterWorkflowStmtImpl;
        if (!isExportableType) {
            return false;
        }

        // 检查是否在顶层（父节点链中不能包含函数或 workflow）
        return isTopLevelDeclaration(element);
    }

    /**
     * 检查元素是否是顶层声明（非嵌套在函数/workflow 内部）
     */
    private boolean isTopLevelDeclaration(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            // 到达文件或模块声明，说明是顶层
            if (parent instanceof AsterFile || parent instanceof AsterModuleDeclImpl) {
                return true;
            }
            // 如果父节点是函数或 workflow，说明是嵌套的局部声明
            if (parent instanceof AsterFuncDeclImpl || parent instanceof AsterWorkflowStmtImpl) {
                return false;
            }
            parent = parent.getParent();
        }
        // 如果没有找到文件/模块，保守地返回 false
        return false;
    }

    /**
     * 获取模块的所有导出符号
     *
     * @param moduleFile 模块文件
     * @return 导出符号列表
     */
    public @NotNull List<AsterNamedElement> getExportedSymbols(@NotNull AsterFile moduleFile) {
        List<AsterNamedElement> exports = new ArrayList<>();
        collectExportedSymbols(moduleFile, exports);
        return exports;
    }

    /**
     * 收集导出符号
     */
    private void collectExportedSymbols(@NotNull PsiElement element,
                                        @NotNull List<AsterNamedElement> exports) {
        if (element instanceof AsterNamedElement namedElement) {
            if (isExportableSymbol(namedElement)) {
                exports.add(namedElement);
            }
        }

        for (PsiElement child : element.getChildren()) {
            collectExportedSymbols(child, exports);
        }
    }

    /**
     * 从当前文件中提取所有导入
     * <p>
     * 使用 CachedValuesManager 进行文件级别缓存，当文件内容修改时自动失效
     *
     * @param file 当前文件
     * @return 导入信息映射（别名 -> 模块路径）
     */
    public @NotNull Map<String, ImportInfo> collectImports(@NotNull AsterFile file) {
        // 使用 CachedValuesManager 进行文件级别缓存
        CachedValue<Map<String, ImportInfo>> cachedValue = file.getUserData(IMPORT_CACHE_KEY);

        if (cachedValue == null) {
            cachedValue = cachedValuesManager.createCachedValue(() -> {
                Map<String, ImportInfo> imports = doCollectImports(file);
                // 依赖于文件本身，当文件修改时自动失效
                return CachedValueProvider.Result.create(imports, file);
            }, false);
            file.putUserData(IMPORT_CACHE_KEY, cachedValue);
        }

        Map<String, ImportInfo> result = cachedValue.getValue();
        return result != null ? new HashMap<>(result) : new HashMap<>();
    }

    /**
     * 实际执行导入收集（无缓存）
     * <p>
     * 注意：如果存在相同别名的多个导入，保留第一个导入
     * 后续重复的导入会被忽略（IDE 应该通过其他机制报告冲突）
     */
    private @NotNull Map<String, ImportInfo> doCollectImports(@NotNull AsterFile file) {
        Map<String, ImportInfo> imports = new HashMap<>();

        for (PsiElement child : file.getChildren()) {
            if (child instanceof AsterImportDeclImpl importDecl) {
                ImportInfo info = parseImportDecl(importDecl);
                if (info != null) {
                    // 检查是否已存在相同别名的导入，如果是则保留第一个
                    // 这符合大多数语言的语义：先声明的优先
                    if (!imports.containsKey(info.alias)) {
                        imports.put(info.alias, info);
                    }
                    // 重复别名会被忽略，用户可以通过 IDE 检查或 linter 发现冲突
                }
            }
        }

        return imports;
    }

    /**
     * 解析导入声明
     * <p>
     * 支持以下语法：
     * - import module.path
     * - import module.path as alias
     * - Use module.path.
     * - Use module.path as alias.
     * <p>
     * 处理注释和换行符的情况
     */
    private @Nullable ImportInfo parseImportDecl(@NotNull AsterImportDeclImpl importDecl) {
        String text = importDecl.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 首先移除注释，规范化换行
        String cleaned = removeComments(text);
        String normalized = cleaned.replaceAll("[\\r\\n]+", " ").trim();

        // 移除结尾的句号
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        String modulePath;
        String alias;

        // 使用正则表达式匹配 'as' 关键字（支持多种空白分隔）
        // 模式：前后都有空白的 'as'（不区分大小写）
        java.util.regex.Pattern asPattern = java.util.regex.Pattern.compile(
            "\\s+as\\s+", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = asPattern.matcher(normalized);

        if (matcher.find()) {
            // 有别名
            String pathPart = normalized.substring(0, matcher.start()).trim();
            alias = normalized.substring(matcher.end()).trim();
            // 移除别名中可能的多余空格
            alias = alias.replaceAll("\\s+", "");
            modulePath = extractModulePath(pathPart);
        } else {
            // 无别名，使用最后一段作为别名
            modulePath = extractModulePath(normalized);
            String[] parts = modulePath.split("\\.");
            alias = parts[parts.length - 1];
        }

        if (modulePath.isEmpty()) {
            return null;
        }

        return new ImportInfo(modulePath, alias);
    }

    /**
     * 从导入文本中提取模块路径
     * <p>
     * 处理以下情况：
     * - 移除行注释 (// ...)
     * - 移除块注释
     * - 规范化换行和空格
     * - 支持多行导入语句
     */
    private @NotNull String extractModulePath(@NotNull String text) {
        // 第一步：移除注释
        String cleaned = removeComments(text);

        // 第二步：将换行符替换为空格，规范化空白
        String normalized = cleaned.replaceAll("[\\r\\n]+", " ").trim();

        // 第三步：移除关键字前缀
        String[] keywords = {"import", "use", "from"};
        for (String keyword : keywords) {
            if (normalized.toLowerCase().startsWith(keyword + " ")) {
                normalized = normalized.substring(keyword.length()).trim();
                break;
            }
        }

        // 第四步：移除多余空格，保留点分隔的路径
        // 但保留标识符之间必要的分隔（用于检测 'as' 关键字）
        return normalized.replaceAll("\\s+", "");
    }

    /**
     * 移除文本中的注释
     * <p>
     * 支持三种注释格式：
     * - 行注释 // ...
     * - 块注释 /* ... * /
     * - 行注释 # ...（Aster 核心词法器支持）
     */
    private @NotNull String removeComments(@NotNull String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = text.length();

        while (i < len) {
            // 检查行注释 //
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                // 跳过到行尾
                while (i < len && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }

            // 检查行注释 #（Aster 核心词法器支持）
            if (text.charAt(i) == '#') {
                // 跳过到行尾
                while (i < len && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }

            // 检查块注释 /* */
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                i += 2;
                // 查找 */
                while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < len) {
                    i += 2; // 跳过 */
                }
                continue;
            }

            result.append(text.charAt(i));
            i++;
        }

        return result.toString();
    }

    /**
     * 导入信息
     * <p>
     * 支持两种导入类型：
     * - 模块导入：import aster.math as math（modulePath 指向模块文件）
     * - 符号导入：import aster.math.add as add（modulePath 指向符号的完整路径）
     * <p>
     * 注意：不缓存导入类型，每次调用时重新计算。
     * 这确保了线程安全和缓存一致性（模块文件变化时能正确检测）。
     */
    public static class ImportInfo {
        public final String modulePath;
        public final String alias;

        /**
         * 导入类型
         */
        public enum ImportType {
            MODULE,  // 模块导入
            SYMBOL,  // 符号导入
            UNKNOWN  // 未知（无法解析）
        }

        /**
         * 导入类型计算结果（不可变，线程安全）
         */
        public static class ImportTypeResult {
            public final ImportType type;
            public final @Nullable String parentModulePath;
            public final @Nullable String symbolName;

            private ImportTypeResult(ImportType type, @Nullable String parentModulePath, @Nullable String symbolName) {
                this.type = type;
                this.parentModulePath = parentModulePath;
                this.symbolName = symbolName;
            }

            static ImportTypeResult module() {
                return new ImportTypeResult(ImportType.MODULE, null, null);
            }

            static ImportTypeResult symbol(@NotNull String parentPath, @NotNull String symbolName) {
                return new ImportTypeResult(ImportType.SYMBOL, parentPath, symbolName);
            }

            static ImportTypeResult unknown() {
                return new ImportTypeResult(ImportType.UNKNOWN, null, null);
            }
        }

        public ImportInfo(@NotNull String modulePath, @NotNull String alias) {
            this.modulePath = modulePath;
            this.alias = alias;
        }

        /**
         * 计算并返回导入类型信息（每次调用都重新计算，确保数据新鲜度）
         * <p>
         * 不使用实例级缓存的原因：
         * 1. 线程安全：避免多线程竞争条件
         * 2. 缓存一致性：模块文件变化时能正确检测
         * <p>
         * 性能影响：模块解析已通过 CachedValuesManager 缓存，重复调用开销较低
         */
        public @NotNull ImportTypeResult computeImportType(@NotNull AsterModuleResolver resolver) {
            // 首先尝试作为模块解析
            AsterFile moduleFile = resolver.resolveModule(modulePath);
            if (moduleFile != null) {
                return ImportTypeResult.module();
            }

            // 尝试作为符号导入解析
            int lastDot = modulePath.lastIndexOf('.');
            if (lastDot > 0) {
                String parentPath = modulePath.substring(0, lastDot);
                String symbolName = modulePath.substring(lastDot + 1);
                AsterFile parentModule = resolver.resolveModule(parentPath);
                if (parentModule != null) {
                    AsterNamedElement symbol = resolver.findExportedSymbol(parentModule, symbolName);
                    if (symbol != null) {
                        return ImportTypeResult.symbol(parentPath, symbolName);
                    }
                }
            }

            return ImportTypeResult.unknown();
        }

        /**
         * 获取导入类型（便捷方法）
         */
        public ImportType getImportType(@NotNull AsterModuleResolver resolver) {
            return computeImportType(resolver).type;
        }

        /**
         * 获取父模块路径（仅符号导入有效）
         */
        public @Nullable String getParentModulePath(@NotNull AsterModuleResolver resolver) {
            return computeImportType(resolver).parentModulePath;
        }

        /**
         * 获取符号名称（仅符号导入有效）
         */
        public @Nullable String getSymbolName(@NotNull AsterModuleResolver resolver) {
            return computeImportType(resolver).symbolName;
        }

        /**
         * 检查引用名称是否匹配此导入
         */
        public boolean matches(@NotNull String referenceName) {
            return referenceName.equals(alias) ||
                   referenceName.startsWith(alias + ".");
        }

        /**
         * 解析引用到完整模块路径
         */
        public @NotNull String resolveFullPath(@NotNull String referenceName) {
            if (referenceName.equals(alias)) {
                return modulePath;
            }
            if (referenceName.startsWith(alias + ".")) {
                String suffix = referenceName.substring(alias.length());
                return modulePath + suffix;
            }
            return referenceName;
        }
    }
}
