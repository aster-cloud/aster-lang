package aster.emitter;

import aster.core.ir.CoreModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.nio.file.Path;

/**
 * 验证 NameEmitter 针对各类 Name 表达式场景的字节码输出。
 */
class NameEmitterTest {

  private NameEmitter emitter;
  private Main.Ctx ctx;
  private ScopeStack scopeStack;

  @BeforeEach
  void setUp() {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    ContextBuilder contextBuilder = new ContextBuilder(module);
    TypeResolver typeResolver = new TypeResolver(
        new ScopeStack(),
        Map.of(),
        Map.of(),
        contextBuilder
    );
    ctx = new Main.Ctx(
        Path.of("."),
        contextBuilder,
        new AtomicInteger(),
        Map.of(),
        new LinkedHashMap<>(),
        Map.of()
    );
    scopeStack = new ScopeStack();
    emitter = new NameEmitter(typeResolver, ctx);
  }

  @Test
  void testLocalVariableAccessInt() {
    // 测试局部变量访问（int 类型）
    Map<String, Integer> env = Map.of("x", 1);
    scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "x";

    boolean handled = emitter.tryEmitName(node, name, "I", "app", 0, env, scopeStack);
    assertTrue(handled);

    AbstractInsnNode[] insns = node.instructions.toArray();
    assertTrue(insns.length > 0);
    assertEquals(Opcodes.ILOAD, insns[0].getOpcode());
    assertEquals(1, ((VarInsnNode) insns[0]).var);
  }

  @Test
  void testLocalVariableAccessWithBoxing() {
    // 测试局部变量访问并装箱（int → Object）
    Map<String, Integer> env = Map.of("x", 1);
    scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "x";

    boolean handled = emitter.tryEmitName(node, name, "Ljava/lang/Object;", "app", 0, env, scopeStack);
    assertTrue(handled);

    // 应该有 ILOAD + Integer.valueOf 调用
    AbstractInsnNode[] insns = node.instructions.toArray();
    assertTrue(insns.length >= 2);
    assertEquals(Opcodes.ILOAD, insns[0].getOpcode());
    assertEquals(Opcodes.INVOKESTATIC, insns[1].getOpcode());

    MethodInsnNode boxingCall = (MethodInsnNode) insns[1];
    assertEquals("java/lang/Integer", boxingCall.owner);
    assertEquals("valueOf", boxingCall.name);
  }

  @Test
  void testLocalVariableTypeConversion() {
    // 测试类型转换（int → long）
    Map<String, Integer> env = Map.of("x", 1);
    scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "x";

    boolean handled = emitter.tryEmitName(node, name, "J", "app", 0, env, scopeStack);
    assertTrue(handled);

    AbstractInsnNode[] insns = node.instructions.toArray();
    assertTrue(insns.length >= 2);
    assertEquals(Opcodes.ILOAD, insns[0].getOpcode());
    assertEquals(Opcodes.I2L, insns[1].getOpcode());
  }

  @Test
  void testLocalVariableLongToDouble() {
    // 测试类型转换（long → double）
    Map<String, Integer> env = Map.of("y", 2);
    scopeStack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);

    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "y";

    boolean handled = emitter.tryEmitName(node, name, "D", "app", 0, env, scopeStack);
    assertTrue(handled);

    AbstractInsnNode[] insns = node.instructions.toArray();
    assertTrue(insns.length >= 2);
    assertEquals(Opcodes.LLOAD, insns[0].getOpcode());
    assertEquals(Opcodes.L2D, insns[1].getOpcode());
  }

  @Test
  void testEnumValueAccess() {
    // 测试枚举值访问
    // 注意：需要 ctx 支持 enumOwner 查找，这里仅验证逻辑分支
    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "SomeEnum";

    boolean handled = emitter.tryEmitName(node, name, null, "app", 0, Map.of(), scopeStack);
    assertTrue(handled);
    // 由于没有实际 enum 定义，会走到 fallback (ACONST_NULL)
  }

  @Test
  void testFieldAccessFallback() {
    // 测试字段访问回退（未找到字段）
    Map<String, Integer> env = Map.of("record", 1);
    scopeStack.declare("record", 1, "Lapp/Record;", ScopeStack.JvmKind.OBJECT);

    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "record.field";

    boolean handled = emitter.tryEmitName(node, name, "I", "app", 0, env, scopeStack);
    assertTrue(handled);
    // 应该有 ALOAD + GETFIELD 指令
    AbstractInsnNode[] insns = node.instructions.toArray();
    assertTrue(insns.length >= 2);
  }

  @Test
  void testBuiltinFieldNotHandled() {
    // 测试内置字段（NOT, ADD 等）
    // 注意：Main.getBuiltinField 是静态方法，这里仅验证逻辑分支
    MethodNode node = new MethodNode();
    CoreModel.Name name = new CoreModel.Name();
    name.name = "unknownBuiltin";

    boolean handled = emitter.tryEmitName(node, name, null, "app", 0, Map.of(), scopeStack);
    assertTrue(handled);
    // 走到 fallback (ACONST_NULL)
    AbstractInsnNode[] insns = node.instructions.toArray();
    assertEquals(1, insns.length);
    assertEquals(Opcodes.ACONST_NULL, insns[0].getOpcode());
  }
}
