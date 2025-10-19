package aster.emitter;

import org.objectweb.asm.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

/**
 * Lambda Apply 方法体发射器
 *
 * 负责生成 Lambda 内部类的 apply 方法实现。
 * Lambda apply 方法有特殊约束：
 * - 参数通过 env Map 传递（闭包变量 + Lambda 参数）
 * - 原始类型通过 primTypes Map 标记（用于装箱）
 * - 返回类型固定为 Object（需要时包装为 Result）
 *
 * Phase 15: 从 Main.java 迁移而来
 */
public class LambdaApplyEmitter {
  private final Main.Ctx ctx;
  private final StdlibInliner stdlibInliner;

  public LambdaApplyEmitter(Main.Ctx ctx) {
    this.ctx = ctx;
    this.stdlibInliner = StdlibInliner.instance();
  }

  /**
   * 发射 Lambda apply 方法体（Block）
   *
   * @param mv MethodVisitor for apply method
   * @param b Lambda body block
   * @param ownerInternal Lambda 内部类的 internal name
   * @param env 环境映射（变量名 → JVM slot）
   * @param primTypes 原始类型映射（变量名 → 类型字符 I/Z/J/D）
   * @param retIsResult 返回值是否需要 Result 包装
   * @param lineNo 行号计数器
   * @return true 如果 block 所有路径都返回
   */
  public boolean emitApplyBlock(
      MethodVisitor mv,
      CoreModel.Block b,
      String ownerInternal,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      boolean retIsResult,
      AtomicInteger lineNo) {
    if (b == null || b.statements == null) return false;
    for (var s : b.statements) {
      var _lbl = new Label();
      mv.visitLabel(_lbl);
      mv.visitLineNumber(lineNo.getAndIncrement(), _lbl);
      if (emitApplyStmt(mv, s, ownerInternal, env, primTypes, retIsResult, lineNo)) return true;
    }
    return false;
  }

