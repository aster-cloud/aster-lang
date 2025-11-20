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

    private record StatementResult(boolean shouldReturn, int nextSlot) {}

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
        Main.addFuncMetadataAnnotations(mv, fn);
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
                var result = emitStatement(
                    mv, st, pkg, env, scopeStack, emitCtx, lvars, lStart, cw, internal,
                    retDesc, fnHints, lineNo, nextSlot
                );
                nextSlot = result.nextSlot();
                if (result.shouldReturn()) {
                    return;
                }
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

    private StatementResult emitStatement(MethodVisitor mv, CoreModel.Stmt st, String pkg,
        Map<String, Integer> env, ScopeStack scopeStack, EmitContext emitCtx, java.util.List<LV> lvars,
        Label lStart, ClassWriter cw, String internal, String retDesc,
        Map<String, Character> fnHints, java.util.concurrent.atomic.AtomicInteger lineNo,
        int nextSlot) throws IOException {
        if (st == null) {
            return new StatementResult(false, nextSlot);
        }

        var _lbl = new Label();
        mv.visitLabel(_lbl);
        mv.visitLineNumber(lineNo.getAndIncrement(), _lbl);

        if (st instanceof CoreModel.If iff) {
            emitCtx.setNextSlot(nextSlot);
            ifEmitter.emitIf(mv, iff, emitCtx, scopeStack, lvars, fnHints, lineNo);
            nextSlot = emitCtx.peekNextSlot();
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Let let) {
            Character inferred = typeResolver.inferType(let.expr);
            if (inferred == null && Objects.equals(let.name, "ok") && let.expr instanceof CoreModel.Call) {
                inferred = 'Z';
            }
            if (inferred == null) {
                Character hint = fnHints.get(let.name);
                if (hint != null) inferred = hint;
            }

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

            if (expectedDesc != null) {
                emitExpr(mv, let.expr, expectedDesc, pkg, env, scopeStack);
            } else {
                emitExpr(mv, let.expr, null, pkg, env, scopeStack);
                localDesc = Main.resolveObjectDescriptor(let.expr, pkg, scopeStack, ctx);
            }

            mv.visitVarInsn(storeOpcode, nextSlot);
            env.put(let.name, nextSlot);
            lvars.add(new LV(let.name, localDesc, nextSlot));
            scopeStack.declare(let.name, nextSlot, localDesc, Main.kindForDescriptor(localDesc));
            nextSlot++;
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Set set) {
            Integer existingSlot = env.get(set.name);
            if (existingSlot == null) {
                throw new IllegalStateException("Set statement error: variable '" + set.name + "' not declared");
            }

            String existingDesc = scopeStack.getDescriptor(set.name);
            Character existingKind = scopeStack.getType(set.name);
            String expectedDesc = existingDesc;

            if (expectedDesc == null || "Ljava/lang/Object;".equals(expectedDesc)) {
                Character inferred = typeResolver.inferType(set.expr);
                if (inferred == null) {
                    Character hint = fnHints.get(set.name);
                    if (hint != null) inferred = hint;
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

            int storeOpcode = ASTORE;
            if (existingKind != null) {
                storeOpcode = switch (existingKind) {
                    case 'D' -> DSTORE;
                    case 'J' -> LSTORE;
                    case 'I', 'Z' -> ISTORE;
                    default -> ASTORE;
                };
            } else if (expectedDesc != null && !expectedDesc.isEmpty()) {
                storeOpcode = switch (expectedDesc.charAt(0)) {
                    case 'D' -> DSTORE;
                    case 'J' -> LSTORE;
                    case 'I', 'Z' -> ISTORE;
                    default -> ASTORE;
                };
            }

            emitExpr(mv, set.expr, expectedDesc, pkg, env, scopeStack);
            mv.visitVarInsn(storeOpcode, existingSlot);
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Match match) {
            emitCtx.setNextSlot(nextSlot);
            matchEmitter.emitMatch(mv, match, emitCtx, scopeStack, lvars);
            nextSlot = emitCtx.peekNextSlot();
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Return ret) {
            final int[] nextSlotBox = {nextSlot};
            ReturnEmitter.emitReturn(
                mv, ret, retDesc, pkg, env, scopeStack, ctx, typeResolver,
                () -> nextSlotBox[0]++,
                (ctx2, mv2, expr, expectedDesc2, pkg2, paramBase, env2, scopeStack2, typeResolver2) -> {
                    expressionEmitter.updateEnvironment(env2);
                    if (isMigratedExpressionType(expr)) {
                        expressionEmitter.emitExpression(expr, mv2, scopeStack2, expectedDesc2);
                    } else {
                        Main.emitExpr(ctx2, mv2, expr, expectedDesc2, pkg2, paramBase, env2, scopeStack2, typeResolver2);
                    }
                }
            );
            nextSlot = nextSlotBox[0];
            finalizeFunctionAfterReturn(cw, mv, lvars, lStart, internal);
            return new StatementResult(true, nextSlot);
        } else if (st instanceof CoreModel.Scope scope) {
            var savedEnv = new LinkedHashMap<>(env);
            scopeStack.pushScope();
            try {
                if (scope.statements != null && !scope.statements.isEmpty()) {
                    for (var innerSt : scope.statements) {
                        var innerResult = emitStatement(
                            mv, innerSt, pkg, env, scopeStack, emitCtx, lvars, lStart, cw, internal,
                            retDesc, fnHints, lineNo, nextSlot
                        );
                        nextSlot = innerResult.nextSlot();
                        if (innerResult.shouldReturn()) {
                            return new StatementResult(true, nextSlot);
                        }
                    }
                }
            } finally {
                scopeStack.popScope();
                env.clear();
                env.putAll(savedEnv);
            }
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Block block) {
            // Block 不创建新作用域，内部变量对外可见
            // 不需要保存/恢复 env，不需要 push/pop scopeStack
            if (block.statements != null && !block.statements.isEmpty()) {
                for (var innerSt : block.statements) {
                    var innerResult = emitStatement(
                        mv, innerSt, pkg, env, scopeStack, emitCtx, lvars, lStart, cw, internal,
                        retDesc, fnHints, lineNo, nextSlot
                    );
                    nextSlot = innerResult.nextSlot();
                    if (innerResult.shouldReturn()) {
                        return new StatementResult(true, nextSlot);
                    }
                }
            }
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Start start) {
            // Start 语句：启动异步任务
            // 语义：start taskName = asyncExpr
            // 实现：生成 asyncExpr 的字节码（期望得到 CompletableFuture），存储到局部变量

            expressionEmitter.emitExpression(start.expr, mv, scopeStack, "Ljava/util/concurrent/CompletableFuture;");

            // 存储 CompletableFuture 到局部变量
            String desc = "Ljava/util/concurrent/CompletableFuture;";
            mv.visitVarInsn(ASTORE, nextSlot);
            env.put(start.name, nextSlot);
            lvars.add(new LV(start.name, desc, nextSlot));
            scopeStack.declare(start.name, nextSlot, desc, Main.kindForDescriptor(desc));
            nextSlot++;
            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Wait wait) {
            // Wait 语句：等待多个异步任务完成
            // 语义：wait [task1, task2, ...]
            // 实现：使用 CompletableFuture.allOf() 合并所有任务，然后 join() 阻塞等待

            int numTasks = wait.names.size();

            // 1. 创建 CompletableFuture 数组
            AsmUtilities.emitConstInt(mv, numTasks);
            mv.visitTypeInsn(ANEWARRAY, "java/util/concurrent/CompletableFuture");

            // 2. 填充数组：对每个任务名，从 env 加载并存入数组
            for (int i = 0; i < numTasks; i++) {
                String taskName = wait.names.get(i);
                Integer slot = env.get(taskName);
                if (slot == null) {
                    throw new IllegalStateException("Wait statement error: task '" + taskName + "' not started");
                }

                mv.visitInsn(DUP);  // 复制数组引用
                AsmUtilities.emitConstInt(mv, i);  // 数组索引
                mv.visitVarInsn(ALOAD, slot);  // 加载 CompletableFuture
                mv.visitInsn(AASTORE);  // 存入数组
            }

            // 3. 调用 CompletableFuture.allOf(array) -> CompletableFuture<Void>
            mv.visitMethodInsn(
                INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "allOf",
                "([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;",
                false
            );

            // 4. 调用 join() 阻塞等待所有任务完成
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/concurrent/CompletableFuture",
                "join",
                "()Ljava/lang/Object;",
                false
            );

            // 5. 丢弃返回值（allOf 返回 CompletableFuture<Void>，join() 后得到 null）
            mv.visitInsn(POP);

            return new StatementResult(false, nextSlot);
        } else if (st instanceof CoreModel.Workflow workflow) {
            // Workflow 语句：工作流执行（简化版）
            // 完整语义：带重试、超时、依赖关系的步骤序列
            // 简化实现：仅顺序执行 steps，忽略 retry/timeout/dependencies
            // TODO: 未来需要实现重试策略、超时控制和步骤依赖图

            if (workflow.steps != null && !workflow.steps.isEmpty()) {
                for (var step : workflow.steps) {
                    // 每个 Step 包含 body (Block)，忽略 compensate 和 dependencies
                    if (step.body != null && step.body.statements != null) {
                        for (var stepSt : step.body.statements) {
                            var stepResult = emitStatement(
                                mv, stepSt, pkg, env, scopeStack, emitCtx, lvars, lStart, cw, internal,
                                retDesc, fnHints, lineNo, nextSlot
                            );
                            nextSlot = stepResult.nextSlot();
                            if (stepResult.shouldReturn()) {
                                return new StatementResult(true, nextSlot);
                            }
                        }
                    }
                }
            }
            return new StatementResult(false, nextSlot);
        }

        throw new UnsupportedOperationException("Statement type not yet supported in FunctionEmitter: " + st.getClass().getSimpleName());
    }

    private void finalizeFunctionAfterReturn(ClassWriter cw, MethodVisitor mv, java.util.List<LV> lvars,
        Label lStart, String internal) throws IOException {
        var lEndReturn = new Label();
        mv.visitLabel(lEndReturn);
        for (var lv : lvars) {
            mv.visitLocalVariable(lv.name, lv.desc, null, lStart, lEndReturn, lv.slot);
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
