package aster.emitter;

import org.objectweb.asm.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.objectweb.asm.Opcodes.*;

/**
 * IfEmitter - If 控制流字节码生成器
 *
 * 支持两种使用模式：
 * 1. 实例方法模式（FunctionEmitter 使用）：emitIf()
 * 2. 静态回调模式（Main.java 使用）：emitIfStatement(), emitIfApply()
 *
 * 字节码模式:
 * <pre>
 * - 评估条件 (expectedDesc = "Z")
 * - IFEQ lElse (if false, jump to else)
 * - lThen: then 块 (通过 blockEmitter 回调)
 * - GOTO lEnd (if then doesn't return)
 * - lElse: else 块 (通过 blockEmitter 回调)
 * - lEnd: (if either branch doesn't return)
 * </pre>
 */
final class IfEmitter {
    private final Main.Ctx ctx;
    private final TypeResolver typeResolver;
    private final ExpressionEmitter expressionEmitter;

    /**
     * 构造函数（用于 FunctionEmitter 实例方法模式）
     */
    public IfEmitter(Main.Ctx ctx, TypeResolver typeResolver, ExpressionEmitter expressionEmitter) {
        this.ctx = ctx;
        this.typeResolver = typeResolver;
        this.expressionEmitter = expressionEmitter;
    }

    /**
     * 实例方法：生成 If 语句字节码（FunctionEmitter 使用）
     *
     * @param mv MethodVisitor
     * @param iff If 语句节点
     * @param emitCtx 发射上下文
     * @param scopeStack 作用域栈
     * @param lvars 局部变量列表
     * @param fnHints 函数提示
     * @param lineNo 行号计数器
     * @throws Exception 生成过程中的异常
     */
    public void emitIf(MethodVisitor mv, CoreModel.If iff, EmitContext emitCtx,
                      ScopeStack scopeStack, List<FunctionEmitter.LV> lvars,
                      Map<String, Character> fnHints, AtomicInteger lineNo) {
        var lElse = new Label();
        var lEnd = new Label();
        var pkg = emitCtx.getPkg();
        var env = emitCtx.getEnv();

        // Emit condition (expected type: boolean/Z)
        Main.emitExpr(ctx, mv, iff.cond, "Z", pkg, 0, env, scopeStack, typeResolver);
        mv.visitJumpInsn(IFEQ, lElse);

        // Then block
        { var lThen = new Label(); mv.visitLabel(lThen); mv.visitLineNumber(lineNo.getAndIncrement(), lThen); }
        boolean thenRet = emitIfBlock(mv, iff.thenBlock, emitCtx, scopeStack, lvars, fnHints, lineNo);
        if (!thenRet) mv.visitJumpInsn(GOTO, lEnd);

        // Else block
        mv.visitLabel(lElse);
        { var lElseLn = new Label(); mv.visitLabel(lElseLn); mv.visitLineNumber(lineNo.getAndIncrement(), lElseLn); }
        boolean elseRet = false;
        if (iff.elseBlock != null) {
            elseRet = emitIfBlock(mv, iff.elseBlock, emitCtx, scopeStack, lvars, fnHints, lineNo);
        }
        if (!elseRet) mv.visitLabel(lEnd);
    }

