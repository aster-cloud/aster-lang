package io.aster.idea.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import io.aster.idea.psi.AsterNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster Structure View 模型
 * <p>
 * 定义 Structure View 的行为，包括：
 * - 根元素（文件）
 * - 可展示的元素类型
 * - 排序器
 */
public class AsterStructureViewModel extends StructureViewModelBase
    implements StructureViewModel.ElementInfoProvider {

    public AsterStructureViewModel(@NotNull PsiFile psiFile, @Nullable Editor editor) {
        super(psiFile, editor, new AsterStructureViewElement(psiFile));
    }

    @Override
    public Sorter @NotNull [] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        // 命名元素（函数、数据类型等）通常是叶子节点
        Object value = element.getValue();
        return value instanceof AsterNamedElement;
    }

    @Override
    protected Class<?> @NotNull [] getSuitableClasses() {
        return new Class<?>[]{AsterNamedElement.class};
    }
}
