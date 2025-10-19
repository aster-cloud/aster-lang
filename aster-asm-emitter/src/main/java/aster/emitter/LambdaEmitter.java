package aster.emitter;

import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

/**
 * Lambda 表达式字节码发射器
 *
 * 负责生成 Lambda 类定义和实例化字节码。Lambda 类实现 aster.runtime.FnN 接口，
 * 其中 N 为参数数量（0-4）。Lambda 体的处理通过 LambdaBodyEmitter 回调委托给调用者。
 */
final class LambdaEmitter {

    /**
     * Lambda 体字节码发射回调接口
     *
     * 用于处理 Lambda 函数体内的语句和表达式。调用者提供此回调实现，
     * LambdaEmitter 在生成 apply 方法时调用它来填充方法体。
     */
    @FunctionalInterface
    interface LambdaBodyEmitter {
        /**
         * 发射 Lambda 体的字节码
         *
         * @param ctx 编译上下文
         * @param mv 目标方法访问器（apply 方法）
         * @param body Lambda 体的 Block 节点
         * @param internal Lambda 类的内部名称
         * @param env 变量环境（变量名 -> 局部变量 slot）
         * @param primTypes 原始类型映射（变量名 -> 类型字符 'I', 'Z'）
         * @param retIsResult 返回类型是否为 Result
         * @param lineNo 当前行号（原子整数）
         * @return 是否已经返回（true 表示 body 包含 return 语句）
         */
        boolean emitBody(Main.Ctx ctx, MethodVisitor mv, CoreModel.Block body, String internal,
                        Map<String, Integer> env, Map<String, Character> primTypes,
                        boolean retIsResult, AtomicInteger lineNo);
    }

    private final TypeResolver typeResolver;
    private final Main.Ctx ctx;
    private final LambdaBodyEmitter bodyEmitter;

    /**
     * 构造 LambdaEmitter
     *
     * @param typeResolver 类型解析器
     * @param ctx 编译上下文
     * @param bodyEmitter Lambda 体发射回调
     */
    LambdaEmitter(TypeResolver typeResolver, Main.Ctx ctx, LambdaBodyEmitter bodyEmitter) {
        this.typeResolver = typeResolver;
        this.ctx = ctx;
        this.bodyEmitter = bodyEmitter;
    }

    /**
     * 发射 Lambda 表达式的字节码（实例化）
     *
     * 生成 Lambda 类定义（通过 emitLambdaSkeleton），然后在当前方法中生成实例化代码：
     * NEW Lambda$N, DUP, 加载闭包变量, INVOKESPECIAL <init>
     *
     * @param mv 目标方法访问器
     * @param lam Lambda AST 节点
     * @param currentPkg 当前包名
     * @param env 变量环境
     * @param scopeStack 作用域栈（用于获取闭包变量类型）
     */
    void emitLambda(MethodVisitor mv, CoreModel.Lambda lam, String currentPkg,
                   Map<String, Integer> env, ScopeStack scopeStack) {
        int arity = (lam.params == null) ? 0 : lam.params.size();
        String clsName = "Lambda$" + ctx.lambdaSeq().getAndIncrement();
        String internal = Main.toInternal(currentPkg == null ? "" : currentPkg, clsName);

        // 生成 Lambda 类定义
        emitLambdaSkeleton(internal, arity, lam);

        // 实例化 Lambda 对象
        mv.visitTypeInsn(NEW, internal);
        mv.visitInsn(DUP);

        // 加载闭包捕获的变量
        int capN = (lam.captures == null) ? 0 : lam.captures.size();
        for (int i = 0; i < capN; i++) {
            String cname = lam.captures.get(i);
            Integer slot = (env != null) ? env.get(cname) : null;
            if (slot == null) {
                mv.visitInsn(ACONST_NULL);
            } else {
                Character capKind = (scopeStack != null) ? scopeStack.getType(slot) : null;
                if (capKind != null && (capKind == 'I' || capKind == 'Z')) {
                    // int/boolean -> Integer
                    mv.visitVarInsn(ILOAD, slot);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                } else if (capKind != null && capKind == 'J') {
                    // long -> Long
                    mv.visitVarInsn(LLOAD, slot);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                } else if (capKind != null && capKind == 'D') {
                    // double -> Double
                    mv.visitVarInsn(DLOAD, slot);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                } else {
                    // Object
                    mv.visitVarInsn(ALOAD, slot);
                }
            }
        }

        // 调用构造函数
        StringBuilder ctorDesc = new StringBuilder("(");
        for (int i = 0; i < capN; i++) ctorDesc.append("Ljava/lang/Object;");
        ctorDesc.append(")V");
        mv.visitMethodInsn(INVOKESPECIAL, internal, "<init>", ctorDesc.toString(), false);
    }

