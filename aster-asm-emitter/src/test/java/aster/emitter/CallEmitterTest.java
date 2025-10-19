package aster.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 验证 CallEmitter 针对各类调用场景的字节码输出。
 */
class CallEmitterTest {

  private ContextBuilder contextBuilder;

  @BeforeEach
  void setUp() {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    contextBuilder = new ContextBuilder(module);
  }

  @Test
  void testBuiltinOperators() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;

    MethodNode plusNode = emit(helper, makeBinaryCall("+", intLiteral(1), intLiteral(2)), stub, null);
    AbstractInsnNode[] plusInsns = plusNode.instructions.toArray();
    assertEquals(Opcodes.IADD, plusInsns[plusInsns.length - 1].getOpcode());

    MethodNode minusNode = emit(helper, makeBinaryCall("-", intLiteral(3), intLiteral(1)), stub, null);
    AbstractInsnNode[] minusInsns = minusNode.instructions.toArray();
    assertEquals(Opcodes.ISUB, minusInsns[minusInsns.length - 1].getOpcode());

    MethodNode mulNode = emit(helper, makeBinaryCall("*", intLiteral(2), intLiteral(4)), stub, null);
    AbstractInsnNode[] mulInsns = mulNode.instructions.toArray();
    assertEquals(Opcodes.IMUL, mulInsns[mulInsns.length - 1].getOpcode());

    MethodNode divNode = emit(helper, makeBinaryCall("/", intLiteral(8), intLiteral(2)), stub, null);
    AbstractInsnNode[] divInsns = divNode.instructions.toArray();
    assertEquals(Opcodes.IDIV, divInsns[divInsns.length - 1].getOpcode());

    MethodNode ltNode = emit(helper, makeBinaryCall("<", intLiteral(1), intLiteral(2)), stub, null);
    assertContainsJump(ltNode, Opcodes.IF_ICMPLT);

