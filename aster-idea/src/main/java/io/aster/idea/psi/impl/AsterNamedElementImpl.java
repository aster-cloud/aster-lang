package io.aster.idea.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterFileType;
import io.aster.idea.lang.AsterTokenTypes;
import io.aster.idea.psi.AsterNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aster 命名元素基础实现
 */
@SuppressWarnings("serial")
public abstract class AsterNamedElementImpl extends ASTWrapperPsiElement implements AsterNamedElement {

    public AsterNamedElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 查找 IDENT 或 TYPE_IDENT 子节点
        ASTNode identNode = getNode().findChildByType(AsterTokenTypes.IDENT);
        if (identNode == null) {
            identNode = getNode().findChildByType(AsterTokenTypes.TYPE_IDENT);
        }
        return identNode != null ? identNode.getPsi() : null;
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getText() : null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        PsiElement identifier = getNameIdentifier();
        if (identifier == null) {
            throw new IncorrectOperationException("Cannot rename: no name identifier found");
        }

        // 创建临时文件来生成新的标识符节点
        // 根据名称类型选择合适的模板：
        // - 简单标识符：使用 let 语句
        // - 带点的限定名称：使用 import 语句
        String tempCode;
        if (name.contains(".")) {
            // 使用 import 语句生成带点的限定名称
            tempCode = "import " + name;
        } else {
            // 使用 let 语句生成简单标识符
            tempCode = "let " + name + " = 1";
        }

        PsiFile tempFile = PsiFileFactory.getInstance(getProject())
            .createFileFromText("_temp_.aster", AsterFileType.INSTANCE, tempCode);

        // 从临时文件中找到对应的标识符节点
        PsiElement newIdentifier = findIdentifierInFile(tempFile);
        if (newIdentifier == null) {
            throw new IncorrectOperationException("Cannot create new identifier element");
        }

        // 替换旧节点
        identifier.replace(newIdentifier);
        return this;
    }

    /**
     * 在 PSI 文件中查找第一个 IDENT 或 TYPE_IDENT 节点
     */
    private @Nullable PsiElement findIdentifierInFile(@NotNull PsiFile file) {
        // 深度优先遍历查找标识符节点
        return findIdentifierRecursive(file);
    }

    /**
     * 递归查找标识符节点
     */
    private @Nullable PsiElement findIdentifierRecursive(@NotNull PsiElement element) {
        ASTNode node = element.getNode();
        if (node != null) {
            IElementType type = node.getElementType();
            if (type == AsterTokenTypes.IDENT || type == AsterTokenTypes.TYPE_IDENT) {
                return element;
            }
        }
        for (PsiElement child : element.getChildren()) {
            PsiElement found = findIdentifierRecursive(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextOffset() : super.getTextOffset();
    }
}
