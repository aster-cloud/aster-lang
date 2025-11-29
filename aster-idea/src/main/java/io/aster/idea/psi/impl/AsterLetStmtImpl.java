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
 * Aster Let 语句 PSI 元素（变量声明）
 * <p>
 * 语法: let varName = expression
 */
@SuppressWarnings("serial")
public class AsterLetStmtImpl extends AsterNamedElementImpl {

    /**
     * 语句关键字，需要在查找变量名时跳过
     */
    private static final Set<String> KEYWORDS = Set.of("let");

    public AsterLetStmtImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，跳过 'let' 关键字，返回变量名
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
