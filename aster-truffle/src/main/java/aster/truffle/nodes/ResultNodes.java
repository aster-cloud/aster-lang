package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class ResultNodes {
  private ResultNodes() {}

  public static final class OkNode extends Node {
    @Child private Node expr;
    public OkNode(Node expr) { this.expr = expr; }
    public Object execute(VirtualFrame frame) { Profiler.inc("ok"); return new Result.Ok(Exec.exec(expr, frame)); }
  }
  public static final class ErrNode extends Node {
    @Child private Node expr;
    public ErrNode(Node expr) { this.expr = expr; }
    public Object execute(VirtualFrame frame) { Profiler.inc("err"); return new Result.Err(Exec.exec(expr, frame)); }
  }

  public static final class SomeNode extends Node {
    @Child private Node expr;
    public SomeNode(Node expr) { this.expr = expr; }
    public Object execute(VirtualFrame frame) { Profiler.inc("some"); return Exec.exec(expr, frame); }
  }
  public static final class NoneNode extends Node {
    public Object execute(VirtualFrame frame) { Profiler.inc("none"); return null; }
  }

  public static final class Result {
    public static final class Ok { public final Object value; public Ok(Object v) { this.value = v; } @Override public String toString(){ return "Ok("+value+")"; } }
    public static final class Err { public final Object value; public Err(Object v) { this.value = v; } @Override public String toString(){ return "Err("+value+")"; } }
  }
}

