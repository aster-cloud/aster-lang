package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class Exec {
  private Exec() {}

  public static Object exec(Node n, VirtualFrame f) {
    Profiler.inc("exec");
    if (n instanceof AsterExpressionNode expr) return expr.executeGeneric(f);
    if (n instanceof ReturnNode rn) return rn.execute(f);
    if (n instanceof LetNodeEnv lne) return lne.execute(f);
    if (n instanceof SetNodeEnv sne) return sne.execute(f);
    if (n instanceof IfNode in) return in.execute(f);
    if (n instanceof MatchNode mn) return mn.execute(f);
    if (n instanceof BlockNode bn) return bn.execute(f);
    if (n instanceof StartNode sn) return sn.execute(f);
    if (n instanceof WaitNode wn) return wn.execute(f);
    return null;
  }

  public static boolean toBool(Object o) {
    if (o instanceof Boolean b) return b;
    if (o instanceof Number n) return n.doubleValue() != 0.0;
    if (o instanceof String s) {
      var ls = s.trim().toLowerCase();
      if ("true".equals(ls)) return true;
      if ("false".equals(ls)) return false;
      return !ls.isEmpty();
    }
    return o != null;
  }
}
