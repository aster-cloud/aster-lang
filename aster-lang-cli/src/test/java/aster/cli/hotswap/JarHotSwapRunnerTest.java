package aster.cli.hotswap;

import static org.junit.jupiter.api.Assertions.*;

import aster.cli.hotswap.JarHotSwapRunner.RunnerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 验证热插拔运行器在并发、重载、取消与异常路径上的行为。
 */
class JarHotSwapRunnerTest {

  private static JavaCompiler compiler;

  @BeforeAll
  static void ensureCompiler() {
    compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "运行测试需要 JDK 提供的 JavaCompiler");
  }

  @AfterAll
  static void clearCompiler() {
    compiler = null;
  }

  @Test
  void concurrentRunRejected(@TempDir Path tempDir) throws Exception {
    Path jar = buildJar(tempDir, Map.of("BlockingMain", blockingMainSource()));

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jar)) {
      runner.run("BlockingMain", new String[0]);
      TimeUnit.MILLISECONDS.sleep(100);

      RunnerException ex = assertThrows(
          RunnerException.class,
          () -> runner.run("BlockingMain", new String[0]));
      assertTrue(ex.getMessage().contains("已有实例"));

      runner.stop();
      runner.waitForCompletion();
    }
  }

  @Test
  void watchModeReloadWorks(@TempDir Path tempDir) throws Exception {
    Path logFile = tempDir.resolve("run.log");
    Path jar = buildJar(tempDir, Map.of("Main", fileWriterMain("v1")));

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jar)) {
      runner.run("Main", new String[] {logFile.toString()});
      runner.waitForCompletion();

      Path updatedJar = buildJar(tempDir, Map.of("Main", fileWriterMain("v2")));
      Files.copy(updatedJar, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      runner.reload();
      runner.run("Main", new String[] {logFile.toString()});
      runner.waitForCompletion();
    }

    List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
    assertEquals(List.of("v1", "v2"), lines);
  }

  @Test
  void cancelDoesNotCrash(@TempDir Path tempDir) throws Exception {
    Path jar = buildJar(tempDir, Map.of("LongRunningMain", longRunningMainSource()));

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jar)) {
      runner.run("LongRunningMain", new String[0]);
      TimeUnit.MILLISECONDS.sleep(150);
      runner.stop();
      assertDoesNotThrow(runner::waitForCompletion);
    }
  }

  @Test
  void userExceptionPropagates(@TempDir Path tempDir) throws Exception {
    Path jar = buildJar(tempDir, Map.of("CrashMain", crashingMainSource()));

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jar)) {
      runner.run("CrashMain", new String[0]);
      RunnerException ex = assertThrows(RunnerException.class, runner::waitForCompletion);
      assertTrue(ex.getCause() instanceof IllegalStateException);
      assertEquals("boom", ex.getCause().getMessage());
    }
  }

  @Test
  void reloadResetsStaticState(@TempDir Path tempDir) throws Exception {
    Path jar = buildJar(tempDir, Map.of("ReloadMain", reloadableMainSource()));

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jar)) {
      runner.run("ReloadMain", new String[0]);
      runner.waitForCompletion();

      runner.reload();
      runner.run("ReloadMain", new String[0]);
      runner.waitForCompletion();
    }
  }

  private Path buildJar(Path tempDir, Map<String, String> sources) throws IOException {
    Path sourceDir = Files.createDirectories(tempDir.resolve("src-" + System.nanoTime()));
    Path classesDir = Files.createDirectories(tempDir.resolve("classes-" + System.nanoTime()));

    List<Path> sourceFiles = new ArrayList<>();
    for (Map.Entry<String, String> entry : sources.entrySet()) {
      Path file = sourceDir.resolve(entry.getKey() + ".java");
      Files.createDirectories(file.getParent());
      Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
      sourceFiles.add(file);
    }

    try (StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
      Iterable<? extends JavaFileObject> units =
          fileManager.getJavaFileObjectsFromPaths(sourceFiles);
      List<String> options = List.of("-d", classesDir.toString());
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, units);
      assertTrue(task.call(), "编译测试类失败");
    }

    Path jarPath = tempDir.resolve("app-" + System.nanoTime() + ".jar");
    try (var jarStream = new java.util.jar.JarOutputStream(Files.newOutputStream(jarPath))) {
      Files.walkFileTree(classesDir, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (!file.toString().endsWith(".class")) {
            return FileVisitResult.CONTINUE;
          }
          Path relative = classesDir.relativize(file);
          String entryName = relative.toString().replace('\\', '/');
          jarStream.putNextEntry(new java.util.jar.JarEntry(entryName));
          jarStream.write(Files.readAllBytes(file));
          jarStream.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      });
    }

    return jarPath;
  }

  private String blockingMainSource() {
    return """
        import java.util.concurrent.TimeUnit;

        public class BlockingMain {
          public static void main(String[] args) throws Exception {
            while (!Thread.currentThread().isInterrupted()) {
              TimeUnit.MILLISECONDS.sleep(50);
            }
          }
        }
        """;
  }

  private String fileWriterMain(String tag) {
    return ("""
        import java.nio.file.*;

        public class Main {
          public static void main(String[] args) throws Exception {
            Path file = Path.of(args[0]);
            Files.writeString(
                file,
                "%s" + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
          }
        }
        """).formatted(tag);
  }

  private String longRunningMainSource() {
    return """
        public class LongRunningMain {
          public static void main(String[] args) throws Exception {
            try {
              Thread.sleep(5_000);
            } catch (InterruptedException ignored) {
              Thread.currentThread().interrupt();
            }
          }
        }
        """;
  }

  private String crashingMainSource() {
    return """
        public class CrashMain {
          public static void main(String[] args) {
            throw new IllegalStateException("boom");
          }
        }
        """;
  }

  private String reloadableMainSource() {
    return """
        public class ReloadMain {
          private static int COUNTER = 0;

          public static void main(String[] args) {
            if (COUNTER > 0) {
              throw new IllegalStateException("static state leak");
            }
            COUNTER++;
          }
        }
        """;
  }
}
