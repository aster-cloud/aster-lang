package io.aster.idea.lang;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

/**
 * Aster 语言定义
 * <p>
 * 这是 IntelliJ 平台识别 Aster 语言的核心类。
 */
@SuppressWarnings("serial")
public final class AsterLanguage extends Language {

    public static final AsterLanguage INSTANCE = new AsterLanguage();

    private AsterLanguage() {
        super("Aster");
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aster";
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
