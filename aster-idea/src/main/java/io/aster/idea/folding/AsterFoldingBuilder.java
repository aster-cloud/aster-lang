package io.aster.idea.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.aster.idea.psi.AsterElementTypes;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Aster 代码折叠构建器
 * <p>
 * 支持以下元素的折叠：
 * - 函数体
 * - 数据类型定义
 * - 枚举定义
 * - if/match/for/while 块
 * - workflow 定义
 * - 多行注释
 */
public class AsterFoldingBuilder extends FoldingBuilderEx implements DumbAware {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root,
                                                           @NotNull Document document,
                                                           boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        collectFoldingRegions(root, descriptors, document);
        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    /**
     * 递归收集可折叠区域
     */
    private void collectFoldingRegions(@NotNull PsiElement element,
                                       @NotNull List<FoldingDescriptor> descriptors,
                                       @NotNull Document document) {
        ASTNode node = element.getNode();
        if (node == null) {
            return;
        }

        // 检查是否可以折叠此元素
        FoldingDescriptor descriptor = createFoldingDescriptor(element, document);
        if (descriptor != null) {
            descriptors.add(descriptor);
        }

        // 递归处理子元素
        for (PsiElement child : element.getChildren()) {
            collectFoldingRegions(child, descriptors, document);
        }
    }

    /**
     * 为元素创建折叠描述符
     */
    private @Nullable FoldingDescriptor createFoldingDescriptor(@NotNull PsiElement element,
                                                                  @NotNull Document document) {
        ASTNode node = element.getNode();
        if (node == null) {
            return null;
        }

        // 函数声明
        if (element instanceof AsterFuncDeclImpl) {
            return createBlockFoldingDescriptor(element, document, "func...");
        }

        // 数据类型声明
        if (element instanceof AsterDataDeclImpl) {
            return createBlockFoldingDescriptor(element, document, "data...");
        }

        // 枚举声明
        if (element instanceof AsterEnumDeclImpl) {
            return createBlockFoldingDescriptor(element, document, "enum...");
        }

        // 工作流声明
        if (element instanceof AsterWorkflowStmtImpl) {
            return createBlockFoldingDescriptor(element, document, "workflow...");
        }

        // if 语句
        if (node.getElementType() == AsterElementTypes.IF_STMT) {
            return createBlockFoldingDescriptor(element, document, "if...");
        }

        // match 语句
        if (node.getElementType() == AsterElementTypes.MATCH_STMT) {
            return createBlockFoldingDescriptor(element, document, "match...");
        }

        // for 循环
        if (element instanceof AsterForStmtImpl) {
            return createBlockFoldingDescriptor(element, document, "for...");
        }

        // while 循环
        if (element instanceof AsterWhileStmtImpl) {
            return createBlockFoldingDescriptor(element, document, "while...");
        }

        // 代码块
        if (node.getElementType() == AsterElementTypes.BLOCK) {
            TextRange range = element.getTextRange();
            if (isMultiLine(range, document)) {
                return new FoldingDescriptor(node, range, FoldingGroup.newGroup("block"));
            }
        }

        return null;
    }

    /**
     * 创建块级折叠描述符
     */
    private @Nullable FoldingDescriptor createBlockFoldingDescriptor(@NotNull PsiElement element,
                                                                      @NotNull Document document,
                                                                      @NotNull String placeholder) {
        TextRange range = element.getTextRange();

        // 只折叠多行元素
        if (!isMultiLine(range, document)) {
            return null;
        }

        // 从第一行末尾开始折叠（保留声明行）
        int startLine = document.getLineNumber(range.getStartOffset());
        int startLineEnd = document.getLineEndOffset(startLine);

        // 确保折叠范围有效
        if (startLineEnd >= range.getEndOffset()) {
            return null;
        }

        TextRange foldRange = new TextRange(startLineEnd, range.getEndOffset());
        if (foldRange.isEmpty()) {
            return null;
        }

        return new FoldingDescriptor(
            element.getNode(),
            foldRange,
            FoldingGroup.newGroup("aster"),
            placeholder
        );
    }

    /**
     * 检查范围是否跨越多行
     */
    private boolean isMultiLine(@NotNull TextRange range, @NotNull Document document) {
        int startLine = document.getLineNumber(range.getStartOffset());
        int endLine = document.getLineNumber(range.getEndOffset());
        return endLine > startLine;
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // 默认不折叠
        return false;
    }
}
