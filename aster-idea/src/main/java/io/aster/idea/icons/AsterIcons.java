package io.aster.idea.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Aster 插件统一图标入口，避免多处重复加载资源。
 */
public final class AsterIcons {

  public static final Icon FILE = IconLoader.getIcon("/icons/aster.svg", AsterIcons.class);

  private AsterIcons() {}
}
