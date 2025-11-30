package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 字段定义 PSI 元素
 * <p>
 * 语法: fieldName: Type
 */
@SuppressWarnings("serial")
public class AsterFieldDefImpl extends AsterNamedElementImpl {

    public AsterFieldDefImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 字段名是第一个 IDENT 或 TYPE_IDENT
        // CamelCase 字段名会被词法器标记为 TYPE_IDENT
        ASTNode identNode = getNode().findChildByType(AsterTokenTypes.IDENT);
        if (identNode != null) {
            return identNode.getPsi();
        }
        // 回退到 TYPE_IDENT
        ASTNode typeIdentNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        return typeIdentNode != null ? typeIdentNode.getPsi() : null;
    }

    // setName() 继承自 AsterNamedElementImpl，支持重命名
}
