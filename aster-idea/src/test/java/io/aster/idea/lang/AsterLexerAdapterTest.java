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
        @DisplayName("INDENT/DEDENT 被过滤（零宽 token 不暴露给 IntelliJ）")
        void shouldFilterOutIndentDedentTokens() {
            // INDENT/DEDENT 是零宽 token，IntelliJ 要求 token 范围严格递增
            // 因此这些 token 被过滤掉，不会暴露给 IntelliJ
            String source = "define greet\n  return \"hello\"";
            List<TokenInfo> tokens = collectTokens(source);

            boolean hasIndent = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.INDENT);
            boolean hasDedent = tokens.stream()
                .anyMatch(t -> t.type() == AsterTokenTypes.DEDENT);

            assertFalse(hasIndent, "INDENT token 应被过滤（零宽 token）");
            assertFalse(hasDedent, "DEDENT token 应被过滤（零宽 token）");
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
    @DisplayName("增量高亮偏移量计算")
    class IncrementalHighlightingOffsets {

        /**
         * 辅助方法：收集指定范围内的 token
         */
        private List<TokenInfo> collectTokensInRange(String buffer, int startOffset, int endOffset) {
            lexer.start(buffer, startOffset, endOffset, 0);
            List<TokenInfo> tokens = new ArrayList<>();

            while (lexer.getTokenType() != null) {
                int tokenStart = lexer.getTokenStart();
                int tokenEnd = lexer.getTokenEnd();
                // 使用 buffer 而不是切片来获取文本
                String text = buffer.substring(
                    Math.max(0, tokenStart),
                    Math.min(buffer.length(), tokenEnd)
                );
                tokens.add(new TokenInfo(lexer.getTokenType(), tokenStart, tokenEnd, text));
                lexer.advance();
            }

            return tokens;
        }

        @Test
        @DisplayName("startOffset 非零时偏移量应正确（增量高亮场景）")
        void shouldCalculateCorrectOffsetsWhenStartOffsetIsNonZero() {
            // 模拟增量高亮：buffer 包含完整内容，但只分析部分
            String fullBuffer = "# header\nlet x = 42\n# footer";
            int startOffset = 9;  // 从 "let x = 42" 开始
            int endOffset = 20;   // 到 "let x = 42" 结束

            List<TokenInfo> tokens = collectTokensInRange(fullBuffer, startOffset, endOffset);

            // 验证所有 token 偏移量在 [startOffset, endOffset] 范围内
            for (TokenInfo token : tokens) {
                assertTrue(token.start() >= startOffset,
                    String.format("Token '%s' 起始位置 %d 应 >= startOffset %d",
                        token.text(), token.start(), startOffset));
                assertTrue(token.end() <= endOffset,
                    String.format("Token '%s' 结束位置 %d 应 <= endOffset %d",
                        token.text(), token.end(), endOffset));
            }
        }

        @Test
        @DisplayName("多行切片的偏移量应正确")
        void shouldCalculateCorrectOffsetsForMultilineSlice() {
            // 多行 buffer，分析中间部分
            String fullBuffer = "line1\nlet x = 1\nlet y = 2\nline4";
            int startOffset = 6;  // 从 "let x = 1" 开始
            int endOffset = 24;   // 到 "let y = 2" 结束

            List<TokenInfo> tokens = collectTokensInRange(fullBuffer, startOffset, endOffset);

            // 验证偏移量在有效范围内
            for (TokenInfo token : tokens) {
                assertTrue(token.start() >= startOffset && token.start() < endOffset,
                    String.format("Token '%s' 起始位置 %d 应在 [%d, %d) 范围内",
                        token.text(), token.start(), startOffset, endOffset));
                assertTrue(token.end() >= startOffset && token.end() <= endOffset,
                    String.format("Token '%s' 结束位置 %d 应在 [%d, %d] 范围内",
                        token.text(), token.end(), startOffset, endOffset));
            }
        }

        @Test
        @DisplayName("偏移量应与 buffer 内容对应")
        void shouldMatchBufferContent() {
            String fullBuffer = "abc\nhello world\nxyz";
            int startOffset = 4;  // 从 "hello world" 开始
            int endOffset = 15;   // 到 "hello world" 结束

            List<TokenInfo> tokens = collectTokensInRange(fullBuffer, startOffset, endOffset);

            // 验证有 token 被解析
            assertFalse(tokens.isEmpty(), "应解析出 token");

            // 第一个非空白 token 应从 startOffset 开始
            TokenInfo firstToken = tokens.stream()
                .filter(t -> t.type() != AsterTokenTypes.WHITE_SPACE &&
                             t.type() != AsterTokenTypes.NEWLINE)
                .findFirst()
                .orElse(null);

            assertNotNull(firstToken, "应识别 token");
            // 验证第一个 token 偏移量正确
            assertTrue(firstToken.start() >= startOffset,
                "第一个 token 起始位置应 >= startOffset");
            assertTrue(firstToken.end() <= endOffset,
                "第一个 token 结束位置应 <= endOffset");
        }

        @Test
        @DisplayName("第二行起始的切片偏移量应正确")
        void shouldHandleSliceStartingFromSecondLine() {
            String fullBuffer = "first_line\nsecond_line";
            int startOffset = 11;  // 从第二行开始
            int endOffset = fullBuffer.length();

            List<TokenInfo> tokens = collectTokensInRange(fullBuffer, startOffset, endOffset);

            // 第一个 token 应从 startOffset 开始
            if (!tokens.isEmpty()) {
                assertEquals(startOffset, tokens.get(0).start(),
                    "第一个 token 应从 startOffset 开始");
            }
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
