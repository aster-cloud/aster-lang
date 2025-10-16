package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.List;

/**
 * 函数调用节点
 * 支持：
 * 1. Lambda/closure调用
 * 2. Builtin函数调用（通过Builtins注册表）
 */
public final class CallNode extends Node {
  @Child private Node target;
  @Children private final Node[] args;

  public CallNode(Node target, List<Node> args) {
    this.target = target;
    this.args = args.toArray(new Node[0]);
  }

  public Object execute(VirtualFrame frame) {
    Profiler.inc("call");
    Object t = Exec.exec(target, frame);
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: call target=" + t + " (" + (t==null?"null":t.getClass().getName()) + ")");
    }

    // 1. Lambda/closure call
    if (t instanceof LambdaValue lv) {
      Object[] av = new Object[args.length];
      for (int i = 0; i < args.length; i++) av[i] = Exec.exec(args[i], frame);
      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: call args=" + java.util.Arrays.toString(av));
      }
      return lv.apply(av, frame);
    }

    // 2. Builtin function call
    String name = (t instanceof String) ? (String)t : null;
    if (name != null && aster.truffle.runtime.Builtins.has(name)) {
      Object[] av = new Object[args.length];
      for (int i = 0; i < args.length; i++) av[i] = Exec.exec(args[i], frame);
      try {
        Object result = aster.truffle.runtime.Builtins.call(name, av);
        if (result != null) return result;
      } catch (aster.truffle.runtime.Builtins.BuiltinException e) {
        // 转换为运行时异常
        throw new RuntimeException("Builtin call failed: " + name, e);
      }
    }

    // 3. Unknown call target - return null
    return null;
  }
}
