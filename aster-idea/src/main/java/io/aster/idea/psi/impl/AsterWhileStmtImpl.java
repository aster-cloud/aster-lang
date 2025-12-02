package io.aster.idea.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Aster While 循环语句 PSI 元素
 * <p>
 * 语法: while condition:
 *         body
 * <p>
 * While 语句没有命名元素，使用简单的 PSI 包装
 */
@SuppressWarnings("serial")
public class AsterWhileStmtImpl extends ASTWrapperPsiElement {

    public AsterWhileStmtImpl(@NotNull ASTNode node) {
        super(node);
    }
}
