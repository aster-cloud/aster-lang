package aster.emitter;

import aster.core.ir.CoreModel;
import aster.core.lowering.CoreLowering;
import aster.core.parser.AsterCustomLexer;
import aster.core.parser.AsterParser;
import aster.core.parser.AstBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * 轻量级 CLI：通过 Java 前端解析 .aster 文件并调用 ASM emitter 生成 JVM 类。
 *
 * <p>使用方式：</p>
 *
 * <pre>
 * ./gradlew :aster-asm-emitter:compileAster -Paster.source=path/to/file.aster [-Paster.outputDir=build/jvm-classes]
 * </pre>
 */
public final class CompileAsterCli {

  private CompileAsterCli() {}

  public static void main(String[] args) {
    try {
      new CompileAsterCli().run(args);
    } catch (CliException ex) {
      System.err.println(ex.getMessage());
      System.exit(ex.exitCode());
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private void run(String[] args) throws IOException {
    Path sourcePath = resolveSource(args);
    Path outputDir = resolveOutputDir(args);
    Files.createDirectories(outputDir);

    CoreModel.Module module = lowerSource(sourcePath);
    configureEmitterDefaults();
    var result = Main.compile(module, outputDir, Map.of());
    if (!result.success) {
      throw new CliException("编译失败: " + String.join("; ", result.errors), 1);
    }
    System.out.printf(
      Locale.ROOT,
      "✅ 已将 %s 编译到 %s%n",
      sourcePath.toAbsolutePath(),
      outputDir.toAbsolutePath()
    );
  }

  private Path resolveSource(String[] args) {
    String prop = System.getProperty("aster.source");
    if (prop != null && !prop.isBlank()) {
      return Paths.get(prop);
    }
    if (args.length > 0 && !args[0].startsWith("--")) {
      return Paths.get(args[0]);
    }
    throw new CliException("缺少 .aster 源文件路径，可通过 -Paster.source=... 或第一个参数提供。", 2);
  }

  private Path resolveOutputDir(String[] args) {
    String prop = System.getProperty("aster.outputDir");
    if (prop != null && !prop.isBlank()) {
      return Paths.get(prop);
    }
    for (String arg : args) {
      if (arg.startsWith("--out=")) {
        String path = arg.substring("--out=".length());
        if (!path.isBlank()) {
          return Paths.get(path);
        }
      }
    }
    return Paths.get("build/jvm-classes");
  }

  private CoreModel.Module lowerSource(Path sourcePath) throws IOException {
    if (!Files.exists(sourcePath)) {
      throw new CliException("文件不存在: " + sourcePath.toAbsolutePath(), 2);
    }
    var stream = CharStreams.fromPath(sourcePath);
    var lexer = new AsterCustomLexer(stream);
    var tokens = new CommonTokenStream(lexer);
    var parser = new AsterParser(tokens);
    var astModule = new AstBuilder().visitModule(parser.module());
    return new CoreLowering().lowerModule(astModule);
  }

  private void configureEmitterDefaults() {
    Main.DIAG_OVERLOAD = true;
    Main.NULL_STRICT = false;
    Main.NULL_POLICY_OVERRIDE.clear();
  }

  private static final class CliException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int exitCode;

    CliException(String message, int exitCode) {
      super(message);
      this.exitCode = exitCode;
    }

    int exitCode() {
      return exitCode;
    }
  }
}
