package aster.emitter;

import aster.core.ir.CoreModel;

import java.io.IOException;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;

/**
 * 表达式发射器回调接口，用于 CallEmitter 回调主调方法发射参数表达式。
 */
@FunctionalInterface
public interface ExprEmitterCallback {
  void emitExpr(
      MethodVisitor mv,
      CoreModel.Expr expr,
      String expectedDesc,
      String currentPkg,
      int paramBase,
      Map<String, Integer> env,
      ScopeStack scopeStack
  ) throws IOException;
}
