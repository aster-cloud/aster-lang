package aster.emitter;

import aster.core.ir.CoreModel;

import org.objectweb.asm.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.objectweb.asm.Opcodes.*;

/**
 * LambdaMatchEmitter - Lambda Match 表达式字节码生成器
 *
 * 职责：
 * 1. 处理 Lambda apply 上下文中的 Match 表达式
 * 2. 支持3种模式：PatNull（null检查）、PatName/Enum（枚举变体）、PatCtor（构造器递归匹配）
 * 3. 线性遍历 cases，无 tableswitch/lookupswitch 优化
 *
 * 适用场景：
 * - Lambda apply 方法中的 Match 表达式（emitApplyStmt）
 *
 * 字节码模式:
 * <pre>
 * - 评估 scrutinee 表达式 → ASTORE scr
 * - 遍历 cases:
 *   * PatNull: ALOAD scr, IFNONNULL nextCase, emit body
 *   * PatName/Enum: ALOAD scr, GETSTATIC enum, IF_ACMPNE nextCase, emit body
 *   * PatCtor: 递归匹配（emitApplyPatMatchAndBind），emit body
 * - label endLabel
 * </pre>
 */
final class LambdaMatchEmitter {
    /**
     * 表达式生成器回调接口（用于评估 scrutinee）
     */
    @FunctionalInterface
    interface ExprEmitter {
        /**
         * 生成表达式字节码
         * @param mv MethodVisitor
         * @param expr 表达式节点
         * @param env 局部变量环境
         * @param primTypes 原始类型提示
         */
        void emitExpr(MethodVisitor mv, CoreModel.Expr expr, Map<String, Integer> env, Map<String, Character> primTypes);
    }

    /**
     * Case Body 处理器回调接口
     */
    @FunctionalInterface
    interface CaseBodyEmitter {
        /**
         * 生成 case body 字节码
         * @param ctx 编译上下文
         * @param mv MethodVisitor
         * @param body Case body 语句
         * @param ownerInternal 所有者类型内部名称
         * @param env 局部变量环境
         * @param primTypes 原始类型提示
         * @param retIsResult 返回值是否为 Result 类型
         * @param lineNo 行号计数器
         * @return true 如果 body 包含 return 语句
         */
        boolean emitBody(Main.Ctx ctx, MethodVisitor mv, CoreModel.Stmt body, String ownerInternal, Map<String, Integer> env, Map<String, Character> primTypes, boolean retIsResult, AtomicInteger lineNo);
    }

    /**
     * 模式匹配和绑定回调接口（用于 PatCtor）
     */
    @FunctionalInterface
    interface PatMatchEmitter {
        /**
         * 生成模式匹配和变量绑定字节码
         * @param ctx 编译上下文
         * @param mv MethodVisitor
         * @param pattern 模式节点
         * @param valSlot 值所在局部变量 slot
         * @param ownerInternal 所有者类型内部名称
         * @param env 局部变量环境
         * @param primTypes 原始类型提示
         * @param failLabel 匹配失败跳转标签
         */
        void emitPatMatch(Main.Ctx ctx, MethodVisitor mv, CoreModel.Pattern pattern, int valSlot, String ownerInternal, Map<String, Integer> env, Map<String, Character> primTypes, Label failLabel);
    }

    /**
     * 生成 Lambda Match 表达式字节码
     *
     * @param ctx 编译上下文
     * @param mv MethodVisitor
     * @param match Match 表达式节点
     * @param ownerInternal 所有者类型内部名称
     * @param env 局部变量环境
     * @param primTypes 原始类型提示
     * @param retIsResult 返回值是否为 Result 类型
     * @param lineNo 行号计数器
     * @param exprEmitter 表达式生成器回调
     * @param caseBodyEmitter Case body 处理器回调
     * @param patMatchEmitter 模式匹配和绑定回调
     * @return false（未实现）
     */
    static boolean emitMatch(
        Main.Ctx ctx,
        MethodVisitor mv,
        CoreModel.Match match,
        String ownerInternal,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        boolean retIsResult,
        AtomicInteger lineNo,
        ExprEmitter exprEmitter,
        CaseBodyEmitter caseBodyEmitter,
        PatMatchEmitter patMatchEmitter
    ) {
        // 1. 评估 scrutinee 表达式并存入局部变量
        int scr = Main.nextLocal(env);
        exprEmitter.emitExpr(mv, match.expr, env, primTypes);
        mv.visitVarInsn(ASTORE, scr);

        // 2. 创建 endLabel
        var endLabel = new Label();

        // 3. 遍历 cases
        if (match.cases != null) {
            for (var c : match.cases) {
                var nextCase = new Label();

                // 3.1 PatNull 处理：null 检查
                if (c.pattern instanceof CoreModel.PatNull) {
                    mv.visitVarInsn(ALOAD, scr);
                    mv.visitJumpInsn(IFNONNULL, nextCase);
                    // 行号标签
                    {
                        var lCase = new Label();
                        mv.visitLabel(lCase);
                        mv.visitLineNumber(lineNo.getAndIncrement(), lCase);
                    }
                    boolean _ret0 = caseBodyEmitter.emitBody(ctx, mv, c.body, ownerInternal, env, primTypes, retIsResult, lineNo);
                    mv.visitLabel(nextCase);
                    // Do not early-return to ensure all labels are visited for ASM frame computation

                // 3.2 PatName/Enum 处理：枚举变体匹配
                } else if (c.pattern instanceof CoreModel.PatName pn) {
                    // Enum variant with known enum mapping
                    String enumName = ctx.enumOwner(pn.name);
                    if (enumName != null) {
                        String pkgPath = ownerInternal.contains("/")
                            ? ownerInternal.substring(0, ownerInternal.lastIndexOf('/'))
                            : "";
                        String enumInternal = enumName.contains(".")
                            ? enumName.replace('.', '/')
                            : (pkgPath.isEmpty() ? enumName : pkgPath + "/" + enumName);

                        mv.visitVarInsn(ALOAD, scr);
                        mv.visitFieldInsn(GETSTATIC, enumInternal, pn.name, Main.internalDesc(enumInternal));
                        mv.visitJumpInsn(IF_ACMPNE, nextCase);
                        // 行号标签
                        {
                            var lCase = new Label();
                            mv.visitLabel(lCase);
                            mv.visitLineNumber(lineNo.getAndIncrement(), lCase);
                        }
                        boolean _ret1 = caseBodyEmitter.emitBody(ctx, mv, c.body, ownerInternal, env, primTypes, retIsResult, lineNo);
                        mv.visitLabel(nextCase);
                    }

                // 3.3 PatCtor 处理：构造器递归匹配
                } else if (c.pattern instanceof CoreModel.PatCtor) {
                    // Nested pattern support: recursively match and bind; jump to nextCase if any check fails
                    patMatchEmitter.emitPatMatch(ctx, mv, c.pattern, scr, ownerInternal, env, primTypes, nextCase);
                    // 行号标签
                    {
                        var lCase = new Label();
                        mv.visitLabel(lCase);
                        mv.visitLineNumber(lineNo.getAndIncrement(), lCase);
                    }
                    boolean _ret2 = caseBodyEmitter.emitBody(ctx, mv, c.body, ownerInternal, env, primTypes, retIsResult, lineNo);
                    mv.visitLabel(nextCase);
                }
            }
        }

        // 4. 放置 endLabel
        mv.visitLabel(endLabel);
        return false;
    }
}
