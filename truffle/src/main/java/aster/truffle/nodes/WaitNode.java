package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class WaitNode extends Node {
  private final String[] names;
  public WaitNode(String[] names) { this.names = names; }
  public Object execute(VirtualFrame frame) { Profiler.inc("wait"); return null; }
}

