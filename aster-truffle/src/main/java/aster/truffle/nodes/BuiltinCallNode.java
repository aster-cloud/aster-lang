package aster.truffle.nodes;

import aster.truffle.runtime.Builtins;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * BuiltinCallNode - 内联常用 builtin 函数的优化节点
 *
 * 通过 Truffle DSL @Specialization 实现类型特化，直接内联算术、比较、逻辑运算，
 * 消除 CallTarget 调用开销。仅处理已知的 builtin，其他情况 fallback 到 Builtins.call。
 *
 * Phase 2A：当前实现包含算术运算 add/sub/mul/div/mod、比较运算 eq/lt/gt/lte/gte
 * 和逻辑运算 and/or/not (共 13 个 builtin)。
 */
public abstract class BuiltinCallNode extends AsterExpressionNode {
  @CompilationFinal protected final String builtinName;
  @Children protected final AsterExpressionNode[] argNodes;

  protected BuiltinCallNode(String builtinName, AsterExpressionNode[] argNodes) {
    this.builtinName = builtinName;
    this.argNodes = argNodes;
  }

  /**
   * Guards helper methods
   */
  protected boolean isAdd() {
    return "add".equals(builtinName);
  }

  protected boolean isSub() {
    return "sub".equals(builtinName);
  }

  protected boolean isMul() {
    return "mul".equals(builtinName);
  }

  protected boolean isDiv() {
    return "div".equals(builtinName);
  }

  protected boolean isMod() {
    return "mod".equals(builtinName);
  }

  protected boolean isEq() {
    return "eq".equals(builtinName);
  }

  protected boolean isLt() {
    return "lt".equals(builtinName);
  }

  protected boolean isGt() {
    return "gt".equals(builtinName);
  }

  protected boolean isLte() {
    return "lte".equals(builtinName);
  }

  protected boolean isGte() {
    return "gte".equals(builtinName);
  }

  protected boolean isAnd() {
    return "and".equals(builtinName);
  }

  protected boolean isOr() {
    return "or".equals(builtinName);
  }

  protected boolean isNot() {
    return "not".equals(builtinName);
  }

  protected boolean hasTwoArgs() {
    return argNodes.length == 2;
  }

  protected boolean hasOneArg() {
    return argNodes.length == 1;
  }

