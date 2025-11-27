package io.aster.idea.lang;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import aster.core.lexer.TokenKind;

import java.util.HashMap;
import java.util.Map;

/**
 * Aster 语言 Token 类型定义
 * <p>
 * 映射 aster-core 的 TokenKind 到 IntelliJ IElementType
 */
public interface AsterTokenTypes {

    // 文件结束
    IElementType EOF = new AsterTokenType("EOF");

    // 缩进控制
    IElementType NEWLINE = new AsterTokenType("NEWLINE");
    IElementType INDENT = new AsterTokenType("INDENT");
    IElementType DEDENT = new AsterTokenType("DEDENT");

    // 标点符号
    IElementType DOT = new AsterTokenType("DOT");
    IElementType COLON = new AsterTokenType("COLON");
    IElementType COMMA = new AsterTokenType("COMMA");
    IElementType LPAREN = new AsterTokenType("LPAREN");
    IElementType RPAREN = new AsterTokenType("RPAREN");
    IElementType LBRACKET = new AsterTokenType("LBRACKET");
    IElementType RBRACKET = new AsterTokenType("RBRACKET");

    // 运算符
    IElementType EQUALS = new AsterTokenType("EQUALS");
    IElementType PLUS = new AsterTokenType("PLUS");
    IElementType STAR = new AsterTokenType("STAR");
    IElementType MINUS = new AsterTokenType("MINUS");
    IElementType SLASH = new AsterTokenType("SLASH");
    IElementType LT = new AsterTokenType("LT");
    IElementType GT = new AsterTokenType("GT");
    IElementType LTE = new AsterTokenType("LTE");
    IElementType GTE = new AsterTokenType("GTE");
    IElementType NEQ = new AsterTokenType("NEQ");
    IElementType QUESTION = new AsterTokenType("QUESTION");
    IElementType AT = new AsterTokenType("AT");

    // 标识符
    IElementType IDENT = new AsterTokenType("IDENT");
    IElementType TYPE_IDENT = new AsterTokenType("TYPE_IDENT");

    // 字面量
    IElementType STRING = new AsterTokenType("STRING");
    IElementType INT = new AsterTokenType("INT");
    IElementType FLOAT = new AsterTokenType("FLOAT");
    IElementType LONG = new AsterTokenType("LONG");
    IElementType BOOL = new AsterTokenType("BOOL");
    IElementType NULL = new AsterTokenType("NULL");

    // 关键字
    IElementType KEYWORD = new AsterTokenType("KEYWORD");

    // 注释
    IElementType COMMENT = new AsterTokenType("COMMENT");

    // 空白
    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    // 错误
    IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    // Token 集合定义
    TokenSet COMMENTS = TokenSet.create(COMMENT);
    TokenSet WHITESPACES = TokenSet.create(WHITE_SPACE, NEWLINE);
    TokenSet STRINGS = TokenSet.create(STRING);
    TokenSet NUMBERS = TokenSet.create(INT, FLOAT, LONG);
    TokenSet KEYWORDS = TokenSet.create(KEYWORD, BOOL, NULL);
    TokenSet IDENTIFIERS = TokenSet.create(IDENT);
    TokenSet TYPE_IDENTIFIERS = TokenSet.create(TYPE_IDENT);
    TokenSet OPERATORS = TokenSet.create(EQUALS, PLUS, STAR, MINUS, SLASH, LT, GT, LTE, GTE, NEQ, QUESTION, AT);
    TokenSet PUNCTUATION = TokenSet.create(DOT, COLON, COMMA);
    TokenSet PARENS = TokenSet.create(LPAREN, RPAREN);
    TokenSet BRACKETS = TokenSet.create(LBRACKET, RBRACKET);

    /**
     * 将 aster-core 的 TokenKind 映射到 IntelliJ IElementType
     */
    Map<TokenKind, IElementType> TOKEN_KIND_MAP = createTokenKindMap();

    private static Map<TokenKind, IElementType> createTokenKindMap() {
        Map<TokenKind, IElementType> map = new HashMap<>();
        map.put(TokenKind.EOF, EOF);
        map.put(TokenKind.NEWLINE, NEWLINE);
        map.put(TokenKind.INDENT, INDENT);
        map.put(TokenKind.DEDENT, DEDENT);
        map.put(TokenKind.DOT, DOT);
        map.put(TokenKind.COLON, COLON);
        map.put(TokenKind.COMMA, COMMA);
        map.put(TokenKind.LPAREN, LPAREN);
        map.put(TokenKind.RPAREN, RPAREN);
        map.put(TokenKind.LBRACKET, LBRACKET);
        map.put(TokenKind.RBRACKET, RBRACKET);
        map.put(TokenKind.EQUALS, EQUALS);
        map.put(TokenKind.PLUS, PLUS);
        map.put(TokenKind.STAR, STAR);
        map.put(TokenKind.MINUS, MINUS);
        map.put(TokenKind.SLASH, SLASH);
        map.put(TokenKind.LT, LT);
        map.put(TokenKind.GT, GT);
        map.put(TokenKind.LTE, LTE);
        map.put(TokenKind.GTE, GTE);
        map.put(TokenKind.NEQ, NEQ);
        map.put(TokenKind.QUESTION, QUESTION);
        map.put(TokenKind.AT, AT);
        map.put(TokenKind.IDENT, IDENT);
        map.put(TokenKind.TYPE_IDENT, TYPE_IDENT);
        map.put(TokenKind.STRING, STRING);
        map.put(TokenKind.INT, INT);
        map.put(TokenKind.FLOAT, FLOAT);
        map.put(TokenKind.LONG, LONG);
        map.put(TokenKind.BOOL, BOOL);
        map.put(TokenKind.NULL, NULL);
        map.put(TokenKind.KEYWORD, KEYWORD);
        map.put(TokenKind.COMMENT, COMMENT);
        return map;
    }

    /**
     * 获取对应的 IntelliJ token 类型
     */
    static IElementType getElementType(TokenKind kind) {
        IElementType type = TOKEN_KIND_MAP.get(kind);
        return type != null ? type : BAD_CHARACTER;
    }
}
