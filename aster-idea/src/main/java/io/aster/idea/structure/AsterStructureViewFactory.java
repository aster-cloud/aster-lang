package io.aster.idea.structure;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import io.aster.idea.psi.AsterFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster Structure View 工厂
 * <p>
 * 为 Aster 文件提供 Structure View（结构视图），
 * 显示文件中的函数、数据类型、枚举等顶层声明。
 */
public class AsterStructureViewFactory implements PsiStructureViewFactory {

    @Override
    public @Nullable StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
        if (!(psiFile instanceof AsterFile)) {
            return null;
        }

        return new TreeBasedStructureViewBuilder() {
            @Override
            public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new AsterStructureViewModel(psiFile, editor);
            }
        };
    }
}
