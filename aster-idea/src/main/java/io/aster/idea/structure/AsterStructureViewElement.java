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
import io.aster.idea.psi.impl.AsterModuleDeclImpl;
import io.aster.idea.psi.impl.AsterTypeAliasDeclImpl;
import io.aster.idea.psi.impl.AsterWorkflowStmtImpl;
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
        } else if (element instanceof AsterWorkflowStmtImpl) {
            return AsterIcons.WORKFLOW;
        } else if (element instanceof AsterTypeAliasDeclImpl) {
            return AsterIcons.TYPE;
        } else if (element instanceof AsterModuleDeclImpl) {
            return AsterIcons.MODULE;
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
        } else if (element instanceof AsterWorkflowStmtImpl) {
            return "workflow";
        } else if (element instanceof AsterTypeAliasDeclImpl) {
            return "type";
        } else if (element instanceof AsterModuleDeclImpl) {
            return "module";
        }
        return null;
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        if (element instanceof AsterFile file) {
            // 按源码顺序收集直接子元素中的顶层声明
            // 不递归到函数/workflow 内部，确保顺序与源码一致
            List<TreeElement> children = new ArrayList<>();

            for (PsiElement child : file.getChildren()) {
                // 只收集顶层声明类型
                if (isTopLevelDeclaration(child)) {
                    children.add(new AsterStructureViewElement(child));
                }
            }

            return children.toArray(TreeElement.EMPTY_ARRAY);
        }

        // 模块节点也应该可以展开，显示其内部的声明
        if (element instanceof AsterModuleDeclImpl moduleDecl) {
            List<TreeElement> children = new ArrayList<>();

            for (PsiElement child : moduleDecl.getChildren()) {
                // 收集模块内的声明（函数、数据类型、枚举等）
                if (isModuleChildDeclaration(child)) {
                    children.add(new AsterStructureViewElement(child));
                }
            }

            return children.toArray(TreeElement.EMPTY_ARRAY);
        }

        return TreeElement.EMPTY_ARRAY;
    }

    /**
     * 判断是否为模块内的子声明
     */
    private boolean isModuleChildDeclaration(PsiElement element) {
        return element instanceof AsterFuncDeclImpl ||
               element instanceof AsterDataDeclImpl ||
               element instanceof AsterEnumDeclImpl ||
               element instanceof AsterTypeAliasDeclImpl ||
               element instanceof AsterWorkflowStmtImpl;
    }

    /**
     * 判断是否为应该出现在结构视图中的顶层声明
     */
    private boolean isTopLevelDeclaration(PsiElement element) {
        return element instanceof AsterModuleDeclImpl ||
               element instanceof AsterFuncDeclImpl ||
               element instanceof AsterDataDeclImpl ||
               element instanceof AsterEnumDeclImpl ||
               element instanceof AsterTypeAliasDeclImpl ||
               element instanceof AsterWorkflowStmtImpl ||
               (element instanceof AsterLetStmtImpl && isTopLevelLet((AsterLetStmtImpl) element));
    }

    /**
     * 判断是否为顶层 let 语句
     * <p>
     * 检查 let 语句是否在文件或模块的顶层，而不是嵌套在函数、workflow、
     * 循环、match 分支等局部作用域内。
     */
    private boolean isTopLevelLet(AsterLetStmtImpl let) {
        PsiElement parent = let.getParent();
        while (parent != null && !(parent instanceof AsterFile)) {
            // 如果在任何局部作用域边界内，则不是顶层
            if (isLocalScopeBoundary(parent)) {
                return false;
            }
            // 模块声明内的 let 也算顶层
            if (parent instanceof AsterModuleDeclImpl) {
                return true;
            }
            parent = parent.getParent();
        }
        return parent instanceof AsterFile;
    }

    /**
     * 判断元素是否为局部作用域边界
     * <p>
     * 这些元素内部的声明不应该出现在结构视图的顶层
     */
    private boolean isLocalScopeBoundary(PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        var elementType = element.getNode().getElementType();
        return elementType == AsterElementTypes.FUNC_DECL ||
               elementType == AsterElementTypes.BLOCK ||
               elementType == AsterElementTypes.WORKFLOW_STMT ||
               elementType == AsterElementTypes.WORKFLOW_STEP ||
               elementType == AsterElementTypes.MATCH_CASE ||
               elementType == AsterElementTypes.FOR_STMT ||
               elementType == AsterElementTypes.WHILE_STMT ||
               elementType == AsterElementTypes.IF_STMT ||
               elementType == AsterElementTypes.LAMBDA_EXPR;
    }
}
