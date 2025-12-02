package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Aster 函数声明 PSI 元素
 * <p>
 * 支持两种语法：
 * - 自然语言语法: To funcName with param: Type, produce ReturnType:
 * - 传统语法: func funcName(params) -> Type:
 */
@SuppressWarnings("serial")
public class AsterFuncDeclImpl extends AsterNamedElementImpl {

    /**
     * 自然语言语法中的关键字，需要在查找函数名时跳过
     */
    private static final Set<String> NATURAL_KEYWORDS = Set.of(
        "to", "func", "with", "and", "produce", "maybe", "list", "of"
    );

    public AsterFuncDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 遍历所有 IDENT 和 TYPE_IDENT 子节点，跳过自然语言关键字，返回第一个非关键字标识符
        // CamelCase 函数名会被词法器标记为 TYPE_IDENT
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);
        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !NATURAL_KEYWORDS.contains(text.toLowerCase())) {
                return child.getPsi();
            }
        }
        return null;
    }

    // setName 继承自 AsterNamedElementImpl
}
