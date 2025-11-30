package io.aster.idea.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import io.aster.idea.lang.AsterKeywords;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Aster 名称验证器
 * <p>
 * 验证标识符和关键字，用于重命名重构。
 * isIdentifier 验证简单标识符，不支持带点的限定名称。
 * 限定名称（如 math.core）应使用 isQualifiedName 方法验证。
 */
public class AsterNamesValidator implements NamesValidator {

    /**
     * 简单标识符的正则表达式
     */
    private static final Pattern SIMPLE_IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * 限定名称的正则表达式（支持点分隔的标识符，如 math.core）
     */
    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*");

    @Override
    public boolean isKeyword(@NotNull String name, Project project) {
        return AsterKeywords.isKeyword(name);
    }

    @Override
    public boolean isIdentifier(@NotNull String name, Project project) {
        if (name.isEmpty()) {
            return false;
        }
        if (isKeyword(name, project)) {
            return false;
        }
        // 只验证简单标识符，不支持带点的限定名称
        return SIMPLE_IDENTIFIER_PATTERN.matcher(name).matches();
    }

    /**
     * 验证限定名称（支持点分隔的标识符，如 math.core）
     * <p>
     * 用于验证模块路径等需要支持点分隔的名称。
     *
     * @param name 要验证的名称
     * @param project 项目上下文
     * @return 如果是有效的限定名称返回 true
     */
    public boolean isQualifiedName(@NotNull String name, Project project) {
        if (name.isEmpty()) {
            return false;
        }
        // 检查每个段是否为关键字
        for (String segment : name.split("\\.")) {
            if (isKeyword(segment, project)) {
                return false;
            }
        }
        return QUALIFIED_NAME_PATTERN.matcher(name).matches();
    }
}
