package io.aster.idea.psi;

import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * Aster 命名元素接口
 * <p>
 * 所有具有名称的 PSI 元素（如函数、数据类型、变量）的基接口。
 * 支持重命名重构和导航功能。
 */
public interface AsterNamedElement extends PsiNameIdentifierOwner {
}
