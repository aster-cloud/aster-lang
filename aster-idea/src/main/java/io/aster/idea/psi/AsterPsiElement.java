package io.aster.idea.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 通用 PSI 元素
 * <p>
 * 包装 AST 节点为 PSI 元素
 */
@SuppressWarnings("serial")
public class AsterPsiElement extends ASTWrapperPsiElement {

    public AsterPsiElement(@NotNull ASTNode node) {
        super(node);
    }
}
