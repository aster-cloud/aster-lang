package aster.core.lexer;

import aster.core.util.StringEscapes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * CNL 词法分析器
 * <p>
 * 将规范化的 CNL 源代码转换为 Token 流。这是 Aster 编译管道的第二步。
 * <p>
 * <b>功能</b>：
 * <ul>
 *   <li>识别关键字、标识符、字面量、运算符和标点符号</li>
 *   <li>处理缩进敏感的语法（INDENT/DEDENT token）</li>
 *   <li>跟踪每个 token 的位置信息（行号和列号）</li>
 *   <li>支持注释的 trivia 通道分类（inline 或 standalone）</li>
 * </ul>
 * <p>
 * <b>缩进规则</b>：
 * <ul>
 *   <li>Aster 使用 2 空格缩进</li>
 *   <li>缩进必须是偶数个空格</li>
 *   <li>缩进变化会生成 INDENT 或 DEDENT token</li>
 * </ul>
 *
 * @see <a href="https://github.com/anthropics/aster-lang">Aster Lang 文档</a>
 */
public final class Lexer {

    private final String input;
    private int index;
    private int line;
    private int col;
    private final List<Token> tokens;
    private final Deque<Integer> indentStack;

    /**
     * 关键字列表（用于识别保留字）
     */
    private static final List<String> KEYWORDS = List.of(
        "true", "false", "null",
        "to", "return", "match", "when", "define",
        "this", "module", "is", "it", "performs",
        "as", "one", "of", "option", "result",
        "wait", "for", "each", "some", "ok", "err"
    );

    public Lexer(String input) {
        this.input = input;
        this.index = 0;
        this.line = 1;
        this.col = 1;
        this.tokens = new ArrayList<>();
        this.indentStack = new ArrayDeque<>();
        this.indentStack.push(0);
    }

    /**
     * 对规范化的 CNL 源代码进行词法分析，生成 Token 流
     *
     * @param source 规范化后的 CNL 源代码（应先通过 Canonicalizer 处理）
     * @return Token 列表
     * @throws LexerException 当遇到非法字符或缩进错误时抛出
     */
    public static List<Token> lex(String source) {
        Lexer lexer = new Lexer(source);
        return lexer.tokenize();
    }

    private List<Token> tokenize() {
        // Skip UTF-8 BOM if present
        if (input.length() > 0 && input.charAt(0) == 0xFEFF) {
            index++;
            col++;
        }

        while (!isAtEnd()) {
            char ch = peek();

            // Line comments: '//' or '#'
            if (ch == '#') {
                emitCommentToken(1);
                continue;
            }
            if (ch == '/' && peekNext() == '/') {
                emitCommentToken(2);
                continue;
            }
            // Division operator (must come after '//' comment check)
            if (ch == '/') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.SLASH, "/", start, null);
                continue;
            }

            // Newline + indentation
            if (ch == '\n' || ch == '\r') {
                handleNewline();
                continue;
            }

            // Whitespace
            if (ch == ' ' || ch == '\t') {
                next();
                continue;
            }

