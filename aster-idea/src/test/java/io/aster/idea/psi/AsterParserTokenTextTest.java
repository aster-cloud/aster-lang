package io.aster.idea.psi;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.testFramework.ParsingTestCase;
import io.aster.idea.lang.AsterLexerAdapter;

/**
 * 测试 PsiBuilder 的 getTokenText 返回值
 */
public class AsterParserTokenTextTest extends ParsingTestCase {

    public AsterParserTokenTextTest() {
        super("parser", "aster", new AsterParserDefinition());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    public void testBuilderTokenText() {
        String source = "func add";
        AsterParserDefinition parserDef = new AsterParserDefinition();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
            parserDef,
            new AsterLexerAdapter(),
            source
        );

        System.out.println("=== PsiBuilder Token Text Test ===");
        System.out.println("Source: [" + source + "] (length=" + source.length() + ")");

        while (!builder.eof()) {
            String tokenText = builder.getTokenText();
            int offset = builder.getCurrentOffset();
            // PsiBuilder doesn't expose end offset directly, but we can infer from the text
            int inferredEnd = offset + (tokenText != null ? tokenText.length() : 0);
            System.out.printf("Token: type=%s, text='%s', range=[%d,%d)%n",
                builder.getTokenType(), tokenText, offset, inferredEnd);
            builder.advanceLexer();
        }
    }

    /**
     * 直接测试 AsterLexerAdapter 的 getTokenStart/getTokenEnd
     */
    public void testLexerAdapterDirectly() {
        String source = "func add";
        AsterLexerAdapter lexer = new AsterLexerAdapter();
        lexer.start(source, 0, source.length(), 0);

        System.out.println("=== AsterLexerAdapter Direct Test ===");
        System.out.println("Source: [" + source + "] (length=" + source.length() + ")");

        while (lexer.getTokenType() != null) {
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            String text = source.substring(start, Math.min(end, source.length()));
            System.out.printf("Token: type=%s, range=[%d,%d), text='%s'%n",
                lexer.getTokenType(), start, end, text);
            lexer.advance();
        }
    }

    /**
     * 测试增量高亮场景的偏移量计算
     */
    public void testIncrementalHighlighting() {
        String fullBuffer = "# header\nlet x = 42\n# footer";
        int startOffset = 9;
        int endOffset = 20;

        System.out.println("=== Incremental Highlighting Test ===");
        System.out.println("Full buffer: [" + fullBuffer + "]");
        System.out.println("Slice: [" + fullBuffer.substring(startOffset, endOffset) + "]");
        System.out.println("Range: [" + startOffset + ", " + endOffset + ")");

        AsterLexerAdapter lexer = new AsterLexerAdapter();
        lexer.start(fullBuffer, startOffset, endOffset, 0);

        while (lexer.getTokenType() != null) {
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            String text = fullBuffer.substring(start, Math.min(end, fullBuffer.length()));
            System.out.printf("Token: type=%s, range=[%d,%d), text='%s'%n",
                lexer.getTokenType(), start, end, text);
            if (end > endOffset) {
                System.out.println("  *** ERROR: end > endOffset!");
            }
            lexer.advance();
        }
    }
}
