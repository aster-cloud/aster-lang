package io.aster.idea.formatting;

import com.intellij.formatting.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import io.aster.idea.lang.AsterLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 代码格式化模型构建器
 * <p>
 * 提供基于缩进的代码格式化支持。
 */
public class AsterFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiFile file = formattingContext.getPsiElement().getContainingFile();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();

        // 创建根格式化块
        AsterBlock rootBlock = new AsterBlock(
            file.getNode(),
            null,
            Indent.getNoneIndent(),
            settings
        );

        return FormattingModelProvider.createFormattingModelForPsiFile(
            file,
            rootBlock,
            settings
        );
    }
}
