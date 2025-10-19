package aster.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 负责 Return 语句字节码生成的统一工具类。
 *
 * 职责：
 * - 统一处理所有 Return 语句的字节码生成
 * - 处理特殊的 Result 类型 try-catch 包装
 * - 根据返回类型选择正确的返回指令
 *
 * 设计原则：
 * - 静态工具类，无状态
 * - 消除 Return 处理的代码重复
 * - 保持与原实现的字节码一致性
 */
public class ReturnEmitter {

    /**
     * Slot 分配回调接口
     */
    @FunctionalInterface
    public interface SlotProvider {
        /**
         * 分配一个新的局部变量 slot
         * @return 新分配的 slot 编号
         */
        int allocate();
    }

    /**
     * 表达式生成回调接口
     */
    @FunctionalInterface
    public interface ExprEmitter {
        /**
         * 生成表达式字节码
         *
         * @param ctx 上下文
         * @param mv MethodVisitor
         * @param expr 表达式节点
         * @param expectedDesc 期望的类型描述符（可选）
         * @param pkg 包名
         * @param paramBase 参数基址
         * @param env 局部变量环境
         * @param scopeStack 作用域栈
         * @param typeResolver 类型解析器
         * @throws IOException 如果生成失败
         */
        void emitExpr(Main.Ctx ctx, MethodVisitor mv, CoreModel.Expr expr, String expectedDesc,
                      String pkg, int paramBase, Map<String, Integer> env,
                      ScopeStack scopeStack, TypeResolver typeResolver) throws IOException;
    }

    private ReturnEmitter() {
        // 静态工具类，禁止实例化
    }

    /**
     * 发射 Return 语句的字节码（emitFunc 上下文）。
     *
     * @param mv MethodVisitor
     * @param ret Return 语句节点
     * @param retDesc 返回类型描述符
     * @param pkg 包名
     * @param env 环境（变量名 -> slot 映射）
     * @param scopeStack 作用域栈
     * @param ctx 上下文
     * @param typeResolver 类型解析器（用于函数 schema 查找和类型推断）
     * @param slotProvider Slot 分配回调
     * @param exprEmitter 表达式生成回调
     * @throws IOException 如果发射过程失败
     */
    public static void emitReturn(MethodVisitor mv, CoreModel.Return ret, String retDesc,
                                  String pkg, Map<String, Integer> env,
                                  ScopeStack scopeStack, Main.Ctx ctx, TypeResolver typeResolver,
                                  SlotProvider slotProvider, ExprEmitter exprEmitter) throws IOException {
        // 特殊处理：Result 类型的 try-catch 包装
        if (retDesc.equals("Laster/runtime/Result;") && ret.expr instanceof CoreModel.Call) {
            emitResultTryCatch(mv, ret, pkg, env, scopeStack, ctx, typeResolver, slotProvider, exprEmitter);
            return;
        }

        // 常规 return：发射表达式 + 返回指令
        exprEmitter.emitExpr(ctx, mv, ret.expr, retDesc, pkg, 0, env, scopeStack, typeResolver);
        emitReturnInstruction(mv, retDesc);
    }

    /**
     * 发射 Result 类型的 try-catch 包装
     */
    private static void emitResultTryCatch(MethodVisitor mv, CoreModel.Return ret, String pkg,
                                          Map<String, Integer> env, ScopeStack scopeStack,
                                          Main.Ctx ctx, TypeResolver typeResolver,
                                          SlotProvider slotProvider, ExprEmitter exprEmitter) throws IOException {
        var lTryStart = new Label();
        var lTryEnd = new Label();
        var lCatch = new Label();
        var lRet = new Label();

        mv.visitTryCatchBlock(lTryStart, lTryEnd, lCatch, "java/lang/Throwable");

        // 使用 SlotProvider 分配临时槽位
        int resSlot = slotProvider.allocate();
        int tmpSlot = slotProvider.allocate();
        int exSlot = slotProvider.allocate();

        // Try 块：执行调用并包装为 Ok
        mv.visitLabel(lTryStart);
        exprEmitter.emitExpr(ctx, mv, ret.expr, null, pkg, 0, env, scopeStack, typeResolver);
        mv.visitVarInsn(ASTORE, tmpSlot);
        mv.visitTypeInsn(NEW, "aster/runtime/Ok");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, tmpSlot);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Ok", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitVarInsn(ASTORE, resSlot);
        mv.visitLabel(lTryEnd);
        mv.visitJumpInsn(GOTO, lRet);

        // Catch 块：捕获异常并包装为 Err
        mv.visitLabel(lCatch);
        mv.visitVarInsn(ASTORE, exSlot);
        mv.visitTypeInsn(NEW, "aster/runtime/Err");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, exSlot);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Err", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitVarInsn(ASTORE, resSlot);
        mv.visitJumpInsn(GOTO, lRet);

        // Return 结果
        mv.visitLabel(lRet);
        mv.visitVarInsn(ALOAD, resSlot);
        mv.visitInsn(ARETURN);

        // 局部变量表声明
        mv.visitLocalVariable("_res", "Laster/runtime/Result;", null, lTryStart, lRet, resSlot);
        mv.visitLocalVariable("_tmp", "Ljava/lang/Object;", null, lTryStart, lRet, tmpSlot);
        mv.visitLocalVariable("_ex", "Ljava/lang/Throwable;", null, lCatch, lRet, exSlot);
    }

    /**
     * 根据返回类型发射相应的返回指令
     * 注意：与原实现保持一致，仅处理 I 和 Z 基本类型，其他全部使用 ARETURN
     */
    private static void emitReturnInstruction(MethodVisitor mv, String retDesc) {
        if (retDesc.equals("I") || retDesc.equals("Z")) {
            mv.visitInsn(IRETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
    }
}
