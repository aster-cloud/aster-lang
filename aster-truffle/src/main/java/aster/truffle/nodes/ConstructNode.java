package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConstructNode extends AsterExpressionNode {
  private final String typeName;
  private final Map<String, AsterExpressionNode> fields;
  public ConstructNode(String typeName, Map<String, AsterExpressionNode> fields) {
    this.typeName = typeName;
    this.fields = fields;
  }
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Profiler.inc("construct");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("_type", typeName);
    for (var e : fields.entrySet()) out.put(e.getKey(), Exec.exec(e.getValue(), frame));
    return out;
  }
}
