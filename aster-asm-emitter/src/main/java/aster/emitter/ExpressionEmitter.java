package aster.emitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.NEW;

/**
 * 负责将 CoreModel 表达式分发给具体的字节码生成逻辑。
 */
public final class ExpressionEmitter {
  @SuppressWarnings("unused")
  private final ContextBuilder context;
  @SuppressWarnings("unused")
  private final TypeResolver typeResolver;
  private final ScopeStack defaultScope;
  private final Map<String, String> stringPool;
  @SuppressWarnings("unused")
  private final Main.Ctx ctx;
  @SuppressWarnings("unused")
  private final String currentPkg;
  @SuppressWarnings("unused")
  private final int paramBase;
  private Map<String, Integer> baseEnv;
  private final NameEmitter nameEmitter;
  private final CallEmitter callEmitter;

  public ExpressionEmitter(ContextBuilder context, TypeResolver typeResolver, ScopeStack scope, Map<String, String> stringPool, NameEmitter nameEmitter, CallEmitter callEmitter) {
    this(context, typeResolver, scope, stringPool, null, null, 0, null, nameEmitter, callEmitter);
  }

  public ExpressionEmitter(Main.Ctx ctx, String currentPkg, int paramBase, Map<String, Integer> env, ScopeStack scope, TypeResolver typeResolver, NameEmitter nameEmitter, CallEmitter callEmitter) {
    this(Objects.requireNonNull(ctx, "ctx").contextBuilder(), typeResolver, scope, ctx.stringPool(), ctx, currentPkg, paramBase, env, nameEmitter, callEmitter);
  }

  private ExpressionEmitter(
    ContextBuilder context,
    TypeResolver typeResolver,
    ScopeStack scope,
    Map<String, String> stringPool,
    Main.Ctx ctx,
    String currentPkg,
    int paramBase,
    Map<String, Integer> env,
    NameEmitter nameEmitter,
    CallEmitter callEmitter
  ) {
    this.context = Objects.requireNonNull(context, "context");
    this.typeResolver = typeResolver;
    this.defaultScope = scope;
    this.stringPool = stringPool;
    this.ctx = ctx;
    this.currentPkg = currentPkg;
    this.paramBase = paramBase;
    this.baseEnv = env;
    this.nameEmitter = Objects.requireNonNull(nameEmitter, "nameEmitter");
    this.callEmitter = Objects.requireNonNull(callEmitter, "callEmitter");
  }

  public void emitExpression(CoreModel.Expr expr, MethodVisitor mv, ScopeStack scopeOverride, String expectedDesc) {
    Objects.requireNonNull(expr, "expr");
    Objects.requireNonNull(mv, "mv");
    ScopeStack effectiveScope = scopeOverride != null ? scopeOverride : defaultScope;
    if (effectiveScope != null) {
      // 常量表达式当前不依赖作用域信息，该判断用于保持字段引用以便未来扩展
    }
    switch (expr) {
      case CoreModel.IntE intExpr -> emitInt(intExpr, mv, expectedDesc);
      case CoreModel.Bool boolExpr -> emitBool(boolExpr, mv, expectedDesc);
      case CoreModel.StringE stringExpr -> emitString(stringExpr, mv);
      case CoreModel.LongE longExpr -> emitLong(longExpr, mv, expectedDesc);
      case CoreModel.DoubleE doubleExpr -> emitDouble(doubleExpr, mv, expectedDesc);
      case CoreModel.NullE ignored -> emitNull(mv);
      case CoreModel.NoneE ignored -> emitNull(mv);
      case CoreModel.Name nameExpr -> emitName(nameExpr, mv, effectiveScope, expectedDesc);
      case CoreModel.Call callExpr -> emitCall(callExpr, mv, effectiveScope, expectedDesc);
      case CoreModel.Ok ok -> emitOk(ok, mv, effectiveScope, expectedDesc);
      case CoreModel.Err err -> emitErr(err, mv, effectiveScope, expectedDesc);
      case CoreModel.Some some -> emitSome(some, mv, effectiveScope, expectedDesc);
      case CoreModel.Construct construct -> emitConstruct(construct, mv, effectiveScope, expectedDesc);
      case CoreModel.Lambda lambda -> emitLambda(lambda, mv, effectiveScope);
      case null, default -> throw new UnsupportedOperationException("Expression type not yet migrated: " + expr.getClass().getSimpleName());
    }
  }

