package io.aster.idea.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Aster 插件统一图标入口，避免多处重复加载资源。
 */
public final class AsterIcons {

  public static final Icon FILE = IconLoader.getIcon("/icons/aster.svg", AsterIcons.class);

  // Structure View 图标（使用 IntelliJ 内置图标）
  public static final Icon FUNCTION = AllIcons.Nodes.Function;
  public static final Icon DATA = AllIcons.Nodes.Class;
  public static final Icon ENUM = AllIcons.Nodes.Enum;
  public static final Icon VARIABLE = AllIcons.Nodes.Variable;
  public static final Icon WORKFLOW = AllIcons.Nodes.Artifact;
  public static final Icon TYPE = AllIcons.Nodes.Type;
  public static final Icon MODULE = AllIcons.Nodes.Module;

  private AsterIcons() {}
}
