package io.aster.idea.psi;

import com.intellij.psi.tree.IElementType;
import io.aster.idea.lang.AsterLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Aster PSI 复合元素类型
 * <p>
 * 用于表示 Aster 语言的 PSI 复合节点（非叶子节点）。
 */
public class AsterElementType extends IElementType {

    public AsterElementType(@NonNls @NotNull String debugName) {
        super(debugName, AsterLanguage.INSTANCE);
    }
}
