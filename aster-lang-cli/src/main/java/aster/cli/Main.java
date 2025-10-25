package aster.cli;

import aster.cli.CommandLineParser.CommandLineException;
import aster.cli.CommandLineParser.ParsedCommand;
import aster.cli.TypeScriptBridge.BridgeException;

// 程序入口，仅负责命令解析与路由，将具体逻辑委派给 CommandHandler。
public final class Main {
  private static final int EXIT_USAGE = 1;
  private static final int EXIT_SYSTEM_ERROR = 3;

  private Main() {}

  public static void main(String[] args) {
    System.exit(run(args));
  }

  static int run(String[] args) {
    final ParsedCommand parsed = CommandLineParser.parse(args);
    CommandHandler handler = null;
    try {
      final TypeScriptBridge bridge = new TypeScriptBridge();
      handler =
          new CommandHandler(
              bridge, new PathResolver(bridge.projectRoot()), new DiagnosticFormatter(), new VersionReader());
      return dispatch(parsed, handler);
    } catch (CommandLineException e) {
      System.err.println("参数错误: %s".formatted(e.getMessage()));
      if (handler != null) handler.printUsage();
      return EXIT_USAGE;
    } catch (BridgeException e) {
      System.err.println("系统错误: %s".formatted(e.getMessage()));
      return EXIT_SYSTEM_ERROR;
    }
  }

  private static int dispatch(ParsedCommand parsed, CommandHandler handler)
      throws CommandLineException, BridgeException {
    if (parsed.command() == null) return handleNoCommand(parsed, handler);
    if (parsed.isHelpRequested()) {
      handler.printUsage();
      return 0;
    }
    if (parsed.isVersionRequested()) {
      handler.printVersion();
      return 0;
    }
    return switch (parsed.command()) {
      case "compile" -> handler.handleCompile(parsed);
      case "typecheck" -> handler.handleTypecheck(parsed);
      case "jar" -> handler.handleJar(parsed);
      case "parse" -> handler.handlePassThrough(parsed, "native:cli:parse");
      case "core" -> handler.handlePassThrough(parsed, "native:cli:core");
      case "version" -> {
        handler.printVersion();
        yield 0;
      }
      case "help", "--help", "-h" -> {
        handler.printUsage();
        yield 0;
      }
      default -> {
        System.err.println("未知命令: %s".formatted(parsed.command()));
        handler.printUsage();
        yield EXIT_USAGE;
      }
    };
  }

  private static int handleNoCommand(ParsedCommand parsed, CommandHandler handler) {
    if (parsed.isHelpRequested()) {
      handler.printUsage();
      return 0;
    }
    if (parsed.isVersionRequested()) {
      handler.printVersion();
      return 0;
    }
    handler.printUsage();
    return EXIT_USAGE;
  }
}
