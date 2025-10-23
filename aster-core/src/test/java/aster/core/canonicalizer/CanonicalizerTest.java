package aster.core.canonicalizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Canonicalizer 单元测试
 * <p>
 * 验证 Canonicalizer 的各项规范化功能是否正确。
 */
class CanonicalizerTest {

    private Canonicalizer canonicalizer;

    @BeforeEach
    void setUp() {
        canonicalizer = new Canonicalizer();
    }

    // ============================================================
    // 换行符规范化测试
    // ============================================================

    @Test
    void testNormalizeNewlines_CRLF() {
        String input = "line1\r\nline2\r\nline3";
        String expected = "line1\nline2\nline3";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testNormalizeNewlines_CR() {
        String input = "line1\rline2\rline3";
        String expected = "line1\nline2\nline3";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testNormalizeNewlines_Mixed() {
        String input = "line1\r\nline2\nline3\rline4";
        String expected = "line1\nline2\nline3\nline4";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 制表符转换测试
    // ============================================================

    @Test
    void testTabsToSpaces() {
        String input = "\tIndented with tab";
        String expected = "  Indented with tab";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testMultipleTabs() {
        String input = "\t\tDouble indented";
        String expected = "    Double indented";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 行注释移除测试
    // ============================================================

    @Test
    void testRemoveLineComments_DoubleSlash() {
        String input = "code line\n// comment\nmore code";
        String expected = "code line\n\nmore code";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveLineComments_Hash() {
        String input = "code line\n# comment\nmore code";
        String expected = "code line\n\nmore code";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveLineComments_Indented() {
        String input = "code\n  // indented comment\nmore";
        String expected = "code\n\nmore";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 智能引号规范化测试
    // ============================================================

    @Test
    void testSmartQuotesToStraight_DoubleQuotes() {
        String input = "Text with \u201Csmart quotes\u201D";  // Unicode 智能双引号
        String expected = "Text with \"smart quotes\"";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testSmartQuotesToStraight_SingleQuotes() {
        String input = "Text with \u2018smart quotes\u2019";  // Unicode 智能单引号
        String expected = "Text with 'smart quotes'";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 空白符规范化测试
    // ============================================================

    @Test
    void testCollapseMultipleSpaces() {
        String input = "word1    word2     word3";
        String expected = "word1 word2 word3";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveSpacesBeforePunctuation() {
        String input = "word1 , word2 . word3 :";
        String expected = "word1, word2. word3:";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testPreserveIndentation() {
        String input = "  indented   line  with   spaces";
        String expected = "  indented line with spaces";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 多词关键字大小写规范化测试
    // ============================================================

    @Test
    void testNormalizeMultiWordKeywords_ModuleIs() {
        String input = "This Module Is app.";
        String expected = "this module is app.";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testNormalizeMultiWordKeywords_OneOf() {
        String input = "As One Of the options.";
        String expected = "as one of options.";  // "the" 会被移除
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testNormalizeMultiWordKeywords_WaitFor() {
        String input = "Wait For the result.";
        String expected = "wait for result.";  // "the" 会被移除
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 冠词移除测试
    // ============================================================

    @Test
    void testRemoveArticles_Basic() {
        String input = "define the function to return a value";
        String expected = "define function to return value";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveArticles_PreserveInStrings() {
        String input = "print \"the quick brown fox\"";
        String expected = "print \"the quick brown fox\"";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveArticles_MixedContext() {
        String input = "call the function with \"the parameter\"";
        String expected = "call function with \"the parameter\"";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveArticles_AllArticleTypes() {
        String input = "a function takes an input and returns the result";
        String expected = "function takes input and returns result";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testRemoveArticles_OnlyWithTrailingSpace() {
        String input = "the function with parameter";
        // "the " 会被移除（正则匹配 \b(a|an|the)\b\s）
        String expected = "function with parameter";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 字符串字面量处理测试
    // ============================================================

    @Test
    void testEscapedQuotes() {
        String input = "print \"escaped \\\" quote\"";
        String expected = "print \"escaped \\\" quote\"";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testMultipleStrings() {
        String input = "the \"first\" and the \"second\"";
        String expected = "\"first\" and \"second\"";  // 字符串外的 the 被移除
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testPreserveSpacesInsideString() {
        String input = "Return \"foo  bar\".";
        String expected = "Return \"foo  bar\".";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    // ============================================================
    // 综合测试
    // ============================================================

    @Test
    void testCompleteExample() {
        String input = """
            This Module Is\tapp.
            // Comment line
            To greet,  produce   Text  :
              Return   "Hello,  the world"  .
            """;

        String expected = """
            this module is app.

            To greet, produce Text:
              Return "Hello,  the world".
            """;

        // 注释行保留空白占位，字符串内部空格保持原样
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testIdempotency() {
        String input = "the function returns a value";
        String first = canonicalizer.canonicalize(input);
        String second = canonicalizer.canonicalize(first);
        assertEquals(first, second, "规范化应该是幂等的");
    }

    @Test
    void testEmptyString() {
        String input = "";
        String expected = "";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }

    @Test
    void testWhitespaceOnlyLines() {
        String input = "line1\n   \nline2";
        String expected = "line1\n\nline2";
        assertEquals(expected, canonicalizer.canonicalize(input));
    }
}
