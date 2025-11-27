package io.aster.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 简单解析器
 * <p>
 * 此解析器仅消费所有 token 而不构建复杂的 AST 结构。
 * 完整的语法分析由 aster-core 在外部进行。
 */
public class AsterParser implements PsiParser {

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();

        // 消费所有 token
        while (!builder.eof()) {
            builder.advanceLexer();
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
