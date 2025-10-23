package aster.emitter;

import aster.core.ir.CoreModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 验证 ExpressionEmitter 与 CallEmitter 集成后的调用表达式编译行为。
 */
class ExpressionEmitterCallTest {

  @Test
  void testArithmeticIntrinsicCall() {
    var fixture = newFixture(Map.of());
    MethodNode mv = new MethodNode();
    CoreModel.Call call = makeBinaryCall("+", intLiteral(1), intLiteral(2));

    fixture.emitter.updateEnvironment(fixture.env);
    fixture.emitter.emitExpression(call, mv, fixture.scope, null);

    boolean hasIadd = false;
    for (AbstractInsnNode insn : mv.instructions.toArray()) {
      if (insn.getOpcode() == Opcodes.IADD) {
        hasIadd = true;
        break;
      }
    }
    assertTrue(hasIadd, "应当出现 IADD 指令");
  }

  @Test
  void testStdlibInlineCall() {
    var fixture = newFixture(Map.of());
    MethodNode mv = new MethodNode();
    CoreModel.Call call = makeBinaryCall("Text.concat", stringLiteral("a"), stringLiteral("b"));

    fixture.emitter.updateEnvironment(fixture.env);
    fixture.emitter.emitExpression(call, mv, fixture.scope, "Ljava/lang/String;");

    MethodInsnNode invoke = findLastMethodInsn(mv);
    assertEquals("java/lang/String", invoke.owner);
    assertEquals("concat", invoke.name);
    assertEquals("(Ljava/lang/String;)Ljava/lang/String;", invoke.desc);
  }

  @Test
  void testJvmStaticMethodCall() {
    var fixture = newFixture(Map.of());
    MethodNode mv = new MethodNode();
    CoreModel.Call call = makeBinaryCall("java.lang.Math.max", intLiteral(3), intLiteral(5));

    fixture.emitter.updateEnvironment(fixture.env);
    fixture.emitter.emitExpression(call, mv, fixture.scope, "I");

    MethodInsnNode invoke = findLastMethodInsn(mv);
    assertEquals("java/lang/Math", invoke.owner);
    assertEquals("max", invoke.name);
    assertEquals("(II)I", invoke.desc);
  }

  @Test
  void testGlobalFunctionCall() {
    CoreModel.Func helperFn = new CoreModel.Func();
    helperFn.name = "helper";
    CoreModel.Param param = new CoreModel.Param();
    param.name = "x";
    CoreModel.TypeName intType = new CoreModel.TypeName();
    intType.name = "Int";
    param.type = intType;
    helperFn.params = List.of(param);
    CoreModel.TypeName ret = new CoreModel.TypeName();
    ret.name = "Int";
    helperFn.ret = ret;

    var fixture = newFixture(Map.of("helper", helperFn));
    MethodNode mv = new MethodNode();
    CoreModel.Call call = makeUnaryCall("helper", intLiteral(7));

    fixture.emitter.updateEnvironment(fixture.env);
    fixture.emitter.emitExpression(call, mv, fixture.scope, "I");

    MethodInsnNode invoke = findLastMethodInsn(mv);
    assertEquals("app/helper_fn", invoke.owner);
    assertEquals("helper", invoke.name);
    assertEquals("(I)I", invoke.desc);
  }

  @Test
  void testFallbackWhenCallNotHandled() {
    var module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    var context = new ContextBuilder(module);
    var scope = new ScopeStack();
    var typeResolver = new TypeResolver(scope, Map.of(), Map.of(), context);
    var nameEmitter = new NameEmitter(typeResolver, null);
    var rejectingEmitter = new RejectingCallEmitter(typeResolver);
    var expressionEmitter = new ExpressionEmitter(context, typeResolver, scope, new LinkedHashMap<>(), nameEmitter, rejectingEmitter);

    CoreModel.Call call = new CoreModel.Call();
    call.target = stringLiteral("fallback");
    call.args = List.of();

    MethodNode mv = new MethodNode();
    expressionEmitter.emitExpression(call, mv, scope, null);

    AbstractInsnNode[] insns = mv.instructions.toArray();
    assertEquals(1, insns.length);
    LdcInsnNode ldc = assertInstanceOf(LdcInsnNode.class, insns[0]);
    assertEquals("fallback", ldc.cst);
  }

  private static MethodInsnNode findLastMethodInsn(MethodNode node) {
    AbstractInsnNode[] insns = node.instructions.toArray();
    for (int i = insns.length - 1; i >= 0; i--) {
      if (insns[i] instanceof MethodInsnNode mi) {
        return mi;
      }
    }
    throw new AssertionError("未找到方法调用指令");
  }

  private static CoreModel.Call makeBinaryCall(String name, CoreModel.Expr left, CoreModel.Expr right) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(name);
    call.args = List.of(left, right);
    return call;
  }

  private static CoreModel.Call makeUnaryCall(String name, CoreModel.Expr arg) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(name);
    call.args = List.of(arg);
    return call;
  }

  private static CoreModel.IntE intLiteral(int value) {
    CoreModel.IntE expr = new CoreModel.IntE();
    expr.value = value;
    return expr;
  }

  private static CoreModel.StringE stringLiteral(String value) {
    CoreModel.StringE expr = new CoreModel.StringE();
    expr.value = value;
    return expr;
  }

  private static CoreModel.Name nameExpr(String name) {
    CoreModel.Name expr = new CoreModel.Name();
    expr.name = name;
    return expr;
  }

  private Fixture newFixture(Map<String, CoreModel.Func> functionSchemas) {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    ContextBuilder context = new ContextBuilder(module);
    ScopeStack scope = new ScopeStack();
    Map<String, Map<String, Character>> hints = Map.of();
    TypeResolver typeResolver = new TypeResolver(scope, Map.of(), functionSchemas, context);
    Map<String, String> stringPool = new LinkedHashMap<>();
    Main.Ctx ctx = new Main.Ctx(
        Path.of("build/test-classes"),
        context,
        new AtomicInteger(0),
        hints,
        stringPool,
        functionSchemas
    );
    NameEmitter nameEmitter = new NameEmitter(typeResolver, ctx);
    CallEmitter callEmitter = new CallEmitter(typeResolver, new SignatureResolver(false), ctx, StdlibInliner.instance());
    Map<String, Integer> env = new LinkedHashMap<>();
    ExpressionEmitter emitter = new ExpressionEmitter(ctx, "app", 0, env, scope, typeResolver, nameEmitter, callEmitter);
    return new Fixture(emitter, scope, env);
  }

  private record Fixture(ExpressionEmitter emitter, ScopeStack scope, Map<String, Integer> env) {}

  private static final class RejectingCallEmitter extends CallEmitter {
    RejectingCallEmitter(TypeResolver typeResolver) {
      super(typeResolver, new SignatureResolver(false), null, StdlibInliner.instance());
    }

    @Override
    public boolean tryEmitCall(
        MethodVisitor mv,
        CoreModel.Call call,
        String expectedDesc,
        String currentPkg,
        int paramBase,
        Map<String, Integer> env,
        ScopeStack scopeStack,
        ExprEmitterCallback exprEmitter
    ) throws IOException {
      return false;
    }
  }
}
