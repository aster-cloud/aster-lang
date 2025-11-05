package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class ResultNodes {
  private ResultNodes() {}

  public static final class OkNode extends AsterExpressionNode {
    @Child private AsterExpressionNode expr;
    public OkNode(AsterExpressionNode expr) { this.expr = expr; }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Profiler.inc("ok");
      Object value = Exec.exec(expr, frame);
      return java.util.Map.of("_type", "Ok", "value", value);
    }
  }
  public static final class ErrNode extends AsterExpressionNode {
    @Child private AsterExpressionNode expr;
    public ErrNode(AsterExpressionNode expr) { this.expr = expr; }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Profiler.inc("err");
      Object value = Exec.exec(expr, frame);
      return java.util.Map.of("_type", "Err", "value", value);
    }
  }

  public static final class SomeNode extends AsterExpressionNode {
    @Child private AsterExpressionNode expr;
    public SomeNode(AsterExpressionNode expr) { this.expr = expr; }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Profiler.inc("some");
      Object value = Exec.exec(expr, frame);
      return java.util.Map.of("_type", "Some", "value", value);
    }
  }
  public static final class NoneNode extends AsterExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Profiler.inc("none");
      return java.util.Map.of("_type", "None");
    }
  }
}
