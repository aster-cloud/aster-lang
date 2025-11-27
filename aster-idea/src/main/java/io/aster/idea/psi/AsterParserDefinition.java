package io.aster.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterLanguage;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 解析器定义
 * <p>
 * 定义 Aster 语言的词法分析和解析规则。
 * 当前实现仅提供词法分析支持，完整的语法解析由 aster-core 处理。
 */
public class AsterParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE =
        new IFileElementType(AsterLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new AsterLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        // 返回一个简单的解析器，将所有 token 作为叶子节点
        return new AsterParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return AsterTokenTypes.COMMENTS;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return AsterTokenTypes.WHITESPACES;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return AsterTokenTypes.STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        // 返回通用 PSI 元素
        return new AsterPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new AsterFile(viewProvider);
    }
}
