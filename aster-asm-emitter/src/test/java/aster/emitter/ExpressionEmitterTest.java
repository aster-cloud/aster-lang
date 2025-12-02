package aster.emitter;

import aster.core.ir.CoreModel;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 ExpressionEmitter 对简单常量表达式的字节码输出。
 */
class ExpressionEmitterTest {

  @Test
  void testEmitIntConstant() {
    var emitter = newEmitter();
    var expr = new CoreModel.IntE();
    expr.value = 42;

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    IntInsnNode intInsn = assertInstanceOf(IntInsnNode.class, nodes[0]);
    assertEquals(Opcodes.BIPUSH, intInsn.getOpcode());
    assertEquals(42, intInsn.operand);
  }

  @Test
  void testEmitBoolConstant() {
    var emitter = newEmitter();
    var expr = new CoreModel.Bool();
    expr.value = true;

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(2, nodes.length);
    assertEquals(Opcodes.ICONST_1, nodes[0].getOpcode());
    MethodInsnNode call = assertInstanceOf(MethodInsnNode.class, nodes[1]);
    assertEquals("java/lang/Boolean", call.owner);
    assertEquals("valueOf", call.name);
    assertEquals("(Z)Ljava/lang/Boolean;", call.desc);
  }

  @Test
  void testEmitStringConstant() {
    Map<String, String> pool = new LinkedHashMap<>();
    var emitter = newEmitter(pool);
    var expr = new CoreModel.StringE();
    expr.value = "hello";

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    LdcInsnNode ldc = assertInstanceOf(LdcInsnNode.class, nodes[0]);
    assertEquals("hello", ldc.cst);
    assertTrue(pool.containsKey("hello"));
  }

  @Test
  void testEmitLongConstant() {
    var emitter = newEmitter();
    var expr = new CoreModel.LongE();
    expr.value = 5L;

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    LdcInsnNode ldc = assertInstanceOf(LdcInsnNode.class, nodes[0]);
    assertEquals(5L, ldc.cst);
  }

  @Test
  void testEmitLongToIntConversion() {
    var emitter = newEmitter();
    var expr = new CoreModel.LongE();
    expr.value = 1L;

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, "I");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(2, nodes.length);
    InsnNode lconst = assertInstanceOf(InsnNode.class, nodes[0]);
    assertEquals(Opcodes.LCONST_1, lconst.getOpcode());
    InsnNode convert = assertInstanceOf(InsnNode.class, nodes[1]);
    assertEquals(Opcodes.L2I, convert.getOpcode());
  }

  @Test
  void testEmitDoubleConstantBoxed() {
    var emitter = newEmitter();
    var expr = new CoreModel.DoubleE();
    expr.value = 2.5d;

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(2, nodes.length);
    LdcInsnNode ldc = assertInstanceOf(LdcInsnNode.class, nodes[0]);
    assertEquals(2.5d, ldc.cst);
    MethodInsnNode box = assertInstanceOf(MethodInsnNode.class, nodes[1]);
    assertEquals("java/lang/Double", box.owner);
    assertEquals("valueOf", box.name);
    assertEquals("(D)Ljava/lang/Double;", box.desc);
  }

  @Test
  void testEmitNullConstant() {
    var emitter = newEmitter();
    var expr = new CoreModel.NullE();

    MethodNode mv = new MethodNode();
    emitter.emitExpression(expr, mv, null, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    InsnNode insn = assertInstanceOf(InsnNode.class, nodes[0]);
    assertEquals(Opcodes.ACONST_NULL, insn.getOpcode());
  }

  private ExpressionEmitter newEmitter() {
    return newEmitter(new LinkedHashMap<>());
  }

  private ExpressionEmitter newEmitter(Map<String, String> stringPool) {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    var context = new ContextBuilder(module);
    var scope = new ScopeStack();
    var typeResolver = new TypeResolver(scope, Map.of(), Map.of(), context);
    var nameEmitter = new NameEmitter(typeResolver, null);
    var callEmitter = new CallEmitter(typeResolver, new SignatureResolver(false), null, StdlibInliner.instance());
    return new ExpressionEmitter(context, typeResolver, scope, stringPool, nameEmitter, callEmitter);
  }
}
