package aster.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConstructNode extends Node {
  private final String typeName;
  private final Map<String, Node> fields;
  public ConstructNode(String typeName, Map<String, Node> fields) {
    this.typeName = typeName;
    this.fields = fields;
  }
  public Object execute(VirtualFrame frame) {
    Profiler.inc("construct");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("_type", typeName);
    for (var e : fields.entrySet()) out.put(e.getKey(), Exec.exec(e.getValue(), frame));
    return out;
  }
}

