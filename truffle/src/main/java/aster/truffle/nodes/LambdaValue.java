package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.*;

public final class LambdaValue {
  private final Env env;
  private final List<String> params;
  private final Map<String,Object> captures;
  // Note: LambdaValue is a runtime value object, not a Node subclass.
  // The body Node is stored as-is without @Child annotation.
  // TODO: Refactor to use CallTarget for proper Truffle integration.
  private Node body;

  public LambdaValue(Env env, List<String> params, Map<String,Object> captures, Node body) {
    this.env = env;
    this.params = params == null ? List.of() : params;
    this.captures = captures == null ? Map.of() : captures;
    this.body = body;
  }

  public Object apply(Object[] args, VirtualFrame frame) {
    Profiler.inc("lambda_apply");
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: lambda_apply params=" + params + ", captures=" + captures);
    }
    // Save previous values
    Map<String,Object> prev = new HashMap<>();
    try {
      // Inject captures
      for (var e : captures.entrySet()) {
        prev.put(e.getKey(), env.get(e.getKey()));
        env.set(e.getKey(), e.getValue());
      }
      // Bind params
      int n = Math.min(params.size(), args == null ? 0 : args.length);
      for (int i = 0; i < n; i++) {
        String p = params.get(i);
        prev.put(p, env.get(p));
        env.set(p, args[i]);
      }
      // Execute body; return first Return value or null
      try {
        Object v = Exec.exec(body, frame);
        if (AsterConfig.DEBUG) {
          System.err.println("DEBUG: lambda body returned=" + v);
        }
        return v;
      } catch (ReturnNode.ReturnException r) {
        return r.value;
      }
    } finally {
      // Restore previous
      for (var e : prev.entrySet()) env.set(e.getKey(), e.getValue());
    }
  }
}
