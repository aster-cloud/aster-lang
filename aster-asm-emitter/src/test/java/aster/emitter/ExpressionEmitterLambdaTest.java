package aster.emitter;

import aster.core.ir.CoreModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionEmitter Lambda 表达式集成测试
 *
 * 验证 ExpressionEmitter 通过委托模式正确处理 Lambda 表达式：
 * - Lambda 表达式正确路由到 LambdaEmitter
 * - 生成的字节码与 Legacy 实现一致（NEW, DUP, 加载闭包, INVOKESPECIAL）
 * - 支持不同参数数量和闭包捕获场景
 */
class ExpressionEmitterLambdaTest {

    private ExpressionEmitter expressionEmitter;
    private ScopeStack scopeStack;
    private MethodNode node;
    private Main.Ctx ctx;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        // 创建测试夹具
        CoreModel.Module emptyModule = new CoreModel.Module();
        emptyModule.name = "app";
        emptyModule.decls = List.of();
        ContextBuilder contextBuilder = new ContextBuilder(emptyModule);

        scopeStack = new ScopeStack();
        TypeResolver typeResolver = new TypeResolver(scopeStack, Map.of(), Map.of(), contextBuilder);
        ctx = new Main.Ctx(tempDir, contextBuilder, new AtomicInteger(0), Map.of(), Map.of(), Map.of());

        NameEmitter nameEmitter = new NameEmitter(typeResolver, ctx);
        CallEmitter callEmitter = new CallEmitter(typeResolver, new SignatureResolver(false), ctx, StdlibInliner.instance());

        expressionEmitter = new ExpressionEmitter(
            ctx, "app", 0, new HashMap<>(), scopeStack, typeResolver, nameEmitter, callEmitter
        );

