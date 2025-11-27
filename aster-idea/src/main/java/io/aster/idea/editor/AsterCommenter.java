package io.aster.idea.editor;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 注释器
 * <p>
 * 提供 Aster 语言的注释功能支持。
 * Aster 支持两种行注释风格：'#' 和 '//'
 */
public class AsterCommenter implements Commenter {

    @Override
    public @Nullable String getLineCommentPrefix() {
        return "// ";
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
