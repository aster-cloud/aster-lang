package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

/**
 * Await表达式节点 - 等待异步表达式完成并返回结果
 * 在当前简化实现中，直接执行子表达式（无真正的异步等待）
 */
public final class AwaitNode extends Node {
  @Child private Node expr;

  public AwaitNode(Node expr) {
    this.expr = expr;
  }

  public Object execute(VirtualFrame frame) {
    Profiler.inc("await");
    // 当前简化实现：直接执行子表达式并返回结果
    // 未来可能需要与Start/Wait节点协作实现真正的async/await语义
    return Exec.exec(expr, frame);
  }
}
