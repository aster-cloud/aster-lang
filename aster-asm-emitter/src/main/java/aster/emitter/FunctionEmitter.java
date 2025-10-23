package aster.emitter;

import aster.core.ir.CoreModel;

import org.objectweb.asm.*;

import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * 负责函数字节码生成的类。
 *
 * 职责：
 * - 创建函数类（{functionName}_fn.class）
 * - 生成方法签名和参数注解
 * - 发射函数体字节码
 * - 管理局部变量表
 * - 处理特殊优化（Fast-path）
 *
 * 设计原则：
 * - 保持与 Main.emitFunc 的字节码输出完全一致
 * - 通过 Golden 测试验证正确性
 * - 渐进式迁移，每批次独立验证
 */
public class FunctionEmitter {
    private final Main.Ctx ctx;
    private final ContextBuilder contextBuilder;
    private final TypeResolver typeResolver;
    private final ExpressionEmitter expressionEmitter;
    private final MatchEmitter matchEmitter;
    private final IfEmitter ifEmitter;
    private final StdlibInliner stdlibInliner;

    /**
     * 局部变量表记录
     */
    public record LV(String name, String desc, int slot) {}

    public FunctionEmitter(Main.Ctx ctx, ContextBuilder contextBuilder,
                          TypeResolver typeResolver, ExpressionEmitter expressionEmitter,
                          MatchEmitter matchEmitter, IfEmitter ifEmitter,
                          StdlibInliner stdlibInliner) {
        this.ctx = ctx;
        this.contextBuilder = contextBuilder;
        this.typeResolver = typeResolver;
        this.expressionEmitter = expressionEmitter;
        this.matchEmitter = matchEmitter;
        this.ifEmitter = ifEmitter;
        this.stdlibInliner = Objects.requireNonNull(stdlibInliner, "stdlibInliner");
    }

    /**
     * 发射函数字节码。
     *
     * @param pkg 包名
     * @param mod 模块
     * @param fn 函数定义
     * @throws IOException 如果写入类文件失败
     */
    public void emitFunction(String pkg, CoreModel.Module mod, CoreModel.Func fn) throws IOException {
        // Batch 1: 类创建和方法签名生成
        var className = fn.name + "_fn";
        var internal = Main.toInternal(pkg, className);
        var cw = AsmUtilities.createClassWriter();
        cw.visit(V25, ACC_PUBLIC | ACC_FINAL, internal, null, "java/lang/Object", null);
        cw.visitSource(className + ".java", null);

        var retDesc = Main.jDesc(pkg, fn.ret);
        var paramsDesc = new StringBuilder("(");
        for (var p : fn.params) {
            paramsDesc.append(Main.jDesc(pkg, p.type));
        }
        paramsDesc.append(")").append(retDesc);

        var mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, fn.name, paramsDesc.toString(), null, null);
        for (int idx = 0; idx < fn.params.size(); idx++) {
            var p = fn.params.get(idx);
            mv.visitParameter(p.name, 0);
            Main.emitParameterAnnotations(mv, idx, p);
        }
        Main.addOriginAnnotation(mv, fn.origin);
        mv.visitCode();

        var lStart = new Label();
        mv.visitLabel(lStart);
        mv.visitLineNumber(1, lStart);
        System.out.println(Main.withOrigin("EMIT FUNC: " + pkg + "." + fn.name, fn.origin));

        // 初始化局部变量表
        java.util.List<LV> lvars = new java.util.ArrayList<>();
        for (int i = 0; i < fn.params.size(); i++) {
            lvars.add(new LV(
                fn.params.get(i).name,
                Main.jDesc(pkg, fn.params.get(i).type),
                i
            ));
        }

