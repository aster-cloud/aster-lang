package io.aster.idea.lang;

import aster.core.lexer.Lexer;
import aster.core.lexer.LexerException;
import aster.core.lexer.Token;
import aster.core.lexer.TokenKind;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ 词法分析器适配器
 * <p>
 * 将 aster-core 的 Lexer 适配为 IntelliJ Platform 的 Lexer 接口。
 * 由于 aster-core Lexer 一次性解析整个输入，此适配器缓存所有 token 并提供逐个访问。
 * <p>
 * 重要说明：
 * - 跳过零宽 token（INDENT/DEDENT），因为 IntelliJ 要求 token 范围严格递增
 * - 支持 CRLF 换行符
 * - 词法错误时生成 BAD_CHARACTER token 而非返回空列表
 */
public class AsterLexerAdapter extends LexerBase {

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    private List<Token> tokens;
    private int tokenIndex;

    /** 标记是否发生词法错误 */
    private boolean lexerError;
    /** 词法错误的位置 */
    private int errorOffset;

    // 缓存的偏移量映射（行/列 -> 字符偏移量）
    private int[] lineStartOffsets;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenIndex = 0;
        this.lexerError = false;
        this.errorOffset = endOffset;

        // 使用 aster-core Lexer 进行词法分析（仅处理 [startOffset, endOffset) 区间）
        String source = buffer.subSequence(startOffset, endOffset).toString();

        // 针对被分析的切片构建行偏移量映射（不是整个 buffer）
        buildLineOffsetMap(source);

