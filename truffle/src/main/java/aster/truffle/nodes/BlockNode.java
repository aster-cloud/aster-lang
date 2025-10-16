package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class BlockNode extends Node {
  @Children private final Node[] statements;
  public BlockNode(java.util.List<Node> statements) { this.statements = statements.toArray(new Node[0]); }
  public Object execute(VirtualFrame frame) {
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: block size=" + statements.length);
    }
    for (int i = 0; i < statements.length; i++) {
      Node s = statements[i];
      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: stmt[" + i + "]=" + s.getClass().getSimpleName());
      }
      if (s instanceof ReturnNode r) return r.execute(frame);
      // Delegate execution to generic dispatcher to cover all node kinds (Match, Scope, etc.)
      Exec.exec(s, frame);
    }
    return null;
  }
}
