package aster.emitter;

import aster.core.ir.CoreModel;

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
 * 验证 ExpressionEmitter 对 Construct 表达式的处理逻辑。
 */
class ExpressionEmitterConstructTest {

  @Test
  void emitConstructWithIntField() {
    var fixture = newFixture();

    CoreModel.IntE intExpr = new CoreModel.IntE();
    intExpr.value = 42;

    CoreModel.FieldInit field = new CoreModel.FieldInit();
    field.name = "age";
    field.expr = intExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(field);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, BIPUSH 42, INVOKESPECIAL Person.<init>(I)V
    assertEquals(4, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals(Opcodes.NEW, newInsn.getOpcode());
    assertEquals("app/Person", newInsn.desc);

    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals(Opcodes.INVOKESPECIAL, constructor.getOpcode());
    assertEquals("app/Person", constructor.owner);
    assertEquals("<init>", constructor.name);
    assertEquals("(I)V", constructor.desc);
  }

  @Test
  void emitConstructWithStringField() {
    var fixture = newFixture();

    CoreModel.StringE stringExpr = new CoreModel.StringE();
    stringExpr.value = "John";

    CoreModel.FieldInit field = new CoreModel.FieldInit();
    field.name = "name";
    field.expr = stringExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(field);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, LDC "John", INVOKESPECIAL Person.<init>(Ljava/lang/String;)V
    assertEquals(4, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("app/Person", newInsn.desc);

    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals("(Ljava/lang/String;)V", constructor.desc);
  }

  @Test
  void emitConstructWithMixedFields() {
    var fixture = newFixture();

    CoreModel.IntE ageExpr = new CoreModel.IntE();
    ageExpr.value = 30;

    CoreModel.StringE nameExpr = new CoreModel.StringE();
    nameExpr.value = "Alice";

    CoreModel.FieldInit ageField = new CoreModel.FieldInit();
    ageField.name = "age";
    ageField.expr = ageExpr;

    CoreModel.FieldInit nameField = new CoreModel.FieldInit();
    nameField.name = "name";
    nameField.expr = nameExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(ageField, nameField);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, BIPUSH 30, LDC "Alice", INVOKESPECIAL Person.<init>(ILjava/lang/String;)V
    assertEquals(5, nodes.length);
    TypeInsnNode newInsn = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("app/Person", newInsn.desc);

    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[4]);
    assertEquals("(ILjava/lang/String;)V", constructor.desc);
  }

  @Test
  void emitConstructWithBoolField() {
    var fixture = newFixture();

    CoreModel.Bool boolExpr = new CoreModel.Bool();
    boolExpr.value = true;

    CoreModel.FieldInit field = new CoreModel.FieldInit();
    field.name = "active";
    field.expr = boolExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(field);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, ICONST_1, INVOKESPECIAL Person.<init>(Z)V
    assertEquals(4, nodes.length);
    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals("(Z)V", constructor.desc);
  }

  @Test
  void emitConstructWithLongField() {
    var fixture = newFixture();

    CoreModel.LongE longExpr = new CoreModel.LongE();
    longExpr.value = 9999999999L;

    CoreModel.FieldInit field = new CoreModel.FieldInit();
    field.name = "timestamp";
    field.expr = longExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(field);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, LDC 9999999999L, INVOKESPECIAL Person.<init>(J)V
    assertEquals(4, nodes.length);
    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals("(J)V", constructor.desc);
  }

  @Test
  void emitConstructWithDoubleField() {
    var fixture = newFixture();

    CoreModel.DoubleE doubleExpr = new CoreModel.DoubleE();
    doubleExpr.value = 3.14;

    CoreModel.FieldInit field = new CoreModel.FieldInit();
    field.name = "score";
    field.expr = doubleExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(field);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, LDC2_W 3.14, INVOKESPECIAL Person.<init>(D)V
    assertEquals(4, nodes.length);
    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[3]);
    assertEquals("(D)V", constructor.desc);
  }

