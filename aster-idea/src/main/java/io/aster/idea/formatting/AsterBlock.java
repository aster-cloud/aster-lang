package io.aster.idea.formatting;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.AsterElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Aster 格式化块
 * <p>
 * 定义 Aster 代码的格式化规则，包括缩进、对齐和间距。
 */
public class AsterBlock implements Block {

    private final ASTNode node;
    private final Alignment alignment;
    private final Indent indent;
    private final CodeStyleSettings settings;
    private List<Block> subBlocks;

    public AsterBlock(@NotNull ASTNode node,
                      @Nullable Alignment alignment,
                      @NotNull Indent indent,
                      @NotNull CodeStyleSettings settings) {
        this.node = node;
        this.alignment = alignment;
        this.indent = indent;
        this.settings = settings;
    }

    public @NotNull ASTNode getNode() {
        return node;
    }

    @Override
    public @NotNull TextRange getTextRange() {
        return node.getTextRange();
    }

    @Override
    public @NotNull List<Block> getSubBlocks() {
        if (subBlocks == null) {
            subBlocks = buildSubBlocks();
        }
        return subBlocks;
    }

    /**
     * 构建子块列表
     */
    private List<Block> buildSubBlocks() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = node.getFirstChildNode();

        while (child != null) {
            IElementType type = child.getElementType();

            // 跳过空白节点
            if (type != TokenType.WHITE_SPACE && child.getTextLength() > 0) {
                Indent childIndent = calculateIndent(child);
                blocks.add(new AsterBlock(child, null, childIndent, settings));
            }

            child = child.getTreeNext();
        }