  /**
   * 内联 add (int + int)
   */
  @Specialization(guards = {"isAdd()", "hasTwoArgs()"})
  protected int doAddInt(VirtualFrame frame) {
    Profiler.inc("builtin_add_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a + b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Integer) {
        return (int) result;
      }
      throw new RuntimeException(
          "Builtin 'add' expected int result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 sub (int - int)
   */
  @Specialization(guards = {"isSub()", "hasTwoArgs()"})
  protected int doSubInt(VirtualFrame frame) {
    Profiler.inc("builtin_sub_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a - b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Integer) {
        return (int) result;
      }
      throw new RuntimeException(
          "Builtin 'sub' expected int result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 mul (int * int)
   */
  @Specialization(guards = {"isMul()", "hasTwoArgs()"})
  protected int doMulInt(VirtualFrame frame) {
    Profiler.inc("builtin_mul_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a * b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Integer) {
        return (int) result;
      }
      throw new RuntimeException(
          "Builtin 'mul' expected int result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 div (int / int)，检查除数为零
   */
  @Specialization(guards = {"isDiv()", "hasTwoArgs()"})
  protected int doDivInt(VirtualFrame frame) {
    Profiler.inc("builtin_div_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a / b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Integer) {
        return (int) result;
      }
      throw new RuntimeException(
          "Builtin 'div' expected int result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 mod (int % int)，检查除数为零
   */
  @Specialization(guards = {"isMod()", "hasTwoArgs()"})
  protected int doModInt(VirtualFrame frame) {
    Profiler.inc("builtin_mod_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a % b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Integer) {
        return (int) result;
      }
      throw new RuntimeException(
          "Builtin 'mod' expected int result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 eq (int == int)
   */
  @Specialization(guards = {"isEq()", "hasTwoArgs()"})
  protected boolean doEqInt(VirtualFrame frame) {
    Profiler.inc("builtin_eq_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a == b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'eq' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 lt (int < int)
   */
  @Specialization(guards = {"isLt()", "hasTwoArgs()"})
  protected boolean doLtInt(VirtualFrame frame) {
    Profiler.inc("builtin_lt_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a < b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'lt' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 gt (int > int)
   */
  @Specialization(guards = {"isGt()", "hasTwoArgs()"})
  protected boolean doGtInt(VirtualFrame frame) {
    Profiler.inc("builtin_gt_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a > b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'gt' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 lte (int <= int)
   */
  @Specialization(guards = {"isLte()", "hasTwoArgs()"})
  protected boolean doLteInt(VirtualFrame frame) {
    Profiler.inc("builtin_lte_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a <= b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'lte' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 gte (int >= int)
   */
  @Specialization(guards = {"isGte()", "hasTwoArgs()"})
  protected boolean doGteInt(VirtualFrame frame) {
    Profiler.inc("builtin_gte_inlined");

    try {
      int a = argNodes[0].executeInt(frame);
      int b = argNodes[1].executeInt(frame);
      return a >= b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'gte' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 and (boolean && boolean)
   */
  @Specialization(guards = {"isAnd()", "hasTwoArgs()"})
  protected boolean doAndBoolean(VirtualFrame frame) {
    Profiler.inc("builtin_and_inlined");

    try {
      boolean a = argNodes[0].executeBoolean(frame);
      boolean b = argNodes[1].executeBoolean(frame);
      return a && b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'and' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 or (boolean || boolean)
   */
  @Specialization(guards = {"isOr()", "hasTwoArgs()"})
  protected boolean doOrBoolean(VirtualFrame frame) {
    Profiler.inc("builtin_or_inlined");

    try {
      boolean a = argNodes[0].executeBoolean(frame);
      boolean b = argNodes[1].executeBoolean(frame);
      return a || b;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object arg1 = argNodes[1].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0, arg1});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'or' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * 内联 not (!boolean)
   * 注意：not 只需要 1 个参数
   */
  @Specialization(guards = {"isNot()", "hasOneArg()"})
  protected boolean doNotBoolean(VirtualFrame frame) {
    Profiler.inc("builtin_not_inlined");

    try {
      boolean a = argNodes[0].executeBoolean(frame);
      return !a;
    } catch (Exception e) {
      // Fallback 到通用路径
      Object arg0 = argNodes[0].executeGeneric(frame);
      Object result = Builtins.call(builtinName, new Object[]{arg0});
      if (result instanceof Boolean) {
        return (boolean) result;
      }
      throw new RuntimeException(
          "Builtin 'not' expected boolean result, got: " +
          (result == null ? "null" : result.getClass().getSimpleName()));
    }
  }

  /**
   * Fallback: 调用 Builtins.call（通用路径）
   * 处理所有未内联的 builtin 或类型不匹配的情况
   */
  @Specialization(replaces = {"doAddInt", "doSubInt", "doMulInt", "doDivInt", "doModInt",
                               "doEqInt", "doLtInt", "doGtInt", "doLteInt", "doGteInt",
                               "doAndBoolean", "doOrBoolean", "doNotBoolean"})
  protected Object doGeneric(VirtualFrame frame) {
    Profiler.inc("builtin_call_generic");

    // 执行参数节点
    Object[] args = new Object[argNodes.length];
    for (int i = 0; i < argNodes.length; i++) {
      args[i] = argNodes[i].executeGeneric(frame);
    }

    // 直接调用 Builtins.call，不包装异常以保持原始错误消息
    return Builtins.call(builtinName, args);
  }

  @Override
  public String toString() {
    return "BuiltinCallNode(" + builtinName + ", " + argNodes.length + " args)";
  }
}