        // Batch 2: Fast-path 优化
        // 特殊处理 app.math 和 app.debug 包的简单函数
        if ((Objects.equals(pkg, "app.math") || Objects.equals(pkg, "app.debug")) && fn.params.size() == 2) {
            boolean intInt = fn.params.stream().allMatch(p ->
                p.type instanceof CoreModel.TypeName tn && Objects.equals(tn.name, "Int"));
            if (intInt && fn.ret instanceof CoreModel.TypeName rtn) {
                // Fast-path for add/add2
                if ((Objects.equals(fn.name, "add") || Objects.equals(fn.name, "add2"))
                    && Objects.equals(((CoreModel.TypeName) fn.ret).name, "Int")) {
                    System.out.println(Main.withOrigin("FAST-PATH ADD: emitting ILOAD/ILOAD/IADD/IRETURN", fn.origin));
                    mv.visitVarInsn(ILOAD, 0);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitInsn(IADD);
                    mv.visitInsn(IRETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    var bytes = cw.toByteArray();
                    System.out.println(Main.withOrigin("FAST-PATH ADD: class size=" + bytes.length + " bytes", fn.origin));
                    AsmUtilities.writeClass(ctx.outDir().toString(), internal, bytes);
                    return;
                }
                // Fast-path for cmp/cmp2
                if ((Objects.equals(fn.name, "cmp") || Objects.equals(fn.name, "cmp2"))
                    && Objects.equals(((CoreModel.TypeName) fn.ret).name, "Bool")) {
                    System.out.println(Main.withOrigin("FAST-PATH CMP: emitting IF_ICMPLT logic", fn.origin));
                    var lT = new Label();
                    var lE = new Label();
                    mv.visitVarInsn(ILOAD, 0);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitJumpInsn(IF_ICMPLT, lT);
                    mv.visitInsn(ICONST_0);
                    mv.visitJumpInsn(GOTO, lE);
                    mv.visitLabel(lT);
                    mv.visitInsn(ICONST_1);
                    mv.visitLabel(lE);
                    mv.visitInsn(IRETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    var bytes = cw.toByteArray();
                    System.out.println(Main.withOrigin("FAST-PATH CMP: class size=" + bytes.length + " bytes", fn.origin));
                    AsmUtilities.writeClass(ctx.outDir().toString(), internal, bytes);
                    return;
                }
            }
        }

        // Batch 3: 环境初始化 + Let 语句处理 + EmitContext 封装
        Map<String, Character> fnHints = ctx.funcHints().getOrDefault(pkg + "." + fn.name, java.util.Collections.emptyMap());
        var emitCtx = new EmitContext(fn.params.size(), pkg, retDesc, fnHints);
        int nextSlot = fn.params.size();
        var env = emitCtx.getEnv();

        // 使用注入的 typeResolver 及其关联的 scopeStack（修复依赖注入问题）
        var scopeStack = typeResolver.getScopeStack();

        // 初始化参数到环境
        for (int i = 0; i < fn.params.size(); i++) {
            var p = fn.params.get(i);
            env.put(p.name, i);
            var desc = Main.jDesc(pkg, p.type);
            scopeStack.declare(p.name, i, desc, Main.kindForDescriptor(desc));
        }

        // 处理函数体语句
        if (fn.body != null && fn.body.statements != null && !fn.body.statements.isEmpty()) {
            // 直接使用注入的 typeResolver（修复依赖注入问题）
            // 行号从 2 开始，与原实现一致（Main.java:339）
            var lineNo = new java.util.concurrent.atomic.AtomicInteger(2);

            for (var st : fn.body.statements) {
                var _lbl = new Label();
                mv.visitLabel(_lbl);
                mv.visitLineNumber(lineNo.getAndIncrement(), _lbl);

                if (st instanceof CoreModel.If iff) {
                    // Batch 4: 使用 IfEmitter 处理 If 语句
                    // 同步 EmitContext 的 nextSlot 状态
                    emitCtx.setNextSlot(nextSlot);
                    ifEmitter.emitIf(mv, iff, emitCtx, scopeStack, lvars, fnHints, lineNo);
                    // 同步回 nextSlot 状态
                    nextSlot = emitCtx.peekNextSlot();
                    continue;
                } else if (st instanceof CoreModel.Let let) {
                    // 类型推断
                    Character inferred = typeResolver.inferType(let.expr);
                    if (inferred == null && Objects.equals(let.name, "ok") && let.expr instanceof CoreModel.Call) {
                        inferred = 'Z';
                    }
                    if (inferred == null) {
                        Character hint = fnHints.get(let.name);
                        if (hint != null) inferred = hint;
                    }

                    // 确定描述符和存储指令
                    String expectedDesc = null;
                    String localDesc = "Ljava/lang/Object;";
                    int storeOpcode = ASTORE;

                    if (inferred != null) {
                        switch (inferred) {
                            case 'D' -> {
                                expectedDesc = "D";
                                localDesc = "D";
                                storeOpcode = DSTORE;
                            }
                            case 'J' -> {
                                expectedDesc = "J";
                                localDesc = "J";
                                storeOpcode = LSTORE;
                            }
                            case 'Z' -> {
                                expectedDesc = "Z";
                                localDesc = "Z";
                                storeOpcode = ISTORE;
                            }
                            case 'I' -> {
                                expectedDesc = "I";
                                localDesc = "I";
                                storeOpcode = ISTORE;
                            }
                            default -> { /* fall back to object */ }
                        }
                    }

                    // 发射表达式
                    if (expectedDesc != null) {
                        emitExpr(mv, let.expr, expectedDesc, pkg, env, scopeStack);
                    } else {
                        emitExpr(mv, let.expr, null, pkg, env, scopeStack);
                        localDesc = Main.resolveObjectDescriptor(let.expr, pkg, scopeStack, ctx);
                    }

                    // 存储变量并更新环境
                    mv.visitVarInsn(storeOpcode, nextSlot);
                    env.put(let.name, nextSlot);
                    lvars.add(new LV(let.name, localDesc, nextSlot));
                    scopeStack.declare(let.name, nextSlot, localDesc, Main.kindForDescriptor(localDesc));
                    nextSlot++;
                    continue;
                } else if (st instanceof CoreModel.Match match) {
                    // Batch 3: 使用 MatchEmitter 处理 Match 语句
                    // 同步 EmitContext 的 nextSlot 状态
                    emitCtx.setNextSlot(nextSlot);
                    matchEmitter.emitMatch(mv, match, emitCtx, scopeStack, lvars);
                    // 同步回 nextSlot 状态
                    nextSlot = emitCtx.peekNextSlot();
                    continue;
                } else if (st instanceof CoreModel.Return ret) {
                    // Batch 4: 使用 ReturnEmitter 统一处理 Return 语句
                    // Phase 6: 创建 ExprEmitter 适配器，优先使用 ExpressionEmitter 处理已迁移的表达式类型
                    final int[] nextSlotBox = {nextSlot};  // Mutable wrapper for lambda
                    ReturnEmitter.emitReturn(
                        mv, ret, retDesc, pkg, env, scopeStack, ctx, typeResolver,
                        () -> nextSlotBox[0]++,  // SlotProvider
                        (ctx2, mv2, expr, expectedDesc2, pkg2, paramBase, env2, scopeStack2, typeResolver2) -> {
                            // ExprEmitter 适配器：根据表达式类型路由到 ExpressionEmitter 或 Main.emitExpr
                            expressionEmitter.updateEnvironment(env2);
                            if (isMigratedExpressionType(expr)) {
                                expressionEmitter.emitExpression(expr, mv2, scopeStack2, expectedDesc2);
                            } else {
                                Main.emitExpr(ctx2, mv2, expr, expectedDesc2, pkg2, paramBase, env2, scopeStack2, typeResolver2);
                            }
                        }
                    );
                    nextSlot = nextSlotBox[0];  // 更新 nextSlot

                    var lEndReturn = new Label();
                    mv.visitLabel(lEndReturn);
                    for (var lv : lvars) {
                        mv.visitLocalVariable(lv.name, lv.desc, null, lStart, lEndReturn, lv.slot);
                    }
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();

                    var bytes = cw.toByteArray();
                    AsmUtilities.writeClass(ctx.outDir().toString(), internal, bytes);
                    return;
                }

                // TODO: Batch 6 - 其他语句类型处理
                // 如果遇到其他语句类型，暂时抛出异常
                throw new UnsupportedOperationException("Statement type not yet supported in FunctionEmitter: " + st.getClass().getSimpleName());
            }
        }

        // TODO: Batch 6 - 默认返回处理（如果没有显式 return）
        // 临时实现：简单返回默认值
        Main.emitDefaultReturn(mv, fn.ret);

        // 生成局部变量表
        var lEnd = new Label();
        mv.visitLabel(lEnd);
        for (var lv : lvars) {
            mv.visitLocalVariable(lv.name, lv.desc, null, lStart, lEnd, lv.slot);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        var bytes = cw.toByteArray();
        AsmUtilities.writeClass(ctx.outDir().toString(), internal, bytes);
    }

    /**
     * 判断表达式是否为字面量（Int/Bool/String/Long/Double/Null）
     */
    private static boolean isLiteral(CoreModel.Expr expr) {
        return expr instanceof CoreModel.IntE
            || expr instanceof CoreModel.Bool
            || expr instanceof CoreModel.LongE
            || expr instanceof CoreModel.DoubleE
            || expr instanceof CoreModel.StringE
            || expr instanceof CoreModel.NullE;
    }

    /**
     * 判断表达式是否已迁移到 ExpressionEmitter
     *
     * 已迁移类型：Int/Bool/String/Long/Double/Null/NoneE/Name/Call/Ok/Err/Some/Construct/Lambda
     * 未迁移类型：If/Match/Block/Let 等
     *
     * Package-private 访问级别，允许 IfEmitter 等同包类复用此方法。
     */
    static boolean isMigratedExpressionType(CoreModel.Expr expr) {
        return isLiteral(expr)
            || expr instanceof CoreModel.Name
            || expr instanceof CoreModel.Call
            || expr instanceof CoreModel.Ok
            || expr instanceof CoreModel.Err
            || expr instanceof CoreModel.Some
            || expr instanceof CoreModel.NoneE
            || expr instanceof CoreModel.Construct
            || expr instanceof CoreModel.Lambda;
    }

    /**
     * 发射表达式字节码（优先使用 ExpressionEmitter 处理已迁移的表达式类型）
     */
    private void emitExpr(MethodVisitor mv, CoreModel.Expr expr, String expectedDesc, String pkg,
        Map<String, Integer> env, ScopeStack scopeStack) throws IOException {
        expressionEmitter.updateEnvironment(env);
        if (isMigratedExpressionType(expr)) {
            // 使用 ExpressionEmitter 处理已迁移的表达式类型
            expressionEmitter.emitExpression(expr, mv, scopeStack, expectedDesc);
        } else {
            // 使用 Main.emitExpr 处理尚未迁移的表达式类型
            Main.emitExpr(ctx, mv, expr, expectedDesc, pkg, 0, env, scopeStack, typeResolver);
        }
    }
}
