package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class LetNode extends Node {
  @Child private Node init;
  public LetNode(Node init) { this.init = init; }
  public Object execute(VirtualFrame frame) {
    if (init instanceof LiteralNode lit) return lit.execute(frame);
    return null;
  }
}

