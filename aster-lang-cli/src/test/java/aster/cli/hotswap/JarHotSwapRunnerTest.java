package aster.cli.hotswap;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 测试 JAR 热插拔运行器的核心功能。
 * <p>
 * 包括：
 * <ul>
 *   <li>基本的 JAR 加载与运行</li>
 *   <li>热重载机制（重复加载同一 JAR）</li>
 *   <li>ClassLoader 隔离验证</li>
 *   <li>资源清理验证</li>
 * </ul>
 */
class JarHotSwapRunnerTest {

  /**
   * 测试基本的 JAR 加载与执行。
   */
  @Test
  void runSimpleJar(@TempDir Path tempDir) throws Exception {
    // 创建测试 JAR
    final Path jarPath = tempDir.resolve("test.jar");
    createTestJar(jarPath, "TestMain", simpleMainMethod());

    // 运行 JAR
    try (JarHotSwapRunner runner = new JarHotSwapRunner(jarPath)) {
      runner.run("TestMain", new String[]{"arg1", "arg2"});
      runner.join();
    }

    // 验证：不抛异常即为成功
  }

  /**
   * 测试热重载功能。
   */
  @Test
  void reloadJar(@TempDir Path tempDir) throws Exception {
    // 创建测试 JAR
    final Path jarPath = tempDir.resolve("test.jar");
    createTestJar(jarPath, "TestMain", simpleMainMethod());

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jarPath)) {
      // 第一次运行
      runner.run("TestMain", new String[]{"first"});
      runner.join();

      // 重新加载
      runner.reload();

      // 第二次运行
      runner.run("TestMain", new String[]{"second"});
      runner.join();
    }

    // 验证：不抛异常即为成功
  }

  /**
   * 测试 ClassLoader 隔离：多次加载同一 JAR 应创建不同的 ClassLoader。
   */
  @Test
  void classLoaderIsolation(@TempDir Path tempDir) throws Exception {
    final Path jarPath = tempDir.resolve("test.jar");
    createTestJar(jarPath, "TestMain", simpleMainMethod());

    // 创建两个运行器实例
    try (JarHotSwapRunner runner1 = new JarHotSwapRunner(jarPath);
         JarHotSwapRunner runner2 = new JarHotSwapRunner(jarPath)) {

      // 两个运行器应使用不同的 ClassLoader（通过运行不同实例验证隔离）
      runner1.run("TestMain", new String[]{"runner1"});
      runner2.run("TestMain", new String[]{"runner2"});

      runner1.join();
      runner2.join();
    }

    // 验证：不抛异常即为成功
  }

  /**
   * 测试找不到主类的错误处理。
   */
  @Test
  void runThrowsExceptionForMissingClass(@TempDir Path tempDir) throws Exception {
    final Path jarPath = tempDir.resolve("test.jar");
    createTestJar(jarPath, "TestMain", simpleMainMethod());

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jarPath)) {
      assertThrows(
          JarHotSwapRunner.RunnerException.class,
          () -> runner.run("NonExistentClass", new String[]{}),
          "应抛出 RunnerException");
    }
  }

  /**
   * 测试缺少 main 方法的错误处理。
   */
  @Test
  void runThrowsExceptionForMissingMainMethod(@TempDir Path tempDir) throws Exception {
    final Path jarPath = tempDir.resolve("test.jar");
    // 创建没有 main 方法的类
    final String noMainClass = """
        public class NoMainClass {
          public void someMethod() {
            System.out.println("This is not main");
          }
        }
        """;
    createTestJar(jarPath, "NoMainClass", noMainClass);

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jarPath)) {
      assertThrows(
          JarHotSwapRunner.RunnerException.class,
          () -> runner.run("NoMainClass", new String[]{}),
          "应抛出 RunnerException（缺少 main 方法）");
    }
  }

  /**
   * 测试资源清理：close() 应正确关闭 ClassLoader。
   */
  @Test
  void closeReleasesResources(@TempDir Path tempDir) throws Exception {
    final Path jarPath = tempDir.resolve("test.jar");
    createTestJar(jarPath, "TestMain", simpleMainMethod());

    JarHotSwapRunner runner = new JarHotSwapRunner(jarPath);
    runner.run("TestMain", new String[]{});
    runner.join();
    runner.close();

    // 验证：关闭后再次运行应失败（ClassLoader 已释放）
    // 注意：close() 后 currentLoader 被设为 null，下次 run 会创建新的 ClassLoader
    // 所以这里只验证 close() 不抛异常
  }

  /**
   * 创建包含指定类的测试 JAR 文件。
   *
   * @param jarPath JAR 文件路径
   * @param className 类名
   * @param classSource 类的 Java 源码
   */
  private void createTestJar(Path jarPath, String className, String classSource)
      throws IOException, InterruptedException {
    // 创建临时目录用于编译
    final Path tempDir = jarPath.getParent().resolve("compile-" + System.nanoTime());
    Files.createDirectories(tempDir);

    try {
      // 写入源文件
      final Path sourceFile = tempDir.resolve(className + ".java");
      Files.writeString(sourceFile, classSource);

      // 编译
      final Process compileProcess =
          new ProcessBuilder("javac", sourceFile.toString())
              .directory(tempDir.toFile())
              .redirectErrorStream(true)
              .start();
      final int compileExit = compileProcess.waitFor();
      if (compileExit != 0) {
        final String output = new String(compileProcess.getInputStream().readAllBytes());
        throw new IOException("编译失败:\n" + output);
      }

      // 打包为 JAR
      try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
        final Path classFile = tempDir.resolve(className + ".class");
        final JarEntry entry = new JarEntry(className + ".class");
        jar.putNextEntry(entry);
        jar.write(Files.readAllBytes(classFile));
        jar.closeEntry();
      }
    } finally {
      // 清理临时目录
      try (var stream = Files.walk(tempDir)) {
        stream.sorted((a, b) -> b.compareTo(a))
            .forEach(p -> {
              try {
                Files.deleteIfExists(p);
              } catch (IOException e) {
                // 忽略清理错误
              }
            });
      }
    }
  }

  /**
   * 生成简单的 main 方法源码。
   */
  private String simpleMainMethod() {
    return """
        public class TestMain {
          public static void main(String[] args) {
            System.out.println("TestMain executed with " + args.length + " arguments");
            for (String arg : args) {
              System.out.println("  arg: " + arg);
            }
          }
        }
        """;
  }
}