  @Test
  void emitConstructWithAllPrimitiveFields() {
    var fixture = newFixture();

    CoreModel.IntE intExpr = new CoreModel.IntE();
    intExpr.value = 42;

    CoreModel.Bool boolExpr = new CoreModel.Bool();
    boolExpr.value = true;

    CoreModel.LongE longExpr = new CoreModel.LongE();
    longExpr.value = 123456L;

    CoreModel.DoubleE doubleExpr = new CoreModel.DoubleE();
    doubleExpr.value = 2.5;

    CoreModel.FieldInit ageField = new CoreModel.FieldInit();
    ageField.name = "age";
    ageField.expr = intExpr;

    CoreModel.FieldInit activeField = new CoreModel.FieldInit();
    activeField.name = "active";
    activeField.expr = boolExpr;

    CoreModel.FieldInit timestampField = new CoreModel.FieldInit();
    timestampField.name = "timestamp";
    timestampField.expr = longExpr;

    CoreModel.FieldInit scoreField = new CoreModel.FieldInit();
    scoreField.name = "score";
    scoreField.expr = doubleExpr;

    CoreModel.Construct construct = new CoreModel.Construct();
    construct.typeName = "Person";
    construct.fields = List.of(ageField, activeField, timestampField, scoreField);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(construct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, BIPUSH 42, ICONST_1, LDC 123456L, LDC2_W 2.5, INVOKESPECIAL Person.<init>(IZJD)V
    assertEquals(7, nodes.length);
    MethodInsnNode constructor = assertInstanceOf(MethodInsnNode.class, nodes[6]);
    assertEquals("(IZJD)V", constructor.desc);
  }

  @Test
  void emitConstructWithNestedConstruct() {
    var fixture = newFixture();

    // Inner construct: Address(street="Main St")
    CoreModel.StringE streetExpr = new CoreModel.StringE();
    streetExpr.value = "Main St";

    CoreModel.FieldInit streetField = new CoreModel.FieldInit();
    streetField.name = "street";
    streetField.expr = streetExpr;

    CoreModel.Construct addressConstruct = new CoreModel.Construct();
    addressConstruct.typeName = "Address";
    addressConstruct.fields = List.of(streetField);

    // Outer construct: Person(name="Bob", address=Address(...))
    CoreModel.StringE nameExpr = new CoreModel.StringE();
    nameExpr.value = "Bob";

    CoreModel.FieldInit nameField = new CoreModel.FieldInit();
    nameField.name = "name";
    nameField.expr = nameExpr;

    CoreModel.FieldInit addressField = new CoreModel.FieldInit();
    addressField.name = "address";
    addressField.expr = addressConstruct;

    CoreModel.Construct personConstruct = new CoreModel.Construct();
    personConstruct.typeName = "Person";
    personConstruct.fields = List.of(nameField, addressField);

    MethodNode mv = new MethodNode();
    fixture.emitter.emitExpression(personConstruct, mv, fixture.scope, "Ljava/lang/Object;");

    AbstractInsnNode[] nodes = mv.instructions.toArray();
    // NEW app/Person, DUP, LDC "Bob", (NEW app/Address, DUP, LDC "Main St", INVOKESPECIAL Address.<init>(Ljava/lang/String;)V), INVOKESPECIAL Person.<init>(Ljava/lang/String;Ljava/lang/Object;)V
    // Total: 8 instructions
    assertEquals(8, nodes.length);
    TypeInsnNode outerNew = assertInstanceOf(TypeInsnNode.class, nodes[0]);
    assertEquals("app/Person", outerNew.desc);

    TypeInsnNode innerNew = assertInstanceOf(TypeInsnNode.class, nodes[3]);
    assertEquals("app/Address", innerNew.desc);
  }

  private Fixture newFixture() {
    CoreModel.Module module = new CoreModel.Module();
    module.name = "app";

    // Define Person schema with various field types
    CoreModel.Data personData = new CoreModel.Data();
    personData.name = "Person";

    CoreModel.Field ageField = new CoreModel.Field();
    ageField.name = "age";
    CoreModel.TypeName intType = new CoreModel.TypeName();
    intType.name = "Int";
    ageField.type = intType;

    CoreModel.Field nameField = new CoreModel.Field();
    nameField.name = "name";
    CoreModel.TypeName textType = new CoreModel.TypeName();
    textType.name = "Text";
    nameField.type = textType;

    CoreModel.Field activeField = new CoreModel.Field();
    activeField.name = "active";
    CoreModel.TypeName boolType = new CoreModel.TypeName();
    boolType.name = "Bool";
    activeField.type = boolType;

    CoreModel.Field timestampField = new CoreModel.Field();
    timestampField.name = "timestamp";
    CoreModel.TypeName longType = new CoreModel.TypeName();
    longType.name = "Long";
    timestampField.type = longType;

    CoreModel.Field scoreField = new CoreModel.Field();
    scoreField.name = "score";
    CoreModel.TypeName doubleType = new CoreModel.TypeName();
    doubleType.name = "Double";
    scoreField.type = doubleType;

    CoreModel.Field addressField = new CoreModel.Field();
    addressField.name = "address";
    CoreModel.TypeName addressType = new CoreModel.TypeName();
    addressType.name = "Address";
    addressField.type = addressType;

    personData.fields = List.of(ageField, nameField, activeField, timestampField, scoreField, addressField);

    // Define Address schema
    CoreModel.Data addressData = new CoreModel.Data();
    addressData.name = "Address";

    CoreModel.Field streetField = new CoreModel.Field();
    streetField.name = "street";
    CoreModel.TypeName streetType = new CoreModel.TypeName();
    streetType.name = "Text";
    streetField.type = streetType;

    addressData.fields = List.of(streetField);

    module.decls = List.of(personData, addressData);

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

    private Fixture(ScopeStack scope, ExpressionEmitter emitter) {
      this.scope = scope;
      this.emitter = emitter;
    }
  }
}
