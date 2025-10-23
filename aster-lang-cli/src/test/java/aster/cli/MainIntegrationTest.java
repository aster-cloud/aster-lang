package aster.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainIntegrationTest {

  @BeforeAll
  static void buildTypeScriptArtifacts() throws IOException, InterruptedException {
    final Process process =
        new ProcessBuilder("npm", "run", "build")
            .directory(Path.of("").toAbsolutePath().normalize().toFile())
            .redirectErrorStream(true)
            .start();
    final int exit = process.waitFor();
    if (exit != 0) {
      final String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      fail("npm run build 失败:\n" + output);
    }
  }

  @Test
  void compileGeneratesClass(@TempDir Path tempDir) throws IOException {
    final Path outDir = tempDir.resolve("classes");
    Files.createDirectories(outDir);
    final CliResult result =
        runCli(
            "compile",
            "test/cnl/examples/hello.aster",
            "--output",
            outDir.toString());
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("编译完成"));
    try (var stream = Files.walk(outDir)) {
      assertTrue(
          stream.anyMatch(p -> p.toString().endsWith(".class")),
          "编译应生成至少一个 .class 文件");
    }
  }

  @Test
  void typecheckReportsError() {
    final CliResult result =
        runCli("typecheck", "test/cnl/examples/eff_violation_chain.aster");
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().toLowerCase().contains("error"));
  }

  @Test
  void versionOutputsCurrentVersion() {
    final CliResult result = runCli("version");
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Aster Lang Native CLI"));
  }

  private static CliResult runCli(String... args) {
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
    try {
      final int exitCode = Main.run(args);
      return new CliResult(
          exitCode,
          outBuffer.toString(StandardCharsets.UTF_8),
          errBuffer.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  private record CliResult(int exitCode, String stdout, String stderr) {}
}
