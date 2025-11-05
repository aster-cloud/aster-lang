package aster.truffle.nodes;

import aster.truffle.AsterLanguage;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.List;

/**
 * LambdaNode - 在运行时创建 LambdaValue，动态捕获闭包变量
 */
public final class LambdaNode extends AsterExpressionNode {
  @CompilationFinal private final AsterLanguage language;
  @CompilationFinal private final Env env;
  @CompilationFinal private final List<String> params;
  @CompilationFinal private final List<String> captureNames;
  @Children private final AsterExpressionNode[] captureExprs;
  @CompilationFinal private final CallTarget callTarget;

  public LambdaNode(
      AsterLanguage language,
      Env env,
      List<String> params,
      List<String> captureNames,
      AsterExpressionNode[] captureExprs,
      CallTarget callTarget) {
    this.language = language;
    this.env = env;
    this.params = params;
    this.captureNames = captureNames;
    this.captureExprs = captureExprs;
    this.callTarget = callTarget;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Profiler.inc("lambda_create");

    // Evaluate capture expressions at runtime to get current values
    Object[] capturedValues = new Object[captureExprs.length];
    for (int i = 0; i < captureExprs.length; i++) {
      capturedValues[i] = captureExprs[i].executeGeneric(frame);
    }

    // Create and return LambdaValue with captured values (without effects, for runtime lambda creation)
    return new LambdaValue(env, params, captureNames, capturedValues, callTarget, java.util.Set.of());
  }

  @Override
  public String toString() {
    return "LambdaNode(params=" + params + ", captures=" + captureNames + ")";
  }
}
