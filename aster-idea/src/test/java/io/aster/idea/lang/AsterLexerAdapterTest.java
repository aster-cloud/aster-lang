package io.aster.idea.lang;

import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsterLexerAdapter 单元测试
 */
@DisplayName("AsterLexerAdapter")
class AsterLexerAdapterTest {

    private AsterLexerAdapter lexer;

    @BeforeEach
    void setUp() {
        lexer = new AsterLexerAdapter();
    }

    /**
     * 辅助方法：收集所有 token
     */
    private List<TokenInfo> collectTokens(String source) {
        lexer.start(source, 0, source.length(), 0);
        List<TokenInfo> tokens = new ArrayList<>();

        while (lexer.getTokenType() != null) {
            tokens.add(new TokenInfo(
                lexer.getTokenType(),
                lexer.getTokenStart(),
                lexer.getTokenEnd(),
                source.substring(lexer.getTokenStart(), lexer.getTokenEnd())
            ));
            lexer.advance();
        }

        return tokens;
    }

    record TokenInfo(IElementType type, int start, int end, String text) {}

    @Nested
    @DisplayName("标识符词法分析")
    class IdentifierLexing {

        @Test
        @DisplayName("识别小写标识符")
        void shouldRecognizeLowercaseIdentifier() {
            List<TokenInfo> tokens = collectTokens("hello");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.IDENT, tokens.get(0).type());
            assertEquals("hello", tokens.get(0).text());
        }

