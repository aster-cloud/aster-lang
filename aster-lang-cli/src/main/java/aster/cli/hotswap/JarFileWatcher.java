package aster.cli.hotswap;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * JAR 文件监控器，监控文件变化并触发回调。
 * <p>
 * 使用 Java WatchService API 监控文件系统变化，支持修改、创建、删除事件。
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * JarFileWatcher watcher = new JarFileWatcher(
 *     Paths.get("app.jar"),
 *     (path) -> System.out.println("文件已修改: " + path)
 * );
 * watcher.start();
 * }</pre>
 */
public final class JarFileWatcher implements AutoCloseable {

  private final Path jarPath;
  private final Consumer<Path> onChangeCallback;
  private final WatchService watchService;
  private final Thread watchThread;
  private volatile boolean running = false;

  /**
   * 创建文件监控器。
   *
   * @param jarPath JAR 文件路径
   * @param onChangeCallback 文件变化时的回调函数
   * @throws IOException 如果无法创建 WatchService
   */
  public JarFileWatcher(Path jarPath, Consumer<Path> onChangeCallback) throws IOException {
    this.jarPath = jarPath.toAbsolutePath();
    this.onChangeCallback = onChangeCallback;
    this.watchService = FileSystems.getDefault().newWatchService();

    // 监控 JAR 文件所在的目录
    Path parentDir = this.jarPath.getParent();
    if (parentDir == null) {
      throw new IOException("无法获取文件的父目录: " + jarPath);
    }

    parentDir.register(
        watchService,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE);

    // 创建监控线程
    this.watchThread = new Thread(this::watchLoop, "JarFileWatcher-" + jarPath.getFileName());
    this.watchThread.setDaemon(true);
  }

  /**
   * 启动文件监控。
   */
  public void start() {
    if (!running) {
      running = true;
      watchThread.start();
      System.out.println("✓ 开始监控文件: " + jarPath);
    }
  }

  /**
   * 停止文件监控。
   */
  public void stop() {
    running = false;
    watchThread.interrupt();
  }

  /**
   * 监控循环。
   */
  private void watchLoop() {
    try {
      while (running) {
        WatchKey key;
        try {
          // 等待事件发生（最多1秒超时）
          key = watchService.poll(1, TimeUnit.SECONDS);
          if (key == null) {
            continue;
          }
        } catch (InterruptedException e) {
          break;
        }

        // 处理事件
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();

          // 忽略 OVERFLOW 事件
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          // 获取变化的文件名
          @SuppressWarnings("unchecked")
          WatchEvent<Path> ev = (WatchEvent<Path>) event;
          Path filename = ev.context();
          Path changedFile = jarPath.getParent().resolve(filename);

          // 只处理目标 JAR 文件的变化
          if (changedFile.equals(jarPath)) {
            System.out.println("检测到文件变化: " + kind.name() + " - " + changedFile);

            // 添加短暂延迟，避免文件正在写入时读取
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              break;
            }

            // 触发回调
            try {
              onChangeCallback.accept(changedFile);
            } catch (Exception e) {
              System.err.println("处理文件变化时发生错误: " + e.getMessage());
              e.printStackTrace();
            }
          }
        }

        // 重置 key 以继续接收事件
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    } catch (Exception e) {
      if (running) {
        System.err.println("文件监控发生错误: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  /**
   * 关闭监控器，释放资源。
   */
  @Override
  public void close() {
    stop();
    try {
      watchService.close();
    } catch (IOException e) {
      System.err.println("警告: 关闭 WatchService 时发生错误: " + e.getMessage());
    }
  }
}
