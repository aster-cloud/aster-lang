package io.aster.idea.lang;

import org.junit.Test;

/**
 * Debug test to understand token positions
 */
public class AsterLexerDebugTest {

    @Test
    public void debugFuncAddTokens() {
        AsterLexerAdapter lexer = new AsterLexerAdapter();
        String source = "func add(a: Int, b: Int) -> Int:\n  return a + b";

        System.out.println("Source: [" + source + "]");
        System.out.println("Source length: " + source.length());
        System.out.println("\n=== All tokens ===");

        lexer.start(source, 0, source.length(), 0);

        int tokenNum = 0;
        while (lexer.getTokenType() != null) {
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            String text = source.substring(start, Math.min(end, source.length()));
            String escapedText = text.replace("\n", "\\n").replace("\r", "\\r");

            System.out.printf("Token %d: type=%s, range=[%d,%d), text='%s'%n",
                tokenNum++, lexer.getTokenType(), start, end, escapedText);

            lexer.advance();
        }
    }
}
