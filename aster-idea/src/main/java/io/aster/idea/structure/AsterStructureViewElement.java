package io.aster.idea.structure;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.aster.idea.icons.AsterIcons;
import io.aster.idea.psi.AsterElementTypes;
import io.aster.idea.psi.AsterFile;
import io.aster.idea.psi.AsterNamedElement;
import io.aster.idea.psi.impl.AsterDataDeclImpl;
import io.aster.idea.psi.impl.AsterEnumDeclImpl;
import io.aster.idea.psi.impl.AsterFuncDeclImpl;
import io.aster.idea.psi.impl.AsterLetStmtImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aster Structure View 元素
 * <p>
 * 表示 Structure View 中的单个元素（文件、函数、数据类型等）。
 */
public class AsterStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

    private final NavigatablePsiElement element;

    public AsterStructureViewElement(@NotNull PsiElement element) {
        this.element = (NavigatablePsiElement) element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        element.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return element.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element.canNavigateToSource();
    }

    @Override
    public @NotNull String getAlphaSortKey() {
        String name = element.getName();
        return name != null ? name : "";
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        if (element instanceof AsterFile) {
            return new PresentationData(
                ((AsterFile) element).getName(),
                null,
                AsterIcons.FILE,
                null
            );
        }

        if (element instanceof AsterNamedElement namedElement) {
            String name = namedElement.getName();
            Icon icon = getIconForElement(element);
            String locationString = getLocationString(element);

            return new PresentationData(
                name != null ? name : "<anonymous>",
                locationString,
                icon,
                null
            );
        }

        return new PresentationData(
            element.getText(),
            null,
            null,
            null
        );
    }

    /**
     * 获取元素的图标
     */
    private Icon getIconForElement(PsiElement element) {
        if (element instanceof AsterFuncDeclImpl) {
            return AsterIcons.FUNCTION;
        } else if (element instanceof AsterDataDeclImpl) {
            return AsterIcons.DATA;
        } else if (element instanceof AsterEnumDeclImpl) {
            return AsterIcons.ENUM;
        } else if (element instanceof AsterLetStmtImpl) {
            return AsterIcons.VARIABLE;
        }
        return AsterIcons.FILE;
    }

    /**
     * 获取元素的位置描述
     */
    private String getLocationString(PsiElement element) {
        if (element instanceof AsterFuncDeclImpl) {
            return "function";
        } else if (element instanceof AsterDataDeclImpl) {
            return "data";
        } else if (element instanceof AsterEnumDeclImpl) {
            return "enum";
        } else if (element instanceof AsterLetStmtImpl) {
            return "let";
        }
        return null;
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        if (element instanceof AsterFile) {
            // 收集文件中的所有命名元素
            List<TreeElement> children = new ArrayList<>();

            // 收集函数声明
            Collection<AsterFuncDeclImpl> funcs = PsiTreeUtil.findChildrenOfType(element, AsterFuncDeclImpl.class);
            for (AsterFuncDeclImpl func : funcs) {
                children.add(new AsterStructureViewElement(func));
            }

            // 收集数据类型声明
            Collection<AsterDataDeclImpl> datas = PsiTreeUtil.findChildrenOfType(element, AsterDataDeclImpl.class);
            for (AsterDataDeclImpl data : datas) {
                children.add(new AsterStructureViewElement(data));
            }

            // 收集枚举声明
            Collection<AsterEnumDeclImpl> enums = PsiTreeUtil.findChildrenOfType(element, AsterEnumDeclImpl.class);
            for (AsterEnumDeclImpl enumDecl : enums) {
                children.add(new AsterStructureViewElement(enumDecl));
            }

            // 收集顶层 let 语句（模块级变量）
            Collection<AsterLetStmtImpl> lets = PsiTreeUtil.findChildrenOfType(element, AsterLetStmtImpl.class);
            for (AsterLetStmtImpl let : lets) {
                // 仅包含直接子元素（避免函数内的 let）
                if (let.getParent() instanceof AsterFile || isTopLevelLet(let)) {
                    children.add(new AsterStructureViewElement(let));
                }
            }

            return children.toArray(TreeElement.EMPTY_ARRAY);
        }

        return TreeElement.EMPTY_ARRAY;
    }

    /**
     * 判断是否为顶层 let 语句
     */
    private boolean isTopLevelLet(AsterLetStmtImpl let) {
        PsiElement parent = let.getParent();
        while (parent != null && !(parent instanceof AsterFile)) {
            // 如果在函数或其他块内，则不是顶层
            if (parent.getNode().getElementType() == AsterElementTypes.FUNC_DECL ||
                parent.getNode().getElementType() == AsterElementTypes.BLOCK) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }
}
