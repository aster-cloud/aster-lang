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
    Object c = executeChild(cond, frame);
    boolean b = (c instanceof Boolean) ? (Boolean)c : c != null;
    return b ? executeChild(thenNode, frame) : executeChild(elseNode, frame);
  }
  private static Object executeChild(Node n, VirtualFrame f) {
    if (n instanceof ReturnNode rn) return rn.execute(f);
    if (n instanceof LetNode ln) return ln.execute(f);
    if (n instanceof IfNode in) return in.execute(f);
    if (n instanceof LiteralNode lit) return lit.execute(f);
    return null;
  }
}

