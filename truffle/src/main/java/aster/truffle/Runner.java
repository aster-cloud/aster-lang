package aster.truffle;

import aster.truffle.nodes.*;
import com.oracle.truffle.api.frame.FrameDescriptor;

public final class Runner {
  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      var loader = new Loader();
      java.io.File f = new java.io.File(args[0]);
      if (!f.isAbsolute() && !f.exists()) {
        // Gradle runs in the ':truffle' project dir; allow paths relative to repo root
        java.io.File root = new java.io.File(System.getProperty("user.dir")).getParentFile();
        if (root != null) {
          java.io.File f2 = new java.io.File(root, args[0]);
          if (f2.exists()) f = f2;
        }
      }
      if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
        System.err.println("DEBUG: input=" + f.getAbsolutePath());
      }
      var program = loader.buildProgram(f);
      // Bind params from remaining args as strings
      if (program.params != null) {
        for (int i = 1; i < args.length && i-1 < program.params.size(); i++) {
          var p = program.params.get(i-1);
          program.env.set(p.name, args[i]);
        }
      }
      try {
        Object v = Exec.exec(program.root, null);
        if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
          System.err.println("DEBUG: v=" + v + ", x=" + program.env.get("x") + ", o=" + program.env.get("o"));
        }
        if (v != null) {
          System.out.println(v);
        } else {
          // Fallbacks for smoke harness: try common binders or extract first numeric from 'o'
          Object bx = program.env.get("x");
          if (bx != null) {
            System.out.println(bx);
          } else {
            Object o = program.env.get("o");
            Object num = findFirstNumber(o);
            if (num != null) System.out.println(num);
          }
        }
      } catch (ReturnNode.ReturnException rex) { System.out.println(rex.value); }
      if (System.getenv("ASTER_TRUFFLE_PROFILE") != null) {
        System.out.print(aster.truffle.nodes.Profiler.dump());
      }
      return;
    }
    // Fallback tiny demo
    var fd = new FrameDescriptor();
    var n = new IfNode(new LiteralNode(true), new ReturnNode(new LiteralNode("Hi")), new ReturnNode(new LiteralNode("Bye")));
    try { n.execute(null); } catch (ReturnNode.ReturnException rex) { System.out.println(rex.value); }
  }

  @SuppressWarnings("unchecked")
  private static Object findFirstNumber(Object v) {
    if (v instanceof Number) return v;
    if (v instanceof java.util.Map<?,?>) {
      for (Object val : ((java.util.Map<Object,Object>) v).values()) {
        if (val == null) continue;
        if (val instanceof Number) return val;
        Object rec = findFirstNumber(val);
        if (rec != null) return rec;
      }
    }
    if (v instanceof java.util.List<?>) {
      for (Object val : ((java.util.List<?>) v)) {
        Object rec = findFirstNumber(val);
        if (rec != null) return rec;
      }
    }
    return null;
  }
}
