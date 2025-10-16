package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class LetNode extends Node {
  private final Env env;
  private final String name;
  @Child private Node init;
  public LetNode(Env env, String name, Node init) { this.env = env; this.name = name; this.init = init; }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("let");
    Object v = Exec.exec(init, frame);
    env.set(name, v);
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: let " + name + "=" + v);
    }
    return v;
  }
}
