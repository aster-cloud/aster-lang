package io.aster.idea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import io.aster.idea.lang.AsterKeywords;
import io.aster.idea.lang.AsterTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aster 导入声明 PSI 元素
 * <p>
 * 支持两种语法:
 * - 自然语言: Use module.path. 或 Use module.path as alias.
 * - 传统语法: import module.path as alias
 * <p>
 * 导入声明的"名称"优先级：
 * 1. 如果有 'as' 关键字，返回其后的别名
 * 2. 否则返回模块路径的最后一段（作为默认别名）
 */
@SuppressWarnings("serial")
public class AsterImportDeclImpl extends AsterNamedElementImpl {

    /**
     * 导入关键字，需要在查找模块名时跳过（使用统一定义的子集）
     */
    private static final Set<String> IMPORT_KEYWORDS = Set.of("use", "import");

    public AsterImportDeclImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        // 导入声明的"名称"是别名或模块路径最后一段
        // 优先查找 'as' 关键字后的别名
        TokenSet identSet = TokenSet.create(AsterTokenTypes.IDENT, AsterTokenTypes.TYPE_IDENT);
        ASTNode[] identNodes = getNode().getChildren(identSet);

        // 收集非关键字的标识符
        List<ASTNode> modulePathNodes = new ArrayList<>();
        boolean foundAs = false;
        ASTNode aliasNode = null;

        for (ASTNode child : identNodes) {
            String text = child.getText();
            if (text == null) continue;

            String lower = text.toLowerCase();

            // 跳过 import/use 关键字
            if (IMPORT_KEYWORDS.contains(lower)) {
                continue;
            }

            // 检测 'as' 关键字
            if ("as".equals(lower)) {
                foundAs = true;
                continue;
            }

            // 如果已经找到 'as'，下一个标识符就是别名
            if (foundAs) {
                aliasNode = child;
                break;
            }

            // 收集模块路径的各段
            modulePathNodes.add(child);
        }

        // 优先返回别名
        if (aliasNode != null) {
            return aliasNode.getPsi();
        }

        // 没有别名，返回模块路径的最后一段
        if (!modulePathNodes.isEmpty()) {
            return modulePathNodes.get(modulePathNodes.size() - 1).getPsi();
        }

        return null;
    }

    // setName() 继承自 AsterNamedElementImpl，支持重命名
}
