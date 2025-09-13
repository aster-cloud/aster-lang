package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class SetNode extends Node {
  private final Env env; private final String name; @Child private Node expr;
  public SetNode(Env env, String name, Node expr) { this.env = env; this.name = name; this.expr = expr; }
  public Object execute(VirtualFrame frame) { Profiler.inc("set"); Object v = Exec.exec(expr, frame); env.set(name, v); return v; }
}

