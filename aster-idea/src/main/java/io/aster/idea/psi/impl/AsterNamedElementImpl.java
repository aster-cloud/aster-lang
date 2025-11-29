package io.aster.idea.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.AsterNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 命名元素基础实现
 */
@SuppressWarnings("serial")
public abstract class AsterNamedElementImpl extends ASTWrapperPsiElement implements AsterNamedElement {

    public AsterNamedElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 查找 IDENT 或 TYPE_IDENT 子节点
        ASTNode identNode = getNode().findChildByType(AsterTokenTypes.IDENT);
        if (identNode == null) {
            identNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        }
        return identNode != null ? identNode.getPsi() : null;
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getText() : null;
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextOffset() : super.getTextOffset();
    }
}