    MethodNode gtNode = emit(helper, makeBinaryCall(">", intLiteral(2), intLiteral(1)), stub, null);
    assertContainsJump(gtNode, Opcodes.IF_ICMPGT);
  }

  @Test
  void testTextInterop() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;
    MethodNode node = emit(helper, makeBinaryCall("Text.concat", stringLiteral("a"), stringLiteral("b")), stub, "Ljava/lang/String;");
    MethodInsnNode call = findLastMethodInsn(node);
    assertEquals("java/lang/String", call.owner);
    assertEquals("concat", call.name);
    assertEquals("(Ljava/lang/String;)Ljava/lang/String;", call.desc);
  }

  @Test
  void testListMapInterop() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;

    MethodNode listLength = emit(helper, makeUnaryCall("List.length", new CoreModel.NullE()), stub, null);
    MethodInsnNode listCall = findLastMethodInsn(listLength);
    assertEquals("java/util/List", listCall.owner);
    assertEquals("size", listCall.name);
    assertEquals("()I", listCall.desc);

    MethodNode mapGet = emit(helper, makeBinaryCall("Map.get", new CoreModel.NullE(), stringLiteral("key")), stub, "Ljava/lang/String;");
    MethodInsnNode mapCall = findLastMethodInsn(mapGet);
    assertEquals("java/util/Map", mapCall.owner);
    assertEquals("get", mapCall.name);
    assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", mapCall.desc);
  }

  @Test
  void testStaticMethodCall() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;
    CoreModel.Call call = makeBinaryCall("java.lang.Math.max", intLiteral(3), intLiteral(5));
    MethodNode node = emit(helper, call, stub, "I");
    MethodInsnNode invoke = findLastMethodInsn(node);
    assertEquals("java/lang/Math", invoke.owner);
    assertEquals("max", invoke.name);
    assertEquals("(II)I", invoke.desc);
  }

  @Test
  void testUserDefinedFunction() throws IOException {
    CoreModel.Func func = new CoreModel.Func();
    func.name = "helper";
    CoreModel.Param param = new CoreModel.Param();
    param.name = "x";
    CoreModel.TypeName intType = new CoreModel.TypeName();
    intType.name = "Int";
    param.type = intType;
    func.params = List.of(param);
    CoreModel.TypeName ret = new CoreModel.TypeName();
    ret.name = "Int";
    func.ret = ret;

    Map<String, CoreModel.Func> schemas = Map.of("helper", func);
    var helper = newHelper(schemas);
    StubExprEmitter stub = helper.stub;

    CoreModel.Call call = makeUnaryCall("helper", intLiteral(7));
    MethodNode node = emit(helper, call, stub, "I");
    MethodInsnNode invoke = findLastMethodInsn(node);
    assertEquals("app/helper_fn", invoke.owner);
    assertEquals("helper", invoke.name);
    assertEquals("(I)I", invoke.desc);
  }

  @Test
  void testClosureCall() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;
    CoreModel.Call call = makeUnaryCallWithTarget(nameExpr("closureVar"), stringLiteral("value"));
    MethodNode node = emit(helper, call, stub, "Ljava/lang/String;");
    MethodInsnNode invoke = findLastMethodInsn(node);
    assertEquals("aster/runtime/Fn1", invoke.owner);
    assertEquals("apply", invoke.name);
    assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", invoke.desc);
  }

  @Test
  void testTextToUpperAndToLower() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;

    MethodNode upperNode = emit(helper, makeUnaryCall("Text.toUpper", stringLiteral("hello")), stub, "Ljava/lang/String;");
    MethodInsnNode upperCall = findLastMethodInsn(upperNode);
    assertEquals("java/lang/String", upperCall.owner);
    assertEquals("toUpperCase", upperCall.name);
    assertEquals("()Ljava/lang/String;", upperCall.desc);

    MethodNode lowerNode = emit(helper, makeUnaryCall("Text.toLower", stringLiteral("WORLD")), stub, "Ljava/lang/String;");
    MethodInsnNode lowerCall = findLastMethodInsn(lowerNode);
    assertEquals("java/lang/String", lowerCall.owner);
    assertEquals("toLowerCase", lowerCall.name);
    assertEquals("()Ljava/lang/String;", lowerCall.desc);
  }

  @Test
  void testListIsEmpty() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;

    MethodNode node = emit(helper, makeUnaryCall("List.isEmpty", new CoreModel.NullE()), stub, null);
    MethodInsnNode call = findLastMethodInsn(node);
    assertEquals("java/util/List", call.owner);
    assertEquals("isEmpty", call.name);
    assertEquals("()Z", call.desc);
  }

  @Test
  void testMultiArityClosures() throws IOException {
    var helper = newHelper(Map.of());
    StubExprEmitter stub = helper.stub;

    // Fn0 (zero arguments)
    CoreModel.Call fn0Call = new CoreModel.Call();
    fn0Call.target = nameExpr("fn0Var");
    fn0Call.args = List.of();
    MethodNode fn0Node = emit(helper, fn0Call, stub, null);
    MethodInsnNode fn0Invoke = findLastMethodInsn(fn0Node);
    assertEquals("aster/runtime/Fn0", fn0Invoke.owner);
    assertEquals("apply", fn0Invoke.name);
    assertEquals("()Ljava/lang/Object;", fn0Invoke.desc);

    // Fn2 (two arguments)
    CoreModel.Call fn2Call = new CoreModel.Call();
    fn2Call.target = nameExpr("fn2Var");
    fn2Call.args = List.of(stringLiteral("arg1"), stringLiteral("arg2"));
    MethodNode fn2Node = emit(helper, fn2Call, stub, null);
    MethodInsnNode fn2Invoke = findLastMethodInsn(fn2Node);
    assertEquals("aster/runtime/Fn2", fn2Invoke.owner);
    assertEquals("apply", fn2Invoke.name);
    assertEquals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", fn2Invoke.desc);

    // Fn3 (three arguments)
    CoreModel.Call fn3Call = new CoreModel.Call();
    fn3Call.target = nameExpr("fn3Var");
    fn3Call.args = List.of(stringLiteral("arg1"), stringLiteral("arg2"), stringLiteral("arg3"));
    MethodNode fn3Node = emit(helper, fn3Call, stub, null);
    MethodInsnNode fn3Invoke = findLastMethodInsn(fn3Node);
    assertEquals("aster/runtime/Fn3", fn3Invoke.owner);
    assertEquals("apply", fn3Invoke.name);
    assertEquals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", fn3Invoke.desc);
  }

  private HelperContext newHelper(Map<String, CoreModel.Func> schemas) {
    ScopeStack scopeStack = new ScopeStack();
    TypeResolver typeResolver = new TypeResolver(scopeStack, Map.<String, Character>of(), schemas, contextBuilder);
    SignatureResolver signatureResolver = new SignatureResolver(false);
    Main.Ctx ctx = new Main.Ctx(
        Path.of("."),
        contextBuilder,
        new AtomicInteger(),
        Map.<String, Map<String, Character>>of(),
        new LinkedHashMap<>(),
        schemas
    );
    CallEmitter emitter = new CallEmitter(typeResolver, signatureResolver, ctx, StdlibInliner.instance());
    StubExprEmitter stub = new StubExprEmitter();
    return new HelperContext(emitter, scopeStack, stub);
  }

  private MethodNode emit(HelperContext helper, CoreModel.Call call, StubExprEmitter stub, String expectedDesc) throws IOException {
    MethodNode mv = new MethodNode();
    boolean handled = helper.emitter.tryEmitCall(
        mv,
        call,
        expectedDesc,
        "app",
        0,
        new LinkedHashMap<>(),
        helper.scopeStack,
        stub
    );
    assertTrue(handled);
    return mv;
  }

  private static void assertContainsJump(MethodNode node, int opcode) {
    for (AbstractInsnNode insn : node.instructions.toArray()) {
      if (insn instanceof JumpInsnNode jump && jump.getOpcode() == opcode) {
        return;
      }
    }
    throw new AssertionError("缺少跳转指令: " + opcode);
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

  private CoreModel.Call makeBinaryCall(String name, CoreModel.Expr left, CoreModel.Expr right) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(name);
    call.args = List.of(left, right);
    return call;
  }

  private CoreModel.Call makeUnaryCall(String name, CoreModel.Expr arg) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(name);
    call.args = List.of(arg);
    return call;
  }

  private CoreModel.Call makeUnaryCallWithTarget(CoreModel.Expr target, CoreModel.Expr arg) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(arg);
    return call;
  }

  private CoreModel.IntE intLiteral(int v) {
    CoreModel.IntE e = new CoreModel.IntE();
    e.value = v;
    return e;
  }

  private CoreModel.StringE stringLiteral(String v) {
    CoreModel.StringE e = new CoreModel.StringE();
    e.value = v;
    return e;
  }

  private CoreModel.Name nameExpr(String v) {
    CoreModel.Name e = new CoreModel.Name();
    e.name = v;
    return e;
  }

  private record HelperContext(CallEmitter emitter, ScopeStack scopeStack, StubExprEmitter stub) {}

  private static final class StubExprEmitter implements ExprEmitterCallback {
    final List<String> descriptors = new ArrayList<>();

    @Override
    public void emitExpr(
        MethodVisitor mv,
        CoreModel.Expr expr,
        String expectedDesc,
        String currentPkg,
        int paramBase,
        Map<String, Integer> env,
        ScopeStack scopeStack
    ) {
      descriptors.add(expectedDesc);
      if ("I".equals(expectedDesc)) {
        mv.visitInsn(Opcodes.ICONST_0);
        return;
      }
      if ("Z".equals(expectedDesc)) {
        mv.visitInsn(Opcodes.ICONST_0);
        return;
      }
      if ("J".equals(expectedDesc)) {
        mv.visitInsn(Opcodes.LCONST_0);
        return;
      }
      if ("D".equals(expectedDesc)) {
        mv.visitInsn(Opcodes.DCONST_0);
        return;
      }
      if ("Ljava/lang/String;".equals(expectedDesc) && expr instanceof CoreModel.StringE se) {
        mv.visitLdcInsn(se.value);
        return;
      }
      mv.visitInsn(Opcodes.ACONST_NULL);
    }
  }
}
