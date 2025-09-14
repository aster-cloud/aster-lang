package example;

public final class CLIMain {
  private static void usage() {
    System.out.println("Usage: cli <command> [args]\n" +
        "Commands:\n" +
        "  greet <name>      Print a friendly greeting\n" +
        "  exclaim <text>    Add an exclamation mark\n" +
        "  length <text>     Print text length");
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      usage();
      return;
    }
    String cmd = args[0];
    try {
      switch (cmd) {
        case "greet": {
          if (args.length < 2) { usage(); return; }
          String name = args[1];
          String out = demo.cli.greet_fn.greet(name);
          System.out.println(out);
          return;
        }
        case "exclaim": {
          if (args.length < 2) { usage(); return; }
          String text = args[1];
          String out = demo.cli.exclaim_fn.exclaim(text);
          System.out.println(out);
          return;
        }
        case "length": {
          if (args.length < 2) { usage(); return; }
          String text = args[1];
          int n = demo.cli.length_fn.length(text);
          System.out.println(n);
          return;
        }
        case "help":
        default:
          usage();
      }
    } catch (Throwable t) {
      System.err.println("CLI error: " + t.getMessage());
      t.printStackTrace(System.err);
      System.exit(1);
    }
  }
}

