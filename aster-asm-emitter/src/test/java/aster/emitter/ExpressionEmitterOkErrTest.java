package aster.emitter;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 验证 ExpressionEmitter 对 Ok/Err 表达式的处理逻辑。
 */
class ExpressionEmitterOkErrTest {

  @Test
  void emitOkWithIntLiteral() {
    var fixture = newFixture();

    CoreModel.IntE intExpr = new CoreModel.IntE();
    intExpr.value = 42;

    CoreModel.Ok ok = new CoreModel.Ok();
    ok.expr = intExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(ok, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW aster/runtime/Ok, DUP, BIPUSH 42, Integer.valueOf, INVOKESPECIAL Ok.<init>
    assertEquals(5, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals(Opcodes.NEW, newInsn.getOpcode());
    assertEquals("aster/runtime/Ok", newInsn.desc);

    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[4]);
    assertEquals(Opcodes.INVOKESPECIAL, constructor.getOpcode());
    assertEquals("aster/runtime/Ok", constructor.owner);
    assertEquals("<init>", constructor.name);
    assertEquals("(Ljava/lang/Object;)V", constructor.desc);
  }

  @Test
  void emitOkWithStringLiteral() {
    var fixture = newFixture();

    CoreModel.StringE stringExpr = new CoreModel.StringE();
    stringExpr.value = "success";

    CoreModel.Ok ok = new CoreModel.Ok();
    ok.expr = stringExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(ok, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW, DUP, LDC "success", INVOKESPECIAL
    assertEquals(4, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Ok", newInsn.desc);
  }

  @Test
  void emitErrWithStringLiteral() {
    var fixture = newFixture();

    CoreModel.StringE stringExpr = new CoreModel.StringE();
    stringExpr.value = "error message";

    CoreModel.Err err = new CoreModel.Err();
    err.expr = stringExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(err, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW, DUP, LDC, INVOKESPECIAL
    assertEquals(4, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Err", newInsn.desc);

    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals("aster/runtime/Err", constructor.owner);
    assertEquals("<init>", constructor.name);
    assertEquals("(Ljava/lang/Object;)V", constructor.desc);
  }

  @Test
  void emitErrWithIntLiteral() {
    var fixture = newFixture();

    CoreModel.IntE intExpr = new CoreModel.IntE();
    intExpr.value = 404;

    CoreModel.Err err = new CoreModel.Err();
    err.expr = intExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(err, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW Err, DUP, SIPUSH 404, Integer.valueOf, INVOKESPECIAL
    assertEquals(5, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Err", newInsn.desc);
  }

  @Test
  void emitOkWithNull() {
    var fixture = newFixture();

    CoreModel.NullE nullExpr = new CoreModel.NullE();

    CoreModel.Ok ok = new CoreModel.Ok();
    ok.expr = nullExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(ok, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW, DUP, ACONST_NULL, INVOKESPECIAL
    assertEquals(4, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Ok", newInsn.desc);
  }

  @Test
  void emitNestedOkOk() {
    var fixture = newFixture();

    CoreModel.IntE intExpr = new CoreModel.IntE();
    intExpr.value = 42;

    CoreModel.Ok inner = new CoreModel.Ok();
    inner.expr = intExpr;

    CoreModel.Ok outer = new CoreModel.Ok();
    outer.expr = inner;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(outer, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // Outer: NEW, DUP, (inner: NEW, DUP, BIPUSH 42, Integer.valueOf, INVOKESPECIAL Ok.<init>), INVOKESPECIAL Ok.<init>
    // Total: 8 instructions (inner Ok already returns Object, no boxing needed at outer level)
    assertEquals(8, nodes.length);
    TypeInsnNode outerNew = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Ok", outerNew.desc);

    TypeInsnNode innerNew = assertInstanceOf(TypeInsnNode.class, nodes[2]);
    assertEquals("aster/runtime/Ok", innerNew.desc);
  }

  @Test
  void emitOkWithLongLiteral() {
    var fixture = newFixture();

    CoreModel.LongE longExpr = new CoreModel.LongE();
    longExpr.value = 9999999999L;

    CoreModel.Ok ok = new CoreModel.Ok();
    ok.expr = longExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(ok, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW Ok, DUP, LDC 9999999999L, Long.valueOf, INVOKESPECIAL
    assertEquals(5, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Ok", newInsn.desc);
  }

  @Test
  void emitErrWithDoubleLiteral() {
    var fixture = newFixture();

    CoreModel.DoubleE doubleExpr = new CoreModel.DoubleE();
    doubleExpr.value = 3.14;

    CoreModel.Err err = new CoreModel.Err();
    err.expr = doubleExpr;

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(err, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW Err, DUP, LDC2_W 3.14, Double.valueOf, INVOKESPECIAL
    assertEquals(5, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("aster/runtime/Err", newInsn.desc);
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
