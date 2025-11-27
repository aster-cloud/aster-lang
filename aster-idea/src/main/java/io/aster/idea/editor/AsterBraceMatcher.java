package io.aster.idea.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 括号匹配器
 * <p>
 * 提供括号自动配对和高亮功能
 */
public class AsterBraceMatcher implements PairedBraceMatcher {

    private static final BracePair[] PAIRS = {
        new BracePair(AsterTokenTypes.LPAREN, AsterTokenTypes.RPAREN, false),
        new BracePair(AsterTokenTypes.LBRACKET, AsterTokenTypes.RBRACKET, false)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(
        @NotNull IElementType lbraceType,
        @Nullable IElementType contextType
    ) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
