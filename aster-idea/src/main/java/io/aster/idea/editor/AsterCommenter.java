package io.aster.idea.editor;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 注释器
 * <p>
 * 提供 Aster 语言的注释功能支持。
 * Aster 主要使用 '#' 作为行注释前缀（类似 Python/Shell），
 * 同时也支持 '//' 风格的注释。
 * <p>
 * IDE 的"注释/取消注释"快捷键将使用 '#' 作为默认前缀。
 */
public class AsterCommenter implements Commenter {

    /**
     * 返回行注释前缀
     * 使用 '#' 作为主要注释风格，与语言规范一致
     */
    @Override
    public @Nullable String getLineCommentPrefix() {
        return "# ";
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return null; // Aster 不支持块注释
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }
}
