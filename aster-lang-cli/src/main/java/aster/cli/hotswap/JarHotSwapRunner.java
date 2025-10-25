package aster.cli.hotswap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JAR 文件热插拔运行器，支持动态加载/卸载 JAR 文件。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *   <li>使用独立的 ClassLoader 加载 JAR</li>
 *   <li>支持运行时重新加载 JAR（热插拔）</li>
 *   <li>自动清理旧的 ClassLoader 实例</li>
 *   <li>线程安全的重载机制</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * JarHotSwapRunner runner = new JarHotSwapRunner(Paths.get("app.jar"));
 * runner.run("com.example.Main", new String[]{"arg1", "arg2"});
 *
 * // 修改 JAR 后重新加载
 * runner.reload();
 * runner.run("com.example.Main", new String[]{"arg1", "arg2"});
 * }</pre>
 */
public final class JarHotSwapRunner implements AutoCloseable {

  private final Path jarPath;
  private final AtomicReference<HotSwapClassLoader> currentLoader = new AtomicReference<>();
  private volatile Thread runningThread;

  /**
   * 创建运行器实例。
   *
   * @param jarPath JAR 文件路径
   */
  public JarHotSwapRunner(Path jarPath) {
    this.jarPath = jarPath;
  }

  /**
   * 运行 JAR 中的 main 方法。
   *
   * @param mainClass 主类的全限定名（如 "com.example.Main"）
   * @param args 传递给 main 方法的参数
   * @throws RunnerException 如果运行失败
   */
  public void run(String mainClass, String[] args) throws RunnerException {
    try {
      // 确保 ClassLoader 已加载
      HotSwapClassLoader loader = getOrCreateLoader();

      // 加载主类
      Class<?> clazz = Class.forName(mainClass, true, loader);

      // 查找 main 方法
      Method mainMethod = clazz.getMethod("main", String[].class);

      // 在新线程中运行 main 方法
      runningThread = new Thread(() -> {
        try {
          mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
          System.err.println("运行 main 方法时发生错误: " + e.getMessage());
          e.printStackTrace();
        }
      }, "HotSwap-" + mainClass);

      runningThread.setContextClassLoader(loader);
      runningThread.start();

    } catch (ClassNotFoundException e) {
      throw new RunnerException("找不到主类: " + mainClass, e);
    } catch (NoSuchMethodException e) {
      throw new RunnerException("主类没有 main 方法: " + mainClass, e);
    } catch (Exception e) {
      throw new RunnerException("运行失败: " + e.getMessage(), e);
    }
  }

  /**
   * 等待当前运行的线程结束。
   *
   * @throws InterruptedException 如果等待被中断
   */
  public void join() throws InterruptedException {
    Thread thread = runningThread;
    if (thread != null && thread.isAlive()) {
      thread.join();
    }
  }

  /**
   * 重新加载 JAR 文件。
   * <p>
   * 创建新的 ClassLoader，旧的 ClassLoader 会在不再被引用时被 GC 回收。
   *
   * @throws RunnerException 如果重载失败
   */
  public void reload() throws RunnerException {
    try {
      // 关闭旧的 ClassLoader
      HotSwapClassLoader oldLoader = currentLoader.getAndSet(null);
      if (oldLoader != null) {
        try {
          oldLoader.close();
        } catch (IOException e) {
          System.err.println("警告: 关闭旧 ClassLoader 时发生错误: " + e.getMessage());
        }
      }

      // 创建新的 ClassLoader
      getOrCreateLoader();

      System.out.println("✓ 重新加载完成: " + jarPath);

    } catch (Exception e) {
      throw new RunnerException("重载失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取或创建 ClassLoader。
   */
  private HotSwapClassLoader getOrCreateLoader() throws RunnerException {
    HotSwapClassLoader loader = currentLoader.get();
    if (loader == null) {
      synchronized (this) {
        loader = currentLoader.get();
        if (loader == null) {
          try {
            loader = new HotSwapClassLoader(jarPath);
            currentLoader.set(loader);
          } catch (IOException e) {
            throw new RunnerException("无法加载 JAR 文件: " + jarPath, e);
          }
        }
      }
    }
    return loader;
  }

  /**
   * 关闭运行器，释放资源。
   */
  @Override
  public void close() {
    HotSwapClassLoader loader = currentLoader.getAndSet(null);
    if (loader != null) {
      try {
        loader.close();
      } catch (IOException e) {
        System.err.println("警告: 关闭 ClassLoader 时发生错误: " + e.getMessage());
      }
    }
  }

  /**
   * 运行器异常。
   */
  public static final class RunnerException extends Exception {
    private static final long serialVersionUID = 1L;

    public RunnerException(String message) {
      super(message);
    }

    public RunnerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
