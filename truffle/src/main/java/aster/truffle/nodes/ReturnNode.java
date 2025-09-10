package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;

public final class ReturnNode extends Node {
  public static final class ReturnException extends ControlFlowException {
    public final Object value; public ReturnException(Object v) { this.value = v; }
  }
  @Child private Node expr;
  public ReturnNode(Node expr) { this.expr = expr; }
  public Object execute(VirtualFrame frame) { Object v = exec(expr, frame); throw new ReturnException(v); }
  private static Object exec(Node n, VirtualFrame f) {
    if (n instanceof LiteralNode lit) return lit.execute(f);
    return null;
  }
}

