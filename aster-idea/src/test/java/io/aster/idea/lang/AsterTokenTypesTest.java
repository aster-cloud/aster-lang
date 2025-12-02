package io.aster.idea.lang;

import aster.core.lexer.TokenKind;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterTokenTypes 单元测试
 */
@DisplayName("AsterTokenTypes")
class AsterTokenTypesTest {

    @Nested
    @DisplayName("Token 类型定义")
    class TokenTypeDefinitions {

        @Test
        @DisplayName("所有 token 类型应不为 null")
        void allTokenTypesShouldNotBeNull() {
            assertAll(
                () -> assertNotNull(AsterTokenTypes.EOF, "EOF"),
                () -> assertNotNull(AsterTokenTypes.NEWLINE, "NEWLINE"),
                () -> assertNotNull(AsterTokenTypes.INDENT, "INDENT"),
                () -> assertNotNull(AsterTokenTypes.DEDENT, "DEDENT"),
                () -> assertNotNull(AsterTokenTypes.DOT, "DOT"),
                () -> assertNotNull(AsterTokenTypes.COLON, "COLON"),
                () -> assertNotNull(AsterTokenTypes.COMMA, "COMMA"),
                () -> assertNotNull(AsterTokenTypes.LPAREN, "LPAREN"),
                () -> assertNotNull(AsterTokenTypes.RPAREN, "RPAREN"),
                () -> assertNotNull(AsterTokenTypes.LBRACKET, "LBRACKET"),
                () -> assertNotNull(AsterTokenTypes.RBRACKET, "RBRACKET"),
                () -> assertNotNull(AsterTokenTypes.EQUALS, "EQUALS"),
                () -> assertNotNull(AsterTokenTypes.PLUS, "PLUS"),
                () -> assertNotNull(AsterTokenTypes.STAR, "STAR"),
                () -> assertNotNull(AsterTokenTypes.MINUS, "MINUS"),
                () -> assertNotNull(AsterTokenTypes.SLASH, "SLASH"),
                () -> assertNotNull(AsterTokenTypes.LT, "LT"),
                () -> assertNotNull(AsterTokenTypes.GT, "GT"),
                () -> assertNotNull(AsterTokenTypes.LTE, "LTE"),
                () -> assertNotNull(AsterTokenTypes.GTE, "GTE"),
                () -> assertNotNull(AsterTokenTypes.NEQ, "NEQ"),
                () -> assertNotNull(AsterTokenTypes.QUESTION, "QUESTION"),
                () -> assertNotNull(AsterTokenTypes.AT, "AT"),
                () -> assertNotNull(AsterTokenTypes.IDENT, "IDENT"),
                () -> assertNotNull(AsterTokenTypes.TYPE_IDENT, "TYPE_IDENT"),
                () -> assertNotNull(AsterTokenTypes.STRING, "STRING"),
                () -> assertNotNull(AsterTokenTypes.INT, "INT"),
                () -> assertNotNull(AsterTokenTypes.FLOAT, "FLOAT"),
                () -> assertNotNull(AsterTokenTypes.LONG, "LONG"),
                () -> assertNotNull(AsterTokenTypes.BOOL, "BOOL"),
                () -> assertNotNull(AsterTokenTypes.NULL, "NULL"),
                () -> assertNotNull(AsterTokenTypes.KEYWORD, "KEYWORD"),
                () -> assertNotNull(AsterTokenTypes.COMMENT, "COMMENT"),
                () -> assertNotNull(AsterTokenTypes.WHITE_SPACE, "WHITE_SPACE"),
                () -> assertNotNull(AsterTokenTypes.BAD_CHARACTER, "BAD_CHARACTER")
            );
        }
    }

    @Nested
    @DisplayName("TokenKind 映射")
    class TokenKindMapping {

        @Test
        @DisplayName("EOF 映射正确")
        void shouldMapEof() {
            assertEquals(AsterTokenTypes.EOF, AsterTokenTypes.getElementType(TokenKind.EOF));
        }

        @Test
        @DisplayName("NEWLINE 映射正确")
        void shouldMapNewline() {
            assertEquals(AsterTokenTypes.NEWLINE, AsterTokenTypes.getElementType(TokenKind.NEWLINE));
        }

        @Test
        @DisplayName("INDENT 映射正确")
        void shouldMapIndent() {
            assertEquals(AsterTokenTypes.INDENT, AsterTokenTypes.getElementType(TokenKind.INDENT));
        }

        @Test
        @DisplayName("DEDENT 映射正确")
        void shouldMapDedent() {
            assertEquals(AsterTokenTypes.DEDENT, AsterTokenTypes.getElementType(TokenKind.DEDENT));
        }

