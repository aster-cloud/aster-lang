package aster.cli;

import aster.cli.CommandLineParser.CommandLineException;
import aster.cli.CommandLineParser.ParsedCommand;
import aster.cli.TypeScriptBridge.BridgeException;
import aster.cli.TypeScriptBridge.Result;
import aster.cli.compiler.CompilerBackend;
import aster.cli.compiler.JavaCompilerBackend;
import aster.cli.compiler.TypeScriptCompilerBackend;
import aster.cli.hotswap.JarFileWatcher;
import aster.cli.hotswap.JarHotSwapRunner;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 集中处理所有 CLI 子命令，统一依赖注入与公共逻辑，避免 Main 承担过多职责。
 * <p>
 * <b>编译器后端选择</b>：
 * <ul>
 *   <li>默认使用 TypeScript 编译器（当前成熟实现）</li>
 *   <li>设置 ASTER_COMPILER=java 使用 Java 编译器（渐进式迁移中）</li>
 *   <li>设置 ASTER_COMPILER=typescript 显式指定 TypeScript 编译器</li>
 * </ul>
 */
public final class CommandHandler {
  private static final int EXIT_COMPILER_ERROR = 2;
  private static final String DEFAULT_COMPILE_OUT = "build/jvm-classes";
  private static final String DEFAULT_JAR_OUT = "build/aster-out/aster.jar";

  private final TypeScriptBridge bridge;
  private final CompilerBackend backend;
  private final PathResolver pathResolver;
  private final DiagnosticFormatter diagnosticFormatter;
  private final VersionReader versionReader;

  public CommandHandler(
      TypeScriptBridge bridge,
      PathResolver pathResolver,
      DiagnosticFormatter diagnosticFormatter,
      VersionReader versionReader) {
    this(bridge, pathResolver, diagnosticFormatter, versionReader, selectBackend(bridge));
  }

  /**
   * 构造函数（支持注入自定义编译器后端，便于测试）
   */
  public CommandHandler(
      TypeScriptBridge bridge,
      PathResolver pathResolver,
      DiagnosticFormatter diagnosticFormatter,
      VersionReader versionReader,
      CompilerBackend backend) {
    this.bridge = bridge;
    this.pathResolver = pathResolver;
    this.diagnosticFormatter = diagnosticFormatter;
    this.versionReader = versionReader;
    this.backend = backend;

    // 输出编译器选择信息（调试模式）
    if ("true".equals(System.getenv("ASTER_DEBUG"))) {
      System.err.println("[DEBUG] 使用编译器后端: " + backend.getType());
    }
  }

  /**
   * 根据环境变量选择编译器后端
   * <p>
   * 优先从环境变量读取 ASTER_COMPILER，如果未设置则从系统属性读取（用于测试）
   */
  private static CompilerBackend selectBackend(TypeScriptBridge bridge) {
    String compilerType = System.getenv("ASTER_COMPILER");
    if (compilerType == null) {
      compilerType = System.getProperty("ASTER_COMPILER");  // 测试时使用系统属性
    }
    return "java".equals(compilerType)
        ? new JavaCompilerBackend()  // Phase 2: 纯 Java 实现，不再依赖 TypeScript Bridge
        : new TypeScriptCompilerBackend(bridge);
  }

  /**
   * 处理 compile 命令，通过编译器后端完成编译。
   */
  public int handleCompile(ParsedCommand parsed) throws CommandLineException, BridgeException {
    final List<String> args = parsed.arguments();
    if (args.isEmpty()) {
      throw new CommandLineException("compile 需要指定源文件");
    }
    final Path source = pathResolver.resolveExistingFile(args.getFirst(), "源文件");
    final Path output =
        pathResolver.resolvePath(parsed.options().getOrDefault("output", DEFAULT_COMPILE_OUT), "输出目录");

    final List<String> cliArgs = new ArrayList<>();
    cliArgs.add(source.toString());
    cliArgs.add("--out");
    cliArgs.add(output.toString());
    if (parsed.flag("json")) {
      cliArgs.add("--json");
    }

    final Result result = backend.execute("native:cli:class", cliArgs);
    return handleSuccessOrDiagnostics(
        result, "编译完成: %s".formatted(output), "编译失败");
  }

