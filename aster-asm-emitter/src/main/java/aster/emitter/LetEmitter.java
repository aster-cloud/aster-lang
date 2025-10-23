package aster.emitter;

import aster.core.ir.CoreModel;

import org.objectweb.asm.*;
import java.util.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * LetEmitter - Let 语句字节码生成器
 *
 * 职责：
 * 1. 变量绑定（Let 语句）
 * 2. 类型推断（基于 TypeResolver 和 fnHints）
 * 3. Boxing/Unboxing（int ↔ Integer, boolean ↔ Boolean, double ↔ Double, long ↔ Long）
 * 4. 环境更新（env, scopeStack, lvars）
 *
 * 适用场景：
 * - emitFunc 中的 Let 语句处理
 *
 * 字节码模式：
 * <pre>
 * 1. 类型推断：TypeResolver.inferType() + fnHints
 * 2. 生成表达式：exprEmitter.emitExpr(expectedDesc)
 * 3. 存储变量：ISTORE/LSTORE/DSTORE/ASTORE
 * 4. 更新环境：env.put(), scopeStack.declare(), lvars.add()
 * </pre>
 */
final class LetEmitter {

    /**
     * 表达式生成回调接口
     */
    @FunctionalInterface
    interface ExprEmitter {
        /**
         * 生成表达式字节码
         *
         * @param mv MethodVisitor
         * @param expr 表达式节点
         * @param expectedDesc 期望的类型描述符（可选）
         * @param currentPkg 当前包名
         * @param paramBase 参数基址
         * @param env 局部变量环境
         * @param scopeStack 作用域栈
         * @param typeResolver 类型解析器
         */
        void emitExpr(
            MethodVisitor mv,
            CoreModel.Expr expr,
            String expectedDesc,
            String currentPkg,
            int paramBase,
            Map<String, Integer> env,
            ScopeStack scopeStack,
            TypeResolver typeResolver
        );
    }

    /**
     * 对象描述符解析回调接口
     */
    @FunctionalInterface
    interface ObjectDescriptorResolver {
        /**
         * 解析对象表达式的具体描述符
         *
         * @param expr 表达式节点
         * @param currentPkg 当前包名
         * @param scopeStack 作用域栈
         * @param ctx 上下文
         * @return 对象描述符（例如 "Ljava/lang/String;"）
         */
        String resolveObjectDescriptor(
            CoreModel.Expr expr,
            String currentPkg,
            ScopeStack scopeStack,
            Main.Ctx ctx
        );
    }

    /**
     * LV 工厂回调接口（用于创建 Main.LV 实例）
     */
    @FunctionalInterface
    interface LVFactory {
        /**
         * 创建 LV 实例
         *
         * @param name 变量名
         * @param desc 类型描述符
         * @param slot 局部变量 slot
         * @return LV 实例（Main 中定义的 record）
         */
        Object createLV(String name, String desc, int slot);
    }

    /**
     * 生成 Let 语句字节码
     *
     * @param mv MethodVisitor
     * @param let Let 语句节点
     * @param pkg 包名
     * @param env 局部变量环境（变量名 → slot 映射）
     * @param scopeStack 作用域栈
     * @param typeResolver 类型解析器
     * @param fnHints 函数类型提示（变量名 → 类型字符）
     * @param ctx 上下文
     * @param exprEmitter 表达式生成回调
     * @param objectDescriptorResolver 对象描述符解析回调
     * @param lvars LocalVariableTable 条目列表
     * @param lvFactory LV 工厂（创建 Main.LV 实例）
     * @param nextSlot 当前下一个可用局部变量 slot
     * @return 更新后的下一个可用局部变量 slot
     */
    static int emitLetStatement(
        MethodVisitor mv,
        CoreModel.Let let,
        String pkg,
        Map<String, Integer> env,
        ScopeStack scopeStack,
        TypeResolver typeResolver,
        Map<String, Character> fnHints,
        Main.Ctx ctx,
        ExprEmitter exprEmitter,
        ObjectDescriptorResolver objectDescriptorResolver,
        List<Object> lvars,
        LVFactory lvFactory,
        int nextSlot
    ) {
        // 1. 类型推断（TypeResolver + fnHints + 特殊规则）
        Character inferred = typeResolver.inferType(let.expr);

        // 特殊规则：如果变量名是 "ok" 且表达式是 Call，推断为 boolean
        if (inferred == null && Objects.equals(let.name, "ok") && let.expr instanceof CoreModel.Call) {
            inferred = 'Z';
        }

        // Fallback 到 fnHints
        if (inferred == null) {
            Character hint = fnHints.get(let.name);
            if (hint != null) inferred = hint;
        }

        // 2. 确定 expectedDesc 和 storeOpcode
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

        // 3. 生成表达式字节码
        if (expectedDesc != null) {
            exprEmitter.emitExpr(mv, let.expr, expectedDesc, pkg, 0, env, scopeStack, typeResolver);
        } else {
            exprEmitter.emitExpr(mv, let.expr, null, pkg, 0, env, scopeStack, typeResolver);
            localDesc = objectDescriptorResolver.resolveObjectDescriptor(let.expr, pkg, scopeStack, ctx);
        }

        // 4. 存储变量
        mv.visitVarInsn(storeOpcode, nextSlot);

        // 5. 更新环境
        env.put(let.name, nextSlot);
        lvars.add(lvFactory.createLV(let.name, localDesc, nextSlot));
        scopeStack.declare(let.name, nextSlot, localDesc, Main.kindForDescriptor(localDesc));

        // 6. 返回下一个 slot
        return nextSlot + 1;
    }
}
