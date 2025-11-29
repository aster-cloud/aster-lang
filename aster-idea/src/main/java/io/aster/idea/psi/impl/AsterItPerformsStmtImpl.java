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
 * Aster "It performs" 自然语言工作流语句 PSI 元素
 * <p>
 * 语法: It performs action.
 * 或: It performs action with param: value.
 */
@SuppressWarnings("serial")
public class AsterItPerformsStmtImpl extends AsterNamedElementImpl {

    /**
     * 语句关键字，需要在查找动作名时跳过
     */
    private static final Set<String> KEYWORDS = Set.of("it", "performs", "with");

    public AsterItPerformsStmtImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // "It performs" 语句的"名称"是动作名
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，跳过关键字，返回动作名
        // CamelCase 动作名会被词法器标记为 TYPE_IDENT
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