    /**
     * 辅助方法：生成 If 块内的语句（for FunctionEmitter instance method）
     */
    private boolean emitIfBlock(MethodVisitor mv, CoreModel.Block block, EmitContext emitCtx,
                                ScopeStack scopeStack, List<FunctionEmitter.LV> lvars,
                                Map<String, Character> fnHints, AtomicInteger lineNo) {
        try {
            if (block == null || block.statements == null || block.statements.isEmpty()) {
                return false;
            }

            var pkg = emitCtx.getPkg();
            var env = emitCtx.getEnv();
            var retDesc = emitCtx.getRetDesc();
            int nextSlot = emitCtx.peekNextSlot();

            for (var st : block.statements) {
                if (st instanceof CoreModel.Return ret) {
                    // Phase 6: 创建 ExprEmitter 适配器，复用 emitExpr 方法
                    final int[] nextSlotBox = {nextSlot};  // Mutable wrapper for lambda
                    ReturnEmitter.emitReturn(
                        mv, ret, retDesc, pkg, env, scopeStack, ctx, typeResolver,
                        () -> nextSlotBox[0]++,  // SlotProvider
                        (ctx2, mv2, expr, expectedDesc2, pkg2, paramBase, env2, scopeStack2, typeResolver2) -> {
                            // ExprEmitter 适配器：复用 emitExpr 方法（已支持 ExpressionEmitter 路由）
                            emitExpr(mv2, expr, expectedDesc2, pkg2, env2, scopeStack2);
                        }
                    );
                    nextSlot = nextSlotBox[0];  // 更新 nextSlot
                    emitCtx.setNextSlot(nextSlot);
                    return true;
                } else if (st instanceof CoreModel.Let let) {
                    // 处理 Let 语句：支持变量声明和重新赋值
                    Character inferred = typeResolver.inferType(let.expr);
                    if (inferred == null) {
                        Character hint = fnHints.get(let.name);
                        if (hint != null) inferred = hint;
                    }

                    String expectedDesc = null;
                    String localDesc = "Ljava/lang/Object;";
                    int storeOpcode = ASTORE;

                    if (inferred != null) {
                        switch (inferred) {
                            case 'D' -> { expectedDesc = "D"; localDesc = "D"; storeOpcode = DSTORE; }
                            case 'J' -> { expectedDesc = "J"; localDesc = "J"; storeOpcode = LSTORE; }
                            case 'Z' -> { expectedDesc = "Z"; localDesc = "Z"; storeOpcode = ISTORE; }
                            case 'I' -> { expectedDesc = "I"; localDesc = "I"; storeOpcode = ISTORE; }
                            default -> { /* fall back to object */ }
                        }
                    }

                    if (expectedDesc != null) {
                        emitExpr(mv, let.expr, expectedDesc, pkg, env, scopeStack);
                    } else {
                        emitExpr(mv, let.expr, null, pkg, env, scopeStack);
                        localDesc = Main.resolveObjectDescriptor(let.expr, pkg, scopeStack, ctx);
                    }

                    // 检查是否为变量重新赋值（reuse existing slot）
                    Integer existingSlot = env.get(let.name);
                    if (existingSlot != null) {
                        // 重新赋值：使用现有 slot
                        mv.visitVarInsn(storeOpcode, existingSlot);
                    } else {
                        // 新变量：分配新 slot
                        mv.visitVarInsn(storeOpcode, nextSlot);
                        env.put(let.name, nextSlot);
                        lvars.add(new FunctionEmitter.LV(let.name, localDesc, nextSlot));
                        scopeStack.declare(let.name, nextSlot, localDesc, Main.kindForDescriptor(localDesc));
                        nextSlot++;
                        emitCtx.setNextSlot(nextSlot);
                    }
                } else if (st instanceof CoreModel.If nestedIf) {
                    // 处理嵌套 If 语句 - Handle nested If statement
                    emitIf(mv, nestedIf, emitCtx, scopeStack, lvars, fnHints, lineNo);
                } else {
                    throw new UnsupportedOperationException("Statement type not supported in IfEmitter: " + st.getClass().getSimpleName());
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit if block", e);
        }
    }

    /**
     * 辅助方法：发射表达式（for instance method）
     * Phase 6: 重构使用 FunctionEmitter.isMigratedExpressionType，扩展支持 Name/Call/Ok/Err/Construct/Lambda
     */
    private void emitExpr(MethodVisitor mv, CoreModel.Expr expr, String expectedDesc, String pkg,
                         Map<String, Integer> env, ScopeStack scopeStack) {
        try {
            expressionEmitter.updateEnvironment(env);
            if (FunctionEmitter.isMigratedExpressionType(expr)) {
                expressionEmitter.emitExpression(expr, mv, scopeStack, expectedDesc);
            } else {
                Main.emitExpr(ctx, mv, expr, expectedDesc, pkg, 0, env, scopeStack, typeResolver);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit expression", e);
        }
    }

    // ==================== 静态回调模式（Main.java 使用） ====================

    /**
     * 块处理器回调接口
     */
    @FunctionalInterface
    interface BlockEmitter {
        boolean emitBlock(MethodVisitor mv, CoreModel.Block block);
    }

    /**
     * 条件表达式生成器回调接口
     */
    @FunctionalInterface
    interface ExprEmitter {
        void emitExpr(MethodVisitor mv, CoreModel.Expr expr, String expectedDesc);
    }

    /**
     * 简单表达式生成器回调接口 (Lambda apply 上下文)
     */
    @FunctionalInterface
    interface SimpleExprEmitter {
        void emitSimpleExpr(MethodVisitor mv, CoreModel.Expr expr);
    }

    /**
     * 生成函数体/Case 上下文中的 If 语句（静态方法）
     */
    static boolean emitIfStatement(
        MethodVisitor mv,
        CoreModel.Expr condition,
        CoreModel.Block thenBlock,
        CoreModel.Block elseBlock,
        ExprEmitter conditionEmitter,
        BlockEmitter blockEmitter,
        AtomicInteger lineNo
    ) {
        var lElse = new Label();
        var lEnd = new Label();

        // Emit condition (expected type: boolean/Z)
        conditionEmitter.emitExpr(mv, condition, "Z");
        mv.visitJumpInsn(IFEQ, lElse);

        // Then block
        { var lThen = new Label(); mv.visitLabel(lThen); mv.visitLineNumber(lineNo.getAndIncrement(), lThen); }
        boolean thenRet = blockEmitter.emitBlock(mv, thenBlock);
        if (!thenRet) mv.visitJumpInsn(GOTO, lEnd);

        // Else block
        mv.visitLabel(lElse);
        { var lElseLn = new Label(); mv.visitLabel(lElseLn); mv.visitLineNumber(lineNo.getAndIncrement(), lElseLn); }
        boolean elseRet = false;
        if (elseBlock != null) elseRet = blockEmitter.emitBlock(mv, elseBlock);
        if (!elseRet) mv.visitLabel(lEnd);

        return thenRet && elseRet;
    }

    /**
     * 生成 Lambda apply 上下文中的 If 语句（静态方法）
     */
    static boolean emitIfApply(
        MethodVisitor mv,
        CoreModel.Expr condition,
        CoreModel.Block thenBlock,
        CoreModel.Block elseBlock,
        SimpleExprEmitter simpleExprEmitter,
        BlockEmitter blockEmitter,
        AtomicInteger lineNo
    ) {
        var lElse = new Label();
        var lEnd = new Label();

        // Emit condition (leaves Boolean object on stack)
        simpleExprEmitter.emitSimpleExpr(mv, condition);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        mv.visitJumpInsn(IFEQ, lElse);

        // Then block
        { var lThen = new Label(); mv.visitLabel(lThen); mv.visitLineNumber(lineNo.getAndIncrement(), lThen); }
        boolean thenRet = blockEmitter.emitBlock(mv, thenBlock);
        if (!thenRet) mv.visitJumpInsn(GOTO, lEnd);

        // Else block
        mv.visitLabel(lElse);
        { var lElseLn = new Label(); mv.visitLabel(lElseLn); mv.visitLineNumber(lineNo.getAndIncrement(), lElseLn); }
        boolean elseRet = false;
        if (elseBlock != null) elseRet = blockEmitter.emitBlock(mv, elseBlock);
        if (!elseRet) mv.visitLabel(lEnd);

        return thenRet && elseRet;
    }
}
