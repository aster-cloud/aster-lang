package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class LiteralNode extends Node {
  private final Object value;
  public LiteralNode(Object value) { this.value = value; }
  public Object execute(VirtualFrame frame) { return value; }
}

