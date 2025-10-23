package aster.emitter;

import aster.core.ir.CoreModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
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
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * 验证 CallEmitter 集成 StdlibInliner 后的内联行为。
 */
class CallEmitterStdlibTest {

  private ContextBuilder contextBuilder;

  @BeforeEach
  void setUp() {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";
    module.decls = List.of();
    contextBuilder = new ContextBuilder(module);
  }

  @Test
  void textConcatInline() throws IOException {
    var helper = newHelper();
    Map<String, Integer> env = new LinkedHashMap<>();
    MethodNode node = emit(
        helper,
        makeBinaryCall("Text.concat", stringLiteral("hello"), stringLiteral("world")),
        env,
        "Ljava/lang/String;"
    );

    assertTrue(
        containsInvoke(node, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;"),
        "Text.concat 应当被内联为 String.concat 调用"
    );
    assertEquals(
        2,
        countInvoke(node, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;"),
        "Text.concat 内联过程中应对两个参数执行 String.valueOf"
    );
  }

  @Test
  void listLengthInline() throws IOException {
    var helper = newHelper();
    Map<String, Integer> env = new LinkedHashMap<>();
    env.put("xs", 0);
    helper.scopeStack.declare("xs", 0, "Ljava/util/List;", ScopeStack.JvmKind.OBJECT);

    MethodNode node = emit(
        helper,
        makeUnaryCall("List.length", nameExpr("xs")),
        env,
        "Ljava/lang/Object;"  // 期望装箱结果
    );

    assertTrue(
        containsInvoke(node, "java/util/List", "size", "()I"),
        "List.length 应映射为 List.size"
    );
    assertTrue(
        containsInvoke(node, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;"),
        "List.length 结果应装箱为 Integer"
    );
  }

  @Test
  void mapGetInline() throws IOException {
    var helper = newHelper();
    Map<String, Integer> env = new LinkedHashMap<>();
    env.put("dict", 0);
    helper.scopeStack.declare("dict", 0, "Ljava/util/Map;", ScopeStack.JvmKind.OBJECT);

    MethodNode node = emit(
        helper,
        makeBinaryCall("Map.get", nameExpr("dict"), stringLiteral("key")),
        env,
        "Ljava/lang/String;"
    );

    assertTrue(
        containsInvoke(node, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
        "Map.get 应映射为 Map.get 调用"
    );
    assertTrue(
        containsCheckCast(node, "java/util/Map"),
        "Map.get 内联前应对第一个参数执行 CHECKCAST"
    );
  }

  @Test
  void textToUpperFallsBack() throws IOException {
    var helper = newHelper();
    Map<String, Integer> env = new LinkedHashMap<>();
    MethodNode node = emit(
        helper,
        makeUnaryCall("Text.toUpper", stringLiteral("lower")),
        env,
        "Ljava/lang/String;"
    );

    assertTrue(
        containsInvoke(node, "java/lang/String", "toUpperCase", "()Ljava/lang/String;"),
        "Text.toUpper 应继续沿用原有路径"
    );
    assertEquals(
        0,
        countInvoke(node, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;"),
        "回退路径不应插入 String.valueOf"
    );
  }

  private HelperContext newHelper() {
    ScopeStack scopeStack = new ScopeStack();
    TypeResolver typeResolver = new TypeResolver(scopeStack, Map.of(), Map.of(), contextBuilder);
    SignatureResolver signatureResolver = new SignatureResolver(false);
    Main.Ctx ctx = new Main.Ctx(
        Path.of("."),
        contextBuilder,
        new AtomicInteger(),
        Map.of(),
        new LinkedHashMap<>(),
        Map.of()
    );
    CallEmitter emitter = new CallEmitter(typeResolver, signatureResolver, ctx, StdlibInliner.instance());
    return new HelperContext(emitter, scopeStack, new StubExprEmitter());
  }

  private MethodNode emit(
      HelperContext helper,
      CoreModel.Call call,
      Map<String, Integer> env,
      String expectedDesc
  ) throws IOException {
    MethodNode mv = new MethodNode();
    boolean handled = helper.emitter.tryEmitCall(
        mv,
        call,
        expectedDesc,
        "app",
        0,
        env,
        helper.scopeStack,
        helper.stub
    );
    assertTrue(handled, "调用应由 CallEmitter 处理");
    return mv;
  }

  private static boolean containsInvoke(MethodNode node, String owner, String name, String desc) {
    for (AbstractInsnNode insn : node.instructions.toArray()) {
      if (insn instanceof MethodInsnNode mi) {
        if (owner.equals(mi.owner) && name.equals(mi.name) && desc.equals(mi.desc)) {
          return true;
        }
      }
    }
    return false;
  }

  private static int countInvoke(MethodNode node, String owner, String name, String desc) {
    int count = 0;
    for (AbstractInsnNode insn : node.instructions.toArray()) {
      if (insn instanceof MethodInsnNode mi) {
        if (owner.equals(mi.owner) && name.equals(mi.name) && desc.equals(mi.desc)) {
          count++;
        }
      }
    }
    return count;
  }

  private static boolean containsCheckCast(MethodNode node, String internalName) {
    for (AbstractInsnNode insn : node.instructions.toArray()) {
      if (insn instanceof TypeInsnNode cast && cast.getOpcode() == Opcodes.CHECKCAST) {
        if (internalName.equals(cast.desc)) {
          return true;
        }
      }
    }
    return false;
  }

  private CoreModel.Call makeUnaryCall(String fn, CoreModel.Expr arg) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(fn);
    call.args = List.of(arg);
    return call;
  }

  private CoreModel.Call makeBinaryCall(String fn, CoreModel.Expr arg0, CoreModel.Expr arg1) {
    CoreModel.Call call = new CoreModel.Call();
    call.target = nameExpr(fn);
    call.args = List.of(arg0, arg1);
    return call;
  }

  private CoreModel.Name nameExpr(String value) {
    CoreModel.Name name = new CoreModel.Name();
    name.name = value;
    return name;
  }

  private CoreModel.StringE stringLiteral(String value) {
    CoreModel.StringE literal = new CoreModel.StringE();
    literal.value = value;
    return literal;
  }

  private record HelperContext(CallEmitter emitter, ScopeStack scopeStack, StubExprEmitter stub) {}

  private static final class StubExprEmitter implements ExprEmitterCallback {
    @Override
    public void emitExpr(
        org.objectweb.asm.MethodVisitor mv,
        CoreModel.Expr expr,
        String expectedDesc,
        String currentPkg,
        int paramBase,
        Map<String, Integer> env,
        ScopeStack scopeStack
    ) {
      if (expr instanceof CoreModel.StringE se) {
        mv.visitLdcInsn(se.value);
        return;
      }
      if (expr instanceof CoreModel.IntE intE) {
        mv.visitLdcInsn(Integer.valueOf(intE.value));
        return;
      }
      if (expr instanceof CoreModel.Name name && env != null && env.containsKey(name.name)) {
        mv.visitVarInsn(Opcodes.ALOAD, env.get(name.name));
        return;
      }
      mv.visitInsn(Opcodes.ACONST_NULL);
    }
  }
}
