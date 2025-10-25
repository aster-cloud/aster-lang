package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class StartNode extends Node {
  private final Env env; private final String name; @Child private Node expr;
  public StartNode(Env env, String name, Node expr) { this.env = env; this.name = name; this.expr = expr; }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("start");
    throw new UnsupportedOperationException(
        "异步操作 (start) 在 Truffle 后端尚未支持。" +
        "请使用 Java 或 TypeScript 后端运行异步代码。"
    );
  }
}

