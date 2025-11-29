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
 * Aster 导入声明 PSI 元素
 * <p>
 * 支持两种语法:
 * - 自然语言: Use module.path. 或 Use module.path as alias.
 * - 传统语法: import module.path as alias
 */
@SuppressWarnings("serial")
public class AsterImportDeclImpl extends AsterNamedElementImpl {

    /**
     * 导入关键字，需要在查找模块名时跳过
     */
    private static final Set<String> KEYWORDS = Set.of("use", "import", "as");

    public AsterImportDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 导入声明的"名称"是模块路径的第一部分
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，跳过关键字
        // CamelCase 模块名会被词法器标记为 TYPE_IDENT
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
