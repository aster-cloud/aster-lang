package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 类型别名声明 PSI 元素
 * <p>
 * 语法: type AliasName = TargetType
 */
@SuppressWarnings("serial")
public class AsterTypeAliasDeclImpl extends AsterNamedElementImpl {

    public AsterTypeAliasDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 类型别名名优先查找 TYPE_IDENT（大写开头）
        ASTNode typeIdentNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        if (typeIdentNode != null) {
            return typeIdentNode.getPsi();
        }
        // 回退到 IDENT（小写开头的情况）
        ASTNode identNode = getNode().findChildByType(AsterTokenTypes.IDENT);
        return identNode != null ? identNode.getPsi() : null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename not yet supported");
    }
}
