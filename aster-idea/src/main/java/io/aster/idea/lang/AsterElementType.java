package io.aster.idea.lang;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 语言 PSI 元素类型
 */
public class AsterElementType extends IElementType {

    public AsterElementType(@NotNull @NonNls String debugName) {
        super(debugName, AsterLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "AsterElementType." + super.toString();
    }
}
