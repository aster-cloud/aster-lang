package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class WaitNode extends Node {
  private final String[] names;
  public WaitNode(String[] names) { this.names = names; }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("wait");
    throw new UnsupportedOperationException(
        "异步操作 (wait) 在 Truffle 后端尚未支持。" +
        "请使用 Java 或 TypeScript 后端运行异步代码。"
    );
  }
}

