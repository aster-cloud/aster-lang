package aster.truffle.nodes;

import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import java.util.*;

public final class LambdaValue {
  private final Env env;
  private final List<String> params;
  private final List<String> captureNames;
  private final Object[] capturedValues;
  private final CallTarget callTarget;
  private final Node legacyBody;  // For backward compatibility with non-CallTarget mode
  private final java.util.Set<String> requiredEffects;  // Effect 元数据（如 ["IO", "Async"]）

  /**
   * 创建 LambdaValue (新版本，使用 CallTarget + 闭包捕获)
   *
   * @param env 环境 (用于向后兼容)
   * @param params 参数名列表
   * @param captureNames 闭包变量名列表
   * @param capturedValues 闭包变量值数组
   * @param callTarget Lambda 的 CallTarget
   * @param requiredEffects 函数所需 effects（如 ["IO", "Async"]）
   */
  public LambdaValue(Env env, List<String> params, List<String> captureNames, Object[] capturedValues, CallTarget callTarget, java.util.Set<String> requiredEffects) {
    this.env = env;
    this.params = params == null ? List.of() : params;
    this.captureNames = captureNames == null ? List.of() : captureNames;
    this.capturedValues = capturedValues != null ? capturedValues : new Object[0];
    this.callTarget = callTarget;
    this.legacyBody = null;
    this.requiredEffects = requiredEffects != null ? java.util.Set.copyOf(requiredEffects) : java.util.Set.of();
  }

  /**
   * 旧版本构造器 (向后兼容，使用 CallTarget)
   */
  public LambdaValue(Env env, List<String> params, Map<String,Object> captures, CallTarget callTarget) {
    this.env = env;
    this.params = params == null ? List.of() : params;
    this.captureNames = captures != null ? new java.util.ArrayList<>(captures.keySet()) : List.of();
    this.capturedValues = captures != null ? captures.values().toArray() : new Object[0];
    this.callTarget = callTarget;
    this.legacyBody = null;
    this.requiredEffects = java.util.Set.of();  // 向后兼容，默认无 effect 要求
  }

  /**
   * 遗留构造器 (用于 LoaderTest 等不使用 Polyglot API 的测试)
   *
   * @param env 环境
   * @param params 参数名列表
   * @param captures 闭包变量映射
   * @param body Lambda 函数体节点 (非 CallTarget 模式)
   */
  public LambdaValue(Env env, List<String> params, Map<String,Object> captures, Node body) {
    this.env = env;
    this.params = params == null ? List.of() : params;
    this.captureNames = captures != null ? new java.util.ArrayList<>(captures.keySet()) : List.of();
    this.capturedValues = captures != null ? captures.values().toArray() : new Object[0];
    this.callTarget = null;
    this.legacyBody = body;
    this.requiredEffects = java.util.Set.of();  // 遗留模式，默认无 effect 要求
  }

  public CallTarget getCallTarget() {
    return callTarget;
  }

  public Object[] getCapturedValues() {
    return capturedValues;
  }

  /**
   * 获取函数所需的 effects 列表（不可变副本）
   */
  public java.util.Set<String> getRequiredEffects() {
    return requiredEffects;
  }

  public Object apply(Object[] args, VirtualFrame frame) {
    Profiler.inc("lambda_apply");
    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: lambda_apply params=" + params + ", captureNames=" + captureNames + ", args.length=" + (args != null ? args.length : 0) + ", mode=" + (callTarget != null ? "CallTarget" : "legacy"));
    }

    // Legacy mode: execute body node directly using Env
    if (legacyBody != null) {
      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: legacy lambda mode, binding params to Env");
      }
      // Bind parameters to Env
      if (args != null) {
        for (int i = 0; i < params.size() && i < args.length; i++) {
          env.set(params.get(i), args[i]);
        }
      }
      // Execute body
      try {
        return Exec.exec(legacyBody, frame);
      } catch (ReturnNode.ReturnException r) {
        return r.value;
      }
    }

    // CallTarget mode: prepare arguments and call
    // Format: [param0, param1, ..., paramN, capture0, capture1, ...]
    int paramCount = args != null ? args.length : 0;
    int captureCount = capturedValues.length;
    Object[] callArgs = new Object[paramCount + captureCount];

    // Copy parameter values
    if (args != null) {
      System.arraycopy(args, 0, callArgs, 0, paramCount);
    }

    // Append captured values
    System.arraycopy(capturedValues, 0, callArgs, paramCount, captureCount);

    if (AsterConfig.DEBUG) {
      System.err.println("DEBUG: lambda calling with " + callArgs.length + " args (params=" + paramCount + ", captures=" + captureCount + ")");
    }

    // Call the Lambda's CallTarget
    try {
      Object result = callTarget.call(callArgs);
      if (AsterConfig.DEBUG) {
        System.err.println("DEBUG: lambda CallTarget returned=" + result);
      }
      return result;
    } catch (ReturnNode.ReturnException r) {
      return r.value;
    }
  }
}