        @Test
        @DisplayName("识别类型标识符（首字母大写）")
        void shouldRecognizeTypeIdentifier() {
            List<TokenInfo> tokens = collectTokens("Person");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.TYPE_IDENT, tokens.get(0).type());
            assertEquals("Person", tokens.get(0).text());
        }

        @Test
        @DisplayName("识别带下划线的标识符")
        void shouldRecognizeIdentifierWithUnderscore() {
            List<TokenInfo> tokens = collectTokens("user_name");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.IDENT, tokens.get(0).type());
            assertEquals("user_name", tokens.get(0).text());
        }
    }

    @Nested
    @DisplayName("字面量词法分析")
    class LiteralLexing {

        @Test
        @DisplayName("识别整数字面量")
        void shouldRecognizeIntegerLiteral() {
            List<TokenInfo> tokens = collectTokens("42");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.INT, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别浮点数字面量")
        void shouldRecognizeFloatLiteral() {
            List<TokenInfo> tokens = collectTokens("3.14");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.FLOAT, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别长整数字面量")
        void shouldRecognizeLongLiteral() {
            List<TokenInfo> tokens = collectTokens("100L");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.LONG, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别字符串字面量")
        void shouldRecognizeStringLiteral() {
            List<TokenInfo> tokens = collectTokens("\"hello world\"");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.STRING, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别布尔字面量 true")
        void shouldRecognizeTrueLiteral() {
            List<TokenInfo> tokens = collectTokens("true");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.BOOL, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别布尔字面量 false")
        void shouldRecognizeFalseLiteral() {
            List<TokenInfo> tokens = collectTokens("false");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.BOOL, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别 null 字面量")
        void shouldRecognizeNullLiteral() {
            List<TokenInfo> tokens = collectTokens("null");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.NULL, tokens.get(0).type());
        }
    }

    @Nested
    @DisplayName("运算符词法分析")
    class OperatorLexing {

        @Test
        @DisplayName("识别算术运算符")
        void shouldRecognizeArithmeticOperators() {
            List<TokenInfo> tokens = collectTokens("+ - * /");

            // 过滤掉空白
            List<TokenInfo> operatorTokens = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE)
                .toList();

            assertEquals(4, operatorTokens.size());
            assertEquals(AsterTokenTypes.PLUS, operatorTokens.get(0).type());
            assertEquals(AsterTokenTypes.MINUS, operatorTokens.get(1).type());
            assertEquals(AsterTokenTypes.STAR, operatorTokens.get(2).type());
            assertEquals(AsterTokenTypes.SLASH, operatorTokens.get(3).type());
        }

        @Test
        @DisplayName("识别比较运算符")
        void shouldRecognizeComparisonOperators() {
            List<TokenInfo> tokens = collectTokens("< > <= >= !=");

            List<TokenInfo> operatorTokens = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE)
                .toList();

            assertEquals(5, operatorTokens.size());
            assertEquals(AsterTokenTypes.LT, operatorTokens.get(0).type());
            assertEquals(AsterTokenTypes.GT, operatorTokens.get(1).type());
            assertEquals(AsterTokenTypes.LTE, operatorTokens.get(2).type());
            assertEquals(AsterTokenTypes.GTE, operatorTokens.get(3).type());
            assertEquals(AsterTokenTypes.NEQ, operatorTokens.get(4).type());
        }

        @Test
        @DisplayName("识别赋值运算符")
        void shouldRecognizeAssignmentOperator() {
            List<TokenInfo> tokens = collectTokens("=");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.EQUALS, tokens.get(0).type());
        }
    }

    @Nested
    @DisplayName("标点符号词法分析")
    class PunctuationLexing {

        @Test
        @DisplayName("识别括号")
        void shouldRecognizeParentheses() {
            List<TokenInfo> tokens = collectTokens("()");

            assertEquals(2, tokens.size());
            assertEquals(AsterTokenTypes.LPAREN, tokens.get(0).type());
            assertEquals(AsterTokenTypes.RPAREN, tokens.get(1).type());
        }

        @Test
        @DisplayName("识别方括号")
        void shouldRecognizeBrackets() {
            List<TokenInfo> tokens = collectTokens("[]");

            assertEquals(2, tokens.size());
            assertEquals(AsterTokenTypes.LBRACKET, tokens.get(0).type());
            assertEquals(AsterTokenTypes.RBRACKET, tokens.get(1).type());
        }

        @Test
        @DisplayName("识别标点符号")
        void shouldRecognizePunctuation() {
            List<TokenInfo> tokens = collectTokens(". : ,");

            List<TokenInfo> punctTokens = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE)
                .toList();

            assertEquals(3, punctTokens.size());
            assertEquals(AsterTokenTypes.DOT, punctTokens.get(0).type());
            assertEquals(AsterTokenTypes.COLON, punctTokens.get(1).type());
            assertEquals(AsterTokenTypes.COMMA, punctTokens.get(2).type());
        }
    }

    @Nested
    @DisplayName("注释词法分析")
    class CommentLexing {

        @Test
        @DisplayName("识别 # 风格注释")
        void shouldRecognizeHashComment() {
            List<TokenInfo> tokens = collectTokens("# this is a comment");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.COMMENT, tokens.get(0).type());
        }

        @Test
        @DisplayName("识别 // 风格注释")
        void shouldRecognizeSlashComment() {
            List<TokenInfo> tokens = collectTokens("// this is a comment");

            assertEquals(1, tokens.size());
            assertEquals(AsterTokenTypes.COMMENT, tokens.get(0).type());
        }
    }

    @Nested
    @DisplayName("复杂表达式词法分析")
    class ComplexExpressionLexing {

        @Test
        @DisplayName("分析函数调用表达式")
        void shouldLexFunctionCallExpression() {
            List<TokenInfo> tokens = collectTokens("add(1, 2)");

            List<TokenInfo> nonWhitespaceTokens = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE)
                .toList();

            assertEquals(6, nonWhitespaceTokens.size());
            assertEquals(AsterTokenTypes.IDENT, nonWhitespaceTokens.get(0).type()); // add
            assertEquals(AsterTokenTypes.LPAREN, nonWhitespaceTokens.get(1).type()); // (
            assertEquals(AsterTokenTypes.INT, nonWhitespaceTokens.get(2).type()); // 1
            assertEquals(AsterTokenTypes.COMMA, nonWhitespaceTokens.get(3).type()); // ,
            assertEquals(AsterTokenTypes.INT, nonWhitespaceTokens.get(4).type()); // 2
            assertEquals(AsterTokenTypes.RPAREN, nonWhitespaceTokens.get(5).type()); // )
        }

        @Test
        @DisplayName("分析属性访问表达式")
        void shouldLexPropertyAccessExpression() {
            List<TokenInfo> tokens = collectTokens("person.name");

            assertEquals(3, tokens.size());
            assertEquals(AsterTokenTypes.IDENT, tokens.get(0).type()); // person
            assertEquals(AsterTokenTypes.DOT, tokens.get(1).type()); // .
            assertEquals(AsterTokenTypes.IDENT, tokens.get(2).type()); // name
        }

        @Test
        @DisplayName("分析类型注解表达式")
        void shouldLexTypeAnnotationExpression() {
            List<TokenInfo> tokens = collectTokens("name: Text");

            List<TokenInfo> nonWhitespaceTokens = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE)
                .toList();

            assertEquals(3, nonWhitespaceTokens.size());
            assertEquals(AsterTokenTypes.IDENT, nonWhitespaceTokens.get(0).type()); // name
            assertEquals(AsterTokenTypes.COLON, nonWhitespaceTokens.get(1).type()); // :
            assertEquals(AsterTokenTypes.TYPE_IDENT, nonWhitespaceTokens.get(2).type()); // Text
        }
    }

    @Nested
    @DisplayName("缩进处理")
    class IndentationHandling {

        @Test
        @DisplayName("识别缩进 token")
        void shouldRecognizeIndentToken() {
            String source = "define greet\n  return \"hello\"";
            List<TokenInfo> tokens = collectTokens(source);

            boolean hasIndent = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.INDENT);

            assertTrue(hasIndent, "应识别 INDENT token");
        }

        @Test
        @DisplayName("识别换行 token")
        void shouldRecognizeNewlineToken() {
            String source = "line1\nline2";
            List<TokenInfo> tokens = collectTokens(source);

            boolean hasNewline = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.NEWLINE);

            assertTrue(hasNewline, "应识别 NEWLINE token");
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("处理空输入")
        void shouldHandleEmptyInput() {
            List<TokenInfo> tokens = collectTokens("");

            assertTrue(tokens.isEmpty(), "空输入应返回空 token 列表");
        }

        @Test
        @DisplayName("处理纯空白输入")
        void shouldHandleWhitespaceOnlyInput() {
            List<TokenInfo> tokens = collectTokens("   ");

            // 空白可能被跳过或返回 WHITE_SPACE token
            assertTrue(tokens.isEmpty() ||
                tokens.stream().allMatch(t -> t.type() == AsterTokenTypes.WHITE_SPACE));
        }

        @Test
        @DisplayName("getState 返回 0")
        void shouldReturnZeroState() {
            lexer.start("test", 0, 4, 0);
            assertEquals(0, lexer.getState(), "状态应始终为 0");
        }

        @Test
        @DisplayName("getBufferSequence 返回正确的缓冲区")
        void shouldReturnCorrectBufferSequence() {
            String source = "hello";
            lexer.start(source, 0, source.length(), 0);

            assertEquals(source, lexer.getBufferSequence().toString());
        }

        @Test
        @DisplayName("getBufferEnd 返回正确的结束位置")
        void shouldReturnCorrectBufferEnd() {
            String source = "hello";
            lexer.start(source, 0, source.length(), 0);

            assertEquals(source.length(), lexer.getBufferEnd());
        }
    }
}
