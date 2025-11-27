package io.aster.idea.lang;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 语言 Token 类型
 */
public class AsterTokenType extends IElementType {

    public AsterTokenType(@NotNull @NonNls String debugName) {
        super(debugName, AsterLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "AsterTokenType." + super.toString();
    }
}
