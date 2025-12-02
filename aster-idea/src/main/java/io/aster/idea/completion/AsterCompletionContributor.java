package io.aster.idea.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import io.aster.idea.icons.AsterIcons;
import io.aster.idea.lang.AsterKeywords;
import io.aster.idea.lang.AsterLanguage;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.AsterFile;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.AsterElementTypes;
import io.aster.idea.psi.impl.*;
import io.aster.idea.reference.AsterModuleResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aster 代码补全贡献者
 * <p>
 * 提供以下补全功能：
 * - 关键字补全
 * - 标识符补全（函数名、变量名、类型名）
 * - 内置类型补全
 */
public class AsterCompletionContributor extends CompletionContributor {

    /**
     * Aster 关键字列表（引用统一定义）
     */
    private static final String[] KEYWORDS = AsterKeywords.COMPLETION_KEYWORDS;

    /**
     * 内置类型列表
     */
    private static final String[] BUILTIN_TYPES = {
        "Int", "Long", "Double", "Float", "String", "Bool",
        "List", "Map", "Set", "Option", "Result",
        "Unit", "Any", "Nothing"
    };

    @SuppressWarnings("this-escape")
    public AsterCompletionContributor() {
        // 在 Aster 文件中的任意位置提供补全
        extend(CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(AsterLanguage.INSTANCE),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result) {
                    PsiElement position = parameters.getPosition();
                    PsiFile file = parameters.getOriginalFile();

                    if (!(file instanceof AsterFile)) {
                        return;
                    }

                    // 添加关键字补全
                    addKeywordCompletions(result, position);

                    // 添加内置类型补全
                    addBuiltinTypeCompletions(result);

                    // 添加当前文件中定义的标识符
                    addLocalIdentifierCompletions(result, (AsterFile) file, position);

                    // 添加导入模块的符号（跨文件补全）
                    addImportedSymbolCompletions(result, (AsterFile) file, position);
                }
            });
    }

    /**
     * 添加关键字补全
     */
    private void addKeywordCompletions(@NotNull CompletionResultSet result, PsiElement position) {
        for (String keyword : KEYWORDS) {
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withIcon(AsterIcons.FILE)
                    .withTypeText("keyword")
                    .withBoldness(true)
                    .withInsertHandler((ctx, item) -> {
                        // 某些关键字后自动添加空格
                        if (needsSpaceAfter(keyword)) {
                            ctx.getDocument().insertString(ctx.getTailOffset(), " ");
                            ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset());
                        }
                    })
            );
        }
    }

    /**
     * 判断关键字后是否需要空格
     */
    private boolean needsSpaceAfter(String keyword) {
        return switch (keyword) {
            case "let", "set", "func", "data", "enum", "type", "import", "from",
                 "if", "elif", "match", "for", "in", "while", "case",
                 "module", "workflow", "step", "start", "wait", "uses",
                 "to", "with", "and", "produce", "it", "performs" -> true;
            default -> false;
        };
    }

    /**
     * 添加内置类型补全
     */
    private void addBuiltinTypeCompletions(@NotNull CompletionResultSet result) {
        for (String type : BUILTIN_TYPES) {
            result.addElement(
                LookupElementBuilder.create(type)
                    .withIcon(AsterIcons.TYPE)
                    .withTypeText("type")
                    .withItemTextItalic(true)
            );
        }
    }

    /**
     * 添加本地标识符补全（函数、变量、类型等）
     * 按作用域规则：只补全当前位置可见的标识符
     */
    private void addLocalIdentifierCompletions(@NotNull CompletionResultSet result,
                                                @NotNull AsterFile file,
                                                PsiElement position) {
        Set<String> addedNames = new HashSet<>();
        int positionOffset = position.getTextOffset();

        // 1. 首先收集当前作用域内的局部变量（只取位置之前的）
        collectLocalVariables(position, result, addedNames, positionOffset);

        // 2. 然后收集顶层定义（函数、类型等 - 全局可见）
        for (PsiElement child : file.getChildren()) {
            collectTopLevelElements(child, result, addedNames);
        }
    }

    /**
     * 收集当前作用域内的局部变量（向上遍历作用域链）
     */
    private void collectLocalVariables(PsiElement position,
                                       CompletionResultSet result,
                                       Set<String> addedNames,
                                       int positionOffset) {
        PsiElement scope = position.getParent();

        while (scope != null && !(scope instanceof AsterFile)) {
            for (PsiElement child : scope.getChildren()) {
                // 只添加位置之前定义的局部变量
                if (child.getTextOffset() >= positionOffset) {
                    break;
                }
                if (child instanceof AsterNamedElement namedElement) {
                    // 只收集局部变量类型（let, parameter, for 变量）
                    if (isLocalVariable(namedElement)) {
                        String name = namedElement.getName();
                        if (name != null && !name.isEmpty() && !addedNames.contains(name)) {
                            addedNames.add(name);
                            result.addElement(createLookupElement(namedElement, name));
                        }
                    }
                }
            }
            scope = scope.getParent();
        }
    }

    /**
     * 判断是否为局部变量类型（用于收集局部变量补全）
     * 只包括：let 语句、参数、for 变量
     */
    private boolean isLocalVariable(AsterNamedElement element) {
        return element instanceof AsterLetStmtImpl ||
               element instanceof AsterParameterImpl ||
               element instanceof AsterForStmtImpl;
    }

    /**
     * 判断是否为局部声明（不应该出现在全局补全列表中）
     * 包括：局部变量、参数、for 循环变量、it-performs 等
     * 注意：workflow 不在此处判断，因为顶层 workflow 应该是全局可见的，
     * 只有嵌套在函数或其他 workflow 内部的声明才视为局部（由下方父节点检查处理）
     */
    private boolean isLocalDeclaration(AsterNamedElement element) {
        // 直接判断类型（这些类型总是局部的）
        if (element instanceof AsterLetStmtImpl ||
            element instanceof AsterParameterImpl ||
            element instanceof AsterForStmtImpl ||
            element instanceof AsterItPerformsStmtImpl) {
            return true;
        }

        // 额外检查：如果元素在函数体或 workflow 体内部，则视为局部声明
        PsiElement parent = element.getParent();
        while (parent != null && !(parent instanceof AsterFile)) {
            // 如果父节点是函数或 workflow，则这是局部声明
            if (parent instanceof AsterFuncDeclImpl || parent instanceof AsterWorkflowStmtImpl) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    /**
     * 收集顶层元素（函数、类型等 - 全局可见，不受位置限制）
     */
    private void collectTopLevelElements(PsiElement element,
                                         CompletionResultSet result,
                                         Set<String> addedNames) {
        if (element instanceof AsterNamedElement namedElement) {
            // 只收集顶层定义类型（不是局部声明）
            if (!isLocalDeclaration(namedElement)) {
                String name = namedElement.getName();
                if (name != null && !name.isEmpty() && !addedNames.contains(name)) {
                    addedNames.add(name);
                    result.addElement(createLookupElement(namedElement, name));
                }
            }
        }

        // 递归处理子元素（查找嵌套的顶层定义）
        for (PsiElement child : element.getChildren()) {
            collectTopLevelElements(child, result, addedNames);
        }
    }

    /**
     * 根据元素类型创建补全项
     */
    private LookupElementBuilder createLookupElement(AsterNamedElement element, String name) {
        String typeText = getTypeText(element);
        LookupElementBuilder builder = LookupElementBuilder.create(name)
            .withIcon(getIconForElement(element))
            .withTypeText(typeText);

        // 函数补全时添加括号
        if (element instanceof AsterFuncDeclImpl) {
            builder = builder.withInsertHandler((ctx, item) -> {
                ctx.getDocument().insertString(ctx.getTailOffset(), "()");
                ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset() - 1);
            });
        }

        return builder;
    }

    /**
     * 获取元素的类型描述文本
     */
    private String getTypeText(AsterNamedElement element) {
        if (element instanceof AsterFuncDeclImpl) {
            return "function";
        } else if (element instanceof AsterDataDeclImpl) {
            return "data";
        } else if (element instanceof AsterEnumDeclImpl) {
            return "enum";
        } else if (element instanceof AsterTypeAliasDeclImpl) {
            return "type";
        } else if (element instanceof AsterLetStmtImpl) {
            return "variable";
        } else if (element instanceof AsterParameterImpl) {
            return "parameter";
        } else if (element instanceof AsterFieldDefImpl) {
            return "field";
        } else if (element instanceof AsterModuleDeclImpl) {
            return "module";
        } else if (element instanceof AsterImportDeclImpl) {
            return "import";
        } else if (element instanceof AsterWorkflowStmtImpl) {
            return "workflow";
        } else if (element instanceof AsterForStmtImpl) {
            return "loop variable";
        }
        return "identifier";
    }

    /**
     * 根据元素类型获取对应图标
     */
    private javax.swing.Icon getIconForElement(AsterNamedElement element) {
        if (element instanceof AsterFuncDeclImpl) {
            return AsterIcons.FUNCTION;
        } else if (element instanceof AsterDataDeclImpl) {
            return AsterIcons.DATA;
        } else if (element instanceof AsterEnumDeclImpl) {
            return AsterIcons.ENUM;
        } else if (element instanceof AsterTypeAliasDeclImpl) {
            return AsterIcons.TYPE;
        } else if (element instanceof AsterLetStmtImpl ||
                   element instanceof AsterParameterImpl ||
                   element instanceof AsterForStmtImpl ||
                   element instanceof AsterFieldDefImpl) {
            return AsterIcons.VARIABLE;
        } else if (element instanceof AsterModuleDeclImpl ||
                   element instanceof AsterImportDeclImpl) {
            return AsterIcons.MODULE;
        } else if (element instanceof AsterWorkflowStmtImpl) {
            return AsterIcons.WORKFLOW;
        }
        return AsterIcons.FILE;
    }

    /**
     * 添加导入模块的符号补全（跨文件）
     * <p>
     * 补全策略：
     * - 检测上下文：判断光标前是否已有 "alias." 或 "alias.submodule."
     * - 有前缀时：解析完整路径，显示对应模块的符号
     * - 无前缀时：插入完整的 "alias.symbol"
     * <p>
     * 支持多级路径：
     * - "math." → 显示 math 模块的符号
     * - "math.core." → 解析 math.core 子模块，显示其符号
     */
    private void addImportedSymbolCompletions(@NotNull CompletionResultSet result,
                                               @NotNull AsterFile file,
                                               @NotNull PsiElement position) {
        Project project = file.getProject();
        AsterModuleResolver resolver = AsterModuleResolver.getInstance(project);

        // 收集当前文件的所有导入
        Map<String, AsterModuleResolver.ImportInfo> imports = resolver.collectImports(file);

        // 检测光标前是否已有别名前缀（如 "math." 或 "math.core."）
        // 返回完整路径和首段别名信息
        PrefixInfo prefixInfo = findExistingAliasPrefix(position, imports);

        for (AsterModuleResolver.ImportInfo importInfo : imports.values()) {
            // 使用缓存模式：计算一次导入类型并复用结果
            AsterModuleResolver.ImportInfo.ImportTypeResult typeResult = importInfo.computeImportType(resolver);
            AsterModuleResolver.ImportInfo.ImportType importType = typeResult.type;

            boolean isModuleImport = (importType == AsterModuleResolver.ImportInfo.ImportType.MODULE);
            boolean isSymbolImport = (importType == AsterModuleResolver.ImportInfo.ImportType.SYMBOL);

            // 如果没有前缀，添加导入别名作为补全项
            if (prefixInfo == null) {
                String typeText;
                if (isModuleImport) {
                    typeText = "module: " + importInfo.modulePath;
                } else if (isSymbolImport) {
                    typeText = "symbol: " + importInfo.modulePath;
                } else {
                    typeText = "import: " + importInfo.modulePath;
                }

                result.addElement(
                    LookupElementBuilder.create(importInfo.alias)
                        .withIcon(AsterIcons.MODULE)
                        .withTypeText(typeText)
                        .withItemTextItalic(true)
                );
            }

            // 检查前缀是否匹配当前导入的别名
            boolean aliasMatches = prefixInfo != null && importInfo.alias.equals(prefixInfo.alias);

            if (aliasMatches) {
                // 只有模块导入才支持 alias.symbol 形式的补全
                // 符号导入（如 use aster.math.add as add）不应该有 add.* 补全
                if (isModuleImport) {
                    // 构建要解析的完整模块路径
                    // 如果用户输入 "math.core."，而 math 指向 "aster.math"
                    // 则尝试解析 "aster.math.core"
                    String moduleToResolve = buildModulePath(importInfo.modulePath, prefixInfo.subPath);

                    // 解析模块并添加其导出符号
                    AsterFile resolvedModule = resolver.resolveModule(moduleToResolve);
                    if (resolvedModule != null) {
                        addModuleSymbolCompletions(result, resolver, resolvedModule, moduleToResolve, true);
                    } else if (prefixInfo.subPath != null && !prefixInfo.subPath.isEmpty()) {
                        // 子路径解析失败时，回退到基础模块
                        // 这允许用户在输入部分路径时仍能看到父模块的符号
                        // 例如：输入 "math.c" 时显示 "aster.math" 的符号（IntelliJ 会过滤匹配 "c" 的项）
                        AsterFile moduleFile = resolver.resolveModule(importInfo.modulePath);
                        if (moduleFile != null) {
                            addModuleSymbolCompletionsWithSubPathFallback(
                                result, resolver, moduleFile, importInfo.modulePath,
                                prefixInfo.subPath, importInfo.alias);
                        }
                    }
                }
                // 符号导入时用户输入 alias. 不提供任何补全，因为符号不是模块
            } else if (prefixInfo == null && isModuleImport) {
                // 无前缀且是模块导入，添加完整的 alias.symbol 补全
                AsterFile moduleFile = resolver.resolveModule(importInfo.modulePath);
                if (moduleFile != null) {
                    addModuleSymbolCompletions(result, resolver, moduleFile, importInfo.modulePath, false, importInfo.alias);
                }
            }
            // 符号导入或有前缀但不匹配的情况，跳过 alias.* 补全
        }
    }

    /**
     * 构建完整的模块路径
     *
     * @param basePath 基础模块路径（如 "aster.math"）
     * @param subPath 子路径（如 "core"），可能为 null
     * @return 完整路径（如 "aster.math.core"）
     */
    private String buildModulePath(String basePath, String subPath) {
        if (subPath == null || subPath.isEmpty()) {
            return basePath;
        }
        return basePath + "." + subPath;
    }

    /**
     * 添加模块符号补全（带前缀版本）
     */
    private void addModuleSymbolCompletions(@NotNull CompletionResultSet result,
                                            @NotNull AsterModuleResolver resolver,
                                            @NotNull AsterFile moduleFile,
                                            @NotNull String modulePath,
                                            boolean prefixMatches) {
        addModuleSymbolCompletions(result, resolver, moduleFile, modulePath, prefixMatches, null);
    }

    /**
     * 添加模块符号补全
     *
     * @param prefixMatches 是否有匹配的前缀
     * @param alias 别名（仅在无前缀时使用）
     */
    private void addModuleSymbolCompletions(@NotNull CompletionResultSet result,
                                            @NotNull AsterModuleResolver resolver,
                                            @NotNull AsterFile moduleFile,
                                            @NotNull String modulePath,
                                            boolean prefixMatches,
                                            String alias) {
        List<AsterNamedElement> exports = resolver.getExportedSymbols(moduleFile);
        for (AsterNamedElement export : exports) {
            String name = export.getName();
            if (name != null && !name.isEmpty()) {
                if (prefixMatches) {
                    // 已有匹配的前缀，只插入符号名
                    result.addElement(
                        LookupElementBuilder.create(name)
                            .withIcon(getIconForElement(export))
                            .withTypeText(getTypeText(export) + " from " + modulePath)
                            .withInsertHandler((ctx, item) -> {
                                if (export instanceof AsterFuncDeclImpl) {
                                    ctx.getDocument().insertString(ctx.getTailOffset(), "()");
                                    ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset() - 1);
                                }
                            })
                    );
                } else if (alias != null) {
                    // 无前缀，插入完整的 alias.symbol
                    String qualifiedName = alias + "." + name;
                    result.addElement(
                        LookupElementBuilder.create(qualifiedName)
                            .withIcon(getIconForElement(export))
                            .withTypeText(getTypeText(export) + " from " + modulePath)
                            .withPresentableText(name)
                            .withTailText(" (" + alias + ")", true)
                            .withInsertHandler((ctx, item) -> {
                                if (export instanceof AsterFuncDeclImpl) {
                                    ctx.getDocument().insertString(ctx.getTailOffset(), "()");
                                    ctx.getEditor().getCaretModel().moveToOffset(ctx.getTailOffset() - 1);
                                }
                            })
                    );
                }
            }
        }
    }

    /**
     * 添加模块符号补全（子路径解析失败时的回退版本）
     * <p>
     * 当用户输入的子路径无法解析为有效模块时，回退到父模块并显示其符号。
     * 补全项会替换用户输入的无效子路径，确保生成有效的引用。
     *
     * @param result 补全结果集
     * @param resolver 模块解析器
     * @param moduleFile 父模块文件
     * @param modulePath 父模块路径
     * @param invalidSubPath 无效的子路径（将被替换）
     * @param alias 导入别名
     */
    private void addModuleSymbolCompletionsWithSubPathFallback(
            @NotNull CompletionResultSet result,
            @NotNull AsterModuleResolver resolver,
            @NotNull AsterFile moduleFile,
            @NotNull String modulePath,
            @NotNull String invalidSubPath,
            @NotNull String alias) {
        List<AsterNamedElement> exports = resolver.getExportedSymbols(moduleFile);
        for (AsterNamedElement export : exports) {
            String name = export.getName();
            if (name != null && !name.isEmpty()) {
                // 计算需要替换的字符数：无效子路径 + 前面的点
                int charsToRemove = invalidSubPath.length() + 1; // +1 for the dot

                result.addElement(
                    LookupElementBuilder.create(name)
                        .withIcon(getIconForElement(export))
                        .withTypeText(getTypeText(export) + " from " + modulePath)
                        .withTailText(" (fallback)", true)
                        .withInsertHandler((ctx, item) -> {
                            // 删除无效的子路径（包括前面的点）
                            int startOffset = ctx.getStartOffset() - charsToRemove;
                            if (startOffset >= 0) {
                                ctx.getDocument().deleteString(startOffset, ctx.getStartOffset());
                                // 调整光标位置
                                ctx.getEditor().getCaretModel().moveToOffset(
                                    ctx.getTailOffset() - charsToRemove);
                            }
                            // 如果是函数，添加括号
                            if (export instanceof AsterFuncDeclImpl) {
                                int newTailOffset = ctx.getTailOffset() - charsToRemove;
                                ctx.getDocument().insertString(newTailOffset, "()");
                                ctx.getEditor().getCaretModel().moveToOffset(newTailOffset + 1);
                            }
                        })
                );
            }
        }
    }

    /**
     * 前缀信息：包含别名和子路径
     */
    private record PrefixInfo(String alias, String subPath) {}

    /**
     * 查找光标前已存在的别名前缀
     * <p>
     * 向左遍历找到完整的限定路径，返回别名和子路径信息。
     * 例如：
     * <ul>
     *   <li>"math." 返回 PrefixInfo("math", null)</li>
     *   <li>"math.core." 返回 PrefixInfo("math", "core")</li>
     *   <li>"math.core.sub." 返回 PrefixInfo("math", "core.sub")</li>
     * </ul>
     *
     * @param position 当前光标位置
     * @param imports 导入表，用于验证别名是否有效
     * @return 前缀信息，如果没有有效前缀则返回 null
     */
    private PrefixInfo findExistingAliasPrefix(@NotNull PsiElement position,
                                               @NotNull Map<String, AsterModuleResolver.ImportInfo> imports) {
        // 收集完整的限定路径段
        List<String> segments = new ArrayList<>();
        PsiElement current = position;

        while (true) {
            // 跳过空白和注释，查找点号
            PsiElement prev = PsiTreeUtil.skipWhitespacesAndCommentsBackward(current);

            if (prev == null || !".".equals(prev.getText())) {
                break;
            }

            // 跳过点号前的空白和注释，查找标识符
            PsiElement beforeDot = PsiTreeUtil.skipWhitespacesAndCommentsBackward(prev);

            if (beforeDot == null) {
                break;
            }

            String text = beforeDot.getText();
            if (text == null || text.isEmpty() || !Character.isJavaIdentifierStart(text.charAt(0))) {
                break;
            }

            // 在列表头部插入这一段
            segments.add(0, text);
            current = beforeDot;
        }

        // 如果没有找到任何段，返回 null
        if (segments.isEmpty()) {
            return null;
        }

        // 第一段是别名，后续段是子路径
        String alias = segments.get(0);
        if (!imports.containsKey(alias)) {
            // 别名不在导入表中，返回 null
            return null;
        }

        // 构建子路径（从第二段开始）
        String subPath = null;
        if (segments.size() > 1) {
            subPath = String.join(".", segments.subList(1, segments.size()));
        }

        return new PrefixInfo(alias, subPath);
    }
}