  void updateEnvironment(Map<String, Integer> env) {
    this.baseEnv = env;
  }

  private void emitInt(CoreModel.IntE expr, MethodVisitor mv, String expectedDesc) {
    if ("J".equals(expectedDesc)) {
      AsmUtilities.emitConstLong(mv, expr.value);
      return;
    }
    if ("D".equals(expectedDesc)) {
      AsmUtilities.emitConstDouble(mv, (double) expr.value);
      return;
    }
    AsmUtilities.emitConstInt(mv, expr.value);
    AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
  }

  private void emitBool(CoreModel.Bool expr, MethodVisitor mv, String expectedDesc) {
    mv.visitInsn(expr.value ? ICONST_1 : ICONST_0);
    AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
  }

  private void emitString(CoreModel.StringE expr, MethodVisitor mv) {
    String value = expr.value;
    String pooled = value;
    if (stringPool != null) {
      pooled = stringPool.computeIfAbsent(value, k -> k);
    }
    AsmUtilities.emitConstString(mv, pooled);
  }

  private void emitLong(CoreModel.LongE expr, MethodVisitor mv, String expectedDesc) {
    AsmUtilities.emitConstLong(mv, expr.value);
    if ("I".equals(expectedDesc) || "Z".equals(expectedDesc)) {
      mv.visitInsn(L2I);
      return;
    }
    if ("D".equals(expectedDesc)) {
      mv.visitInsn(L2D);
      AsmUtilities.boxPrimitiveResult(mv, 'D', expectedDesc);
      return;
    }
    AsmUtilities.boxPrimitiveResult(mv, 'J', expectedDesc);
  }

  private void emitDouble(CoreModel.DoubleE expr, MethodVisitor mv, String expectedDesc) {
    AsmUtilities.emitConstDouble(mv, expr.value);
    AsmUtilities.boxPrimitiveResult(mv, 'D', expectedDesc);
  }

  private void emitNull(MethodVisitor mv) {
    mv.visitInsn(ACONST_NULL);
  }

  private void emitName(CoreModel.Name name, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    ScopeStack activeScope = (scopeStack != null) ? scopeStack : defaultScope;
    Map<String, Integer> env = baseEnv;
    boolean handled = nameEmitter.tryEmitName(
      mv,
      name,
      expectedDesc,
      currentPkg,
      paramBase,
      env,
      activeScope
    );
    if (!handled) {
      mv.visitInsn(ACONST_NULL);
    }
  }

