package aster.truffle.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConstructNode extends AsterExpressionNode {
  private final String typeName;
  private final String[] fieldNames;
  @Children private final AsterExpressionNode[] fieldNodes;

  protected ConstructNode(String typeName, Map<String, AsterExpressionNode> fields) {
    this.typeName = typeName;
    this.fieldNames = fields.keySet().toArray(new String[0]);
    this.fieldNodes = fields.values().toArray(new AsterExpressionNode[0]);
  }

  public static ConstructNode create(String typeName, Map<String, AsterExpressionNode> fields) {
    return ConstructNodeGen.create(typeName, fields);
  }

  @Specialization
  @ExplodeLoop
  protected Map<String, Object> doConstruct(VirtualFrame frame) {
    Profiler.inc("construct");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("_type", typeName);
    for (int i = 0; i < fieldNames.length; i++) {
      out.put(fieldNames[i], fieldNodes[i].executeGeneric(frame));
    }
    return out;
  }
}
