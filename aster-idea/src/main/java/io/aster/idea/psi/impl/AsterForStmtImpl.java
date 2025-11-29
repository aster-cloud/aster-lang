package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Aster For 循环语句 PSI 元素
 * <p>
 * 语法: for item in collection:
 *         body
 */
@SuppressWarnings("serial")
public class AsterForStmtImpl extends AsterNamedElementImpl {

    /**
     * for 语句关键字，需要在查找循环变量时跳过
     */
    private static final Set<String> KEYWORDS = Set.of("for", "in");

    public AsterForStmtImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // for 循环的"名称"是循环变量
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，返回第一个非关键字（即循环变量）
        // CamelCase 变量名会被词法器标记为 TYPE_IDENT
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);
        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !KEYWORDS.contains(text.toLowerCase())) {
                return child.getPsi();
            }
        }
        return null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename not yet supported");
    }
}