  /**
   * 处理 typecheck 命令，支持 --caps 参数注入额外能力。
   */
  public int handleTypecheck(ParsedCommand parsed) throws CommandLineException, BridgeException {
    final List<String> args = parsed.arguments();
    if (args.isEmpty()) {
      throw new CommandLineException("typecheck 需要指定源文件");
    }
    final Path source = pathResolver.resolveExistingFile(args.getFirst(), "源文件");
    final Map<String, String> envOverrides = buildCapsEnv(parsed);

    final List<String> cliArgs = new ArrayList<>();
    cliArgs.add(source.toString());
    if (parsed.flag("json")) {
      cliArgs.add("--json");
    }

    final Result result = backend.execute("native:cli:typecheck", cliArgs, envOverrides);
    if (!result.diagnostics().isEmpty() && result.exitCode() == 0) {
      diagnosticFormatter.reportDiagnostics(result, "类型检查失败");
      return EXIT_COMPILER_ERROR;
    }
    final String successMessage =
        result.diagnostics().isEmpty() && result.stdout().isBlank()
            ? "类型检查通过"
            : result.stdout();
    return handleSuccessOrDiagnostics(result, successMessage, "类型检查失败");
  }

  /**
   * 处理 jar 命令，可选源文件，默认输出到 build/aster-out。
   */
  public int handleJar(ParsedCommand parsed) throws CommandLineException, BridgeException {
    final List<String> args = parsed.arguments();
    final List<String> cliArgs = new ArrayList<>();
    if (!args.isEmpty()) {
      final Path source = pathResolver.resolveExistingFile(args.getFirst(), "源文件");
      cliArgs.add(source.toString());
    }
    final Path output =
        pathResolver.resolvePath(parsed.options().getOrDefault("output", DEFAULT_JAR_OUT), "输出文件");
    cliArgs.add("--out");
    cliArgs.add(output.toString());
    if (parsed.flag("json")) {
      cliArgs.add("--json");
    }

    final Result result = backend.execute("native:cli:jar", cliArgs);
    return handleSuccessOrDiagnostics(
        result, "JAR 打包完成: %s".formatted(output), "JAR 打包失败");
  }

  /**
   * 处理 parse/core 之类的透传命令。
   */
  public int handlePassThrough(ParsedCommand parsed, String script)
      throws CommandLineException, BridgeException {
    final List<String> args = parsed.arguments();
    if (args.isEmpty()) {
      throw new CommandLineException("%s 需要提供输入文件".formatted(script));
    }
    final Path source = pathResolver.resolveExistingFile(args.getFirst(), "源文件");
    final List<String> cliArgs = new ArrayList<>();
    cliArgs.add(source.toString());
    if (parsed.flag("json")) {
      cliArgs.add("--json");
    }

    final Result result = backend.execute(script, cliArgs);
    return handleSuccessOrDiagnostics(result, "", "子命令执行失败");
  }

  /**
   * 输出版本信息，包含当前时间戳。
   */
  public void printVersion() {
    final String version = versionReader.readVersion(bridge.projectRoot()).orElse("未知版本");
    final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Pacific/Auckland"));
    System.out.println("Aster Lang Native CLI v%s".formatted(version));
    System.out.println("构建时间: %s".formatted(now));
  }

  /**
   * 处理 run 命令，支持热插拔运行 JAR 文件。
   */
  public int handleRun(ParsedCommand parsed) throws CommandLineException {
    final List<String> args = parsed.arguments();
    if (args.isEmpty()) {
      throw new CommandLineException("run 需要指定 JAR 文件");
    }

    final Path jarPath = pathResolver.resolveExistingFile(args.getFirst(), "JAR 文件");
    final String mainClass = parsed.options().get("main");
    if (mainClass == null || mainClass.isBlank()) {
      throw new CommandLineException("run 需要指定主类（使用 --main 参数）");
    }

    // 获取传递给应用的参数（跳过 JAR 路径）
    final String[] appArgs = args.size() > 1
        ? args.subList(1, args.size()).toArray(new String[0])
        : new String[0];

    final boolean watchMode = parsed.flag("watch");

    try (JarHotSwapRunner runner = new JarHotSwapRunner(jarPath)) {
      if (watchMode) {
        System.out.println("启动热插拔监控模式...");
        System.out.println("主类: " + mainClass);
        System.out.println("JAR: " + jarPath);
        System.out.println();

        final BlockingQueue<Path> reloadQueue = new LinkedBlockingQueue<>();
        try (JarFileWatcher watcher = new JarFileWatcher(jarPath, (path) -> {
          reloadQueue.offer(path);
          runner.stop();
        })) {
          runner.run(mainClass, appArgs);
          watcher.start();
          System.out.println();
          System.out.println("按 Ctrl+C 停止监控...");
          runWatchLoop(runner, mainClass, appArgs, reloadQueue);
        }
      } else {
        System.out.println("运行 JAR: " + jarPath);
        System.out.println("主类: " + mainClass);
        System.out.println();
        runner.run(mainClass, appArgs);
        runner.waitForCompletion();
      }

      return 0;

    } catch (JarHotSwapRunner.RunnerException e) {
      System.err.println("运行失败: " + e.getMessage());
      return EXIT_COMPILER_ERROR;
    } catch (InterruptedException e) {
      System.err.println("运行被中断: " + e.getMessage());
      Thread.currentThread().interrupt();
      return EXIT_COMPILER_ERROR;
    } catch (Exception e) {
      System.err.println("发生错误: " + e.getMessage());
      e.printStackTrace();
      return EXIT_COMPILER_ERROR;
    }
  }

