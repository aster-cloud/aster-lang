package io.aster.idea.lang;

import aster.core.lexer.Lexer;
import aster.core.lexer.LexerException;
import aster.core.lexer.Token;
import aster.core.lexer.TokenKind;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * IntelliJ 词法分析器适配器
 * <p>
 * 将 aster-core 的 Lexer 适配为 IntelliJ Platform 的 Lexer 接口。
 * 由于 aster-core Lexer 一次性解析整个输入，此适配器缓存所有 token 并提供逐个访问。
 */
public class AsterLexerAdapter extends LexerBase {

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    private List<Token> tokens;
    private int tokenIndex;

    // 缓存的偏移量映射（行/列 -> 字符偏移量）
    private int[] lineStartOffsets;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenIndex = 0;

        // 计算行偏移量映射
        buildLineOffsetMap(buffer);

        // 使用 aster-core Lexer 进行词法分析
        String source = buffer.subSequence(startOffset, endOffset).toString();
        try {
            this.tokens = Lexer.lex(source);
        } catch (LexerException e) {
            // 词法错误时返回空 token 列表，让语法高亮器处理整个文本为错误
            this.tokens = Collections.emptyList();
        }
    }

    /**
     * 构建行偏移量映射表，用于将 Position(line, col) 转换为字符偏移量
     */
    private void buildLineOffsetMap(CharSequence buffer) {
        // 统计行数
        int lineCount = 1;
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.charAt(i) == '\n') {
                lineCount++;
            }
        }

        lineStartOffsets = new int[lineCount + 1];
        lineStartOffsets[0] = 0;
        lineStartOffsets[1] = 0;

        int lineIndex = 2;
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.charAt(i) == '\n' && lineIndex < lineStartOffsets.length) {
                lineStartOffsets[lineIndex++] = i + 1;
            }
        }
    }

    /**
     * 将 aster-core 的 Position 转换为字符偏移量
     */
    private int positionToOffset(int line, int col) {
        if (line < 1 || line >= lineStartOffsets.length) {
            return endOffset;
        }
        int lineStart = lineStartOffsets[line];
        return startOffset + lineStart + (col - 1);
    }

    @Override
    public int getState() {
        return 0; // Aster 词法分析器无状态
    }

    @Override
    public @Nullable IElementType getTokenType() {
        if (tokens == null || tokenIndex >= tokens.size()) {
            return null;
        }
        Token token = tokens.get(tokenIndex);
        // 跳过 EOF token
        if (token.kind() == TokenKind.EOF) {
            return null;
        }
        return AsterTokenTypes.getElementType(token.kind());
    }

    @Override
    public int getTokenStart() {
        if (tokens == null || tokenIndex >= tokens.size()) {
            return endOffset;
        }
        Token token = tokens.get(tokenIndex);
        return positionToOffset(token.start().line(), token.start().col());
    }

    @Override
    public int getTokenEnd() {
        if (tokens == null || tokenIndex >= tokens.size()) {
            return endOffset;
        }
        Token token = tokens.get(tokenIndex);

        // 对于某些 token，end 位置可能与 start 相同（例如 INDENT/DEDENT）
        // 需要根据 token 值计算实际长度
        int start = positionToOffset(token.start().line(), token.start().col());
        int end = positionToOffset(token.end().line(), token.end().col());

        // 确保 end >= start
        if (end <= start) {
            // 根据 token 类型估算长度
            end = start + getTokenLength(token);
        }

        return Math.min(end, endOffset);
    }

    /**
     * 根据 token 类型和值估算 token 长度
     */
    private int getTokenLength(Token token) {
        return switch (token.kind()) {
            case NEWLINE -> 1;
            case INDENT, DEDENT, EOF -> 0;
            case STRING -> {
                Object value = token.value();
                // 字符串长度 + 2（引号）
                yield value != null ? value.toString().length() + 2 : 2;
            }
            case COMMENT -> {
                Object value = token.value();
                if (value instanceof aster.core.lexer.CommentValue cv) {
                    yield cv.raw().length();
                }
                yield 1;
            }
            case LTE, GTE, NEQ -> 2;
            default -> {
                Object value = token.value();
                yield value != null ? value.toString().length() : 1;
            }
        };
    }

    @Override
    public void advance() {
        if (tokens != null && tokenIndex < tokens.size()) {
            tokenIndex++;
        }
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
