package io.aster.idea.lang;

import java.util.Set;

/**
 * Aster 语言关键字统一定义
 * <p>
 * 集中管理所有关键字，避免在多处重复定义。
 * 其他模块应引用此类的关键字集合。
 */
public final class AsterKeywords {

    /**
     * 完整关键字集合（用于验证和过滤）
     */
    public static final Set<String> ALL = Set.of(
        // 声明关键字
        "module", "import", "from", "func", "data", "enum", "type",
        // 控制流关键字
        "if", "elif", "else", "match", "case", "for", "in", "while",
        // 语句关键字
        "let", "set", "return",
        // 工作流关键字
        "workflow", "step", "compensate", "timeout", "retry", "start", "wait", "uses",
        // 自然语言语法关键字
        "this", "define", "use", "capabilities",
        "to", "with", "and", "produce", "it", "performs",
        "as", "one", "is", "a", "an",
        // 值关键字
        "true", "false", "null", "none", "some", "ok", "err",
        // 效果关键字
        "io", "cpu", "pure",
        // 类型相关
        "maybe", "list", "of"
    );

    /**
     * 导入相关关键字
     */
    public static final Set<String> IMPORT = Set.of("import", "use", "from", "as");

    /**
     * 模块声明相关自然语言关键字
     */
    public static final Set<String> MODULE_NATURAL = Set.of(
        "this", "module", "is", "capabilities"
    );

    /**
     * 函数声明相关自然语言关键字
     */
    public static final Set<String> FUNC_NATURAL = Set.of(
        "to", "define", "with", "produce", "and"
    );

    /**
     * 数据类型声明相关自然语言关键字
     */
    public static final Set<String> DATA_NATURAL = Set.of(
        "define", "with", "a", "an", "and"
    );

    /**
     * Let 语句关键字
     */
    public static final Set<String> LET = Set.of("let");

    /**
     * For 循环关键字
     */
    public static final Set<String> FOR = Set.of("for", "in");

    /**
     * Workflow 语句关键字
     */
    public static final Set<String> WORKFLOW = Set.of("workflow", "uses");

    /**
     * It performs 语句关键字
     */
    public static final Set<String> IT_PERFORMS = Set.of("it", "performs", "with");

    /**
     * 补全时显示的关键字列表
     */
    public static final String[] COMPLETION_KEYWORDS = {
        // 声明关键字
        "module", "import", "from", "func", "data", "enum", "type",
        // 控制流关键字
        "if", "elif", "else", "match", "case", "for", "in", "while",
        // 语句关键字
        "let", "set", "return",
        // 工作流关键字
        "workflow", "step", "compensate", "timeout", "retry", "start", "wait", "uses",
        // 自然语言关键字
        "to", "with", "and", "produce", "it", "performs",
        // 值关键字
        "true", "false", "null", "none", "some", "ok", "err",
        // 效果关键字
        "io", "cpu", "pure"
    };

    /**
     * 判断是否为关键字（大小写不敏感）
     */
    public static boolean isKeyword(String text) {
        return text != null && ALL.contains(text.toLowerCase());
    }

    private AsterKeywords() {}
}
