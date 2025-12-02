package io.aster.idea.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import io.aster.idea.icons.AsterIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * 定义 .aster 文件的基本属性，供 IntelliJ 文件类型框架识别。
 */
public final class AsterFileType extends LanguageFileType {

  public static final AsterFileType INSTANCE = new AsterFileType();

  private AsterFileType() {
    super(AsterLanguage.INSTANCE);
  }

  @Override
  public @NonNls @NotNull String getName() {
    return "Aster";
  }

  @Override
  public @NotNull @NlsContexts.Label String getDescription() {
    return "Aster DSL 源文件";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "aster";
  }

  @Override
  public @Nullable Icon getIcon() {
    return AsterIcons.FILE;
  }
}
