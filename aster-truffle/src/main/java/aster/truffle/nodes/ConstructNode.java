package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConstructNode extends AsterExpressionNode {
  private final String typeName;
  private final String[] fieldNames;
  @Children private final AsterExpressionNode[] fieldNodes;

  public ConstructNode(String typeName, Map<String, AsterExpressionNode> fields) {
    this.typeName = typeName;
    this.fieldNames = fields.keySet().toArray(new String[0]);
    this.fieldNodes = fields.values().toArray(new AsterExpressionNode[0]);
  }

  @Override
  @ExplodeLoop
  public Object executeGeneric(VirtualFrame frame) {
    Profiler.inc("construct");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("_type", typeName);
    for (int i = 0; i < fieldNames.length; i++) {
      out.put(fieldNames[i], fieldNodes[i].executeGeneric(frame));
    }
    return out;
  }
}