  private void emitCall(CoreModel.Call call, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    Map<String, Integer> env = baseEnv;
    ScopeStack effectiveScope = (scopeStack != null) ? scopeStack : defaultScope;
    try {
      boolean handled = callEmitter.tryEmitCall(
        mv,
        call,
        expectedDesc,
        currentPkg,
        paramBase,
        env,
        effectiveScope,
        (visitor, expr, desc, pkg, base, scopedEnv, inlineScope) -> {
          Map<String, Integer> previousEnv = baseEnv;
          if (scopedEnv != null) {
            baseEnv = scopedEnv;
          }
          try {
            ScopeStack nextScope = (inlineScope != null) ? inlineScope : effectiveScope;
            emitExpression(expr, visitor, nextScope, desc);
          } finally {
            baseEnv = previousEnv;
          }
        }
      );
      if (handled) {
        return;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    if (call.target != null) {
      emitExpression(call.target, mv, effectiveScope, expectedDesc);
    } else {
      mv.visitInsn(ACONST_NULL);
    }
  }

  private void emitOk(CoreModel.Ok ok, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    mv.visitTypeInsn(NEW, "aster/runtime/Ok");
    mv.visitInsn(DUP);
    emitExpression(ok.expr, mv, scopeStack, "Ljava/lang/Object;");
    mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Ok", "<init>", "(Ljava/lang/Object;)V", false);
  }

  private void emitErr(CoreModel.Err err, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    mv.visitTypeInsn(NEW, "aster/runtime/Err");
    mv.visitInsn(DUP);
    emitExpression(err.expr, mv, scopeStack, "Ljava/lang/Object;");
    mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Err", "<init>", "(Ljava/lang/Object;)V", false);
  }

  /**
   * 发射 Some 表达式（策略1：直接值传递）
   *
   * Maybe 类型使用 null 表示 None，非 null 表示 Some(value)
   * Some(value) 直接发射 value 的字节码，不需要对象包装
   *
   * 这种策略基于以下假设：
   * 1) aster-runtime 中不存在 Some 包装类
   * 2) Maybe 类型是语法糖，运行时使用 null/非null 表示
   * 3) 与 Result 类型不同，Maybe 不需要错误处理开销
   *
   * 如果 Golden 测试失败，将切换到策略2（创建 Some 包装类）
   */
  private void emitSome(CoreModel.Some some, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    // 直接发射内部表达式的字节码，无包装
    emitExpression(some.expr, mv, scopeStack, expectedDesc);
  }

  private void emitConstruct(CoreModel.Construct cons, MethodVisitor mv, ScopeStack scopeStack, String expectedDesc) {
    // Resolve package: use currentPkg if available, otherwise use module name from context
    String pkg = currentPkg != null ? currentPkg : context.module().name;
    String internal = cons.typeName.contains(".")
        ? cons.typeName.replace('.', '/')
        : toInternal(pkg, cons.typeName);
    mv.visitTypeInsn(NEW, internal);
    mv.visitInsn(DUP);

    CoreModel.Data dataType = context.lookupData(cons.typeName);
    StringBuilder descSb = new StringBuilder("(");

    for (var f : cons.fields) {
      String fieldDesc = "Ljava/lang/Object;";

      if (dataType != null) {
        for (var schemaField : dataType.fields) {
          if (schemaField.name.equals(f.name)) {
            if (schemaField.type instanceof CoreModel.TypeName tn) {
              fieldDesc = switch (tn.name) {
                case "Int" -> "I";
                case "Bool" -> "Z";
                case "Long" -> "J";
                case "Double" -> "D";
                case "Text" -> "Ljava/lang/String;";
                default -> "Ljava/lang/Object;";
              };
            }
            break;
          }
        }
      }

      emitExpression(f.expr, mv, scopeStack, fieldDesc);
      descSb.append(fieldDesc);
    }

    descSb.append(")V");
    mv.visitMethodInsn(INVOKESPECIAL, internal, "<init>", descSb.toString(), false);
  }

  /**
   * 发射 Lambda 表达式（委托给 LambdaEmitter）
   *
   * Lambda 表达式生成过程：
   * 1. 创建 LambdaBodyEmitter 回调，用于处理 Lambda 体
   * 2. 实例化 LambdaEmitter 并调用 emitLambda
   * 3. LambdaEmitter 负责：
   *    - 生成 Lambda$N 类（实现 aster.runtime.FnN 接口）
   *    - 生成闭包字段（cap$<varname>）
   *    - 生成构造函数和 apply 方法
   *    - 在当前方法中生成实例化代码（NEW, DUP, 加载闭包, INVOKESPECIAL）
   *
   * @throws UnsupportedOperationException 如果 ExpressionEmitter 构造时未提供 Main.Ctx
   */
  private void emitLambda(CoreModel.Lambda lam, MethodVisitor mv, ScopeStack scopeStack) {
    // Lambda 表达式需要 Main.Ctx 来生成类文件和管理 Lambda 序列号
    if (ctx == null) {
      throw new UnsupportedOperationException(
        "Lambda expressions require Main.Ctx. " +
        "Use ExpressionEmitter(Main.Ctx, ...) constructor instead of ExpressionEmitter(ContextBuilder, ...) constructor."
      );
    }

    // 创建 body emitter 回调 - 委托给 Main.emitApplyBlock 处理 Lambda 体
    LambdaEmitter.LambdaBodyEmitter bodyEmitter = (ctx2, mv2, body, internal, env, primTypes, retIsResult, lineNo) -> {
      return Main.emitApplyBlock(ctx2, mv2, body, internal, env, primTypes, retIsResult, lineNo);
    };

    // 创建 LambdaEmitter 并执行
    LambdaEmitter lambdaEmitter = new LambdaEmitter(typeResolver, ctx, bodyEmitter);
    lambdaEmitter.emitLambda(mv, lam, currentPkg, baseEnv, scopeStack);
  }

  private static String toInternal(String pkg, String cls) {
    if (pkg == null || pkg.isEmpty()) return cls;
    return pkg.replace('.', '/') + "/" + cls;
  }
}
