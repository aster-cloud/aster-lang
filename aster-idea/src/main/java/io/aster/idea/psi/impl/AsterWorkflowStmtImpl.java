package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Aster 工作流语句 PSI 元素
 * <p>
 * 语法: workflow name(params) uses [caps]:
 */
@SuppressWarnings("serial")
public class AsterWorkflowStmtImpl extends AsterNamedElementImpl {

    /**
     * 关键字，需要在查找工作流名时跳过
     */
    private static final Set<String> KEYWORDS = Set.of("workflow", "uses");

    public AsterWorkflowStmtImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，跳过关键字，返回第一个非关键字标识符
        // CamelCase 工作流名会被词法器标记为 TYPE_IDENT
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);
        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !KEYWORDS.contains(text.toLowerCase())) {
                return child.getPsi();
            }
        }
        return null;
    }

    // setName() 继承自 AsterNamedElementImpl，支持重命名
}
