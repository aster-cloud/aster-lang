package io.aster.idea.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterLexerAdapter;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Aster 语法高亮器
 * <p>
 * 为不同类型的 token 分配颜色属性
 */
public class AsterSyntaxHighlighter extends SyntaxHighlighterBase {

    // 文本属性键定义
    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("ASTER_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey STRING =
        createTextAttributesKey("ASTER_STRING", DefaultLanguageHighlighterColors.STRING);

    public static final TextAttributesKey NUMBER =
        createTextAttributesKey("ASTER_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey COMMENT =
        createTextAttributesKey("ASTER_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("ASTER_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);

    public static final TextAttributesKey TYPE_IDENTIFIER =
        createTextAttributesKey("ASTER_TYPE_IDENTIFIER", DefaultLanguageHighlighterColors.CLASS_NAME);

    public static final TextAttributesKey OPERATOR =
        createTextAttributesKey("ASTER_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    public static final TextAttributesKey PUNCTUATION =
        createTextAttributesKey("ASTER_PUNCTUATION", DefaultLanguageHighlighterColors.DOT);

    public static final TextAttributesKey PARENTHESES =
        createTextAttributesKey("ASTER_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);

    public static final TextAttributesKey BRACKETS =
        createTextAttributesKey("ASTER_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);

    public static final TextAttributesKey BAD_CHARACTER =
        createTextAttributesKey("ASTER_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    // 属性键数组（用于返回）
    private static final TextAttributesKey[] KEYWORD_KEYS = {KEYWORD};
    private static final TextAttributesKey[] STRING_KEYS = {STRING};
    private static final TextAttributesKey[] NUMBER_KEYS = {NUMBER};
    private static final TextAttributesKey[] COMMENT_KEYS = {COMMENT};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = {IDENTIFIER};
    private static final TextAttributesKey[] TYPE_IDENTIFIER_KEYS = {TYPE_IDENTIFIER};
    private static final TextAttributesKey[] OPERATOR_KEYS = {OPERATOR};
    private static final TextAttributesKey[] PUNCTUATION_KEYS = {PUNCTUATION};
    private static final TextAttributesKey[] PARENTHESES_KEYS = {PARENTHESES};
    private static final TextAttributesKey[] BRACKETS_KEYS = {BRACKETS};
    private static final TextAttributesKey[] BAD_CHARACTER_KEYS = {BAD_CHARACTER};
    private static final TextAttributesKey[] EMPTY_KEYS = {};

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new AsterLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == null) {
            return EMPTY_KEYS;
        }

        // 关键字
        if (AsterTokenTypes.KEYWORDS.contains(tokenType)) {
            return KEYWORD_KEYS;
        }

        // 字符串
        if (AsterTokenTypes.STRINGS.contains(tokenType)) {
            return STRING_KEYS;
        }

        // 数字
        if (AsterTokenTypes.NUMBERS.contains(tokenType)) {
            return NUMBER_KEYS;
        }

        // 注释
        if (AsterTokenTypes.COMMENTS.contains(tokenType)) {
            return COMMENT_KEYS;
        }

        // 类型标识符
        if (AsterTokenTypes.TYPE_IDENTIFIERS.contains(tokenType)) {
            return TYPE_IDENTIFIER_KEYS;
        }

        // 普通标识符
        if (AsterTokenTypes.IDENTIFIERS.contains(tokenType)) {
            return IDENTIFIER_KEYS;
        }

        // 运算符
        if (AsterTokenTypes.OPERATORS.contains(tokenType)) {
            return OPERATOR_KEYS;
        }

        // 标点符号
        if (AsterTokenTypes.PUNCTUATION.contains(tokenType)) {
            return PUNCTUATION_KEYS;
        }

        // 括号
        if (AsterTokenTypes.PARENS.contains(tokenType)) {
            return PARENTHESES_KEYS;
        }

        // 方括号
        if (AsterTokenTypes.BRACKETS.contains(tokenType)) {
            return BRACKETS_KEYS;
        }

        // 错误字符
        if (tokenType == AsterTokenTypes.BAD_CHARACTER) {
            return BAD_CHARACTER_KEYS;
        }

        return EMPTY_KEYS;
    }
}
