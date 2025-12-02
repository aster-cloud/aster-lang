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
 * LambdaEmitter 单元测试
 *
 * 测试 Lambda 表达式字节码生成的正确性，包括：
 * - 不同参数数量的 Lambda（Fn0, Fn1, Fn2）
 * - 闭包捕获（无捕获、单个捕获、多个捕获）
 * - 不同类型的闭包变量（int, long, double, Object）
 */
class LambdaEmitterTest {

    private TypeResolver typeResolver;
    private Main.Ctx ctx;
    private LambdaEmitter.LambdaBodyEmitter mockBodyEmitter;
    private LambdaEmitter emitter;
    private MethodNode node;
    private ScopeStack scopeStack;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        // 创建 ContextBuilder
        CoreModel.Module emptyModule = new CoreModel.Module();
        emptyModule.decls = List.of();
        ContextBuilder contextBuilder = new ContextBuilder(emptyModule);

        typeResolver = new TypeResolver(new ScopeStack(), Map.of(), Map.of(), contextBuilder);
        ctx = new Main.Ctx(tempDir, contextBuilder, new AtomicInteger(0), Map.of(), Map.of(), Map.of());
        scopeStack = new ScopeStack();

        // Mock LambdaBodyEmitter：简单返回 null
        mockBodyEmitter = (c, mv, body, internal, env, primTypes, retIsResult, lineNo) -> {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            return true;
        };

        emitter = new LambdaEmitter(typeResolver, ctx, mockBodyEmitter);

        // 创建 MethodNode 用于字节码检查
        node = new MethodNode(Opcodes.ASM9);
    }

    /**
     * 创建简单的 Lambda 对象
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
     * 测试无参数 Lambda（Fn0）且无闭包捕获
     * 示例：() -> 42
     */
    @Test
    void testLambdaFn0WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(0, null);

        emitter.emitLambda(node, lam, "app", Map.of(), scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证字节码序列：NEW, DUP, INVOKESPECIAL <init>
        assertTrue(insns.length >= 3);
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertTrue(((TypeInsnNode) insns[0]).desc.contains("Lambda$"));

        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        assertEquals(Opcodes.INVOKESPECIAL, insns[2].getOpcode());
        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name);
        assertEquals("()V", initCall.desc);  // 无闭包参数
    }

    /**
     * 测试单参数 Lambda（Fn1）且无闭包捕获
     * 示例：(x) -> x + 1
     */
    @Test
    void testLambdaFn1WithoutCaptures() {
        CoreModel.Lambda lam = createLambda(1, null);

        emitter.emitLambda(node, lam, "app", Map.of(), scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：NEW, DUP, INVOKESPECIAL <init>()V
        assertTrue(insns.length >= 3);
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        MethodInsnNode initCall = (MethodInsnNode) insns[2];
        assertEquals("<init>", initCall.name);
        assertEquals("()V", initCall.desc);  // Fn1 但无闭包，构造函数仍为 ()V
    }

    /**
     * 测试 Lambda 捕获单个 int 变量
     * 示例：let x = 42; (a) -> a + x
     */
    @Test
    void testLambdaWithIntCapture() {
        CoreModel.Lambda lam = createLambda(1, List.of("x"));

        Map<String, Integer> env = Map.of("x", 1);
        scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：NEW, DUP, ILOAD 1, Integer.valueOf, INVOKESPECIAL <init>(Object)V
        assertTrue(insns.length >= 5);
        assertEquals(Opcodes.NEW, insns[0].getOpcode());
        assertEquals(Opcodes.DUP, insns[1].getOpcode());

        assertEquals(Opcodes.ILOAD, insns[2].getOpcode());
        assertEquals(1, ((VarInsnNode) insns[2]).var);

        assertEquals(Opcodes.INVOKESTATIC, insns[3].getOpcode());
        MethodInsnNode boxingCall = (MethodInsnNode) insns[3];
        assertEquals("java/lang/Integer", boxingCall.owner);
        assertEquals("valueOf", boxingCall.name);
        assertEquals("(I)Ljava/lang/Integer;", boxingCall.desc);

        assertEquals(Opcodes.INVOKESPECIAL, insns[4].getOpcode());
        MethodInsnNode initCall = (MethodInsnNode) insns[4];
        assertEquals("<init>", initCall.name);
        assertEquals("(Ljava/lang/Object;)V", initCall.desc);  // 1 个闭包参数
    }

    /**
     * 测试 Lambda 捕获 long 变量
     */
    @Test
    void testLambdaWithLongCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("y"));

        Map<String, Integer> env = Map.of("y", 2);
        scopeStack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：LLOAD 2, Long.valueOf
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
        assertTrue(foundLLoad, "应该有 LLOAD 指令");
        assertTrue(foundLongValueOf, "应该有 Long.valueOf 调用");
    }

    /**
     * 测试 Lambda 捕获 double 变量
     */
    @Test
    void testLambdaWithDoubleCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("z"));

        Map<String, Integer> env = Map.of("z", 3);
        scopeStack.declare("z", 3, "D", ScopeStack.JvmKind.DOUBLE);

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：DLOAD 3, Double.valueOf
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
        assertTrue(foundDLoad, "应该有 DLOAD 指令");
        assertTrue(foundDoubleValueOf, "应该有 Double.valueOf 调用");
    }

    /**
     * 测试 Lambda 捕获 Object 变量
     */
    @Test
    void testLambdaWithObjectCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("obj"));

        Map<String, Integer> env = Map.of("obj", 5);
        scopeStack.declare("obj", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：ALOAD 5 (直接加载 Object，不装箱)
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
        assertTrue(foundALoad, "应该有 ALOAD 5 指令");
    }

    /**
     * 测试 Lambda 捕获多个变量（混合类型）
     */
    @Test
    void testLambdaWithMultipleCaptures() {
        CoreModel.Lambda lam = createLambda(0, List.of("x", "y", "obj"));

        Map<String, Integer> env = Map.of("x", 1, "y", 2, "obj", 5);
        scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);
        scopeStack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);
        scopeStack.declare("obj", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证构造函数调用：<init>(Object, Object, Object)V
        MethodInsnNode initCall = null;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                initCall = (MethodInsnNode) insn;
                break;
            }
        }
        assertNotNull(initCall);
        assertEquals("<init>", initCall.name);
        assertEquals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", initCall.desc);
    }

    /**
     * 测试 Lambda 捕获不存在的变量（env 中没有）
     * 应该生成 ACONST_NULL
     */
    @Test
    void testLambdaWithMissingCapture() {
        CoreModel.Lambda lam = createLambda(0, List.of("missing"));

        Map<String, Integer> env = Map.of();  // 空环境，"missing" 不存在

        emitter.emitLambda(node, lam, "app", env, scopeStack);

        AbstractInsnNode[] insns = node.instructions.toArray();

        // 验证：应该有 ACONST_NULL
        boolean foundNull = false;
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.ACONST_NULL) {
                foundNull = true;
                break;
            }
        }
        assertTrue(foundNull, "缺失的闭包变量应该生成 ACONST_NULL");
    }
}