        return blocks;
    }

    /**
     * 计算子节点的缩进
     */
    private Indent calculateIndent(@NotNull ASTNode child) {
        IElementType parentType = node.getElementType();
        IElementType childType = child.getElementType();

        // 函数体、数据类型体等需要缩进
        if (isBlockLevelElement(parentType)) {
            // 块内的语句需要正常缩进
            if (isIndentableElement(childType)) {
                return Indent.getNormalIndent();
            }
        }

        // 代码块内容需要缩进
        if (parentType == AsterElementTypes.BLOCK) {
            return Indent.getNormalIndent();
        }

        // if/match/for/while 体需要缩进
        if (isControlFlowElement(parentType)) {
            if (isIndentableElement(childType) || childType == AsterElementTypes.BLOCK) {
                return Indent.getNormalIndent();
            }
        }

        // match case 内容需要缩进
        if (parentType == AsterElementTypes.MATCH_CASE) {
            if (isIndentableElement(childType)) {
                return Indent.getNormalIndent();
            }
        }

        // workflow step 内容需要缩进
        if (parentType == AsterElementTypes.WORKFLOW_STEP ||
            parentType == AsterElementTypes.WORKFLOW_COMPENSATE ||
            parentType == AsterElementTypes.WORKFLOW_TIMEOUT ||
            parentType == AsterElementTypes.WORKFLOW_RETRY) {
            if (isIndentableElement(childType)) {
                return Indent.getNormalIndent();
            }
        }

        return Indent.getNoneIndent();
    }

    /**
     * 判断是否为块级元素（需要子元素缩进）
     */
    private boolean isBlockLevelElement(IElementType type) {
        return type == AsterElementTypes.FUNC_DECL ||
               type == AsterElementTypes.DATA_DECL ||
               type == AsterElementTypes.ENUM_DECL ||
               type == AsterElementTypes.WORKFLOW_STMT ||
               type == AsterElementTypes.MODULE_DECL;
    }

    /**
     * 判断是否为控制流元素
     */
    private boolean isControlFlowElement(IElementType type) {
        return type == AsterElementTypes.IF_STMT ||
               type == AsterElementTypes.MATCH_STMT ||
               type == AsterElementTypes.FOR_STMT ||
               type == AsterElementTypes.WHILE_STMT;
    }

    /**
     * 判断是否为需要缩进的子元素
     * <p>
     * 包括语句、声明和块级结构，用于格式化时确定缩进规则
     */
    private boolean isIndentableElement(IElementType type) {
        return type == AsterElementTypes.LET_STMT ||
               type == AsterElementTypes.SET_STMT ||
               type == AsterElementTypes.RETURN_STMT ||
               type == AsterElementTypes.IF_STMT ||
               type == AsterElementTypes.MATCH_STMT ||
               type == AsterElementTypes.FOR_STMT ||
               type == AsterElementTypes.WHILE_STMT ||
               type == AsterElementTypes.START_STMT ||
               type == AsterElementTypes.WAIT_STMT ||
               type == AsterElementTypes.IT_PERFORMS_STMT ||
               type == AsterElementTypes.WORKFLOW_STMT ||
               type == AsterElementTypes.WORKFLOW_STEP ||
               type == AsterElementTypes.FUNC_DECL ||
               type == AsterElementTypes.DATA_DECL ||
               type == AsterElementTypes.ENUM_DECL ||
               type == AsterElementTypes.TYPE_ALIAS_DECL ||
               type == AsterElementTypes.FIELD_DEF ||
               type == AsterElementTypes.ENUM_VARIANT ||
               type == AsterElementTypes.PARAMETER ||
               type == AsterElementTypes.MATCH_CASE;
    }

    @Override
    public @Nullable Wrap getWrap() {
        return null;
    }

    @Override
    public @Nullable Indent getIndent() {
        return indent;
    }

    @Override
    public @Nullable Alignment getAlignment() {
        return alignment;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        // 基本间距规则
        if (child1 == null) {
            return null;
        }

        ASTNode node1 = ((AsterBlock) child1).getNode();
        ASTNode node2 = ((AsterBlock) child2).getNode();
        IElementType type1 = node1.getElementType();
        IElementType type2 = node2.getElementType();

        // 关键字后需要空格
        if (type1 == AsterTokenTypes.KEYWORD) {
            return Spacing.createSpacing(1, 1, 0, false, 0);
        }

        // 冒号后需要空格
        if (type1 == AsterTokenTypes.COLON) {
            return Spacing.createSpacing(1, 1, 0, false, 0);
        }

        // 冒号前不需要空格
        if (type2 == AsterTokenTypes.COLON) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 逗号后需要空格
        if (type1 == AsterTokenTypes.COMMA) {
            return Spacing.createSpacing(1, 1, 0, false, 0);
        }

        // 逗号前不需要空格
        if (type2 == AsterTokenTypes.COMMA) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 左括号后不需要空格
        if (type1 == AsterTokenTypes.LPAREN) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 右括号前不需要空格
        if (type2 == AsterTokenTypes.RPAREN) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 左方括号后不需要空格
        if (type1 == AsterTokenTypes.LBRACKET) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 右方括号前不需要空格
        if (type2 == AsterTokenTypes.RBRACKET) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 点操作符前后不需要空格
        if (type1 == AsterTokenTypes.DOT || type2 == AsterTokenTypes.DOT) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        // 运算符前后需要空格
        if (isOperator(type1) || isOperator(type2)) {
            return Spacing.createSpacing(1, 1, 0, false, 0);
        }

        return null;
    }

    /**
     * 判断是否为运算符
     */
    private boolean isOperator(IElementType type) {
        return type == AsterTokenTypes.EQUALS ||
               type == AsterTokenTypes.PLUS ||
               type == AsterTokenTypes.MINUS ||
               type == AsterTokenTypes.STAR ||
               type == AsterTokenTypes.SLASH ||
               type == AsterTokenTypes.LT ||
               type == AsterTokenTypes.GT ||
               type == AsterTokenTypes.LTE ||
               type == AsterTokenTypes.GTE ||
               type == AsterTokenTypes.NEQ;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        // 为新子节点提供默认缩进
        IElementType type = node.getElementType();

        if (isBlockLevelElement(type) || isControlFlowElement(type) ||
            type == AsterElementTypes.BLOCK ||
            type == AsterElementTypes.MATCH_CASE) {
            return new ChildAttributes(Indent.getNormalIndent(), null);
        }

        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    @Override
    public boolean isIncomplete() {
        // 判断块是否不完整（例如缺少右括号）
        return false;
    }

    @Override
    public boolean isLeaf() {
        return node.getFirstChildNode() == null;
    }
}
