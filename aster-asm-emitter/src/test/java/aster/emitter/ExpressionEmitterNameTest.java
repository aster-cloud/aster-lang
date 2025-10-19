package aster.emitter;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 验证 ExpressionEmitter 对 Name 表达式的处理逻辑。
 */
class ExpressionEmitterNameTest {

  @Test
  void emitLocalVariableLoadsPrimitiveSlot() {
    var fixture = newFixture();
    fixture.env.put("x", 1);
    fixture.scope.declare("x", 1, "I", ScopeStack.JvmKind.INT);
    fixture.emitter.updateEnvironment(fixture.env);

    CoreModel.Name expr = new CoreModel.Name();
    expr.name = "x";

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(expr, mv, fixture.scope, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    VarInsnNode load = assertInstanceOf(VarInsnNode.class, nodes[0]);
    assertEquals(Opcodes.ILOAD, load.getOpcode());
    assertEquals(1, load.var);
  }

  @Test
  void emitParameterBoxesWhenExpectedObject() {
    var fixture = newFixture();
    fixture.env.put("param", 0);
    fixture.scope.declare("param", 0, "I", ScopeStack.JvmKind.INT);
    fixture.emitter.updateEnvironment(fixture.env);

    CoreModel.Name expr = new CoreModel.Name();
    expr.name = "param";

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(expr, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(2, nodes.length);
    VarInsnNode load = assertInstanceOf(VarInsnNode.class, nodes[0]);
    assertEquals(Opcodes.ILOAD, load.getOpcode());
    assertEquals(0, load.var);
    MethodInsnNode boxing = assertInstanceOf(MethodInsnNode.class, nodes[1]);
    assertEquals("java/lang/Integer", boxing.owner);
    assertEquals("valueOf", boxing.name);
    assertEquals("(I)Ljava/lang/Integer;", boxing.desc);
  }

  @Test
  void emitBuiltinFunctionReference() {
    var fixture = newFixture();
    fixture.emitter.updateEnvironment(fixture.env);

    CoreModel.Name expr = new CoreModel.Name();
    expr.name = "not";

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(expr, mv, fixture.scope, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    FieldInsnNode field = assertInstanceOf(FieldInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Builtins", field.owner);
    assertEquals("NOT", field.name);
    assertEquals("Laster/runtime/Fn1;", field.desc);
  }

  @Test
  void emitFieldAccessFromLocalObject() {
    var fixture = newFixture();
    fixture.env.put("point", 2);
    fixture.scope.declare("point", 2, "Lapp/Point;", ScopeStack.JvmKind.OBJECT);
    fixture.emitter.updateEnvironment(fixture.env);

    CoreModel.Name expr = new CoreModel.Name();
    expr.name = "point.x";

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(expr, mv, fixture.scope, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(2, nodes.length);
    VarInsnNode load = assertInstanceOf(VarInsnNode.class, nodes[0]);
    assertEquals(Opcodes.ALOAD, load.getOpcode());
    assertEquals(2, load.var);
    FieldInsnNode field = assertInstanceOf(FieldInsnNode.class, nodes[1]);
    assertEquals("app/Point", field.owner);
    assertEquals("x", field.name);
    assertEquals("Ljava/lang/Object;", field.desc);
  }

  @Test
  void emitFallbackNullForUnknownName() {
    var fixture = newFixture();
    fixture.emitter.updateEnvironment(fixture.env);

    CoreModel.Name expr = new CoreModel.Name();
    expr.name = "unknownSymbol";

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(expr, mv, fixture.scope, null);

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    assertEquals(1, nodes.length);
    InsnNode nullInsn = assertInstanceOf(InsnNode.class, nodes[0]);
    assertEquals(Opcodes.ACONST_NULL, nullInsn.getOpcode());
  }

  private Fixture newFixture() {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    var context = new ContextBuilder(module);
    var scope = new ScopeStack();
    var typeResolver = new TypeResolver(scope, Map.of(), Map.of(), context);
    var nameEmitter = new NameEmitter(typeResolver, null);
    var callEmitter = new CallEmitter(typeResolver, new SignatureResolver(false), null, StdlibInliner.instance());
    var emitter = new ExpressionEmitter(context, typeResolver, scope, new LinkedHashMap<>(), nameEmitter, callEmitter);
    return new Fixture(scope, emitter);
  }

  private static final class Fixture {
    final ScopeStack scope;
    final ExpressionEmitter emitter;
    final Map<String, Integer> env = new LinkedHashMap<>();

    private Fixture(ScopeStack scope, ExpressionEmitter emitter) {
      this.scope = scope;
      this.emitter = emitter;
    }
  }
}
