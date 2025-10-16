package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class MatchNode extends Node {
  private final Env env;
  @Child private Node scrutinee;
  @Children private CaseNode[] cases;

  public MatchNode(Env env, Node scrutinee, java.util.List<CaseNode> cases) {
    this.env = env;
    this.scrutinee = scrutinee;
    this.cases = cases.toArray(new CaseNode[0]);
  }

  public Object execute(VirtualFrame frame) {
    Profiler.inc("match");
    Object s = Exec.exec(scrutinee, frame);
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: match scrutinee=" + s);
    }
    for (CaseNode c : cases) {
      if (c.matchesAndBind(s, env)) {
        if (AsterConfig.DEBUG) {
          System.err.println("DEBUG: case matched");
        }
        return c.execute(frame);
      }
    }
    return null;
  }

  public static abstract class PatternNode extends Node {
    public abstract boolean matchesAndBind(Object s, Env env);
  }

  public static final class PatNullNode extends PatternNode {
    @Override public boolean matchesAndBind(Object s, Env env) { return s == null; }
  }

  // ctor match: match map with _type == typeName; bind positional names if present
  public static final class PatCtorNode extends PatternNode {
    private final String typeName;
    private final java.util.List<String> bindNames;
    private final java.util.List<PatternNode> args;
    public PatCtorNode(String typeName, java.util.List<String> bindNames) { this(typeName, bindNames, java.util.List.of()); }
    public PatCtorNode(String typeName, java.util.List<String> bindNames, java.util.List<PatternNode> args) { this.typeName = typeName; this.bindNames = bindNames; this.args = (args == null ? java.util.List.of() : args); }
    @Override @SuppressWarnings("unchecked") public boolean matchesAndBind(Object s, Env env) {
      if (!(s instanceof java.util.Map)) return false;
      var m = (java.util.Map<String,Object>) s;
      Object t = m.get("_type");
      if (!(t instanceof String) || !typeName.equals(t)) return false;
      // Iterate insertion order, skipping _type; bind names and recurse into args when present.
      int idx = 0;
      for (var e : m.entrySet()) {
        if ("_type".equals(e.getKey())) continue;
        if (idx < args.size()) {
          PatternNode pn = args.get(idx);
          if (pn instanceof PatNameNode) {
            String bn = ((PatNameNode)pn).name; // access private via same top-level class
            if (!(bn == null || bn.isEmpty() || "_".equals(bn))) env.set(bn, e.getValue());
          } else {
            if (!pn.matchesAndBind(e.getValue(), env)) return false;
          }
        } else if (idx < (bindNames == null ? 0 : bindNames.size())) {
          String bn = bindNames.get(idx);
          if (bn != null && !bn.isEmpty() && !"_".equals(bn)) env.set(bn, e.getValue());
        }
        idx++;
      }
      return true;
    }
  }

  // Name match: match if s equals the variant name or map with _type == name; else non-null catch-all
  public static final class PatNameNode extends PatternNode {
    private final String name;
    public PatNameNode(String name) { this.name = name; }
    @Override @SuppressWarnings("unchecked") public boolean matchesAndBind(Object s, Env env) {
      if (s == null) return false;
      if (s instanceof String) return name.equals(s);
      if (s instanceof java.util.Map) {
        var m = (java.util.Map<String,Object>) s;
        Object v = m.get("value");
        if (v instanceof String && name.equals(v)) return true; // enum variant value
        Object t = m.get("_type");
        if (t instanceof String && name.equals(t)) return true; // constructor type fallback
        return false;
      }
      return true; // fallback catch-all on non-null values
    }
  }

  // Int pattern match: 匹配整数字面量
  public static final class PatIntNode extends PatternNode {
    private final int value;
    public PatIntNode(int value) { this.value = value; }
    @Override public boolean matchesAndBind(Object s, Env env) {
      if (s instanceof Integer i) return value == i.intValue();
      if (s instanceof Long l) return value == l.longValue();
      if (s instanceof Double d) return value == d.doubleValue();
      return false;
    }
  }

  public static final class CaseNode extends Node {
    @Child private PatternNode pat;
    @Child private Node body;
    public CaseNode(PatternNode pat, Node body) { this.pat = pat; this.body = body; }
    public boolean matchesAndBind(Object s, Env env) { return pat.matchesAndBind(s, env); }
    public Object execute(VirtualFrame frame) { return Exec.exec(body, frame); }
  }
}
