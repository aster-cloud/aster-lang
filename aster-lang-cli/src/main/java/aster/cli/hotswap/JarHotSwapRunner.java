package aster.cli.hotswap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * JAR 文件热插拔运行器，负责串行化运行、重载与资源管理。
 */
public final class JarHotSwapRunner implements AutoCloseable {

  private final Path jarPath;
  private HotSwapClassLoader currentLoader;
  private ExecutionSlot currentSlot;

  public JarHotSwapRunner(Path jarPath) {
    this.jarPath = jarPath;
  }

  /**
   * 启动指定主类的执行线程，若已有线程未结束则拒绝并提示调用方停止后再运行。
   */
  public synchronized void run(String mainClass, String[] args) throws RunnerException {
    if (currentSlot != null && !currentSlot.isDone()) {
      throw new RunnerException("已有实例正在运行，请先停止");
    }

    HotSwapClassLoader loader = ensureLoaderLocked();
    Class<?> targetClass = loadMainClass(loader, mainClass);
    Method mainMethod = resolveMainMethod(targetClass, mainClass);
    String[] safeArgs = args == null ? new String[0] : args.clone();

    Callable<Void> callable = createExecutionCallable(mainClass, mainMethod, safeArgs);
    ExecutionSlot slot = new ExecutionSlot(callable, loader, buildThreadName(mainClass));
    currentSlot = slot;
    slot.execute();
  }

  /**
   * 停止当前运行线程，通过中断触发退出，调用方可随后调用 waitForCompletion() 等待完成。
   */
  public synchronized void stop() {
    if (currentSlot != null && !currentSlot.isDone()) {
      currentSlot.cancel();
    }
  }

  /**
   * 等待当前执行完成，统一传播用户程序异常，取消操作将被静默视为正常返回。
   */
  public void waitForCompletion() throws RunnerException, InterruptedException {
    ExecutionSlot slot;
    synchronized (this) {
      slot = currentSlot;
    }
    if (slot == null) {
      return;
    }
    try {
      slot.waitForCompletion();
    } finally {
      synchronized (this) {
        if (currentSlot == slot && slot.isDone()) {
          currentSlot = null;
        }
      }
    }
  }

  /**
   * 重新加载 JAR 文件：先停止旧线程，再关闭旧加载器，最后创建新的加载器。
   */
  public synchronized void reload() throws RunnerException {
    ExecutionSlot slot = currentSlot;
    if (slot != null && !slot.isDone()) {
      slot.cancel();
    }
    waitForSlot(slot);
    closeCurrentLoaderLocked();
    currentLoader = createClassLoader();
  }

  @Override
  public synchronized void close() throws RunnerException {
    ExecutionSlot slot = currentSlot;
    if (slot != null && !slot.isDone()) {
      slot.cancel();
    }
    waitForSlot(slot);
    closeCurrentLoaderLocked();
  }

  private void waitForSlot(ExecutionSlot slot) throws RunnerException {
    if (slot == null) {
      currentSlot = null;
      return;
    }
    try {
      slot.waitForCompletion();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RunnerException("等待用户程序结束时被中断", e);
    }
    if (slot.isDone()) {
      currentSlot = null;
    }
  }

  private HotSwapClassLoader ensureLoaderLocked() throws RunnerException {
    if (currentLoader == null) {
      currentLoader = createClassLoader();
    }
    return currentLoader;
  }

  private HotSwapClassLoader createClassLoader() throws RunnerException {
    try {
      return new HotSwapClassLoader(jarPath);
    } catch (IOException e) {
      throw new RunnerException("无法加载 JAR 文件: " + jarPath, e);
    }
  }

  private void closeCurrentLoaderLocked() throws RunnerException {
    if (currentLoader == null) {
      return;
    }
    try {
      currentLoader.close();
    } catch (IOException e) {
      throw new RunnerException("关闭 ClassLoader 失败: " + jarPath, e);
    } finally {
      currentLoader = null;
    }
  }

  private Class<?> loadMainClass(ClassLoader loader, String mainClass) throws RunnerException {
    try {
      return Class.forName(mainClass, true, loader);
    } catch (ClassNotFoundException e) {
      throw new RunnerException("找不到主类: " + mainClass, e);
    }
  }

  private Method resolveMainMethod(Class<?> clazz, String mainClass) throws RunnerException {
    try {
      Method main = clazz.getMethod("main", String[].class);
      if (!Modifier.isStatic(main.getModifiers())) {
        throw new RunnerException("主类的 main 方法必须是静态方法: " + mainClass);
      }
      return main;
    } catch (NoSuchMethodException e) {
      throw new RunnerException("主类没有 main(String[]) 方法: " + mainClass, e);
    }
  }

  private Callable<Void> createExecutionCallable(String mainClass, Method mainMethod, String[] args) {
    return () -> {
      try {
        mainMethod.invoke(null, (Object) args);
        return null;
      } catch (IllegalAccessException e) {
        throw new RunnerException("无法访问 main 方法: " + mainClass, e);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        throw new RunnerException("用户程序执行失败: " + cause.getMessage(), cause);
      }
    };
  }

  private String buildThreadName(String mainClass) {
    String simple = mainClass.contains(".")
        ? mainClass.substring(mainClass.lastIndexOf('.') + 1)
        : mainClass;
    return "HotSwap-" + simple;
  }

  /**
   * 将运行态封装为原子槽位，FutureTask 保证 run/stop 同步。
   */
  private static final class ExecutionSlot {
    private final FutureTask<Void> task;
    private final Thread worker;

    ExecutionSlot(Callable<Void> callable, ClassLoader loader, String threadName) {
      this.task = new FutureTask<>(callable);
      this.worker = new Thread(task, threadName);
      this.worker.setContextClassLoader(loader);
    }

    void execute() {
      worker.start();
    }

    void cancel() {
      task.cancel(true);
    }

    boolean isDone() {
      return task.isDone();
    }

    void waitForCompletion() throws RunnerException, InterruptedException {
      try {
        task.get();
      } catch (CancellationException ignored) {
        // 取消视为正常完成
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RunnerException runnerException) {
          throw runnerException;
        }
        if (cause instanceof InvocationTargetException ite && ite.getCause() != null) {
          cause = ite.getCause();
        }
        throw new RunnerException("用户程序执行失败: " + cause.getMessage(), cause);
      }
    }
  }

  /**
   * 运行器统一异常类型。
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
