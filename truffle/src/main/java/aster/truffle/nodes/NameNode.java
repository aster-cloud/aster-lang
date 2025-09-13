package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class NameNode extends Node {
  private final Env env;
  private final String name;
  public NameNode(Env env, String name) { this.env = env; this.name = name; }
  public Object execute(VirtualFrame frame) { Profiler.inc("name"); return env.get(name); }
}
