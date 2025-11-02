package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LiteralNode extends AsterExpressionNode {
  private final Object value;
  public LiteralNode(Object value) { this.value = value; }
  @Override
  public Object executeGeneric(VirtualFrame frame) { Profiler.inc("literal"); return value; }
}