    /**
     * 生成 Lambda 类的完整字节码（骨架生成）
     *
     * 生成的类结构：
     * - 实现 aster.runtime.FnN 接口（N = arity）
     * - 闭包字段：cap$<varname> (Object 类型)
     * - 构造函数：接收闭包变量并初始化字段
     * - apply 方法：构建环境后调用 bodyEmitter 处理 Lambda 体
     *
     * @param internal Lambda 类的内部名称
     * @param arity Lambda 参数数量（0-4）
     * @param lam Lambda AST 节点
     */
    private void emitLambdaSkeleton(String internal, int arity, CoreModel.Lambda lam) {
        var cw = AsmUtilities.createClassWriter();

        // 确定实现的接口和 apply 方法签名
        String[] ifaces;
        String applyDesc;
        if (arity == 0) {
            ifaces = new String[] { "aster/runtime/Fn0" };
            applyDesc = "()Ljava/lang/Object;";
        } else if (arity == 1) {
            ifaces = new String[] { "aster/runtime/Fn1" };
            applyDesc = "(Ljava/lang/Object;)Ljava/lang/Object;";
        } else if (arity == 2) {
            ifaces = new String[] { "aster/runtime/Fn2" };
            applyDesc = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        } else if (arity == 3) {
            ifaces = new String[] { "aster/runtime/Fn3" };
            applyDesc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        } else {
            ifaces = new String[] { "aster/runtime/Fn4" };
            applyDesc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        }

        cw.visit(V25, ACC_PUBLIC | ACC_FINAL, internal, null, "java/lang/Object", ifaces);
        Main.addOriginAnnotation(cw, lam.origin);

        // 生成闭包字段（cap$<varname>）
        int capN = (lam.captures == null) ? 0 : lam.captures.size();
        for (int i = 0; i < capN; i++) {
            String fname = "cap$" + lam.captures.get(i);
            cw.visitField(ACC_PRIVATE | ACC_FINAL, fname, "Ljava/lang/Object;", null, null).visitEnd();
        }

        // 生成构造函数
        var ctorDesc = new StringBuilder("(");
        for (int i = 0; i < capN; i++) ctorDesc.append("Ljava/lang/Object;");
        ctorDesc.append(")V");
        var mv = cw.visitMethod(ACC_PUBLIC, "<init>", ctorDesc.toString(), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        int slot = 1;
        for (int i = 0; i < capN; i++) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, slot++);
            mv.visitFieldInsn(PUTFIELD, internal, "cap$" + lam.captures.get(i), "Ljava/lang/Object;");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 生成 apply 方法
        var mv2 = cw.visitMethod(ACC_PUBLIC, "apply", applyDesc, null, null);
        Main.addOriginAnnotation(mv2, lam.origin);
        mv2.visitCode();

        // 构建环境：参数和闭包变量映射到局部变量 slot
        java.util.Map<String, Integer> env = new java.util.HashMap<>();
        java.util.Map<String, Character> primTypes = new java.util.HashMap<>();
        int next = 1;

        // 映射参数
        if (lam.params != null) {
            for (var p : lam.params) {
                env.put(p.name, next++);
                if (p.type instanceof CoreModel.TypeName tn) {
                    if (java.util.Objects.equals(tn.name, "Int")) primTypes.put(p.name, 'I');
                    else if (java.util.Objects.equals(tn.name, "Bool")) primTypes.put(p.name, 'Z');
                }
            }
        }

        // 加载闭包字段到局部变量
        int capN2 = (lam.captures == null) ? 0 : lam.captures.size();
        for (int i = 0; i < capN2; i++) {
            String cname = lam.captures.get(i);
            int slotIdx = next++;
            mv2.visitVarInsn(ALOAD, 0);
            mv2.visitFieldInsn(GETFIELD, internal, "cap$" + cname, "Ljava/lang/Object;");
            mv2.visitVarInsn(ASTORE, slotIdx);
            env.put(cname, slotIdx);
        }

        // 调用回调处理 Lambda 体
        boolean didReturn = false;
        if (lam.body != null && lam.body.statements != null) {
            boolean retIsResult = (lam.ret instanceof CoreModel.Result);
            java.util.concurrent.atomic.AtomicInteger lineNo = new java.util.concurrent.atomic.AtomicInteger(1);
            didReturn = bodyEmitter.emitBody(ctx, mv2, lam.body, internal, env, primTypes, retIsResult, lineNo);
        }

        // 如果 body 没有返回，添加默认返回 null
        if (!didReturn) {
            mv2.visitInsn(ACONST_NULL);
            mv2.visitInsn(ARETURN);
        }
        mv2.visitMaxs(0, 0);
        mv2.visitEnd();

        // 写入类文件
        try {
            AsmUtilities.writeClass(ctx.outDir().toString(), internal, cw.toByteArray());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
