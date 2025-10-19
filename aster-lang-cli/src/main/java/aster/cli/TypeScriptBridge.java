package aster.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 封装对 TypeScript CLI 的调用逻辑，通过 npm run 子进程完成实际编译/类型检查工作。
 * 该类负责：
 * <ul>
 *   <li>查找 node/npm 可执行文件</li>
 *   <li>设置工作目录为仓库根目录</li>
 *   <li>处理标准输出、错误输出与超时</li>
 *   <li>解析诊断信息，供 CLI 主流程使用</li>
 * </ul>
 */
public final class TypeScriptBridge {
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
  private static final Pattern DIAGNOSTIC_PATTERN =
      Pattern.compile("^(?<file>[^:]+):(\\s*)(?<line>\\d+):(\\s*)(?<column>\\d+):\\s*(?<level>error|warning|info)\\s*:\\s*(?<message>.+)$");
  private static final Pattern SIMPLE_PATTERN =
      Pattern.compile("^(ERROR|WARN|INFO):\\s*(?<message>.+)$");

  private final Path projectRoot;
  private final Path npmExecutable;
  private final Path nodeExecutable;
  private final Duration timeout;
  private final ProcessFactory processFactory;
  private final ExecutorService ioExecutor;
  private final List<Process> activeProcesses =
      Collections.synchronizedList(new ArrayList<>());

  /**
   * 默认构造函数，使用 60 秒超时与真实进程工厂。
   *
   * @throws BridgeException 当无法定位仓库根目录或 npm 可执行文件不存在时抛出
   */
  public TypeScriptBridge() throws BridgeException {
    this(
        findProjectRoot(),
        DEFAULT_TIMEOUT,
        new DefaultProcessFactory(),
        locateExecutable("npm"),
        locateExecutable("node"));
  }

