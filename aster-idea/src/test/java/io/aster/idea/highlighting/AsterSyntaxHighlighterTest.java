package io.aster.idea.highlighting;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import io.aster.idea.lang.AsterTokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterSyntaxHighlighter 单元测试
 */
@DisplayName("AsterSyntaxHighlighter")
class AsterSyntaxHighlighterTest {

    private AsterSyntaxHighlighter highlighter;

    @BeforeEach
    void setUp() {
        highlighter = new AsterSyntaxHighlighter();
    }

    @Nested
    @DisplayName("关键字高亮")
    class KeywordHighlighting {

        @Test
        @DisplayName("BOOL token 应使用关键字高亮")
        void shouldHighlightBoolAsKeyword() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.BOOL);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.KEYWORD, keys[0]);
        }

        @Test
        @DisplayName("NULL token 应使用关键字高亮")
        void shouldHighlightNullAsKeyword() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.NULL);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.KEYWORD, keys[0]);
        }

        @Test
        @DisplayName("KEYWORD token 应使用关键字高亮")
        void shouldHighlightKeywordToken() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.KEYWORD);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.KEYWORD, keys[0]);
        }
    }

    @Nested
    @DisplayName("字符串高亮")
    class StringHighlighting {

        @Test
        @DisplayName("STRING token 应使用字符串高亮")
        void shouldHighlightString() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.STRING);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.STRING, keys[0]);
        }
    }

    @Nested
    @DisplayName("数字高亮")
    class NumberHighlighting {

        @Test
        @DisplayName("INT token 应使用数字高亮")
        void shouldHighlightInt() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.INT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.NUMBER, keys[0]);
        }

        @Test
        @DisplayName("FLOAT token 应使用数字高亮")
        void shouldHighlightFloat() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.FLOAT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.NUMBER, keys[0]);
        }

        @Test
        @DisplayName("LONG token 应使用数字高亮")
        void shouldHighlightLong() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.LONG);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.NUMBER, keys[0]);
        }
    }

    @Nested
    @DisplayName("注释高亮")
    class CommentHighlighting {

        @Test
        @DisplayName("COMMENT token 应使用注释高亮")
        void shouldHighlightComment() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.COMMENT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.COMMENT, keys[0]);
        }
    }

    @Nested
    @DisplayName("标识符高亮")
    class IdentifierHighlighting {

        @Test
        @DisplayName("IDENT token 应使用标识符高亮")
        void shouldHighlightIdentifier() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.IDENT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.IDENTIFIER, keys[0]);
        }

        @Test
        @DisplayName("TYPE_IDENT token 应使用类型标识符高亮")
        void shouldHighlightTypeIdentifier() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.TYPE_IDENT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.TYPE_IDENTIFIER, keys[0]);
        }
    }

    @Nested
    @DisplayName("运算符高亮")
    class OperatorHighlighting {

        @Test
        @DisplayName("PLUS token 应使用运算符高亮")
        void shouldHighlightPlus() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.PLUS);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.OPERATOR, keys[0]);
        }

        @Test
        @DisplayName("MINUS token 应使用运算符高亮")
        void shouldHighlightMinus() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.MINUS);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.OPERATOR, keys[0]);
        }

        @Test
        @DisplayName("STAR token 应使用运算符高亮")
        void shouldHighlightStar() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.STAR);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.OPERATOR, keys[0]);
        }

        @Test
        @DisplayName("SLASH token 应使用运算符高亮")
        void shouldHighlightSlash() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.SLASH);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.OPERATOR, keys[0]);
        }

        @Test
        @DisplayName("比较运算符应使用运算符高亮")
        void shouldHighlightComparisonOperators() {
            assertAll(
                () -> assertEquals(AsterSyntaxHighlighter.OPERATOR,
                    highlighter.getTokenHighlights(AsterTokenTypes.LT)[0]),
                () -> assertEquals(AsterSyntaxHighlighter.OPERATOR,
                    highlighter.getTokenHighlights(AsterTokenTypes.GT)[0]),
                () -> assertEquals(AsterSyntaxHighlighter.OPERATOR,
                    highlighter.getTokenHighlights(AsterTokenTypes.LTE)[0]),
                () -> assertEquals(AsterSyntaxHighlighter.OPERATOR,
                    highlighter.getTokenHighlights(AsterTokenTypes.GTE)[0]),
                () -> assertEquals(AsterSyntaxHighlighter.OPERATOR,
                    highlighter.getTokenHighlights(AsterTokenTypes.NEQ)[0])
            );
        }
    }

    @Nested
    @DisplayName("标点符号高亮")
    class PunctuationHighlighting {

        @Test
        @DisplayName("DOT token 应使用标点高亮")
        void shouldHighlightDot() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.DOT);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.PUNCTUATION, keys[0]);
        }

        @Test
        @DisplayName("COLON token 应使用标点高亮")
        void shouldHighlightColon() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.COLON);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.PUNCTUATION, keys[0]);
        }

        @Test
        @DisplayName("COMMA token 应使用标点高亮")
        void shouldHighlightComma() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.COMMA);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.PUNCTUATION, keys[0]);
        }
    }

    @Nested
    @DisplayName("括号高亮")
    class BracketHighlighting {

        @Test
        @DisplayName("LPAREN token 应使用括号高亮")
        void shouldHighlightLeftParen() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.LPAREN);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.PARENTHESES, keys[0]);
        }

        @Test
        @DisplayName("RPAREN token 应使用括号高亮")
        void shouldHighlightRightParen() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.RPAREN);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.PARENTHESES, keys[0]);
        }

        @Test
        @DisplayName("LBRACKET token 应使用方括号高亮")
        void shouldHighlightLeftBracket() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.LBRACKET);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.BRACKETS, keys[0]);
        }

        @Test
        @DisplayName("RBRACKET token 应使用方括号高亮")
        void shouldHighlightRightBracket() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.RBRACKET);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.BRACKETS, keys[0]);
        }
    }

    @Nested
    @DisplayName("错误字符高亮")
    class BadCharacterHighlighting {

        @Test
        @DisplayName("BAD_CHARACTER token 应使用错误高亮")
        void shouldHighlightBadCharacter() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.BAD_CHARACTER);

            assertEquals(1, keys.length);
            assertEquals(AsterSyntaxHighlighter.BAD_CHARACTER, keys[0]);
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("null token 应返回空数组")
        void shouldReturnEmptyArrayForNullToken() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(null);

            assertEquals(0, keys.length);
        }

        @Test
        @DisplayName("未知 token 类型应返回空数组")
        void shouldReturnEmptyArrayForUnknownToken() {
            TextAttributesKey[] keys = highlighter.getTokenHighlights(AsterTokenTypes.NEWLINE);

            assertEquals(0, keys.length);
        }

        @Test
        @DisplayName("getHighlightingLexer 应返回 AsterLexerAdapter 实例")
        void shouldReturnAsterLexerAdapter() {
            assertInstanceOf(
                io.aster.idea.lang.AsterLexerAdapter.class,
                highlighter.getHighlightingLexer()
            );
        }
    }
}