        try {
            List<Token> rawTokens = Lexer.lex(source);
            // 过滤掉零宽 token（INDENT/DEDENT），因为 IntelliJ 要求 token 范围严格递增
            this.tokens = filterZeroWidthTokens(rawTokens);
        } catch (LexerException e) {
            // 词法错误时标记错误位置，后续会生成 BAD_CHARACTER token
            this.lexerError = true;
            this.tokens = new ArrayList<>();
            // 尝试从异常消息中提取错误位置
            this.errorOffset = extractErrorOffset(e, source);
        }
    }

    /**
     * 过滤掉零宽 token（INDENT/DEDENT/EOF）
     * IntelliJ 要求 token 范围严格递增，零宽 token 会违反此约定
     */
    private List<Token> filterZeroWidthTokens(List<Token> rawTokens) {
        List<Token> filtered = new ArrayList<>();
        for (Token token : rawTokens) {
            TokenKind kind = token.kind();
            // 跳过 INDENT、DEDENT 和 EOF（它们是逻辑 token，没有实际文本）
            if (kind != TokenKind.INDENT && kind != TokenKind.DEDENT && kind != TokenKind.EOF) {
                filtered.add(token);
            }
        }
        return filtered;
    }

    /**
     * 从 LexerException 中提取错误位置
     */
    private int extractErrorOffset(LexerException e, String source) {
        // LexerException 通常包含行列信息，尝试解析
        String msg = e.getMessage();
        if (msg != null && msg.contains("line")) {
            // 尝试解析 "line X, col Y" 格式
            try {
                int lineIdx = msg.indexOf("line");
                int colIdx = msg.indexOf("col");
                if (lineIdx >= 0 && colIdx > lineIdx) {
                    String lineStr = msg.substring(lineIdx + 5, colIdx).replaceAll("[^0-9]", "");
                    String colStr = msg.substring(colIdx + 4).replaceAll("[^0-9]", "");
                    if (!lineStr.isEmpty() && !colStr.isEmpty()) {
                        int line = Integer.parseInt(lineStr);
                        int col = Integer.parseInt(colStr);
                        return positionToOffset(line, col);
                    }
                }
            } catch (NumberFormatException ignored) {
                // 解析失败，使用默认位置
            }
        }
        // 默认从开头标记为错误
        return startOffset;
    }

    /**
     * 构建行偏移量映射表，用于将 Position(line, col) 转换为字符偏移量
     * 注意：映射基于被分析的切片，而非整个 buffer
     * 支持 LF (\n) 和 CRLF (\r\n) 换行符
     */
    private void buildLineOffsetMap(CharSequence slice) {
        // 统计行数（支持 CRLF 和 LF）
        int lineCount = 1;
        for (int i = 0; i < slice.length(); i++) {
            char c = slice.charAt(i);
            if (c == '\n') {
                lineCount++;
            } else if (c == '\r') {
                // 检查是否是 CRLF
                if (i + 1 < slice.length() && slice.charAt(i + 1) == '\n') {
                    // CRLF，跳过 \n（下次循环会处理）
                } else {
                    // 单独的 \r（旧 Mac 风格）
                    lineCount++;
                }
            }
        }

        lineStartOffsets = new int[lineCount + 1];
        lineStartOffsets[0] = 0;
        lineStartOffsets[1] = 0;

        int lineIndex = 2;
        for (int i = 0; i < slice.length(); i++) {
            char c = slice.charAt(i);
            if (c == '\n') {
                if (lineIndex < lineStartOffsets.length) {
                    lineStartOffsets[lineIndex++] = i + 1;
                }
            } else if (c == '\r') {
                if (i + 1 < slice.length() && slice.charAt(i + 1) == '\n') {
                    // CRLF：下一行从 \n 之后开始
                    if (lineIndex < lineStartOffsets.length) {
                        lineStartOffsets[lineIndex++] = i + 2;
                    }
                    i++; // 跳过 \n
                } else {
                    // 单独的 \r
                    if (lineIndex < lineStartOffsets.length) {
                        lineStartOffsets[lineIndex++] = i + 1;
                    }
                }
            }
        }
    }

    /**
     * 将 aster-core 的 Position 转换为字符偏移量
     * lineStartOffsets 是相对于切片的偏移，需要加上 startOffset 转换为 buffer 绝对偏移
     */
    private int positionToOffset(int line, int col) {
        if (line < 1 || line >= lineStartOffsets.length) {
            return endOffset;
        }
        // lineStartOffsets[line] 是切片内的相对偏移
        int lineStartInSlice = lineStartOffsets[line];
        // 加上 startOffset 和列偏移得到 buffer 绝对偏移
        return startOffset + lineStartInSlice + (col - 1);
    }

    @Override
    public int getState() {
        return 0; // Aster 词法分析器无状态
    }

    @Override
    public @Nullable IElementType getTokenType() {
        // 处理词法错误情况：返回 BAD_CHARACTER
        if (lexerError && tokenIndex >= tokens.size()) {
            return AsterTokenTypes.BAD_CHARACTER;
        }

        if (tokens == null || tokenIndex >= tokens.size()) {
            return null;
        }
        Token token = tokens.get(tokenIndex);
        // 跳过 EOF token（已在 filterZeroWidthTokens 中过滤，但保留此检查以防万一）
        if (token.kind() == TokenKind.EOF) {
            return null;
        }
        return AsterTokenTypes.getElementType(token.kind());
    }

    @Override
    public int getTokenStart() {
        // 处理词法错误情况：返回错误位置
        if (lexerError && tokenIndex >= tokens.size()) {
            return errorOffset;
        }

        if (tokens == null || tokenIndex >= tokens.size()) {
            return endOffset;
        }
        Token token = tokens.get(tokenIndex);
        return positionToOffset(token.start().line(), token.start().col());
    }

    @Override
    public int getTokenEnd() {
        // 处理词法错误情况：返回文件末尾
        if (lexerError && tokenIndex >= tokens.size()) {
            return endOffset;
        }

        if (tokens == null || tokenIndex >= tokens.size()) {
            return endOffset;
        }
        Token token = tokens.get(tokenIndex);

        // 计算 token 的结束位置
        int start = positionToOffset(token.start().line(), token.start().col());
        int end = positionToOffset(token.end().line(), token.end().col());

        // 确保 end > start（IntelliJ 要求 token 范围严格递增）
        if (end <= start) {
            // 根据 token 类型估算长度
            end = start + getTokenLength(token);
        }

        return Math.min(end, endOffset);
    }

    /**
     * 根据 token 类型和值估算 token 长度
     * 注意：INDENT/DEDENT/EOF 已被过滤，不会调用此方法
     */
    private int getTokenLength(Token token) {
        return switch (token.kind()) {
            case NEWLINE -> 1;
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
        } else if (lexerError && tokenIndex == tokens.size()) {
            // 词法错误时，advance 后标记为已处理
            lexerError = false;
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
