package aster.cli;

import static org.junit.jupiter.api.Assertions.*;

import aster.cli.TypeScriptBridge.BridgeException;
import aster.cli.TypeScriptBridge.Diagnostic;
import aster.cli.TypeScriptBridge.ProcessFactory;
import aster.cli.TypeScriptBridge.Result;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TypeScriptBridgeTest {
  private static Path projectRoot;
  private static Path npmExecutable;
  private static Path nodeExecutable;

  @BeforeAll
  static void resolveExecutables() throws BridgeException {
    projectRoot = Path.of("").toAbsolutePath().normalize();
    npmExecutable = TypeScriptBridge.locateExecutable("npm");
    nodeExecutable = TypeScriptBridge.locateExecutable("node");
  }

  @Test
  void executeCommandReturnsStdout() throws Exception {
    final FakeProcess process = new FakeProcess(0, "ok", "");
    final ProcessFactory factory =
        (command, env, dir) -> {
          assertTrue(command.contains("native:cli:class"));
          assertEquals(projectRoot, dir);
          return process;
        };
    final TypeScriptBridge bridge =
        new TypeScriptBridge(projectRoot, Duration.ofSeconds(5), factory, npmExecutable,
            nodeExecutable);
    final Result result = bridge.executeCommand("native:cli:class", List.of("file.aster"));
    assertEquals(0, result.exitCode());
    assertEquals("ok", result.stdout());
    assertTrue(result.diagnostics().isEmpty());
  }

  @Test
  void executeCommandParsesDiagnostics() throws Exception {
    final FakeProcess process =
        new FakeProcess(1, "", "file.aster:12:5: error: something bad happened");
    final TypeScriptBridge bridge =
        new TypeScriptBridge(
            projectRoot,
            Duration.ofSeconds(5),
            (command, env, dir) -> process,
            npmExecutable,
            nodeExecutable);
    final Result result =
        bridge.executeCommand("native:cli:typecheck", List.of("file.aster"), Map.of());
    assertEquals(1, result.exitCode());
    assertEquals(1, result.diagnostics().size());
    final Diagnostic diagnostic = result.diagnostics().getFirst();
    assertEquals("file.aster", diagnostic.file().orElseThrow());
    assertEquals(12, diagnostic.line());
    assertEquals(5, diagnostic.column());
    assertEquals("error", diagnostic.severity());
  }

  @Test
  void executeCommandTimesOut() throws Exception {
    final FakeProcess process = FakeProcess.timeoutProcess();
    final TypeScriptBridge bridge =
        new TypeScriptBridge(
            projectRoot,
            Duration.ofMillis(50),
            (command, env, dir) -> process,
            npmExecutable,
            nodeExecutable);
    final BridgeException exception =
        assertThrows(
            BridgeException.class,
            () -> bridge.executeCommand("native:cli:class", List.of("file.aster")));
    assertTrue(exception.getMessage().contains("超时"));
    assertTrue(process.destroyCalled);
  }

  @Test
  void parseDiagnosticsSupportsSimpleFormat() throws Exception {
    final TypeScriptBridge bridge =
        new TypeScriptBridge(
            projectRoot,
            Duration.ofSeconds(5),
            (command, env, dir) -> new FakeProcess(1, "", ""),
            npmExecutable,
            nodeExecutable);
    final List<Diagnostic> diagnostics =
        bridge.parseDiagnostics("ERROR: violation detected");
    assertEquals(1, diagnostics.size());
    assertEquals("error", diagnostics.getFirst().severity());
  }

  private static final class FakeProcess extends Process {
    private final int exitCode;
    private final byte[] stdout;
    private final byte[] stderr;
    private boolean finished;
    private boolean forceTimeout;
    private boolean destroyed;
    private boolean destroyCalled;

    FakeProcess(int exitCode, String stdout, String stderr) {
      this.exitCode = exitCode;
      this.stdout = stdout.getBytes(StandardCharsets.UTF_8);
      this.stderr = stderr.getBytes(StandardCharsets.UTF_8);
    }

    static FakeProcess timeoutProcess() {
      final FakeProcess process = new FakeProcess(1, "", "");
      process.forceTimeout = true;
      return process;
    }

    @Override
    public OutputStream getOutputStream() {
      return OutputStream.nullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(stdout);
    }

    @Override
    public InputStream getErrorStream() {
      return new ByteArrayInputStream(stderr);
    }

    @Override
    public int waitFor() {
      finished = true;
      return exitCode;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
      if (forceTimeout) {
        return false;
      }
      finished = true;
      return true;
    }

    @Override
    public int exitValue() {
      if (!finished) {
        throw new IllegalThreadStateException();
      }
      return exitCode;
    }

    @Override
    public void destroy() {
      destroyCalled = true;
      destroyed = true;
    }

    @Override
    public Process destroyForcibly() {
      destroyed = true;
      return this;
    }

    @Override
    public boolean isAlive() {
      return !finished && !destroyed;
    }
  }
}
