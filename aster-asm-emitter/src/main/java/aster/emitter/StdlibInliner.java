package aster.emitter;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Stdlib 函数内联器 - 表驱动的 stdlib 函数内联
 *
 * 职责：
 * - 将 stdlib 函数调用内联为 JVM 字节码
 * - 使用表驱动设计消除重复代码
 * - 支持 Text, List, Map 操作和算术运算
 *
 * 设计原则：
 * - 纯静态工具类，零状态
 * - 配置化的内联规则
 * - 回调模式生成参数表达式
 */
final class StdlibInliner {

    private static final StdlibInliner INSTANCE = new StdlibInliner();

    static StdlibInliner instance() {
        return INSTANCE;
    }

    /**
     * 简单表达式生成回调接口
     */
    @FunctionalInterface
    interface SimpleExprEmitter {
        void emitSimpleExpr(MethodVisitor mv, CoreModel.Expr expr, Map<String, Integer> env, Map<String, Character> primTypes);
    }

    /**
     * Null安全性警告回调接口
     */
    @FunctionalInterface
    interface NullabilityWarner {
        void warnNullability(String functionName, List<CoreModel.Expr> args);
    }

    /**
     * 内联规则类型
     */
    enum InlineType {
        /** 简单方法调用 */
        SIMPLE_METHOD,
        /** 需要 String.valueOf 包装的方法 */
        STRING_WRAPPED,
        /** 需要类型转换的方法 */
        CAST_WRAPPED,
        /** 算术运算 */
        ARITHMETIC,
        /** Boolean 运算 */
        BOOLEAN_OP
    }

    /**
     * 内联规则定义
     */
    record InlineRule(
        InlineType type,
        String targetClass,      // 目标类（java/lang/String）
        String method,           // 方法名（concat）
        String descriptor,       // 方法描述符
        int invokeOpcode,        // INVOKEVIRTUAL/INVOKESTATIC/INVOKEINTERFACE
        String boxedType,        // 返回值装箱类型（java/lang/Integer, java/lang/Boolean）
        boolean needsNullCheck,  // 是否需要 nullability 检查
        String[] argCasts        // 参数类型转换（CHECKCAST 类型）
    ) {}

    /**
     * Stdlib 函数映射表
     */
    private static final Map<String, InlineRule> INLINE_RULES = new HashMap<>();

