package aster.emitter;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * CaseBodyEmitter - Match 表达式 Case Body 字节码生成器
 *
 * 职责：
 * 1. 处理 Return 语句：生成 IRETURN/ARETURN 指令
 * 2. 处理 Block 语句：遍历查找 Return 并生成 GOTO
 *
 * 适用场景：
 * - 函数体 Match 表达式 (emitFunc)
 * - lookupswitch/tableswitch 优化路径
 *
 * 字节码模式:
 * <pre>
 * - 评估表达式 (通过 ExprEmitter 回调)
 * - IRETURN (如果 retDesc = "I" 或 "Z")
 * - ARETURN (否则)
 * - GOTO endLabel (如果 Block 没有 return 且 endLabel != null)
 * </pre>
 */
final class CaseBodyEmitter {
    /**
     * 表达式生成器回调接口
     */
    @FunctionalInterface
    interface ExprEmitter {
        /**
         * 生成表达式字节码
         *
         * @param mv MethodVisitor
         * @param expr 表达式节点
         * @param expectedDesc 期望的类型描述符 (例如 "I", "Z", "Ljava/lang/String;")
         */
        void emitExpr(MethodVisitor mv, CoreModel.Expr expr, String expectedDesc);
    }

    /**
     * 生成完整 Case Body 字节码（处理 Return + Block + GOTO）
     *
     * 用于函数体 Match 表达式的 Case Body 处理，支持：
     * 1. 直接 Return 语句：生成表达式求值 + IRETURN/ARETURN
     * 2. Block 语句：遍历查找 Return 并处理，如果没有 Return 则生成 GOTO
     *
     * @param mv MethodVisitor
     * @param body Case Body 语句 (Return 或 Block)
     * @param retDesc 返回值类型描述符 (例如 "I", "Z", "Ljava/lang/String;")
     * @param exprEmitter 表达式生成器回调
     * @param endLabel 结束标签 (nullable: 如果为 null 则不生成 GOTO，用于 tableswitch default case)
     * @return true 如果生成了 return 指令，false 否则
     */
    static boolean emitCaseBody(
        MethodVisitor mv,
        CoreModel.Stmt body,
        String retDesc,
        ExprEmitter exprEmitter,
        Label endLabel
    ) {
        if (body instanceof CoreModel.Return rr) {
            exprEmitter.emitExpr(mv, rr.expr, retDesc);
            emitReturnInsn(mv, retDesc);
            return true;
        } else if (body instanceof CoreModel.Block bb) {
            for (var st : bb.statements) {
                if (st instanceof CoreModel.Return r2) {
                    exprEmitter.emitExpr(mv, r2.expr, retDesc);
                    emitReturnInsn(mv, retDesc);
                    return true;
                }
            }
            if (endLabel != null) {
                mv.visitJumpInsn(GOTO, endLabel);
            }
            return false;
        }
        return false;
    }

    /**
     * 生成简化 Case Body 字节码（仅处理 Return）
     *
     * 用于部分匹配位置（PatNull, PatCtor, Enum 等），这些位置只处理 Return 语句，
     * 不涉及 Block 或 GOTO 逻辑。
     *
     * @param mv MethodVisitor
     * @param body Case Body 语句 (仅 Return)
     * @param retDesc 返回值类型描述符 (例如 "I", "Z", "Ljava/lang/String;")
     * @param exprEmitter 表达式生成器回调
     */
    static void emitCaseBodySimple(
        MethodVisitor mv,
        CoreModel.Stmt body,
        String retDesc,
        ExprEmitter exprEmitter
    ) {
        if (body instanceof CoreModel.Return rr) {
            exprEmitter.emitExpr(mv, rr.expr, retDesc);
            emitReturnInsn(mv, retDesc);
        }
    }

    /**
     * 辅助方法：生成 IRETURN 或 ARETURN 指令
     *
     * 根据返回值类型描述符选择合适的 return 指令：
     * - "I" (int) 或 "Z" (boolean) → IRETURN
     * - 其他类型 → ARETURN
     *
     * @param mv MethodVisitor
     * @param retDesc 返回值类型描述符
     */
    private static void emitReturnInsn(MethodVisitor mv, String retDesc) {
        if (retDesc.equals("I") || retDesc.equals("Z")) {
            mv.visitInsn(IRETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
    }
}