  /**
   * 提供注入点便于测试。
   */
  TypeScriptBridge(
      Path projectRoot,
      Duration timeout,
      ProcessFactory processFactory,
      Path npmExecutable,
      Path nodeExecutable)
      throws BridgeException {
    this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
    this.timeout = Objects.requireNonNull(timeout, "timeout");
    this.processFactory = Objects.requireNonNull(processFactory, "processFactory");
    this.npmExecutable = Objects.requireNonNull(npmExecutable, "npmExecutable").toAbsolutePath();
    this.nodeExecutable = Objects.requireNonNull(nodeExecutable, "nodeExecutable").toAbsolutePath();
    ensureExecutableAvailable(this.npmExecutable);
    ensureExecutableAvailable(this.nodeExecutable);
    this.ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownGracefully));
  }

  /**
   * 执行 npm run 命令，继承全部环境变量并允许额外覆盖。
   *
   * @param command npm 脚本名称
   * @param args 传递给脚本的参数（会放在 -- 之后）
   * @param envOverrides 额外的环境变量覆盖
   * @return 结果对象，包含退出码与输出
   * @throws BridgeException 当命令超时或执行异常时抛出
   */
  public Result executeCommand(String command, List<String> args, Map<String, String> envOverrides)
      throws BridgeException {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(args, "args");
    Objects.requireNonNull(envOverrides, "envOverrides");

    final List<String> fullCommand = new ArrayList<>();
    fullCommand.add(npmExecutable.toString());
    fullCommand.add("run");
    fullCommand.add(command);
    fullCommand.add("--");
    fullCommand.addAll(args);

    final Map<String, String> environment =
        new java.util.HashMap<>(System.getenv());
    environment.putAll(envOverrides);

    final Process process;
    try {
      process = processFactory.start(fullCommand, environment, projectRoot);
    } catch (IOException e) {
      throw new BridgeException(
          "执行命令失败: %s".formatted(String.join(" ", fullCommand)), e);
    }

    activeProcesses.add(process);

    final CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
    final CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

    boolean finished;
    try {
      finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new BridgeException("命令在等待过程中被中断", e);
    }

    if (!finished) {
      process.destroy();
      try {
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
          process.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
      }
      throw new BridgeException(
          "命令执行超时（%d 秒）: %s".formatted(timeout.toSeconds(), command));
    }

    final int exitCode = process.exitValue();
    activeProcesses.remove(process);

    final String stdout = stdoutFuture.join();
    final String stderr = stderrFuture.join();
    final List<Diagnostic> diagnostics =
        parseDiagnostics(stdout + System.lineSeparator() + stderr);
    return new Result(exitCode, stdout, stderr, diagnostics);
  }

  /**
   * executeCommand 的便捷重载，无额外环境变量。
   */
  public Result executeCommand(String command, List<String> args) throws BridgeException {
    return executeCommand(command, args, Map.of());
  }

  /**
   * 在 PATH 中查找 node 可执行文件。
   */
  public Path findNodeExecutable() {
    return nodeExecutable;
  }

  /**
   * 在 PATH 中查找 npm 可执行文件。
   */
  public Path findNpmExecutable() {
    return npmExecutable;
  }

  /**
   * 解析诊断信息，支持两种输出格式：
   * <pre>
   * file.aster:12:5: error: message
   * ERROR: message
   * </pre>
   *
   * @param combinedOutput stderr/stdout 聚合字符串
   * @return 诊断列表
   */
  public List<Diagnostic> parseDiagnostics(String combinedOutput) {
    if (combinedOutput == null || combinedOutput.isBlank()) {
      return List.of();
    }
    final String[] lines = combinedOutput.split("\\R");
    final List<Diagnostic> diagnostics = new ArrayList<>();
    for (String line : lines) {
      final Matcher detailed = DIAGNOSTIC_PATTERN.matcher(line.trim());
      if (detailed.matches()) {
        diagnostics.add(
            Diagnostic.fromNullable(
                detailed.group("file"),
                Integer.parseInt(detailed.group("line")),
                Integer.parseInt(detailed.group("column")),
                detailed.group("level").toLowerCase(Locale.ROOT),
                detailed.group("message").trim()));
        continue;
      }
      final Matcher simple = SIMPLE_PATTERN.matcher(line.trim());
      if (simple.matches()) {
        diagnostics.add(
            Diagnostic.fromNullable(
                null,
                -1,
                -1,
                line.startsWith("ERROR") ? "error" : line.startsWith("WARN") ? "warning" : "info",
                simple.group("message").trim()));
      }
    }
    return diagnostics;
  }

  /**
   * 返回仓库根目录，供 Main 读取版本信息或其他资源。
   */
  public Path projectRoot() {
    return projectRoot;
  }

  private void shutdownGracefully() {
    synchronized (activeProcesses) {
      for (Process process : activeProcesses) {
        process.destroy();
        try {
          process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          process.destroyForcibly();
        }
      }
      activeProcesses.clear();
    }
    ioExecutor.shutdownNow();
  }

  private CompletableFuture<String> readStreamAsync(InputStream inputStream) {
    return CompletableFuture.supplyAsync(
        () -> {
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            final StringBuilder builder = new StringBuilder();
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
              if (!first) {
                builder.append(System.lineSeparator());
              }
              builder.append(line);
              first = false;
            }
            return builder.toString();
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        },
        ioExecutor);
  }

  static Path locateExecutable(String binary) throws BridgeException {
    final String pathEnv = System.getenv("PATH");
    if (pathEnv == null || pathEnv.isBlank()) {
      throw new BridgeException("PATH 环境变量未设置，无法查找 %s".formatted(binary));
    }
    final String[] candidates = pathEnv.split(java.io.File.pathSeparator);
    final List<String> extensions =
        System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? List.of(".exe", ".cmd", ".bat", "")
            : List.of("");
    for (String candidate : candidates) {
      for (String ext : extensions) {
        final Path executable =
            Paths.get(candidate).resolve(binary + ext).toAbsolutePath().normalize();
        if (Files.isRegularFile(executable) && Files.isExecutable(executable)) {
          return executable;
        }
      }
    }
    throw new BridgeException("未找到可执行文件: %s".formatted(binary));
  }

  private void ensureExecutableAvailable(Path executable) throws BridgeException {
    if (!Files.isExecutable(executable)) {
      throw new BridgeException("可执行文件不可用: %s".formatted(executable));
    }
  }

  private static Path findProjectRoot() throws BridgeException {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.exists(current.resolve("package.json"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new BridgeException("未能定位项目根目录（缺少 package.json）");
  }

  /**
   * 简单进程工厂接口，方便在测试中用假进程替换。
   */
  interface ProcessFactory {
    Process start(List<String> command, Map<String, String> env, Path workingDirectory)
        throws IOException;
  }

  private static final class DefaultProcessFactory implements ProcessFactory {
    @Override
    public Process start(List<String> command, Map<String, String> env, Path workingDirectory)
        throws IOException {
      final ProcessBuilder builder = new ProcessBuilder(command);
      builder.directory(workingDirectory.toFile());
      builder.redirectErrorStream(false);
      builder.environment().clear();
      builder.environment().putAll(env);
      return builder.start();
    }
  }

  /**
   * 子进程执行结果。
   */
  public static final record Result(int exitCode, String stdout, String stderr, List<Diagnostic> diagnostics) {
    public Result {
      stdout = stdout == null ? "" : stdout;
      stderr = stderr == null ? "" : stderr;
      diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
  }

  /**
   * 诊断信息结构体。
   */
  public static final record Diagnostic(Optional<String> file, int line, int column, String severity, String message) {
    public Diagnostic {
      file = file == null ? Optional.empty() : file;
      severity = Objects.requireNonNull(severity, "severity");
      message = Objects.requireNonNull(message, "message");
    }

    public static Diagnostic fromNullable(
        String file, int line, int column, String severity, String message) {
      return new Diagnostic(Optional.ofNullable(file), line, column, severity, message);
    }
  }

  /**
   * 桥接异常，统一封装 IO、超时与缺失依赖错误。
   */
  public static final class BridgeException extends Exception {
    private static final long serialVersionUID = 1L;

    BridgeException(String message) {
      super(message);
    }

    BridgeException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
