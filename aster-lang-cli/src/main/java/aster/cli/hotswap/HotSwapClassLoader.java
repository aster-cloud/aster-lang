package aster.cli.hotswap;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * 隔离的类加载器，用于实现 JAR 文件的热插拔。
 * <p>
 * 每次重新加载时创建新的实例，旧实例会被 GC 回收，从而卸载旧版本的类。
 * <p>
 * <b>设计要点：</b>
 * <ul>
 *   <li>使用 null 作为父加载器，实现完全隔离（除了 JDK 核心类）</li>
 *   <li>每个实例只加载一个 JAR 文件</li>
 *   <li>实现 Autocloseable 确保资源正确释放</li>
 * </ul>
 */
public final class HotSwapClassLoader extends URLClassLoader {

  /**
   * 创建一个加载指定 JAR 文件的类加载器。
   *
   * @param jarPath JAR 文件路径
   * @throws IOException 如果 JAR 文件不存在或无法访问
   */
  public HotSwapClassLoader(Path jarPath) throws IOException {
    super(
        new URL[] {jarPath.toUri().toURL()},
        // 使用 null 作为父加载器，实现类隔离
        // 这样每个 HotSwapClassLoader 实例都有独立的类命名空间
        null);
  }

  /**
   * 获取加载器的描述信息（用于调试）。
   */
  @Override
  public String toString() {
    return "HotSwapClassLoader@" + Integer.toHexString(hashCode()) + "[" + getURLs()[0] + "]";
  }
}
