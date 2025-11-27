package io.aster.idea.highlighting;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 语法高亮器工厂
 * <p>
 * 为 IntelliJ Platform 提供 Aster 文件的语法高亮器实例
 */
public class AsterSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

    @Override
    public @NotNull SyntaxHighlighter getSyntaxHighlighter(
        @Nullable Project project,
        @Nullable VirtualFile virtualFile
    ) {
        return new AsterSyntaxHighlighter();
    }
}
