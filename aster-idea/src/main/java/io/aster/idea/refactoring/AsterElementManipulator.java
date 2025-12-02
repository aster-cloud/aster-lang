package io.aster.idea.refactoring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 元素操作器
 * <p>
 * 支持对标识符元素进行重命名等操作。
 */
public class AsterElementManipulator extends AbstractElementManipulator<PsiElement> {

    @Override
    public @Nullable PsiElement handleContentChange(@NotNull PsiElement element,
                                                     @NotNull TextRange range,
                                                     String newContent) throws IncorrectOperationException {
        // 对于 Leaf 元素（IDENT 或 TYPE_IDENT token），直接替换文本
        if (element instanceof LeafPsiElement leafElement) {
            String oldText = element.getText();
            String newText = oldText.substring(0, range.getStartOffset()) +
                             newContent +
                             oldText.substring(range.getEndOffset());

            // 创建新的 Leaf 节点并替换
            LeafPsiElement newLeaf = (LeafPsiElement) leafElement.replaceWithText(newText);
            return newLeaf;
        }

        return element;
    }

    @Override
    public @NotNull TextRange getRangeInElement(@NotNull PsiElement element) {
        return new TextRange(0, element.getTextLength());
    }
}