        @Test
        @DisplayName("标点符号映射正确")
        void shouldMapPunctuation() {
            assertAll(
                () -> assertEquals(AsterTokenTypes.DOT, AsterTokenTypes.getElementType(TokenKind.DOT)),
                () -> assertEquals(AsterTokenTypes.COLON, AsterTokenTypes.getElementType(TokenKind.COLON)),
                () -> assertEquals(AsterTokenTypes.COMMA, AsterTokenTypes.getElementType(TokenKind.COMMA)),
                () -> assertEquals(AsterTokenTypes.LPAREN, AsterTokenTypes.getElementType(TokenKind.LPAREN)),
                () -> assertEquals(AsterTokenTypes.RPAREN, AsterTokenTypes.getElementType(TokenKind.RPAREN)),
                () -> assertEquals(AsterTokenTypes.LBRACKET, AsterTokenTypes.getElementType(TokenKind.LBRACKET)),
                () -> assertEquals(AsterTokenTypes.RBRACKET, AsterTokenTypes.getElementType(TokenKind.RBRACKET))
            );
        }

        @Test
        @DisplayName("运算符映射正确")
        void shouldMapOperators() {
            assertAll(
                () -> assertEquals(AsterTokenTypes.EQUALS, AsterTokenTypes.getElementType(TokenKind.EQUALS)),
                () -> assertEquals(AsterTokenTypes.PLUS, AsterTokenTypes.getElementType(TokenKind.PLUS)),
                () -> assertEquals(AsterTokenTypes.STAR, AsterTokenTypes.getElementType(TokenKind.STAR)),
                () -> assertEquals(AsterTokenTypes.MINUS, AsterTokenTypes.getElementType(TokenKind.MINUS)),
                () -> assertEquals(AsterTokenTypes.SLASH, AsterTokenTypes.getElementType(TokenKind.SLASH)),
                () -> assertEquals(AsterTokenTypes.LT, AsterTokenTypes.getElementType(TokenKind.LT)),
                () -> assertEquals(AsterTokenTypes.GT, AsterTokenTypes.getElementType(TokenKind.GT)),
                () -> assertEquals(AsterTokenTypes.LTE, AsterTokenTypes.getElementType(TokenKind.LTE)),
                () -> assertEquals(AsterTokenTypes.GTE, AsterTokenTypes.getElementType(TokenKind.GTE)),
                () -> assertEquals(AsterTokenTypes.NEQ, AsterTokenTypes.getElementType(TokenKind.NEQ)),
                () -> assertEquals(AsterTokenTypes.QUESTION, AsterTokenTypes.getElementType(TokenKind.QUESTION)),
                () -> assertEquals(AsterTokenTypes.AT, AsterTokenTypes.getElementType(TokenKind.AT))
            );
        }

        @Test
        @DisplayName("标识符映射正确")
        void shouldMapIdentifiers() {
            assertAll(
                () -> assertEquals(AsterTokenTypes.IDENT, AsterTokenTypes.getElementType(TokenKind.IDENT)),
                () -> assertEquals(AsterTokenTypes.TYPE_IDENT, AsterTokenTypes.getElementType(TokenKind.TYPE_IDENT))
            );
        }

        @Test
        @DisplayName("字面量映射正确")
        void shouldMapLiterals() {
            assertAll(
                () -> assertEquals(AsterTokenTypes.STRING, AsterTokenTypes.getElementType(TokenKind.STRING)),
                () -> assertEquals(AsterTokenTypes.INT, AsterTokenTypes.getElementType(TokenKind.INT)),
                () -> assertEquals(AsterTokenTypes.FLOAT, AsterTokenTypes.getElementType(TokenKind.FLOAT)),
                () -> assertEquals(AsterTokenTypes.LONG, AsterTokenTypes.getElementType(TokenKind.LONG)),
                () -> assertEquals(AsterTokenTypes.BOOL, AsterTokenTypes.getElementType(TokenKind.BOOL)),
                () -> assertEquals(AsterTokenTypes.NULL, AsterTokenTypes.getElementType(TokenKind.NULL))
            );
        }

        @Test
        @DisplayName("关键字和注释映射正确")
        void shouldMapKeywordAndComment() {
            assertAll(
                () -> assertEquals(AsterTokenTypes.KEYWORD, AsterTokenTypes.getElementType(TokenKind.KEYWORD)),
                () -> assertEquals(AsterTokenTypes.COMMENT, AsterTokenTypes.getElementType(TokenKind.COMMENT))
            );
        }
    }

    @Nested
    @DisplayName("TokenSet 定义")
    class TokenSetDefinitions {