            // Punctuation - 需要先保存位置再调用 next()
            if (ch == '.') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.DOT, ".", start, null);
                continue;
            }
            if (ch == ':') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.COLON, ":", start, null);
                continue;
            }
            if (ch == ',') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.COMMA, ",", start, null);
                continue;
            }
            if (ch == '(') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.LPAREN, "(", start, null);
                continue;
            }
            if (ch == ')') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.RPAREN, ")", start, null);
                continue;
            }
            if (ch == '[') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.LBRACKET, "[", start, null);
                continue;
            }
            if (ch == ']') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.RBRACKET, "]", start, null);
                continue;
            }
            if (ch == '!') {
                Position start = new Position(line, col);
                next();
                if (peek() == '=') {
                    next();
                    push(TokenKind.NEQ, "!=", start, null);
                } else {
                    throw LexerException.unexpectedCharacter(ch, new Position(line, col));
                }
                continue;
            }
            if (ch == '=') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.EQUALS, "=", start, null);
                continue;
            }
            if (ch == '+') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.PLUS, "+", start, null);
                continue;
            }
            if (ch == '*') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.STAR, "*", start, null);
                continue;
            }
            if (ch == '?') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.QUESTION, "?", start, null);
                continue;
            }
            if (ch == '@') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.AT, "@", start, null);
                continue;
            }
            if (ch == '-') {
                Position start = new Position(line, col);
                next();
                push(TokenKind.MINUS, "-", start, null);
                continue;
            }
            if (ch == '<') {
                Position start = new Position(line, col);
                next();
                if (peek() == '=') {
                    next();
                    push(TokenKind.LTE, "<=", start, null);
                } else {
                    push(TokenKind.LT, "<", start, null);
                }
                continue;
            }
            if (ch == '>') {
                Position start = new Position(line, col);
                next();
                if (peek() == '=') {
                    next();
                    push(TokenKind.GTE, ">=", start, null);
                } else {
                    push(TokenKind.GT, ">", start, null);
                }
                continue;
            }

            // String literal
            if (ch == '"') {
                scanString();
                continue;
            }

            // Identifiers / keywords
            if (isLetter(ch)) {
                scanIdentifierOrKeyword();
                continue;
            }

            // Numbers
            if (isDigit(ch)) {
                scanNumber();
                continue;
            }

            throw LexerException.unexpectedCharacter(ch, new Position(line, col));
        }

        // Close indentation stack
        while (indentStack.size() > 1) {
            indentStack.pop();
            push(TokenKind.DEDENT, null);
        }

        push(TokenKind.EOF, null);
        return tokens;
    }

    // ============================================================
    // 字符操作方法
    // ============================================================

    private boolean isAtEnd() {
        return index >= input.length();
    }

    private char peek() {
        return isAtEnd() ? '\0' : input.charAt(index);
    }

    private char peekNext() {
        return index + 1 >= input.length() ? '\0' : input.charAt(index + 1);
    }

    private char next() {
        if (isAtEnd()) return '\0';
        char ch = input.charAt(index++);
        if (ch == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return ch;
    }

    private boolean isLetter(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isLineBreak(char ch) {
        return ch == '\n' || ch == '\r';
    }

    // ============================================================
    // Token 生成方法
    // ============================================================

    private void push(TokenKind kind, Object value) {
        push(kind, value, new Position(line, col), null);
    }

    private void push(TokenKind kind, Object value, Position start, String channel) {
        Token token = new Token(kind, value, start, new Position(line, col), channel);
        tokens.add(token);
    }

    // ============================================================
    // 缩进处理
    // ============================================================

    private void handleNewline() {
        // 保存位置在消费换行符之前
        Position start = new Position(line, col);

        char ch = peek();
        if (ch == '\r') {
            next();
            if (peek() == '\n') {
                next();
            }
        } else {
            next();
        }

        push(TokenKind.NEWLINE, null, start, null);

        // Measure indentation
        int spaces = 0;
        int k = index;
        while (k < input.length() && input.charAt(k) == ' ') {
            spaces++;
            k++;
        }

        // Skip blank lines
        if (k >= input.length() || input.charAt(k) == '\n') {
            index = k;
            return;
        }

        // Skip comment lines
        if (k < input.length() &&
            (input.charAt(k) == '#' ||
             (input.charAt(k) == '/' && k + 1 < input.length() && input.charAt(k + 1) == '/'))) {
            index = k;
            col += spaces;
            return;
        }

        emitIndentDedent(spaces);
        index = k;
        col += spaces;
    }

    private void emitIndentDedent(int spaces) {
        int last = indentStack.peek();

        if (spaces == last) {
            return;
        }

        if (spaces % 2 != 0) {
            throw LexerException.invalidIndentation(new Position(line, col));
        }

        if (spaces > last) {
            indentStack.push(spaces);
            push(TokenKind.INDENT, null);
        } else {
            while (!indentStack.isEmpty() && spaces < indentStack.peek()) {
                indentStack.pop();
                push(TokenKind.DEDENT, null);
            }
            if (indentStack.isEmpty() || indentStack.peek() != spaces) {
                throw LexerException.inconsistentDedent(new Position(line, col));
            }
        }
    }

    // ============================================================
    // 注释处理
    // ============================================================

    private void emitCommentToken(int prefixLength) {
        Position start = new Position(line, col);
        StringBuilder raw = new StringBuilder();

        for (int j = 0; j < prefixLength; j++) {
            raw.append(next());
        }

        while (!isAtEnd() && !isLineBreak(peek())) {
            raw.append(next());
        }

        String body = raw.substring(prefixLength).replaceFirst("^\\s*", "");
        Token prev = findPrevSignificantToken();
        String trivia = (prev != null && prev.end().line() == start.line()) ? "inline" : "standalone";

        CommentValue commentValue = new CommentValue(raw.toString(), body, trivia);
        push(TokenKind.COMMENT, commentValue, start, "trivia");
    }

    private Token findPrevSignificantToken() {
        for (int idx = tokens.size() - 1; idx >= 0; idx--) {
            Token token = tokens.get(idx);
            if ("trivia".equals(token.channel())) {
                continue;
            }
            if (token.kind() == TokenKind.NEWLINE ||
                token.kind() == TokenKind.INDENT ||
                token.kind() == TokenKind.DEDENT) {
                continue;
            }
            return token;
        }
        return null;
    }

    // ============================================================
    // 字符串字面量扫描
    // ============================================================

    private void scanString() {
        Position start = new Position(line, col);
        next(); // opening quote

        int literalStart = index;
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\\') {
                next(); // consume backslash
                if (isAtEnd()) {
                    throw LexerException.unterminatedString(start);
                }
                next(); // consume escaped char
            } else {
                next();
            }
        }

        if (isAtEnd() || peek() != '"') {
            throw LexerException.unterminatedString(start);
        }

        String raw = input.substring(literalStart, index);
        String value;
        try {
            value = StringEscapes.unescape(raw);
        } catch (IllegalArgumentException ex) {
            throw LexerException.invalidEscape(ex.getMessage(), start);
        }

        next(); // closing quote
        push(TokenKind.STRING, value, start, null);
    }

    // ============================================================
    // 标识符和关键字扫描
    // ============================================================

    private void scanIdentifierOrKeyword() {
        Position start = new Position(line, col);
        StringBuilder word = new StringBuilder();

        while (isLetter(peek()) || isDigit(peek()) || peek() == '_') {
            word.append(next());
        }

        String text = word.toString();
        String lower = text.toLowerCase();

        // Handle booleans/null specially
        if ("true".equals(lower)) {
            push(TokenKind.BOOL, true, start, null);
            return;
        }
        if ("false".equals(lower)) {
            push(TokenKind.BOOL, false, start, null);
            return;
        }
        if ("null".equals(lower)) {
            push(TokenKind.NULL, null, start, null);
            return;
        }

        // Types by capitalized first letter considered TYPE_IDENT
        if (Character.isUpperCase(text.charAt(0))) {
            push(TokenKind.TYPE_IDENT, text, start, null);
        } else {
            push(TokenKind.IDENT, text, start, null);
        }
    }

    // ============================================================
    // 数字字面量扫描
    // ============================================================

    private void scanNumber() {
        Position start = new Position(line, col);
        StringBuilder num = new StringBuilder();

        while (isDigit(peek())) {
            num.append(next());
        }

        // Look for decimal part
        if (peek() == '.' && index + 1 < input.length() && isDigit(input.charAt(index + 1))) {
            num.append(next()); // '.'
            while (isDigit(peek())) {
                num.append(next());
            }
            double val = Double.parseDouble(num.toString());
            push(TokenKind.FLOAT, val, start, null);
            return;
        }

        // Look for long suffix 'L' or 'l'
        if (Character.toLowerCase(peek()) == 'l') {
            next();
            long val = Long.parseLong(num.toString());
            push(TokenKind.LONG, val, start, null);
            return;
        }

        int val = Integer.parseInt(num.toString());
        push(TokenKind.INT, val, start, null);
    }
}
