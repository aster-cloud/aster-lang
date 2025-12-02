package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Aster 数据类型声明 PSI 元素
 * <p>
 * 支持两种语法：
 * - 自然语言语法: Define a TypeName with field: Type.
 * - 传统语法: data TypeName:
 */
@SuppressWarnings("serial")
public class AsterDataDeclImpl extends AsterNamedElementImpl {

    /**
     * 自然语言语法中的关键字，需要在查找类型名时跳过
     */
    private static final Set<String> NATURAL_KEYWORDS = Set.of(
        "define", "data", "a", "an", "with", "and"
    );

    public AsterDataDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 优先查找 TYPE_IDENT（大写开头的类型名）
        ASTNode typeIdentNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        if (typeIdentNode != null) {
            return typeIdentNode.getPsi();
        }

        // 回退：遍历 IDENT 子节点，跳过自然语言关键字
        // （处理某些类型名可能被识别为 IDENT 的情况）
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT);
        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !NATURAL_KEYWORDS.contains(text.toLowerCase())) {
                return child.getPsi();
            }
        }
        return null;
    }

    // setName() 继承自 AsterNamedElementImpl，支持重命名
}
