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
 * - 合成 WHITESPACE token 填充间隙，因为 IntelliJ 要求 token 流覆盖整个源代码
 * - 支持 CRLF 换行符
 * - 词法错误时生成 BAD_CHARACTER token 而非返回空列表
 */
public class AsterLexerAdapter extends LexerBase {

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    /** 统一的 token 列表（包含原始 token 和合成的 WHITESPACE token） */
    private List<AdaptedToken> adaptedTokens;
    private int tokenIndex;

    /** 内部 token 表示，包含类型和范围 */
    private record AdaptedToken(com.intellij.psi.tree.IElementType type, int start, int end) {}

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenIndex = 0;

        // 使用 aster-core Lexer 进行词法分析（仅处理 [startOffset, endOffset) 区间）
        String source = buffer.subSequence(startOffset, endOffset).toString();

        // 使用局部错误恢复进行词法分析
        this.adaptedTokens = lexWithErrorRecovery(source);
    }

    /**
     * 使用局部错误恢复进行词法分析
     * <p>
     * 当遇到词法错误时：
     * 1. 对错误位置之前的内容重新进行词法分析，保留有效 token
     * 2. 仅将错误位置的单个字符标记为 BAD_CHARACTER
     * 3. 尝试继续分析错误位置之后的内容
     * <p>
     * 这样可以避免整个文件因单个词法错误而变成 BAD_CHARACTER，
     * 同时保留错误前的有效 token 用于语法高亮和结构解析。
     */
    private List<AdaptedToken> lexWithErrorRecovery(String source) {
        List<AdaptedToken> result = new ArrayList<>();
        int currentPos = 0;

        while (currentPos < source.length()) {
            String remaining = source.substring(currentPos);
            try {
                List<Token> rawTokens = Lexer.lex(remaining);
                // 成功解析剩余部分，构建 token 并添加到结果
                List<AdaptedToken> tokens = buildAdaptedTokens(rawTokens, remaining.length(), currentPos);
                result.addAll(tokens);
                break; // 成功解析完成
            } catch (LexerException e) {
                // 提取错误在剩余字符串中的相对位置
                int errorPosInRemaining = extractErrorOffsetFromException(e, remaining);

                // 如果错误位置之前有内容，尝试重新 lex 以保留有效 token
                if (errorPosInRemaining > 0) {
                    String prefix = remaining.substring(0, errorPosInRemaining);
                    List<AdaptedToken> prefixTokens = lexPrefixSafely(prefix, currentPos);
                    result.addAll(prefixTokens);
                }

                // 在错误位置添加单个 BAD_CHARACTER token
                int badCharEnd = Math.min(currentPos + errorPosInRemaining + 1, source.length());
                result.add(new AdaptedToken(
                    AsterTokenTypes.BAD_CHARACTER,
                    startOffset + currentPos + errorPosInRemaining,
                    startOffset + badCharEnd
                ));

                // 移动到错误位置之后继续分析
                currentPos = badCharEnd;
            }
        }

        return result;
    }

    /**
     * 安全地对前缀进行词法分析
     * <p>
     * 如果前缀本身也包含错误，递归使用错误恢复机制。
     * 如果完全无法解析，则将整个前缀标记为 WHITESPACE。
     *
     * @param prefix 要分析的前缀字符串
     * @param baseOffset 前缀在原始源码中的起始位置
     * @return 解析后的 token 列表
     */
    private List<AdaptedToken> lexPrefixSafely(String prefix, int baseOffset) {
        try {
            List<Token> rawTokens = Lexer.lex(prefix);
            return buildAdaptedTokens(rawTokens, prefix.length(), baseOffset);
        } catch (LexerException e) {
            // 前缀本身也有错误，递归处理
            // 为避免无限递归，限制递归深度：如果前缀很短就直接标记为 WHITESPACE
            if (prefix.length() <= 1) {
                List<AdaptedToken> result = new ArrayList<>();
                result.add(new AdaptedToken(
                    AsterTokenTypes.WHITE_SPACE,
                    startOffset + baseOffset,
                    startOffset + baseOffset + prefix.length()
                ));
                return result;
            }

            // 对前缀进行错误恢复
            List<AdaptedToken> result = new ArrayList<>();
            int errorPos = extractErrorOffsetFromException(e, prefix);

            // 递归处理错误前的部分
            if (errorPos > 0) {
                String subPrefix = prefix.substring(0, errorPos);
                result.addAll(lexPrefixSafely(subPrefix, baseOffset));
            }

            // 标记错误字符
            int badCharEnd = Math.min(errorPos + 1, prefix.length());
            result.add(new AdaptedToken(
                AsterTokenTypes.BAD_CHARACTER,
                startOffset + baseOffset + errorPos,
                startOffset + baseOffset + badCharEnd
            ));

            // 递归处理错误后的部分
            if (badCharEnd < prefix.length()) {
                String suffix = prefix.substring(badCharEnd);
                result.addAll(lexPrefixSafely(suffix, baseOffset + badCharEnd));
            }

            return result;
        }
    }

    /**
     * 从 LexerException 中提取错误在给定源码中的相对位置
     */
    private int extractErrorOffsetFromException(LexerException e, String source) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("line")) {
            try {
                int lineIdx = msg.indexOf("line");
                int colIdx = msg.indexOf("col");
                if (lineIdx >= 0 && colIdx > lineIdx) {
                    String lineStr = msg.substring(lineIdx + 5, colIdx).replaceAll("[^0-9]", "");
                    String colStr = msg.substring(colIdx + 4).replaceAll("[^0-9]", "");
                    if (!lineStr.isEmpty() && !colStr.isEmpty()) {
                        int line = Integer.parseInt(lineStr);
                        int col = Integer.parseInt(colStr);
                        // 计算在源码中的偏移
                        return positionToOffsetInSource(source, line, col);
                    }
                }
            } catch (NumberFormatException ignored) {
                // 解析失败，返回 0
            }
        }
        return 0;
    }

    /**
     * 在给定源码中将 (line, col) 转换为字符偏移量
     */
    private int positionToOffsetInSource(String source, int line, int col) {
        int currentLine = 1;
        int offset = 0;
        while (offset < source.length() && currentLine < line) {
            char c = source.charAt(offset);
            if (c == '\n') {
                currentLine++;
            } else if (c == '\r') {
                currentLine++;
                if (offset + 1 < source.length() && source.charAt(offset + 1) == '\n') {
                    offset++; // 跳过 CRLF 中的 LF
                }
            }
            offset++;
        }
        return Math.min(offset + col - 1, source.length());
    }

    /**
     * 构建适配后的 token 列表，包含：
     * 1. 过滤掉零宽 token（INDENT/DEDENT/EOF）
     * 2. 合成 WHITESPACE token 填充间隙
     * <p>
     * IntelliJ 要求 token 范围严格递增且覆盖整个源代码
     *
     * @param rawTokens 原始 token 列表
     * @param sourceLength 源码长度
     * @param baseOffset 当前分析片段在整个源码中的基础偏移
     */
    private List<AdaptedToken> buildAdaptedTokens(List<Token> rawTokens, int sourceLength, int baseOffset) {
        List<AdaptedToken> result = new ArrayList<>();
        int currentOffset = 0;

        // 获取被分析片段的源码（用于计算相对位置）
        String fragmentSource = buffer.subSequence(startOffset + baseOffset, startOffset + baseOffset + sourceLength).toString();

        for (Token token : rawTokens) {
            TokenKind kind = token.kind();
            // 跳过 INDENT、DEDENT 和 EOF（它们是逻辑 token，没有实际文本）
            if (kind == TokenKind.INDENT || kind == TokenKind.DEDENT || kind == TokenKind.EOF) {
                continue;
            }

            // 计算 token 在片段中的相对位置（token 的 line/col 是相对于片段的）
            int tokenStart = positionToOffsetInSource(fragmentSource, token.start().line(), token.start().col());
            int tokenEnd = positionToOffsetInSource(fragmentSource, token.end().line(), token.end().col());

            // 确保 end > start
            if (tokenEnd <= tokenStart) {
                tokenEnd = tokenStart + getTokenLength(token, fragmentSource, tokenStart);
            }

            // 如果有间隙，合成 WHITESPACE token
            if (tokenStart > currentOffset) {
                result.add(new AdaptedToken(
                    AsterTokenTypes.WHITE_SPACE,
                    startOffset + baseOffset + currentOffset,
                    startOffset + baseOffset + tokenStart
                ));
            }

            // 添加实际 token（位置需要加上 baseOffset）
            IElementType elementType = AsterTokenTypes.getElementType(kind);
            result.add(new AdaptedToken(
                elementType,
                startOffset + baseOffset + tokenStart,
                startOffset + baseOffset + tokenEnd
            ));
            currentOffset = tokenEnd;
        }

        // 如果末尾有剩余空白，合成最后一个 WHITESPACE token
        if (currentOffset < sourceLength) {
            result.add(new AdaptedToken(
                AsterTokenTypes.WHITE_SPACE,
                startOffset + baseOffset + currentOffset,
                startOffset + baseOffset + sourceLength
            ));
        }

        return result;
    }

    @Override
    public int getState() {
        return 0; // Aster 词法分析器无状态
    }

    @Override
    public @Nullable IElementType getTokenType() {
        if (adaptedTokens == null || tokenIndex >= adaptedTokens.size()) {
            return null;
        }
        return adaptedTokens.get(tokenIndex).type();
    }

    @Override
    public int getTokenStart() {
        if (adaptedTokens == null || tokenIndex >= adaptedTokens.size()) {
            return endOffset;
        }
        return adaptedTokens.get(tokenIndex).start();
    }

    @Override
    public int getTokenEnd() {
        if (adaptedTokens == null || tokenIndex >= adaptedTokens.size()) {
            return endOffset;
        }
        return adaptedTokens.get(tokenIndex).end();
    }

    /**
     * 根据 token 类型和值估算 token 长度
     * <p>
     * 注意：INDENT/DEDENT/EOF 已被过滤，不会调用此方法
     *
     * @param token token 对象
     * @param source 源代码片段
     * @param tokenStart token 在源代码中的起始位置
     */
    private int getTokenLength(Token token, String source, int tokenStart) {
        return switch (token.kind()) {
            case NEWLINE -> 1;
            case STRING -> {
                // 直接从源码中扫描字符串结束位置，处理各种引号类型
                yield scanStringLength(source, tokenStart);
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

    /**
     * 扫描字符串字面量的长度
     * <p>
     * 支持：
     * - 单引号字符串 'text'
     * - 双引号字符串 "text"
     * - 三引号字符串 '''text''' 和 \"\"\"text\"\"\"
     */
    private int scanStringLength(String source, int start) {
        if (start >= source.length()) {
            return 1;
        }

        char firstChar = source.charAt(start);
        if (firstChar != '"' && firstChar != '\'') {
            // 不是字符串起始符，返回最小长度
            return 1;
        }

        // 检查是否是三引号
        boolean isTripleQuoted = (start + 2 < source.length()) &&
                                  source.charAt(start + 1) == firstChar &&
                                  source.charAt(start + 2) == firstChar;

        int pos = start;
        if (isTripleQuoted) {
            // 跳过开头的三个引号
            pos += 3;
            // 查找结束的三引号
            while (pos + 2 < source.length()) {
                if (source.charAt(pos) == firstChar &&
                    source.charAt(pos + 1) == firstChar &&
                    source.charAt(pos + 2) == firstChar) {
                    return (pos + 3) - start;
                }
                pos++;
            }
            // 未找到结束引号，返回到源码末尾
            return source.length() - start;
        } else {
            // 单引号字符串
            pos++;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (c == firstChar) {
                    return (pos + 1) - start;
                }
                if (c == '\\' && pos + 1 < source.length()) {
                    pos++; // 跳过转义字符
                }
                pos++;
            }
            // 未找到结束引号，返回到源码末尾
            return source.length() - start;
        }
    }

    @Override
    public void advance() {
        if (adaptedTokens != null && tokenIndex < adaptedTokens.size()) {
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
