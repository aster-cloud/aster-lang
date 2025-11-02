package aster.truffle;
import aster.truffle.runtime.AsterConfig;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public final class Runner {
  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      // Parse optional function selection flags from args after the JSON path
      String funcName = AsterConfig.DEFAULT_FUNCTION;
      java.util.List<String> argList = new java.util.ArrayList<>(java.util.Arrays.asList(args));
      java.io.File f = new java.io.File(argList.get(0));

      // Support --func=<name>, --fn=<name>, --entry=<name>, or --func <name>
      for (int i = 1; i < argList.size(); ) {
        String a = argList.get(i);
        String name = null;
        if (a.startsWith("--func=")) name = a.substring("--func=".length());
        else if (a.startsWith("--fn=")) name = a.substring("--fn=".length());
        else if (a.startsWith("--entry=")) name = a.substring("--entry=".length());
        else if ("--func".equals(a) || "--fn".equals(a) || "--entry".equals(a)) {
          if (i+1 < argList.size()) { name = argList.get(i+1); argList.remove(i+1); }
        }
        if (name != null && !name.isEmpty()) {
          funcName = name; argList.remove(i); continue;
        }
        i++;
      }

      if (!f.isAbsolute() && !f.exists()) {
        // Gradle runs in the ':truffle' project dir; allow paths relative to repo root
        java.io.File root = new java.io.File(System.getProperty("user.dir")).getParentFile();
        if (root != null) {
          java.io.File f2 = new java.io.File(root, args[0]);
          if (f2.exists()) f = f2;
        }
      }

      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: input=" + f.getAbsolutePath());
        System.err.println("DEBUG: funcName=" + funcName);
      }

      // Create Polyglot context
      try (Context context = Context.newBuilder("aster")
          .allowAllAccess(true)
          .build()) {

        // Load and parse the Aster source file
        Source source = Source.newBuilder("aster", f).build();

        // Evaluate the source (this parses and initializes the program)
        Value result = context.eval(source);

        // TODO: Support function arguments via command line
        // For now, the result from context.eval() is the program execution result

        if (AsterConfig.DEBUG) {
          System.err.println("DEBUG: result=" + result);
        }

        // Print result if available
        if (result != null && !result.isNull()) {
          if (result.isNumber()) {
            System.out.println(result.asInt());
          } else if (result.isString()) {
            System.out.println(result.asString());
          } else if (result.isBoolean()) {
            System.out.println(result.asBoolean());
          } else {
            System.out.println(result);
          }
        }
      }

      if (AsterConfig.PROFILE) {
        System.out.print(aster.truffle.nodes.Profiler.dump());
      }
      return;
    }

    // Fallback: print usage
    System.err.println("Usage: Runner <file.json> [--func=<name>] [args...]");
    System.err.println("  --func=<name>   Specify entry function (default: " + AsterConfig.DEFAULT_FUNCTION + ")");
    System.err.println("  --fn=<name>     Same as --func");
    System.err.println("  --entry=<name>  Same as --func");
  }
}