    static {
        // Text operations
        INLINE_RULES.put("Text.concat", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;",
            INVOKEVIRTUAL, null, false, null
        ));
        INLINE_RULES.put("Text.contains", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z",
            INVOKEVIRTUAL, "java/lang/Boolean", false, null
        ));
        INLINE_RULES.put("Text.equals", new InlineRule(
            InlineType.SIMPLE_METHOD, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            INVOKESTATIC, "java/lang/Boolean", true, null
        ));
        INLINE_RULES.put("Text.replace", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
            INVOKEVIRTUAL, null, true, null
        ));
        INLINE_RULES.put("Text.split", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;",
            INVOKEVIRTUAL, null, true, null
        ));
        INLINE_RULES.put("Text.indexOf", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "indexOf", "(Ljava/lang/String;)I",
            INVOKEVIRTUAL, "java/lang/Integer", true, null
        ));
        INLINE_RULES.put("Text.startsWith", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z",
            INVOKEVIRTUAL, "java/lang/Boolean", true, null
        ));
        INLINE_RULES.put("Text.endsWith", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z",
            INVOKEVIRTUAL, "java/lang/Boolean", true, null
        ));
        INLINE_RULES.put("Text.length", new InlineRule(
            InlineType.STRING_WRAPPED, "java/lang/String", "length", "()I",
            INVOKEVIRTUAL, "java/lang/Integer", false, null
        ));

        // List operations
        INLINE_RULES.put("List.length", new InlineRule(
            InlineType.CAST_WRAPPED, "java/util/List", "size", "()I",
            INVOKEINTERFACE, "java/lang/Integer", false, new String[]{"java/util/List"}
        ));
        INLINE_RULES.put("List.isEmpty", new InlineRule(
            InlineType.CAST_WRAPPED, "java/util/List", "isEmpty", "()Z",
            INVOKEINTERFACE, "java/lang/Boolean", false, new String[]{"java/util/List"}
        ));
        INLINE_RULES.put("List.get", new InlineRule(
            InlineType.CAST_WRAPPED, "java/util/List", "get", "(I)Ljava/lang/Object;",
            INVOKEINTERFACE, null, false, new String[]{"java/util/List", "java/lang/Integer"}
        ));

        // Map operations
        INLINE_RULES.put("Map.get", new InlineRule(
            InlineType.CAST_WRAPPED, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;",
            INVOKEINTERFACE, null, false, new String[]{"java/util/Map", null}
        ));

        // Arithmetic/Boolean operations (special handling)
        INLINE_RULES.put("+", new InlineRule(
            InlineType.ARITHMETIC, null, null, null,
            -1, "java/lang/Integer", false, new String[]{"java/lang/Integer", "java/lang/Integer"}
        ));
        INLINE_RULES.put("not", new InlineRule(
            InlineType.BOOLEAN_OP, null, null, null,
            -1, "java/lang/Boolean", false, new String[]{"java/lang/Boolean"}
        ));
        INLINE_RULES.put("<", new InlineRule(
            InlineType.ARITHMETIC, null, null, null,
            GeneratorAdapter.LT, "java/lang/Boolean", false, new String[]{"java/lang/Integer", "java/lang/Integer"}
        ));
        INLINE_RULES.put(">", new InlineRule(
            InlineType.ARITHMETIC, null, null, null,
            GeneratorAdapter.GT, "java/lang/Boolean", false, new String[]{"java/lang/Integer", "java/lang/Integer"}
        ));
        INLINE_RULES.put("==", new InlineRule(
            InlineType.ARITHMETIC, null, null, null,
            GeneratorAdapter.EQ, "java/lang/Boolean", false, new String[]{"java/lang/Integer", "java/lang/Integer"}
        ));
    }

    private StdlibInliner() {
        // 单例工具类，禁止外部实例化
    }

    /**
     * 尝试内联 stdlib 函数调用
     *
     * @param mv MethodVisitor
     * @param functionName 函数名（例如 "Text.concat"）
     * @param args 参数表达式列表
     * @param env 局部变量环境
     * @param primTypes 原始类型提示
     * @param exprEmitter 表达式生成回调
     * @param nullWarner Nullability 警告回调
     * @return true 如果成功内联，false 如果不支持内联
     */
    static boolean tryInline(
        MethodVisitor mv,
        String functionName,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        SimpleExprEmitter exprEmitter,
        NullabilityWarner nullWarner
    ) {
        return INSTANCE.tryInlineInternal(mv, functionName, args, env, primTypes, exprEmitter, nullWarner);
    }

    boolean tryInline(
        String functionName,
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        SimpleExprEmitter exprEmitter,
        NullabilityWarner nullWarner
    ) {
        return tryInlineInternal(mv, functionName, args, env, primTypes, exprEmitter, nullWarner);
    }

    private boolean tryInlineInternal(
        MethodVisitor mv,
        String functionName,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        SimpleExprEmitter exprEmitter,
        NullabilityWarner nullWarner
    ) {
        var rule = INLINE_RULES.get(functionName);
        if (rule == null) {
            return false;
        }

        // Nullability 检查
        if (rule.needsNullCheck && nullWarner != null) {
            nullWarner.warnNullability(functionName, args);
        }

        // 根据类型生成字节码
        switch (rule.type) {
            case SIMPLE_METHOD -> emitSimpleMethod(mv, args, env, primTypes, rule, exprEmitter);
            case STRING_WRAPPED -> emitStringWrappedMethod(mv, args, env, primTypes, rule, exprEmitter);
            case CAST_WRAPPED -> emitCastWrappedMethod(mv, args, env, primTypes, rule, exprEmitter);
            case ARITHMETIC -> emitArithmetic(mv, args, env, primTypes, rule, exprEmitter, functionName);
            case BOOLEAN_OP -> emitBooleanOp(mv, args, env, primTypes, rule, exprEmitter);
            default -> {
                return false;
            }
        }

        return true;
    }

    /**
     * 生成简单方法调用（无参数包装）
     */
    private static void emitSimpleMethod(
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        InlineRule rule,
        SimpleExprEmitter exprEmitter
    ) {
        // 生成参数
        for (var arg : args) {
            exprEmitter.emitSimpleExpr(mv, arg, env, primTypes);
        }

        // 调用方法
        mv.visitMethodInsn(rule.invokeOpcode, rule.targetClass, rule.method, rule.descriptor,
            rule.invokeOpcode == INVOKEINTERFACE);

        // 装箱返回值
        if (rule.boxedType != null) {
            boxPrimitive(mv, rule.boxedType);
        }
    }

    /**
     * 生成 String.valueOf 包装的方法调用
     */
    private static void emitStringWrappedMethod(
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        InlineRule rule,
        SimpleExprEmitter exprEmitter
    ) {
        // 生成参数（每个参数用 String.valueOf 包装）
        for (var arg : args) {
            exprEmitter.emitSimpleExpr(mv, arg, env, primTypes);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        }

        // 调用方法
        mv.visitMethodInsn(rule.invokeOpcode, rule.targetClass, rule.method, rule.descriptor,
            rule.invokeOpcode == INVOKEINTERFACE);

        // Text.split 特殊处理：转换为 List
        if ("split".equals(rule.method)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
        }

        // 装箱返回值
        if (rule.boxedType != null) {
            boxPrimitive(mv, rule.boxedType);
        }
    }

    /**
     * 生成类型转换包装的方法调用（List/Map operations）
     */
    private static void emitCastWrappedMethod(
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        InlineRule rule,
        SimpleExprEmitter exprEmitter
    ) {
        // 生成参数（带类型转换）
        for (int i = 0; i < args.size(); i++) {
            exprEmitter.emitSimpleExpr(mv, args.get(i), env, primTypes);

            // 应用类型转换
            if (rule.argCasts != null && i < rule.argCasts.length && rule.argCasts[i] != null) {
                mv.visitTypeInsn(CHECKCAST, rule.argCasts[i]);
                // 如果是 Integer 类型，需要 unbox
                if ("java/lang/Integer".equals(rule.argCasts[i])) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                }
            }
        }

        // 调用方法
        mv.visitMethodInsn(rule.invokeOpcode, rule.targetClass, rule.method, rule.descriptor,
            rule.invokeOpcode == INVOKEINTERFACE);

        // 装箱返回值
        if (rule.boxedType != null) {
            boxPrimitive(mv, rule.boxedType);
        }
    }

    /**
     * 生成算术运算（+, <, >, ==）
     */
    private static void emitArithmetic(
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        InlineRule rule,
        SimpleExprEmitter exprEmitter,
        String functionName
    ) {
        // 生成两个参数（unbox 为 int）
        for (var arg : args) {
            exprEmitter.emitSimpleExpr(mv, arg, env, primTypes);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        }

        if ("+".equals(functionName)) {
            // 加法：使用 AsmUtilities.emitAdd
            var gen = AsmUtilities.createGenerator(mv, ACC_PUBLIC | ACC_STATIC, "__synthetic__", "()V");
            AsmUtilities.emitAdd(gen, Type.INT_TYPE);
        } else {
            // 比较运算：使用 AsmUtilities.emitCompare
            var lTrue = new Label();
            var lEnd = new Label();
            var gen = AsmUtilities.createGenerator(mv, ACC_PUBLIC | ACC_STATIC, "__synthetic__", "()V");
            AsmUtilities.emitCompare(gen, Type.INT_TYPE, rule.invokeOpcode, lTrue);
            mv.visitInsn(ICONST_0);
            mv.visitJumpInsn(GOTO, lEnd);
            mv.visitLabel(lTrue);
            mv.visitInsn(ICONST_1);
            mv.visitLabel(lEnd);
        }

        // 装箱返回值
        boxPrimitive(mv, rule.boxedType);
    }

    /**
     * 生成 Boolean 运算（not）
     */
    private static void emitBooleanOp(
        MethodVisitor mv,
        List<CoreModel.Expr> args,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        InlineRule rule,
        SimpleExprEmitter exprEmitter
    ) {
        // 生成参数（unbox 为 boolean）
        exprEmitter.emitSimpleExpr(mv, args.get(0), env, primTypes);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);

        // Boolean negation: IFEQ (if false -> true)
        var lTrue = new Label();
        var lEnd = new Label();
        mv.visitJumpInsn(IFEQ, lTrue);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, lEnd);
        mv.visitLabel(lTrue);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(lEnd);

        // 装箱返回值
        boxPrimitive(mv, rule.boxedType);
    }

    /**
     * 装箱原始类型
     */
    private static void boxPrimitive(MethodVisitor mv, String boxedType) {
        if (boxedType == null) return;

        if ("java/lang/Integer".equals(boxedType)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if ("java/lang/Boolean".equals(boxedType)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        }
    }
}
