package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.List;

public final class CallNode extends Node {
  @Child private Node target;
  @Children private final Node[] args;
  public CallNode(Node target, List<Node> args) {
    this.target = target;
    this.args = args.toArray(new Node[0]);
  }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("call");
    Object t = Exec.exec(target, frame);
    if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
      System.err.println("DEBUG: call target=" + t + " (" + (t==null?"null":t.getClass().getName()) + ")");
    }
    // Lambda/closure call
    if (t instanceof LambdaValue lv) {
      Object[] av = new Object[args.length];
      for (int i = 0; i < args.length; i++) av[i] = Exec.exec(args[i], frame);
      if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
        System.err.println("DEBUG: call args=" + java.util.Arrays.toString(av));
      }
      return lv.apply(av, frame);
    }
    String name = (t instanceof String) ? (String)t : (t instanceof NameNode ? null : null);
    // For Name targets, Loader will usually wrap target as LiteralNode of the name
    if (target instanceof NameNode) {
      // Resolve name literal from environment? fallthrough
    }
    if (t instanceof String) name = (String)t;
    Object[] av = new Object[args.length];
    for (int i = 0; i < args.length; i++) av[i] = Exec.exec(args[i], frame);
    if ("not".equals(name) && av.length == 1) return !Exec.toBool(av[0]);
    if ("Text.concat".equals(name) && av.length == 2) return String.valueOf(av[0]) + String.valueOf(av[1]);
    if ("Text.toUpper".equals(name) && av.length == 1) return String.valueOf(av[0]).toUpperCase();
    if ("Text.startsWith".equals(name) && av.length == 2) return String.valueOf(av[0]).startsWith(String.valueOf(av[1]));
    if ("Text.indexOf".equals(name) && av.length == 2) return String.valueOf(av[0]).indexOf(String.valueOf(av[1]));
    if ("List.length".equals(name) && av.length == 1 && av[0] instanceof java.util.List<?> l) return l.size();
    if ("List.get".equals(name) && av.length == 2 && av[0] instanceof java.util.List<?> l && av[1] instanceof Number n) return l.get(n.intValue());
    if ("await".equals(name) && av.length == 1) return av[0];
    return null;
  }

}
