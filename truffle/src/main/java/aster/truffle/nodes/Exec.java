package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class Exec {
  private Exec() {}
  public static Object exec(Node n, VirtualFrame f) {
    Profiler.inc("exec");
    if (n instanceof LiteralNode lit) return lit.execute(f);
    if (n instanceof NameNode nn) return nn.execute(f);
    if (n instanceof CallNode cn) return cn.execute(f);
    if (n instanceof ReturnNode rn) return rn.execute(f);
    if (n instanceof LetNode ln) return ln.execute(f);
    if (n instanceof IfNode in) return in.execute(f);
    if (n instanceof ConstructNode cn2) return cn2.execute(f);
    if (n instanceof MatchNode mn) return mn.execute(f);
    if (n instanceof BlockNode bn) return bn.execute(f);
    if (n instanceof ResultNodes.OkNode ok) return ok.execute(f);
    if (n instanceof ResultNodes.ErrNode er) return er.execute(f);
    if (n instanceof ResultNodes.SomeNode sm) return sm.execute(f);
    if (n instanceof ResultNodes.NoneNode nn2) return nn2.execute(f);
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
