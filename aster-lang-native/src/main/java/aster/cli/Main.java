package aster.cli;

/**
 * Aster Lang Native Image CLI 入口
 *
 * 用途：提供命令行接口用于 CNL 编译、类型检查和代码生成
 * 支持的命令：
 * - compile: 编译 CNL 文件到字节码
 * - typecheck: 仅类型检查
 * - version: 显示版本信息
 */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }

    String command = args[0];
    switch (command) {
      case "version":
      case "--version":
      case "-v":
        printVersion();
        break;

      case "compile":
        if (args.length < 2) {
          System.err.println("Error: compile command requires a file path");
          printUsage();
          System.exit(1);
        }
        compile(args[1]);
        break;

      case "typecheck":
        if (args.length < 2) {
          System.err.println("Error: typecheck command requires a file path");
          printUsage();
          System.exit(1);
        }
        typecheck(args[1]);
        break;

      case "help":
      case "--help":
      case "-h":
        printUsage();
        break;

      default:
        System.err.println("Unknown command: " + command);
        printUsage();
        System.exit(1);
    }
  }

  private static void printVersion() {
    System.out.println("Aster Lang v0.2.0 (Native)");
    System.out.println("Built with GraalVM Native Image");
  }

  private static void printUsage() {
    System.out.println("Aster Lang - Natural Language Programming");
    System.out.println();
    System.out.println("Usage: aster <command> [options]");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  compile <file>    Compile CNL file to JVM bytecode");
    System.out.println("  typecheck <file>  Type-check CNL file without compilation");
    System.out.println("  version           Show version information");
    System.out.println("  help              Show this help message");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  aster compile hello.aster");
    System.out.println("  aster typecheck hello.aster");
  }

  private static void compile(String filePath) {
    System.out.println("Compiling: " + filePath);
    System.out.println("(Implementation: call TypeScript compiler via JNI or subprocess)");
    System.out.println("TODO: Integrate with aster-runtime compilation pipeline");
    // TODO: 实现编译逻辑
    // 方案1: 通过 JNI 调用 Node.js 运行 TypeScript 编译器
    // 方案2: 将 TypeScript 编译器移植为 Java（长期方案）
    // 方案3: 通过 subprocess 调用 npm run emit:class（短期方案）
  }

  private static void typecheck(String filePath) {
    System.out.println("Type-checking: " + filePath);
    System.out.println("(Implementation: call TypeScript type-checker)");
    System.out.println("TODO: Integrate with aster-runtime typecheck pipeline");
    // TODO: 实现类型检查逻辑
  }
}