        node = new MethodNode(Opcodes.ASM9);
    }

    /**
     * 创建测试用的 Lambda 对象
     */
    private CoreModel.Lambda createLambda(int arity, List<String> captures) {
        CoreModel.Lambda lam = new CoreModel.Lambda();
        lam.params = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            CoreModel.Param param = new CoreModel.Param();
            param.name = "p" + i;
            CoreModel.TypeName type = new CoreModel.TypeName();
            type.name = "Object";
            param.type = type;
            lam.params.add(param);
        }
        lam.ret = null;
        lam.captures = captures;
        CoreModel.Block body = new CoreModel.Block();
        body.statements = List.of();
        lam.body = body;
        return lam;
    }

    /**
     * 测试无参 Lambda（Fn0）无闭包
     * 示例：() -> 42
     */
    @Test
    void testEmitLambdaFn0WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(0, null);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证字节码序列：NEW Lambda$N, DUP, INVOKESPECIAL <init>()V
        assertTrue(insns.length >= 3, "应该至少有 3 条指令 (NEW, DUP, INVOKESPECIAL)");
        assertEquals(Opcodes.NEW, insns[0].getOpcode(), "第 1 条指令应该是 NEW");
        assertTrue(((TypeInsnNode) insns[0]).desc.contains("Lambda$"), "应该创建 Lambda$N 类");

        assertEquals(Opcodes.DUP, insns[1].getOpcode(), "第 2 条指令应该是 DUP");

        assertEquals(Opcodes.INVOKESPECIAL, insns[2].getOpcode(), "第 3 条指令应该是 INVOKESPECIAL");
        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name, "应该调用构造函数");
        assertEquals("()V", initCall.desc, "无闭包时构造函数签名应为 ()V");
    }

    /**
     * 测试单参 Lambda（Fn1）无闭包
     * 示例：(x) -> x + 1
     */
    @Test
    void testEmitLambdaFn1WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(1, null);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        assertTrue(insns.length >= 3, "应该至少有 3 条指令");
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name);
        assertEquals("()V", initCall.desc, "Fn1 无闭包时构造函数签名仍为 ()V");
    }

    /**
     * 测试 Lambda 捕获 int 变量
     * 示例：let x = 42; (a) -> a + x
     */
    @Test
    void testEmitLambdaWithIntCapture() {
        CoreModel.Lambda lam = createLambda(1, List.of("x"));

        Map<String, Integer> env = new HashMap<>();
        env.put("x", 1);
        expressionEmitter.updateEnvironment(env);
        scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证字节码序列：NEW, DUP, ILOAD 1, Integer.valueOf, INVOKESPECIAL <init>(Object)V
        assertTrue(insns.length >= 5, "应该至少有 5 条指令");
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        // 验证 int 闭包变量装箱
        assertEquals(Opcodes.ILOAD, insns[2].getOpcode(), "应该加载 int 变量");
        assertEquals(1, ((VarInsnNode) insns[2]).var, "应该加载 slot 1");

        assertEquals(Opcodes.INVOKESTATIC, insns[3].getOpcode(), "应该装箱");
        MethodInsnNode boxingCall = (MethodInsnNode) insns[3];
        assertEquals("java/lang/Integer", boxingCall.owner, "应该调用 Integer.valueOf");
        assertEquals("valueOf", boxingCall.name);

        assertEquals(Opcodes.INVOKESPECIAL, insns[4].getOpcode());
        MethodInsnNode initCall = (MethodInsnNode) insns[4];
        assertEquals("(Ljava/lang/Object;)V", initCall.desc, "构造函数应接收 1 个 Object 参数");
    }

    /**
     * 测试 Lambda 捕获 long 变量
     */
    @Test
    void testEmitLambdaWithLongCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("y"));

        Map<String, Integer> env = new HashMap<>();
        env.put("y", 2);
        expressionEmitter.updateEnvironment(env);
        scopeStack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证 long 装箱
        boolean foundLLoad = false;
        boolean foundLongValueOf = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.LLOAD) {
                assertEquals(2, ((VarInsnNode) insn).var);
                foundLLoad = true;
            }
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("java/lang/Long".equals(call.owner) && "valueOf".equals(call.name)) {
                    foundLongValueOf = true;
                }
            }
        }
        assertTrue(foundLLoad, "应该有 LLOAD 指令加载 long 变量");
        assertTrue(foundLongValueOf, "应该有 Long.valueOf 装箱调用");
    }

    /**
     * 测试 Lambda 捕获 double 变量
     */
    @Test
    void testEmitLambdaWithDoubleCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("z"));

        Map<String, Integer> env = new HashMap<>();
        env.put("z", 3);
        expressionEmitter.updateEnvironment(env);
        scopeStack.declare("z", 3, "D", ScopeStack.JvmKind.DOUBLE);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证 double 装箱
        boolean foundDLoad = false;
        boolean foundDoubleValueOf = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.DLOAD) {
                assertEquals(3, ((VarInsnNode) insn).var);
                foundDLoad = true;
            }
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("java/lang/Double".equals(call.owner) && "valueOf".equals(call.name)) {
                    foundDoubleValueOf = true;
                }
            }
        }
        assertTrue(foundDLoad, "应该有 DLOAD 指令加载 double 变量");
        assertTrue(foundDoubleValueOf, "应该有 Double.valueOf 装箱调用");
    }

    /**
     * 测试 Lambda 捕获 Object 变量（无需装箱）
     */
    @Test
    void testEmitLambdaWithObjectCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("obj"));

        Map<String, Integer> env = new HashMap<>();
        env.put("obj", 5);
        expressionEmitter.updateEnvironment(env);
        scopeStack.declare("obj", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证直接 ALOAD（不装箱）
        boolean foundALoad = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.ALOAD) {
                VarInsnNode varInsn = (VarInsnNode) insn;
                if (varInsn.var == 5) {
                    foundALoad = true;
                    break;
                }
            }
        }
        assertTrue(foundALoad, "应该有 ALOAD 5 指令直接加载 Object");
    }

    /**
     * 测试 Lambda 捕获多个混合类型变量
     * 示例：let x = 1, y = 2L, obj = "hello"; () -> ...
     */
    @Test
    void testEmitLambdaWithMultipleCaptures() {
        CoreModel.Lambda lam = createLambda(0, List.of("x", "y", "obj"));

        Map<String, Integer> env = new HashMap<>();
        env.put("x", 1);
        env.put("y", 2);
        env.put("obj", 5);
        expressionEmitter.updateEnvironment(env);
        scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);
        scopeStack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);
        scopeStack.declare("obj", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证构造函数签名：<init>(Object, Object, Object)V
        MethodInsnNode initCall = null;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                initCall = (MethodInsnNode) insn;
                break;
            }
        }
        assertNotNull(initCall, "应该有构造函数调用");
        assertEquals("<init>", initCall.name);
        assertEquals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", initCall.desc,
            "构造函数应接收 3 个 Object 参数（装箱后）");
    }

    /**
     * 测试 Lambda 捕获不存在的变量（env 中缺失）
     * 应该生成 ACONST_NULL
     */
    @Test
    void testEmitLambdaWithMissingCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("missing"));

        expressionEmitter.updateEnvironment(Map.of());  // 空环境

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证有 ACONST_NULL 指令
        boolean foundNull = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.ACONST_NULL) {
                foundNull = true;
                break;
            }
        }
        assertTrue(foundNull, "缺失的闭包变量应该生成 ACONST_NULL");
    }

    /**
     * 测试双参 Lambda（Fn2）无闭包
     * 示例：(x, y) -> x + y
     */
    @Test
    void testEmitLambdaFn2WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(2, null);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        assertTrue(insns.length >= 3, "应该至少有 3 条指令");
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name);
        assertEquals("()V", initCall.desc, "Fn2 无闭包时构造函数签名为 ()V");
    }

    /**
     * 测试三参 Lambda（Fn3）无闭包
     * 示例：(x, y, z) -> x + y + z
     */
    @Test
    void testEmitLambdaFn3WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(3, null);

        expressionEmitter.emitExpression(lam, node, scopeStack, null);

        AbstractInsnNode[] insns = node.instructions.toArray();

        assertTrue(insns.length >= 3, "应该至少有 3 条指令");
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name);
        assertEquals("()V", initCall.desc, "Fn3 无闭包时构造函数签名为 ()V");
    }
}
