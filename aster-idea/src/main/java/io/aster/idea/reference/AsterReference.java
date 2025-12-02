package io.aster.idea.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.psi.AsterFile;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Aster 引用实现
 * <p>
 * 支持 Go to Definition 和 Find Usages 功能。
 * 解析标识符引用到其定义位置，包括跨文件引用。
 * <p>
 * 特别处理限定引用（如 alias.symbol）：
 * - qualifiedPrefix 保存别名前缀
 * - 用于跨文件符号解析
 */
public class AsterReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private final String referenceName;
    private final @Nullable String qualifiedPrefix;

    /**
     * 创建引用（无限定前缀）
     */
    public AsterReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
        this(element, rangeInElement, null);
    }

    /**
     * 创建引用（带限定前缀）
     *
     * @param element 引用元素
     * @param rangeInElement 元素内的范围
     * @param qualifiedPrefix 限定前缀（别名），用于跨文件引用解析
     */
    public AsterReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement,
                          @Nullable String qualifiedPrefix) {
        super(element, rangeInElement);
        this.referenceName = element.getText().substring(
            rangeInElement.getStartOffset(),
            rangeInElement.getEndOffset()
        );
        this.qualifiedPrefix = qualifiedPrefix;
    }

    @Override
    public @NotNull ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiFile containingFile = myElement.getContainingFile();

        if (containingFile instanceof AsterFile asterFile) {
            // 如果有限定前缀（如 A.symbol），只进行跨文件解析
            // 限定引用失败时不回退到本地定义，以避免错误跳转
            if (qualifiedPrefix != null) {
                String qualifiedName = qualifiedPrefix + "." + referenceName;
                AsterNamedElement crossFileDefinition = findCrossFileDefinition(asterFile, qualifiedName);
                if (crossFileDefinition != null) {
                    return new ResolveResult[]{new PsiElementResolveResult(crossFileDefinition)};
                }
                // 限定引用解析失败，返回空结果而不是回退到本地
                // 这确保 "math.add" 在缺少导入时不会错误跳转到本地 "add"
                return ResolveResult.EMPTY_ARRAY;
            }

            // 无限定前缀时的正常解析流程：
            // 1. 按作用域查找定义（shadowing：找到第一个匹配后立即返回）
            AsterNamedElement definition = findDefinition(asterFile, referenceName);
            if (definition != null) {
                return new ResolveResult[]{new PsiElementResolveResult(definition)};
            }

            // 2. 跨文件查找：通过导入解析
            AsterNamedElement crossFileDefinition = findCrossFileDefinition(asterFile, referenceName);
            if (crossFileDefinition != null) {
                return new ResolveResult[]{new PsiElementResolveResult(crossFileDefinition)};
            }
        }

        return ResolveResult.EMPTY_ARRAY;
    }

    @Override
    public @Nullable PsiElement resolve() {
        ResolveResult[] results = multiResolve(false);
        // 返回第一个匹配（最近作用域的定义）
        return results.length > 0 ? results[0].getElement() : null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        // 变体由 CompletionContributor 提供
        return EMPTY_ARRAY;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        // 直接替换标识符文本，不依赖全局 ElementManipulator
        if (myElement instanceof com.intellij.psi.impl.source.tree.LeafPsiElement leafElement) {
            TextRange range = getRangeInElement();
            String oldText = myElement.getText();
            String newText = oldText.substring(0, range.getStartOffset()) +
                             newElementName +
                             oldText.substring(range.getEndOffset());
            return (PsiElement) leafElement.replaceWithText(newText);
        }
        throw new IncorrectOperationException("Cannot rename: element is not a leaf element");
    }

    /**
     * 在文件中查找指定名称的定义（按作用域优先级，shadowing 语义）
     * 返回最近作用域中的第一个匹配，如果没有则返回 null
     */
    private @Nullable AsterNamedElement findDefinition(@NotNull AsterFile file, @NotNull String name) {
        // 首先在当前作用域向上查找（局部变量优先，shadowing）
        PsiElement scope = myElement.getParent();
        while (scope != null && !(scope instanceof AsterFile)) {
            AsterNamedElement found = findInScope(scope, name, myElement.getTextOffset());
            if (found != null) {
                return found;  // 找到即返回（shadowing：近作用域覆盖远作用域）
            }
            scope = scope.getParent();
        }

        // 然后在文件顶层查找（函数、类型定义 - 全局可见）
        if (scope instanceof AsterFile) {
            return findTopLevel(scope, name);
        }

        return null;
    }

    /**
     * 在单个作用域中查找定义（只查找当前位置之前的定义）
     */
    private @Nullable AsterNamedElement findInScope(@NotNull PsiElement scope,
                                                     @NotNull String name,
                                                     int currentOffset) {
        for (PsiElement child : scope.getChildren()) {
            // 只查找当前元素之前的定义
            if (child.getTextOffset() >= currentOffset) {
                break;
            }
            if (child instanceof AsterNamedElement namedElement) {
                if (name.equals(namedElement.getName())) {
                    return namedElement;  // 找到第一个匹配立即返回
                }
            }
        }
        return null;
    }

    /**
     * 在顶层查找定义（函数、数据类型、枚举等 - 全局可见）
     * <p>
     * 只查找真正的顶层声明，不包括局部变量、参数等。
     */
    private @Nullable AsterNamedElement findTopLevel(@NotNull PsiElement scope, @NotNull String name) {
        for (PsiElement child : scope.getChildren()) {
            AsterNamedElement found = findTopLevelRecursive(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 递归查找顶层定义
     * <p>
     * 只返回真正的顶层声明类型（函数、数据类型、枚举、workflow、类型别名），
     * 不返回局部声明（let 变量、参数、for 循环变量等）。
     * 同时避免进入函数体、块等局部作用域进行搜索。
     */
    private @Nullable AsterNamedElement findTopLevelRecursive(@NotNull PsiElement element,
                                                               @NotNull String name) {
        // 只匹配顶层声明类型
        if (element instanceof AsterNamedElement namedElement) {
            if (isTopLevelDeclarationType(namedElement) && name.equals(namedElement.getName())) {
                return namedElement;
            }
        }

        // 不进入函数体或 workflow 体搜索（这些包含局部作用域）
        if (element instanceof AsterFuncDeclImpl || element instanceof AsterWorkflowStmtImpl) {
            // 已经检查过这个元素本身，不再递归其子节点
            return null;
        }

        // 递归查找嵌套定义（仅在非局部作用域内）
        for (PsiElement child : element.getChildren()) {
            AsterNamedElement found = findTopLevelRecursive(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 判断元素是否是顶层声明类型
     * <p>
     * 顶层声明包括：函数、数据类型、枚举、workflow、类型别名
     * 不包括：let 变量、参数、for 循环变量、字段定义、it-performs 等
     */
    private boolean isTopLevelDeclarationType(@NotNull AsterNamedElement element) {
        return element instanceof AsterFuncDeclImpl ||
               element instanceof AsterDataDeclImpl ||
               element instanceof AsterEnumDeclImpl ||
               element instanceof AsterWorkflowStmtImpl ||
               element instanceof AsterTypeAliasDeclImpl;
    }

    /**
     * 跨文件查找定义
     * <p>
     * 通过导入声明解析引用，支持：
     * - 模块导入：import aster.math as math（别名指向模块）
     * - 符号导入：import aster.math.add as add（别名指向具体符号）
     * - 限定引用：math.add（通过模块别名访问符号）
     */
    private @Nullable AsterNamedElement findCrossFileDefinition(
            @NotNull AsterFile file,
            @NotNull String name) {
        Project project = file.getProject();
        AsterModuleResolver resolver = AsterModuleResolver.getInstance(project);

        // 收集当前文件的所有导入
        Map<String, AsterModuleResolver.ImportInfo> imports = resolver.collectImports(file);

        // 检查引用是否匹配某个导入
        for (AsterModuleResolver.ImportInfo importInfo : imports.values()) {
            if (importInfo.matches(name)) {
                // 计算一次导入类型并缓存结果，避免重复解析
                AsterModuleResolver.ImportInfo.ImportTypeResult typeResult = importInfo.computeImportType(resolver);
                AsterModuleResolver.ImportInfo.ImportType importType = typeResult.type;

                // 处理符号导入（如 import aster.math.add as add）
                if (importType == AsterModuleResolver.ImportInfo.ImportType.SYMBOL) {
                    // 对于符号导入，别名直接指向符号
                    if (name.equals(importInfo.alias)) {
                        String parentPath = typeResult.parentModulePath;
                        String symbolName = typeResult.symbolName;
                        if (parentPath != null && symbolName != null) {
                            AsterFile parentModule = resolver.resolveModule(parentPath);
                            if (parentModule != null) {
                                return resolver.findExportedSymbol(parentModule, symbolName);
                            }
                        }
                    }
                    // 符号别名不支持限定引用（add.xxx 无效）
                    continue;
                }

                // 处理模块导入
                String fullPath = importInfo.resolveFullPath(name);

                // 1. 首先尝试将整个 fullPath 作为模块路径解析
                AsterFile moduleFile = resolver.resolveModule(fullPath);
                if (moduleFile != null) {
                    // 找到了对应的模块文件，返回模块声明
                    AsterNamedElement moduleDecl = findModuleDeclaration(moduleFile);
                    if (moduleDecl != null) {
                        return moduleDecl;
                    }
                }

                // 2. 如果整个路径不是模块，尝试将最后一段作为符号名
                int lastDot = fullPath.lastIndexOf('.');
                if (lastDot > 0) {
                    String modulePath = fullPath.substring(0, lastDot);
                    String symbolName = fullPath.substring(lastDot + 1);

                    // 尝试在父模块中查找符号
                    AsterFile parentModule = resolver.resolveModule(modulePath);
                    if (parentModule != null) {
                        AsterNamedElement symbol = resolver.findExportedSymbol(parentModule, symbolName);
                        if (symbol != null) {
                            return symbol;
                        }
                    }
                }

                // 3. 直接引用模块本身（如 import math，然后使用 math）
                if (name.equals(importInfo.alias) && importType == AsterModuleResolver.ImportInfo.ImportType.MODULE) {
                    moduleFile = resolver.resolveModule(importInfo.modulePath);
                    if (moduleFile != null) {
                        return findModuleDeclaration(moduleFile);
                    }
                }
            }
        }

        // 尝试直接按名称匹配导入（无别名情况）
        for (PsiElement child : file.getChildren()) {
            if (child instanceof AsterImportDeclImpl importDecl) {
                String importName = importDecl.getName();
                if (name.equals(importName)) {
                    // 跳转到导入声明本身
                    return importDecl;
                }
            }
        }

        return null;
    }

    /**
     * 查找模块声明
     */
    private @Nullable AsterNamedElement findModuleDeclaration(@NotNull AsterFile file) {
        for (PsiElement child : file.getChildren()) {
            if (child instanceof io.aster.idea.psi.impl.AsterModuleDeclImpl moduleDecl) {
                return moduleDecl;
            }
        }
        return null;
    }
}
