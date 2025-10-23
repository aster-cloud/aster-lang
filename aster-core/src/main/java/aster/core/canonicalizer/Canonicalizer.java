package aster.core.canonicalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CNL 源码规范化器
 * <p>
 * Canonicalizer 将 CNL 源代码规范化为标准格式，是 Aster 编译管道的第一步。
 * <p>
 * <b>功能</b>：
 * <ul>
 *   <li>规范化换行符为 {@code \n}</li>
 *   <li>将制表符转换为 2 个空格（Aster 使用 2 空格缩进）</li>
 *   <li>移除行注释（{@code //} 和 {@code #}）</li>
 *   <li>规范化引号（智能引号 → 直引号）</li>
 *   <li>规范化多词关键字大小写（如 "This Module Is" → "this module is"）</li>
 *   <li>去除冠词（a, an, the），但保留字符串字面量内的冠词</li>
 *   <li>规范化空白符（折叠多余空格，保持缩进）</li>
 * </ul>
 * <p>
 * <b>注意</b>：
 * <ul>
 *   <li>缩进具有语法意义，必须保持精确</li>
 *   <li>字符串字面量内的内容不受影响</li>
 *   <li>标识符的大小写保持原样</li>
 * </ul>
 *
 * @see <a href="https://github.com/anthropics/aster-lang">Aster Lang 文档</a>
 */
public final class Canonicalizer {

    /**
     * 冠词正则表达式：匹配 a, an, the 加上后面的空白符
     * <p>
     * 使用单词边界 \b 确保只匹配完整单词，匹配后面的空白符一起移除，
     * 避免留下多余空格。
     */
    private static final Pattern ARTICLE_RE = Pattern.compile(
        "\\b(a|an|the)\\b\\s",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 多词关键字列表（按长度降序排列，贪婪匹配）
     * <p>
     * 这些关键字需要规范化为小写，以确保词法分析器能正确识别。
     */
    private static final List<String> MULTI_WORD_KEYWORDS = List.of(
        "this module is",  // MODULE_IS
        "it performs",     // PERFORMS
        "as one of",       // ONE_OF
        "option of",       // OPTION_OF
        "result of",       // RESULT_OF
        "wait for",        // WAIT_FOR
        "for each",        // FOR_EACH
        "some of",         // SOME_OF
        "ok of",           // OK_OF
        "err of"           // ERR_OF
    );

    /**
     * 行注释正则表达式：匹配以 // 或 # 开头的行
     */
    private static final Pattern LINE_COMMENT_RE = Pattern.compile("^\\s*(?://|#)");

    /**
     * 空白规范化所需的正则表达式
     */
    private static final Pattern SPACE_RUN_RE = Pattern.compile("[ \\t]+");
    private static final Pattern PUNCT_NORMAL_RE = Pattern.compile("\\s+([.,:])");
    private static final Pattern PUNCT_FINAL_RE = Pattern.compile("\\s+([.,:!;?])");
    private static final Pattern TRAILING_SPACE_RE = Pattern.compile("\\s+$");

    /**
     * 规范化 CNL 源代码为标准格式
     *
     * @param input 原始 CNL 源代码字符串
     * @return 规范化后的 CNL 源代码
     */
    public String canonicalize(String input) {
        // 1. 规范化换行符为 \n
        String s = input.replaceAll("\\r\\n?", "\n");

        // 2. 将制表符转换为 2 个空格（缩进具有 2 空格语法意义）
        s = s.replace("\t", "  ");

        // 3. 移除行注释（// 和 #）
        s = removeLineComments(s);

        // 4. 规范化智能引号为直引号
        s = s.replace("\u201C", "\"")  // 左双引号 "
             .replace("\u201D", "\"")  // 右双引号 "
             .replace("\u2018", "'")   // 左单引号 '
             .replace("\u2019", "'");  // 右单引号 '

        // 5. 语句终止符检查（保持原样，由 parser 处理）
        // TypeScript 实现中此步骤不做任何修改，仅用于后续 parser 的启发

        // 6. 折叠多余空格，保持缩进
        s = normalizeWhitespace(s);

        // 7. 规范化多词关键字大小写
        s = normalizeMultiWordKeywords(s);

        // 8. 去除冠词（保留字符串字面量内的冠词）
        s = removeArticles(s);

        // 9. 最终空白符规范化（确保幂等性）
        s = finalWhitespaceNormalization(s);

        return s;
    }

    /**
     * 移除行注释（// 和 #）
     */
    private String removeLineComments(String s) {
        StringBuilder result = new StringBuilder();
        String[] lines = s.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }

            String line = lines[i];
            // 注释行保留空行占位以维持行号
            if (LINE_COMMENT_RE.matcher(line).find()) {
                continue;
            }
            result.append(line);
        }

        return result.toString();
    }

    /**
     * 规范化空白符：折叠多余空格，保持缩进
     * <p>
     * 对每一行：
     * - 保留前导空格（缩进）
     * - 折叠多余空格为单个空格
     * - 移除标点前的空格
     */
    private String normalizeWhitespace(String s) {
        StringBuilder result = new StringBuilder();
        String[] lines = s.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(normalizeLine(lines[i], PUNCT_NORMAL_RE, false));
        }

        return result.toString();
    }

    /**
     * 规范化多词关键字大小写
     * <p>
     * 将多词关键字（如 "This Module Is"）转换为小写（"this module is"），
     * 以便词法分析器正确识别。
     */
    private String normalizeMultiWordKeywords(String s) {
        String result = s;

        for (String keyword : MULTI_WORD_KEYWORDS) {
            // 转义正则表达式特殊字符
            String escaped = Pattern.quote(keyword);
            // 大小写不敏感替换
            Pattern pattern = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE);
            result = pattern.matcher(result).replaceAll(keyword);
        }

        return result;
    }

    /**
     * 去除冠词（a, an, the），但保留字符串字面量内的冠词
     * <p>
     * 算法：
     * 1. 将源码分段为字符串内和字符串外的片段
     * 2. 仅在字符串外的片段应用冠词移除正则
     * 3. 重新拼接所有片段
     */
    private String removeArticles(String s) {
        List<Segment> segments = segmentString(s);

        StringBuilder result = new StringBuilder();
        for (Segment segment : segments) {
            if (segment.inString) {
                // 字符串内，保持原样
                result.append(segment.text);
            } else {
                // 字符串外，移除冠词
                String withoutArticles = ARTICLE_RE.matcher(segment.text).replaceAll("");
                result.append(withoutArticles);
            }
        }

        return result.toString();
    }

    /**
     * 将源码分段为字符串内和字符串外的片段
     */
    private List<Segment> segmentString(String s) {
        List<Segment> segments = new ArrayList<>();
        boolean inString = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            current.append(ch);

            if (ch == '"' && !isEscaped(s, i)) {
                if (inString) {
                    // 字符串结束
                    segments.add(new Segment(current.toString(), true));
                    current = new StringBuilder();
                    inString = false;
                } else {
                    // 字符串开始
                    String before = current.substring(0, current.length() - 1);
                    if (!before.isEmpty()) {
                        segments.add(new Segment(before, false));
                    }
                    current = new StringBuilder("\"");
                    inString = true;
                }
            }
        }

        // 添加剩余内容
        if (current.length() > 0) {
            segments.add(new Segment(current.toString(), inString));
        }

        return segments;
    }

    /**
     * 判断指定位置的引号是否被转义
     * <p>
     * 算法：向前扫描反斜杠数量，奇数个反斜杠表示转义。
     */
    private boolean isEscaped(String str, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && str.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    /**
     * 最终空白符规范化（确保幂等性）
     * <p>
     * - 移除仅包含空白的行
     * - 对每行重新规范化空白符
     */
    private String finalWhitespaceNormalization(String s) {
        // 移除仅包含空白的行
        s = s.replaceAll("(?m)^\\s+$", "");

        // 对每行重新规范化
        StringBuilder result = new StringBuilder();
        String[] lines = s.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(normalizeLine(lines[i], PUNCT_FINAL_RE, true));
        }

        return result.toString();
    }

    /**
     * 规范化单行内容，保留字符串字面量原始空白
     */
    private String normalizeLine(String line, Pattern punctuationPattern, boolean trimTrailing) {
        if (line.isEmpty()) {
            return line;
        }

        int indentEnd = 0;
        while (indentEnd < line.length() && Character.isWhitespace(line.charAt(indentEnd))) {
            indentEnd++;
        }

        String indent = line.substring(0, indentEnd);
        String rest = line.substring(indentEnd);
        if (rest.isEmpty()) {
            return indent;
        }

        String normalizedRest = normalizeRest(rest, punctuationPattern, trimTrailing);
        return indent + normalizedRest;
    }

    /**
     * 仅在字符串外部折叠空白与标点前空格
     */
    private String normalizeRest(String rest, Pattern punctuationPattern, boolean trimTrailing) {
        List<Segment> segments = segmentString(rest);
        if (segments.isEmpty()) {
            return rest;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment.inString) {
                builder.append(segment.text);
                continue;
            }

            String normalized = collapseSpaces(segment.text);
            normalized = punctuationPattern.matcher(normalized).replaceAll("$1");

            if (trimTrailing && i == segments.size() - 1) {
                normalized = TRAILING_SPACE_RE.matcher(normalized).replaceAll("");
            }

            builder.append(normalized);
        }

        return builder.toString();
    }

    /**
     * 将连续空白折叠为单个空格
     */
    private String collapseSpaces(String text) {
        return SPACE_RUN_RE.matcher(text).replaceAll(" ");
    }

    /**
     * 源码片段（字符串内或字符串外）
     */
    private static record Segment(String text, boolean inString) {}
}
