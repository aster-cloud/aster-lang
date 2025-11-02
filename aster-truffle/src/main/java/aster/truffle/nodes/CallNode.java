package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import java.util.List;

/**
 * 函数调用节点
 * 支持：
 * 1. Lambda/closure调用 (通过 CallTarget + IndirectCallNode)
 * 2. Builtin函数调用（通过Builtins注册表）
 */
public final class CallNode extends AsterExpressionNode {
  @Child private Node target;
  @Children private final Node[] args;
  @Child private IndirectCallNode indirectCallNode;

  public CallNode(Node target, List<Node> args) {
    this.target = target;
    this.args = args.toArray(new Node[0]);
    this.indirectCallNode = IndirectCallNode.create();
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Profiler.inc("call");
    Object t = Exec.exec(target, frame);
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: call target=" + t + " (" + (t==null?"null":t.getClass().getName()) + ")");
    }

    // 1. Lambda/closure call via IndirectCallNode (with inline cache)
    if (t instanceof LambdaValue lv) {
      Object[] av = new Object[args.length];
      for (int i = 0; i < args.length; i++) av[i] = Exec.exec(args[i], frame);
      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: call args=" + java.util.Arrays.toString(av));
      }

      // Use IndirectCallNode for JIT inline caching and optimization
      com.oracle.truffle.api.CallTarget callTarget = lv.getCallTarget();
      if (callTarget != null) {
        // Pack arguments: [callArgs..., captureValues...]
        // LambdaRootNode expects captures at positions paramCount..paramCount+captureCount-1
        Object[] capturedValues = lv.getCapturedValues();
        Object[] packedArgs = new Object[av.length + capturedValues.length];
        System.arraycopy(av, 0, packedArgs, 0, av.length);
        System.arraycopy(capturedValues, 0, packedArgs, av.length, capturedValues.length);

        try {
          Object result = indirectCallNode.call(callTarget, packedArgs);
          if (AsterConfig.DEBUG) {
            System.err.println("DEBUG: CallTarget result=" + result);
          }
          return result;
        } catch (ReturnNode.ReturnException r) {
          return r.value;
        }
      } else {
        // Fallback to apply() for legacy mode (non-CallTarget)
        return lv.apply(av, frame);
      }
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
        // 转换为运行时异常，包含参数信息用于调试
        String argsInfo = "args=[";
        for (int i = 0; i < av.length; i++) {
          if (i > 0) argsInfo += ", ";
          Object arg = av[i];
          argsInfo += (arg == null ? "null" : arg.getClass().getSimpleName() + ":" + arg);
        }
        argsInfo += "]";
        throw new RuntimeException("Builtin call failed: " + name + " with " + argsInfo + " - " + e.getMessage(), e);
      }
    }

    // 3. Unknown call target - return null
    return null;
  }
}