        @Test
        @DisplayName("COMMENTS TokenSet 应包含 COMMENT")
        void commentsTokenSetShouldContainComment() {
            assertTrue(AsterTokenTypes.COMMENTS.contains(AsterTokenTypes.COMMENT));
        }

        @Test
        @DisplayName("WHITESPACES TokenSet 应包含空白 token")
        void whitespacesTokenSetShouldContainWhitespace() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.WHITESPACES.contains(AsterTokenTypes.WHITE_SPACE)),
                () -> assertTrue(AsterTokenTypes.WHITESPACES.contains(AsterTokenTypes.NEWLINE))
            );
        }

        @Test
        @DisplayName("STRINGS TokenSet 应包含 STRING")
        void stringsTokenSetShouldContainString() {
            assertTrue(AsterTokenTypes.STRINGS.contains(AsterTokenTypes.STRING));
        }

        @Test
        @DisplayName("NUMBERS TokenSet 应包含数字 token")
        void numbersTokenSetShouldContainNumbers() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.NUMBERS.contains(AsterTokenTypes.INT)),
                () -> assertTrue(AsterTokenTypes.NUMBERS.contains(AsterTokenTypes.FLOAT)),
                () -> assertTrue(AsterTokenTypes.NUMBERS.contains(AsterTokenTypes.LONG))
            );
        }

        @Test
        @DisplayName("KEYWORDS TokenSet 应包含关键字 token")
        void keywordsTokenSetShouldContainKeywords() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.KEYWORDS.contains(AsterTokenTypes.KEYWORD)),
                () -> assertTrue(AsterTokenTypes.KEYWORDS.contains(AsterTokenTypes.BOOL)),
                () -> assertTrue(AsterTokenTypes.KEYWORDS.contains(AsterTokenTypes.NULL))
            );
        }

        @Test
        @DisplayName("IDENTIFIERS TokenSet 应包含 IDENT")
        void identifiersTokenSetShouldContainIdent() {
            assertTrue(AsterTokenTypes.IDENTIFIERS.contains(AsterTokenTypes.IDENT));
        }

        @Test
        @DisplayName("TYPE_IDENTIFIERS TokenSet 应包含 TYPE_IDENT")
        void typeIdentifiersTokenSetShouldContainTypeIdent() {
            assertTrue(AsterTokenTypes.TYPE_IDENTIFIERS.contains(AsterTokenTypes.TYPE_IDENT));
        }

        @Test
        @DisplayName("OPERATORS TokenSet 应包含运算符 token")
        void operatorsTokenSetShouldContainOperators() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.EQUALS)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.PLUS)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.MINUS)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.STAR)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.SLASH)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.LT)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.GT)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.LTE)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.GTE)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.NEQ)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.QUESTION)),
                () -> assertTrue(AsterTokenTypes.OPERATORS.contains(AsterTokenTypes.AT))
            );
        }

        @Test
        @DisplayName("PUNCTUATION TokenSet 应包含标点 token")
        void punctuationTokenSetShouldContainPunctuation() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.PUNCTUATION.contains(AsterTokenTypes.DOT)),
                () -> assertTrue(AsterTokenTypes.PUNCTUATION.contains(AsterTokenTypes.COLON)),
                () -> assertTrue(AsterTokenTypes.PUNCTUATION.contains(AsterTokenTypes.COMMA))
            );
        }

        @Test
        @DisplayName("PARENS TokenSet 应包含括号 token")
        void parensTokenSetShouldContainParens() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.PARENS.contains(AsterTokenTypes.LPAREN)),
                () -> assertTrue(AsterTokenTypes.PARENS.contains(AsterTokenTypes.RPAREN))
            );
        }

        @Test
        @DisplayName("BRACKETS TokenSet 应包含方括号 token")
        void bracketsTokenSetShouldContainBrackets() {
            assertAll(
                () -> assertTrue(AsterTokenTypes.BRACKETS.contains(AsterTokenTypes.LBRACKET)),
                () -> assertTrue(AsterTokenTypes.BRACKETS.contains(AsterTokenTypes.RBRACKET))
            );
        }
    }

    @Nested
    @DisplayName("Token 类型属性")
    class TokenTypeProperties {

        @Test
        @DisplayName("所有 AsterTokenType 应属于 Aster 语言")
        void asterTokenTypesShouldBelongToAsterLanguage() {
            IElementType ident = AsterTokenTypes.IDENT;
            assertInstanceOf(AsterTokenType.class, ident);

            String debugName = ident.toString();
            assertTrue(debugName.contains("IDENT"), "应包含 token 名称");
        }
    }
}