  private void runWatchLoop(
      JarHotSwapRunner runner,
      String mainClass,
      String[] appArgs,
      BlockingQueue<Path> reloadQueue)
      throws InterruptedException {
    while (true) {
      try {
        runner.waitForCompletion();
      } catch (JarHotSwapRunner.RunnerException e) {
        System.err.println("运行失败: " + e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }

      final Path changed;
      try {
        changed = reloadQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }
      reloadQueue.clear();

      System.out.println();
      System.out.println("=== 文件已修改，重新加载... ===");
      System.out.println("路径: " + changed);
      try {
        runner.reload();
        runner.run(mainClass, appArgs);
      } catch (JarHotSwapRunner.RunnerException e) {
        System.err.println("重载失败: " + e.getMessage());
      }
    }
  }

  /**
   * 打印 CLI 使用帮助。
   */
  public void printUsage() {
    System.out.println("Aster Lang Native CLI");
    System.out.println();
    System.out.println("用法: aster <命令> [选项]");
    System.out.println();
    System.out.println("核心命令:");
    System.out.println("  compile <file> [--output <dir>]    编译 CNL 为 JVM 字节码");
    System.out.println("  typecheck <file> [--caps <json>]   执行类型检查，可指定能力配置");
    System.out.println("  jar <file?> [--output <file>]      生成 JAR（未提供文件时复用上次编译结果）");
    System.out.println("  run <jar> [args...] --main <class> 运行 JAR 文件（支持热插拔）");
    System.out.println("  version                            查看当前版本");
    System.out.println("  help                               查看帮助");
    System.out.println();
    System.out.println("扩展命令:");
    System.out.println("  parse <file>                       仅解析，输出 AST JSON");
    System.out.println("  core <file>                        降级到 Core IR 输出 JSON");
    System.out.println();
    System.out.println("选项:");
    System.out.println("  -h, --help          显示帮助信息");
    System.out.println("  -v, --version       显示版本信息");
    System.out.println("      --json          以 JSON 格式输出结果");
    System.out.println("      --output PATH   指定输出路径");
    System.out.println("      --caps PATH     指定能力配置文件 (typecheck)");
    System.out.println("      --main CLASS    指定主类（run 命令）");
    System.out.println("      --watch         监控文件变化并自动重载（run 命令）");
    System.out.println();
    System.out.println("环境变量:");
    System.out.println("  ASTER_COMPILER=typescript          使用 TypeScript 编译器（默认）");
    System.out.println("  ASTER_COMPILER=java                使用 Java 编译器（渐进式迁移中）");
    System.out.println("  ASTER_DEBUG=true                   输出编译器后端选择信息");
  }

  private Map<String, String> buildCapsEnv(ParsedCommand parsed) throws CommandLineException {
    final String caps = parsed.option("caps");
    if (caps == null) {
      return Map.of();
    }
    final Path resolved = pathResolver.resolveExistingFile(caps, "capabilities JSON");
    return Map.of("ASTER_CAPS", resolved.toString());
  }

  private int handleSuccessOrDiagnostics(Result result, String successMessage, String defaultError) {
    return switch (result) {
      case Result(int exitCode, String stdout, String _, _)
          when exitCode == 0 -> {
        if (!stdout.isBlank()) {
          System.out.println(stdout);
        }
        if (!successMessage.isBlank()) {
          System.out.println(successMessage);
        }
        yield 0;
      }
      default -> {
        diagnosticFormatter.reportDiagnostics(result, defaultError);
        yield EXIT_COMPILER_ERROR;
      }
    };
  }
}
