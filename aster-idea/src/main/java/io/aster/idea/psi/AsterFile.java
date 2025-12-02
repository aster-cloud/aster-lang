package io.aster.idea.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import io.aster.idea.lang.AsterFileType;
import io.aster.idea.lang.AsterLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Aster PSI 文件
 * <p>
 * 表示 .aster 文件在 PSI 树中的根节点
 */
@SuppressWarnings("serial")
public class AsterFile extends PsiFileBase {

    public AsterFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, AsterLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return AsterFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Aster File";
    }
}
