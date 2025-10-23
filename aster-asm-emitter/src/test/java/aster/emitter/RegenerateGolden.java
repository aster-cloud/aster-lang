package aster.emitter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * 手动工具：重新生成 golden 基线文件
 * 使用方法：在 IDE 中运行此 main 方法，或通过 Gradle 执行
 */
public class RegenerateGolden {

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("用法: RegenerateGolden <scenario> <input-json-path>");
      System.err.println("示例: RegenerateGolden set_statement test/cnl/examples/set_statement_core.json");
      System.exit(1);
    }

    String scenario = args[0];
    String inputPath = args[1];

    Path projectRoot = findProjectRoot();
    Path inputFile = projectRoot.resolve(inputPath);
    Path goldenDir = projectRoot.resolve("aster-asm-emitter/src/test/resources/golden-classes/" + scenario);
    Path tempOutput = Files.createTempDirectory("aster-golden-regen");

    System.out.println("场景: " + scenario);
    System.out.println("输入: " + inputFile);
    System.out.println("Golden 目录: " + goldenDir);
    System.out.println("临时输出: " + tempOutput);

    // 调用 Main 生成字节码
    byte[] payload = Files.readAllBytes(inputFile);
    InputStream original = System.in;
    try {
      System.setIn(new ByteArrayInputStream(payload));
      Main.main(new String[]{tempOutput.toString()});
    } finally {
      System.setIn(original);
    }

    // 清空现有 golden 目录
    if (Files.exists(goldenDir)) {
      try (Stream<Path> paths = Files.walk(goldenDir)) {
        paths.sorted((a, b) -> b.compareTo(a))
             .filter(p -> !p.equals(goldenDir))
             .forEach(p -> {
               try {
                 Files.deleteIfExists(p);
               } catch (Exception ignored) {}
             });
      }
    } else {
      Files.createDirectories(goldenDir);
    }

    // 复制生成的文件到 golden 目录
    try (Stream<Path> paths = Files.walk(tempOutput)) {
      paths.filter(Files::isRegularFile)
           .forEach(source -> {
             try {
               Path relative = tempOutput.relativize(source);
               Path target = goldenDir.resolve(relative);
               Files.createDirectories(target.getParent());
               Files.copy(source, target);
               System.out.println("✅ 复制: " + relative);
             } catch (Exception e) {
               throw new RuntimeException(e);
             }
           });
    }

    // 清理临时目录
    try (Stream<Path> paths = Files.walk(tempOutput)) {
      paths.sorted((a, b) -> b.compareTo(a))
           .forEach(p -> {
             try {
               Files.deleteIfExists(p);
             } catch (Exception ignored) {}
           });
    }

    System.out.println("\n🎉 Golden 基线已更新: " + scenario);
  }

  private static Path findProjectRoot() {
    Path cursor = Paths.get("").toAbsolutePath();
    while (cursor != null) {
      if (Files.exists(cursor.resolve("settings.gradle.kts"))) {
        return cursor;
      }
      cursor = cursor.getParent();
    }
    throw new IllegalStateException("无法定位项目根目录");
  }
}
