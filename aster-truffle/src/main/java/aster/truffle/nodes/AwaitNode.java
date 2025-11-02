package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
/**
 * Await表达式节点 - 等待异步表达式完成并返回结果
 * 在当前简化实现中，直接执行子表达式（无真正的异步等待）
 */
public final class AwaitNode extends AsterExpressionNode {
  @Child private AsterExpressionNode expr;

  public AwaitNode(AsterExpressionNode expr) {
    this.expr = expr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Profiler.inc("await");
    // 当前简化实现：直接执行子表达式并返回结果
    // 未来可能需要与Start/Wait节点协作实现真正的async/await语义
    // 注意：此实现仅为同步执行，不具备真正的异步语义
    throw new UnsupportedOperationException(
        "异步操作 (await) 在 Truffle 后端尚未支持。" +
        "请使用 Java 或 TypeScript 后端运行异步代码。"
    );
  }
}
