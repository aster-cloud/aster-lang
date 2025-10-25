package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class IfNode extends Node {
  @Child private Node cond;
  @Child private Node thenNode;
  @Child private Node elseNode;
  public IfNode(Node cond, Node thenNode, Node elseNode) {
    this.cond = cond; this.thenNode = thenNode; this.elseNode = elseNode;
  }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("if");
    Object c = executeChild(cond, frame);
    boolean b = Exec.toBool(c);
    return b ? executeChild(thenNode, frame) : executeChild(elseNode, frame);
  }
  private static Object executeChild(Node n, VirtualFrame f) { return Exec.exec(n, f); }
}
