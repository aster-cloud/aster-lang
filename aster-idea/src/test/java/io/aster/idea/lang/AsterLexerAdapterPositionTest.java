package io.aster.idea.lang;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 测试 AsterLexerAdapter 的 token 位置计算
 */
public class AsterLexerAdapterPositionTest {

    @Test
    public void testFuncTokenPositions() {
        AsterLexerAdapter lexer = new AsterLexerAdapter();
        String source = "func add";

        lexer.start(source, 0, source.length(), 0);

        // 第一个 token: "func"
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals("Token 'func' start should be 0", 0, lexer.getTokenStart());
        assertEquals("Token 'func' end should be 4 (exclusive)", 4, lexer.getTokenEnd());

        String funcText = source.substring(lexer.getTokenStart(), lexer.getTokenEnd());
        assertEquals("Token text should be 'func'", "func", funcText);

        lexer.advance();

        // 第二个 token: 空格 (合成的 WHITESPACE)
        assertEquals(AsterTokenTypes.WHITE_SPACE, lexer.getTokenType());
        assertEquals("Whitespace start should be 4", 4, lexer.getTokenStart());
        assertEquals("Whitespace end should be 5", 5, lexer.getTokenEnd());

        lexer.advance();

        // 第三个 token: "add"
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals("Token 'add' start should be 5", 5, lexer.getTokenStart());
        assertEquals("Token 'add' end should be 8 (exclusive)", 8, lexer.getTokenEnd());

        String addText = source.substring(lexer.getTokenStart(), lexer.getTokenEnd());
        assertEquals("Token text should be 'add'", "add", addText);
    }

    @Test
    public void testEnumTokenPositions() {
        AsterLexerAdapter lexer = new AsterLexerAdapter();
        String source = "enum Color";

        lexer.start(source, 0, source.length(), 0);

        // 第一个 token: "enum"
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals("Token 'enum' start should be 0", 0, lexer.getTokenStart());
        assertEquals("Token 'enum' end should be 4 (exclusive)", 4, lexer.getTokenEnd());

        String enumText = source.substring(lexer.getTokenStart(), lexer.getTokenEnd());
        assertEquals("Token text should be 'enum'", "enum", enumText);
    }

    @Test
    public void testMultiLineTokenPositions() {
        AsterLexerAdapter lexer = new AsterLexerAdapter();
        String source = "func test\n  return 1";

        lexer.start(source, 0, source.length(), 0);

        // func
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals(0, lexer.getTokenStart());
        assertEquals(4, lexer.getTokenEnd());
        assertEquals("func", source.substring(lexer.getTokenStart(), lexer.getTokenEnd()));

        lexer.advance();

        // 空格 (合成的 WHITESPACE)
        assertEquals(AsterTokenTypes.WHITE_SPACE, lexer.getTokenType());
        assertEquals(4, lexer.getTokenStart());
        assertEquals(5, lexer.getTokenEnd());

        lexer.advance();

        // test
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals(5, lexer.getTokenStart());
        assertEquals(9, lexer.getTokenEnd());
        assertEquals("test", source.substring(lexer.getTokenStart(), lexer.getTokenEnd()));

        lexer.advance();

        // 换行符
        assertEquals(AsterTokenTypes.NEWLINE, lexer.getTokenType());
        assertEquals(9, lexer.getTokenStart());
        assertEquals(10, lexer.getTokenEnd());

        lexer.advance();

        // 缩进空格 (合成的 WHITESPACE)
        assertEquals(AsterTokenTypes.WHITE_SPACE, lexer.getTokenType());
        assertEquals(10, lexer.getTokenStart());
        assertEquals(12, lexer.getTokenEnd());

        lexer.advance();

        // return
        assertEquals(AsterTokenTypes.IDENT, lexer.getTokenType());
        assertEquals("return", source.substring(lexer.getTokenStart(), lexer.getTokenEnd()));
    }
}
