package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 参数 PSI 元素
 * <p>
 * 语法: paramName: Type
 */
@SuppressWarnings("serial")
public class AsterParameterImpl extends AsterNamedElementImpl {

    public AsterParameterImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 参数名是第一个 IDENT 或 TYPE_IDENT
        // CamelCase 参数名会被词法器标记为 TYPE_IDENT
        ASTNode identNode = getNode().findChildByType(AsterTokenTypes.IDENT);
        if (identNode != null) {
            return identNode.getPsi();
        }
        // 回退到 TYPE_IDENT
        ASTNode typeIdentNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        return typeIdentNode != null ? typeIdentNode.getPsi() : null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename not yet supported");
    }
}
