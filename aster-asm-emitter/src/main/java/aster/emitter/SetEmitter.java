package aster.emitter;

import aster.core.ir.CoreModel;

import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 负责 Set 语句的字节码生成。
 *
 * Set 语句用于更新已存在的变量（与 Let 创建新变量不同）。
 *
 * 职责：
 * - 检查变量是否已声明
 * - 发射表达式字节码
 * - 存储到现有局部变量 slot
 *
 * 设计原则：
 * - Set 复用现有 slot，不创建新 slot（与 Let 的关键区别）
 * - 类型由现有变量决定，无需推断新类型
 * - 保持与 Main.emitFunc 的字节码输出一致（当 Set 实现后）
 */
public class SetEmitter {
    private final Main.Ctx ctx;
    private final TypeResolver typeResolver;
    private final ExpressionEmitter expressionEmitter;

    public SetEmitter(Main.Ctx ctx, TypeResolver typeResolver, ExpressionEmitter expressionEmitter) {
        this.ctx = ctx;
        this.typeResolver = typeResolver;
        this.expressionEmitter = expressionEmitter;
    }

    /**
     * 发射 Set 语句字节码。
     *
     * Set 语句更新已存在的变量，关键步骤：
     * 1. 检查变量是否已声明（必须存在于 env 中）
     * 2. 获取现有变量的 slot 和类型信息
     * 3. 发射表达式字节码（期望类型与现有变量一致）
     * 4. 存储到现有 slot（不创建新 slot）
     *
     * @param mv          MethodVisitor
     * @param set         Set 语句
     * @param pkg         包名
     * @param env         环境（变量名 → slot 映射）
     * @param scopeStack  作用域栈（类型信息）
     * @param fnHints     函数类型提示
     * @throws IOException 如果字节码生成失败
     */
    public void emitSet(
        MethodVisitor mv,
        CoreModel.Set set,
        String pkg,
        Map<String, Integer> env,
        ScopeStack scopeStack,
        Map<String, Character> fnHints
    ) throws IOException {
        // 检查变量是否已声明
        Integer existingSlot = env.get(set.name);
        if (existingSlot == null) {
            throw new IllegalStateException(
                "Set statement error: variable '" + set.name + "' not declared. " +
                "Use 'let " + set.name + " = ...' to create a new variable, " +
                "or check for typos in the variable name."
            );
        }

        // 获取现有变量的类型信息
        String existingDesc = scopeStack.getDescriptor(set.name);
        Character existingKind = scopeStack.getType(set.name);

        // 如果 scopeStack 中没有类型信息，尝试从类型推断
        String expectedDesc = existingDesc;
        if (expectedDesc == null || expectedDesc.equals("Ljava/lang/Object;")) {
            // 推断表达式类型（参考 Let 处理）
            Character inferred = typeResolver.inferType(set.expr);
            if (inferred == null && fnHints != null) {
                inferred = fnHints.get(set.name);
            }
            if (inferred != null) {
                expectedDesc = switch (inferred) {
                    case 'D' -> "D";
                    case 'J' -> "J";
                    case 'Z' -> "Z";
                    case 'I' -> "I";
                    default -> "Ljava/lang/Object;";
                };
            }
        }

        // 确定存储指令（基于现有变量类型）
        int storeOpcode = ASTORE;
        if (existingKind != null) {
            storeOpcode = switch (existingKind) {
                case 'D' -> DSTORE;
                case 'J' -> LSTORE;
                case 'I', 'Z' -> ISTORE;
                default -> ASTORE;
            };
        } else if (expectedDesc != null) {
            // 降级：根据 descriptor 推断 opcode
            storeOpcode = switch (expectedDesc.charAt(0)) {
                case 'D' -> DSTORE;
                case 'J' -> LSTORE;
                case 'I', 'Z' -> ISTORE;
                default -> ASTORE;
            };
        }

        // 发射表达式字节码（期望类型与现有变量一致）
        expressionEmitter.updateEnvironment(env);
        if (FunctionEmitter.isMigratedExpressionType(set.expr)) {
            // 使用 ExpressionEmitter 处理已迁移的表达式类型
            expressionEmitter.emitExpression(set.expr, mv, scopeStack, expectedDesc);
        } else {
            // 降级到 Main.emitExpr 处理未迁移的表达式
            Main.emitExpr(ctx, mv, set.expr, expectedDesc, pkg, 0, env, scopeStack, typeResolver);
        }

        // 存储到现有 slot（覆盖现有值，不创建新 slot）
        mv.visitVarInsn(storeOpcode, existingSlot);

        // 注意：无需更新 env、lvars 或 scopeStack
        // Set 只修改现有变量的值，不改变变量的 slot、类型或作用域信息
    }
}