  /**
   * 发射 Lambda apply 方法中的单个语句
   *
   * 支持的语句类型：
   * - Return：直接返回或包装为 Result（try/catch）
   * - Let：绑定局部变量
   * - If：条件分支
   * - Match：模式匹配
   *
   * @return true 如果语句一定返回（不会继续执行后续语句）
   */
  public boolean emitApplyStmt(
      MethodVisitor mv,
      CoreModel.Stmt s,
      String ownerInternal,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      boolean retIsResult,
      AtomicInteger lineNo) {
    if (s instanceof CoreModel.Return r) {
      if (retIsResult && r.expr instanceof CoreModel.Call) {
        // Result 包装路径：try/catch 转 Ok/Err
        var lTryStart = new Label();
        var lTryEnd = new Label();
        var lCatch = new Label();
        var lRet = new Label();
        mv.visitTryCatchBlock(lTryStart, lTryEnd, lCatch, "java/lang/Throwable");
        mv.visitLabel(lTryStart);
        emitApplySimpleExpr(mv, r.expr, env, primTypes);
        int tmp = nextLocal(env);
        mv.visitVarInsn(ASTORE, tmp);
        mv.visitTypeInsn(NEW, "aster/runtime/Ok");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, tmp);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Ok", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitLabel(lTryEnd);
        mv.visitJumpInsn(GOTO, lRet);
        mv.visitLabel(lCatch);
        int ex = nextLocal(env) + 1;
        mv.visitVarInsn(ASTORE, ex);
        mv.visitTypeInsn(NEW, "aster/runtime/Err");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, ex);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Err", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitLabel(lRet);
        // LVT for tmp/ex across try/catch
        mv.visitLocalVariable("_tmp", "Ljava/lang/Object;", null, lTryStart, lRet, tmp);
        mv.visitLocalVariable("_ex", "Ljava/lang/Throwable;", null, lCatch, lRet, ex);
        mv.visitInsn(ARETURN);
        return true;
      } else {
        // 普通 return
        emitApplySimpleExpr(mv, r.expr, env, primTypes);
        mv.visitInsn(ARETURN);
        return true;
      }
    }
    if (s instanceof CoreModel.Let let) {
      emitApplySimpleExpr(mv, let.expr, env, primTypes);
      int slot = nextLocal(env);
      mv.visitVarInsn(ASTORE, slot);
      env.put(let.name, slot);
      return false;
    }
    if (s instanceof CoreModel.If iff) {
      return IfEmitter.emitIfApply(
          mv, iff.cond, iff.thenBlock, iff.elseBlock,
          (m, expr) -> emitApplySimpleExpr(m, expr, env, primTypes),
          (m, block) -> emitApplyBlock(m, block, ownerInternal, env, primTypes, retIsResult, lineNo),
          lineNo
      );
    }
    if (s instanceof CoreModel.Match mm) {
      return LambdaMatchEmitter.emitMatch(
          ctx,
          mv,
          mm,
          ownerInternal,
          env,
          primTypes,
          retIsResult,
          lineNo,
          (m, expr, e, pt) -> emitApplySimpleExpr(m, expr, e, pt),
          (c, m, body, oi, e, pt, rir, ln) -> emitApplyCaseBody(m, body, oi, e, pt, rir, ln),
          (c, m, pattern, valSlot, oi, e, pt, failLabel) -> emitApplyPatMatchAndBind(m, pattern, valSlot, oi, e, pt, failLabel)
      );
    }
    return false;
  }

  /**
   * 发射简单表达式（仅支持 Lambda apply 中允许的表达式子集）
   *
   * 支持的表达式：
   * - StringE：字符串字面量
   * - Name：变量引用（从 env 加载，可能需要装箱）
   * - IntE：整数字面量（装箱为 Integer）
   * - Call：stdlib 函数调用（通过 StdlibInliner 内联）
   *
   * @param mv MethodVisitor
   * @param e 表达式
   * @param env 环境映射
   * @param primTypes 原始类型映射（可选）
   */
  public void emitApplySimpleExpr(
      MethodVisitor mv,
      CoreModel.Expr e,
      Map<String, Integer> env,
      Map<String, Character> primTypes) {
    if (e instanceof CoreModel.StringE s) {
      mv.visitLdcInsn(s.value);
      return;
    }
    if (e instanceof CoreModel.Name n) {
      Integer slot = env.get(n.name);
      if (slot != null) {
        if (primTypes != null && primTypes.containsKey(n.name)) {
          char k = primTypes.get(n.name);
          if (k == 'I') {
            mv.visitVarInsn(ILOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            return;
          }
          if (k == 'Z') {
            mv.visitVarInsn(ILOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            return;
          }
        }
        mv.visitVarInsn(ALOAD, slot);
        return;
      }
      mv.visitInsn(ACONST_NULL);
      return;
    }
    if (e instanceof CoreModel.IntE i) {
      mv.visitLdcInsn(Integer.valueOf(i.value));
      return;
    }
    if (e instanceof CoreModel.Call c && c.target instanceof CoreModel.Name nn) {
      var name = nn.name;
      // 尝试使用 StdlibInliner 内联 stdlib 函数
      if (c.args != null && StdlibInliner.tryInline(
          mv, name, c.args, env, primTypes,
          (m, expr, e2, pt) -> emitApplySimpleExpr(m, expr, e2, pt),
          Main::warnNullability
      )) {
        return;
      }
    }
    mv.visitInsn(ACONST_NULL);
  }

  /**
   * 重载版本（无 primTypes）
   */
  public void emitApplySimpleExpr(MethodVisitor mv, CoreModel.Expr e, Map<String, Integer> env) {
    emitApplySimpleExpr(mv, e, env, null);
  }

  /**
   * 发射 Match case body（LambdaMatchEmitter 回调）
   */
  private boolean emitApplyCaseBody(
      MethodVisitor mv,
      CoreModel.Stmt body,
      String ownerInternal,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      boolean retIsResult,
      AtomicInteger lineNo) {
    if (body instanceof CoreModel.Return r) {
      emitApplySimpleExpr(mv, r.expr, env, primTypes);
      mv.visitInsn(ARETURN);
      return true;
    } else if (body instanceof CoreModel.If iff) {
      return emitApplyStmt(mv, body, ownerInternal, env, primTypes, retIsResult, lineNo);
    } else if (body instanceof CoreModel.Scope sc) {
      CoreModel.Block b = new CoreModel.Block();
      b.statements = sc.statements;
      return emitApplyBlock(mv, b, ownerInternal, env, primTypes, retIsResult, lineNo);
    }
    return false;
  }

  /**
   * 模式匹配和绑定（PatMatchEmitter 委托）
   */
  private void emitApplyPatMatchAndBind(
      MethodVisitor mv,
      CoreModel.Pattern pat,
      int valSlot,
      String ownerInternal,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      Label failLabel) {
    PatMatchEmitter.emitPatMatch(
        mv,
        pat,
        valSlot,
        ownerInternal,
        env,
        primTypes,
        failLabel,
        typeName -> ctx.lookupData(typeName)
    );
  }

  /**
   * 分配下一个可用的局部变量 slot
   */
  private static int nextLocal(Map<String, Integer> env) {
    int max = 0;
    for (var v : env.values()) if (v != null && v > max) max = v;
    return max + 1;
  }
}
