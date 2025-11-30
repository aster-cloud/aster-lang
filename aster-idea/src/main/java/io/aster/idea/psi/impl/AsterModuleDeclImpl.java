package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import io.aster.idea.lang.AsterFileType;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aster 模块声明 PSI 元素
 * <p>
 * 支持两种语法：
 * - 自然语言语法: This module is name.path.
 * - 传统语法: module name.path
 * <p>
 * 模块名可以是限定名称（如 demo.workflow.linear），
 * 由多个标识符和点组成。
 */
@SuppressWarnings("serial")
public class AsterModuleDeclImpl extends AsterNamedElementImpl {

    /**
     * 自然语言语法中的关键字，需要在查找模块名时跳过
     */
    private static final Set<String> NATURAL_KEYWORDS = Set.of(
        "this", "module", "is"
    );

    public AsterModuleDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 返回第一个非关键字标识符作为名称标识符（用于导航）
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);
        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !NATURAL_KEYWORDS.contains(text.toLowerCase())) {
                return child.getPsi();
            }
        }
        return null;
    }

    /**
     * 获取完整的模块限定名称（如 demo.workflow.linear）
     */
    @Override
    public String getName() {
        List<String> parts = new ArrayList<>();
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);

        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !NATURAL_KEYWORDS.contains(text.toLowerCase())) {
                parts.add(text);
            }
        }

        return parts.isEmpty() ? null : String.join(".", parts);
    }

    /**
     * 获取模块名中所有标识符节点（用于重命名）
     */
    private List<ASTNode> getQualifiedNameNodes() {
        List<ASTNode> nodes = new ArrayList<>();
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);

        for (ASTNode child : getNode().getChildren(identSet)) {
            String text = child.getText();
            if (text != null && !NATURAL_KEYWORDS.contains(text.toLowerCase())) {
                nodes.add(child);
            }
        }
        return nodes;
    }

    /**
     * 获取模块名中所有点节点（用于重命名）
     */
    private List<ASTNode> getDotNodes() {
        List<ASTNode> nodes = new ArrayList<>();
        for (ASTNode child : getNode().getChildren(TokenSet.create(AsterTokenTypes.DOT))) {
            nodes.add(child);
        }
        return nodes;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        List<ASTNode> identNodes = getQualifiedNameNodes();
        if (identNodes.isEmpty()) {
            throw new IncorrectOperationException("Cannot rename: no name identifier found");
        }

        // 解析新名称的各个部分
        String[] newParts = name.split("\\.");
        String[] oldParts = identNodes.stream()
            .map(ASTNode::getText)
            .toArray(String[]::new);

        // 如果段数相同，逐个替换标识符
        if (newParts.length == oldParts.length) {
            for (int i = 0; i < newParts.length; i++) {
                if (!newParts[i].equals(oldParts[i])) {
                    replaceIdentifier(identNodes.get(i), newParts[i]);
                }
            }
            return this;
        }

        // 段数不同，需要完整重建模块名
        // 创建临时文件来生成新的模块名节点
        String tempCode = "module " + name;
        PsiFile tempFile = PsiFileFactory.getInstance(getProject())
            .createFileFromText("_temp_.aster", AsterFileType.INSTANCE, tempCode);

        // 从临时文件中获取模块声明
        PsiElement tempModuleDecl = findModuleDecl(tempFile);
        if (tempModuleDecl == null) {
            throw new IncorrectOperationException("Cannot create new module declaration");
        }

        // 获取新模块声明中的所有标识符和点节点
        AsterModuleDeclImpl newModuleImpl = (AsterModuleDeclImpl) tempModuleDecl;
        List<ASTNode> newIdentNodes = newModuleImpl.getQualifiedNameNodes();
        List<ASTNode> newDotNodes = newModuleImpl.getDotNodes();

        if (newIdentNodes.isEmpty()) {
            throw new IncorrectOperationException("Cannot parse new module name");
        }

        // 获取当前模块声明中需要替换的范围
        ASTNode firstIdent = identNodes.get(0);
        ASTNode lastIdent = identNodes.get(identNodes.size() - 1);

        // 替换第一个标识符
        ASTNode newFirst = newIdentNodes.get(0).copyElement();
        getNode().replaceChild(firstIdent, newFirst);

        // 删除旧的点和后续标识符
        List<ASTNode> oldDotNodes = getDotNodes();
        for (ASTNode dotNode : oldDotNodes) {
            getNode().removeChild(dotNode);
        }
        for (int i = 1; i < identNodes.size(); i++) {
            getNode().removeChild(identNodes.get(i));
        }

        // 添加新的点和标识符
        ASTNode insertAfter = newFirst;
        for (int i = 0; i < newDotNodes.size(); i++) {
            ASTNode newDot = newDotNodes.get(i).copyElement();
            getNode().addChild(newDot, insertAfter.getTreeNext());
            insertAfter = newDot;

            if (i + 1 < newIdentNodes.size()) {
                ASTNode newIdent = newIdentNodes.get(i + 1).copyElement();
                getNode().addChild(newIdent, insertAfter.getTreeNext());
                insertAfter = newIdent;
            }
        }

        return this;
    }

    /**
     * 替换单个标识符节点
     */
    private void replaceIdentifier(ASTNode identNode, String newName) throws IncorrectOperationException {
        // 创建临时文件生成新标识符
        String tempCode = "let " + newName + " = 1";
        PsiFile tempFile = PsiFileFactory.getInstance(getProject())
            .createFileFromText("_temp_.aster", AsterFileType.INSTANCE, tempCode);

        PsiElement newIdent = findFirstIdentifier(tempFile);
        if (newIdent == null) {
            throw new IncorrectOperationException("Cannot create new identifier");
        }

        identNode.getPsi().replace(newIdent);
    }

    /**
     * 在 PSI 文件中查找模块声明
     */
    private @Nullable PsiElement findModuleDecl(@NotNull PsiFile file) {
        for (PsiElement child : file.getChildren()) {
            if (child instanceof AsterModuleDeclImpl) {
                return child;
            }
        }
        return null;
    }

    /**
     * 在 PSI 文件中查找第一个标识符
     */
    private @Nullable PsiElement findFirstIdentifier(@NotNull PsiElement element) {
        ASTNode node = element.getNode();
        if (node != null) {
            IElementType type = node.getElementType();
            if (type == AsterTokenTypes.IDENT || type == AsterTokenTypes.TYPE_IDENT) {
                return element;
            }
        }
        for (PsiElement child : element.getChildren()) {
            PsiElement found = findFirstIdentifier(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
