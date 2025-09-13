package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class StartNode extends Node {
  private final Env env; private final String name; @Child private Node expr;
  public StartNode(Env env, String name, Node expr) { this.env = env; this.name = name; this.expr = expr; }
  public Object execute(VirtualFrame frame) { Profiler.inc("start"); Object v = Exec.exec(expr, frame); env.set(name, v); return v; }
}

