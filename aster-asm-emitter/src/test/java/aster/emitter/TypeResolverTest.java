package aster.emitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试TypeResolver的类型推断功能
 */
class TypeResolverTest {

  private ScopeStack scopeStack;
  private TypeResolver resolver;

  @BeforeEach
  void setUp() {
    scopeStack = new ScopeStack();
    resolver = new TypeResolver(
        scopeStack,
        Map.of(),  // funcHints
        Map.of(),  // functionSchemas
        Map.of()   // dataSchema
    );
  }

  @Test
  void testInferNull() {
    assertNull(resolver.inferType(null));
  }

  @Test
  void testInferBool() {
    var expr = new CoreModel.Bool();
    expr.value = true;
    assertEquals('Z', resolver.inferType(expr));
  }

  @Test
  void testInferIntE() {
    var expr = new CoreModel.IntE();
    expr.value = 42;
    assertEquals('I', resolver.inferType(expr));
  }

  @Test
  void testInferLongE() {
    var expr = new CoreModel.LongE();
    expr.value = 123456789L;
    assertEquals('J', resolver.inferType(expr));
  }

  @Test
  void testInferDoubleE() {
    var expr = new CoreModel.DoubleE();
    expr.value = 3.14;
    assertEquals('D', resolver.inferType(expr));
  }

  @Test
  void testInferNameFromScopeStack() {
    // 在作用域中声明变量
    scopeStack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    var expr = new CoreModel.Name();
    expr.name = "x";

    assertEquals('I', resolver.inferType(expr));
  }

  @Test
  void testInferNameNotInScope() {
    var expr = new CoreModel.Name();
    expr.name = "unknown";

    assertNull(resolver.inferType(expr));
  }

  @Test
  void testInferOk() {
    var inner = new CoreModel.IntE();
    inner.value = 100;

    var ok = new CoreModel.Ok();
    ok.expr = inner;

    assertEquals('I', resolver.inferType(ok));
  }

  @Test
  void testInferErr() {
    var inner = new CoreModel.LongE();
    inner.value = 200L;

    var err = new CoreModel.Err();
    err.expr = inner;

    assertEquals('J', resolver.inferType(err));
  }

  @Test
  void testInferSome() {
    var inner = new CoreModel.DoubleE();
    inner.value = 2.718;

    var some = new CoreModel.Some();
    some.expr = inner;

    assertEquals('D', resolver.inferType(some));
  }

  @Test
  void testInferCallAddIntegers() {
    var left = new CoreModel.IntE();
    left.value = 10;

    var right = new CoreModel.IntE();
    right.value = 20;

    var target = new CoreModel.Name();
    target.name = "+";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    assertEquals('I', resolver.inferType(call));
  }

  @Test
  void testInferCallAddIntAndLong() {
    var left = new CoreModel.IntE();
    left.value = 10;

    var right = new CoreModel.LongE();
    right.value = 20L;

    var target = new CoreModel.Name();
    target.name = "+";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    // int + long => long
    assertEquals('J', resolver.inferType(call));
  }

  @Test
  void testInferCallAddIntAndDouble() {
    var left = new CoreModel.IntE();
    left.value = 10;

    var right = new CoreModel.DoubleE();
    right.value = 3.14;

    var target = new CoreModel.Name();
    target.name = "+";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    // int + double => double
    assertEquals('D', resolver.inferType(call));
  }

  @Test
  void testInferCallMultiply() {
    var left = new CoreModel.IntE();
    left.value = 5;

    var right = new CoreModel.IntE();
    right.value = 3;

    var target = new CoreModel.Name();
    target.name = "*";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    assertEquals('I', resolver.inferType(call));
  }

  @Test
  void testInferCallDivide() {
    var left = new CoreModel.IntE();
    left.value = 10;

    var right = new CoreModel.IntE();
    right.value = 2;

    var target = new CoreModel.Name();
    target.name = "/";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    assertEquals('I', resolver.inferType(call));
  }

  @Test
  void testInferCallComparison() {
    var left = new CoreModel.IntE();
    left.value = 10;

    var right = new CoreModel.IntE();
    right.value = 20;

    var target = new CoreModel.Name();
    target.name = "<";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    // 比较运算符返回boolean
    assertEquals('Z', resolver.inferType(call));
  }

  @Test
  void testInferCallEquals() {
    var left = new CoreModel.IntE();
    left.value = 42;

    var right = new CoreModel.IntE();
    right.value = 42;

    var target = new CoreModel.Name();
    target.name = "equals";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    assertEquals('Z', resolver.inferType(call));
  }

  @Test
  void testInferCallNot() {
    var arg = new CoreModel.Bool();
    arg.value = true;

    var target = new CoreModel.Name();
    target.name = "not";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(arg);

    assertEquals('Z', resolver.inferType(call));
  }

  @Test
  void testInferCallNegate() {
    var arg = new CoreModel.IntE();
    arg.value = 42;

    var target = new CoreModel.Name();
    target.name = "negate";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(arg);

    // negate preserves type
    assertEquals('I', resolver.inferType(call));
  }

  @Test
  void testInferCallMinusLong() {
    var arg = new CoreModel.LongE();
    arg.value = 100L;

    var target = new CoreModel.Name();
    target.name = "minus";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(arg);

    assertEquals('J', resolver.inferType(call));
  }

  @Test
  void testInferCallWithFuncHints() {
    var target = new CoreModel.Name();
    target.name = "customFunc";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    // 无hints时返回null
    assertNull(resolver.inferType(call));

    // 使用hints创建新resolver
    var resolverWithHints = new TypeResolver(
        scopeStack,
        Map.of("customFunc", 'J'),  // customFunc返回long
        Map.of(),
        Map.of()
    );

    assertEquals('J', resolverWithHints.inferType(call));
  }

  @Test
  void testInferCallWithFunctionSchema() {
    var func = new CoreModel.Func();
    func.name = "getAge";
    var retType = new CoreModel.TypeName();
    retType.name = "Int";
    func.ret = retType;

    var target = new CoreModel.Name();
    target.name = "getAge";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    var resolverWithSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of("getAge", func),  // functionSchemas
        Map.of()
    );

    assertEquals('I', resolverWithSchema.inferType(call));
  }

  @Test
  void testInferCallWithFunctionSchemaReturningBool() {
    var func = new CoreModel.Func();
    func.name = "isValid";
    var retType = new CoreModel.TypeName();
    retType.name = "Bool";
    func.ret = retType;

    var target = new CoreModel.Name();
    target.name = "isValid";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    var resolverWithSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of("isValid", func),
        Map.of()
    );

    assertEquals('Z', resolverWithSchema.inferType(call));
  }

  @Test
  void testInferCallWithFunctionSchemaReturningLong() {
    var func = new CoreModel.Func();
    func.name = "getTimestamp";
    var retType = new CoreModel.TypeName();
    retType.name = "Long";
    func.ret = retType;

    var target = new CoreModel.Name();
    target.name = "getTimestamp";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    var resolverWithSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of("getTimestamp", func),
        Map.of()
    );

    assertEquals('J', resolverWithSchema.inferType(call));
  }

  @Test
  void testInferCallWithFunctionSchemaReturningDouble() {
    var func = new CoreModel.Func();
    func.name = "getRate";
    var retType = new CoreModel.TypeName();
    retType.name = "Double";
    func.ret = retType;

    var target = new CoreModel.Name();
    target.name = "getRate";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    var resolverWithSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of("getRate", func),
        Map.of()
    );

    assertEquals('D', resolverWithSchema.inferType(call));
  }

  @Test
  void testInferCallWithQualifiedFunctionName() {
    var func = new CoreModel.Func();
    func.name = "calculateTotal";
    var retType = new CoreModel.TypeName();
    retType.name = "Int";
    func.ret = retType;

    var target = new CoreModel.Name();
    target.name = "finance.calculateTotal";  // 带包名的函数调用

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of();

    // 注册时使用简单名称
    var resolverWithSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of("calculateTotal", func),
        Map.of()
    );

    // 应该能够通过简单名称查找
    assertEquals('I', resolverWithSchema.inferType(call));
  }

  @Test
  void testInferDottedNameFieldAccess() {
    // 创建数据类型定义
    var data = new CoreModel.Data();
    data.name = "Person";
    var ageField = new CoreModel.Field();
    ageField.name = "age";
    var ageType = new CoreModel.TypeName();
    ageType.name = "Int";
    ageField.type = ageType;
    data.fields = List.of(ageField);

    // 声明person变量
    scopeStack.declare("person", 1, "Laster/Person;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "person.age";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("Person", data)
    );

    assertEquals('I', resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testInferDottedNameDoubleFieldAccess() {
    // 创建数据类型定义
    var data = new CoreModel.Data();
    data.name = "Account";
    var balanceField = new CoreModel.Field();
    balanceField.name = "balance";
    var balanceType = new CoreModel.TypeName();
    balanceType.name = "Double";
    balanceField.type = balanceType;
    data.fields = List.of(balanceField);

    scopeStack.declare("account", 1, "Laster/Account;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "account.balance";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("Account", data)
    );

    assertEquals('D', resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testInferDottedNameBoolFieldAccess() {
    var data = new CoreModel.Data();
    data.name = "Status";
    var activeField = new CoreModel.Field();
    activeField.name = "active";
    var activeType = new CoreModel.TypeName();
    activeType.name = "Bool";
    activeField.type = activeType;
    data.fields = List.of(activeField);

    scopeStack.declare("status", 1, "Laster/Status;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "status.active";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("Status", data)
    );

    assertEquals('Z', resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testInferDottedNameLongFieldAccess() {
    var data = new CoreModel.Data();
    data.name = "Transaction";
    var idField = new CoreModel.Field();
    idField.name = "id";
    var idType = new CoreModel.TypeName();
    idType.name = "Long";
    idField.type = idType;
    data.fields = List.of(idField);

    scopeStack.declare("tx", 1, "Laster/Transaction;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "tx.id";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("Transaction", data)
    );

    assertEquals('J', resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testInferDottedNameObjectFieldAccess() {
    var data = new CoreModel.Data();
    data.name = "User";
    var nameField = new CoreModel.Field();
    nameField.name = "name";
    var nameType = new CoreModel.TypeName();
    nameType.name = "String";  // 非原生类型
    nameField.type = nameType;
    data.fields = List.of(nameField);

    scopeStack.declare("user", 1, "Laster/User;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "user.name";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("User", data)
    );

    // String不是I/J/D/Z，应该返回null（表示Object）
    assertNull(resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testInferDottedNameFieldNotFound() {
    var data = new CoreModel.Data();
    data.name = "Empty";
    data.fields = List.of();

    scopeStack.declare("obj", 1, "Laster/Empty;", ScopeStack.JvmKind.OBJECT);

    var expr = new CoreModel.Name();
    expr.name = "obj.nonexistent";

    var resolverWithDataSchema = new TypeResolver(
        scopeStack,
        Map.of(),
        Map.of(),
        Map.of("Empty", data)
    );

    assertNull(resolverWithDataSchema.inferType(expr));
  }

  @Test
  void testTypeResolverRequiresNonNullScopeStack() {
    assertThrows(NullPointerException.class, () ->
        new TypeResolver(null, Map.of(), Map.of(), Map.of())
    );
  }

  @Test
  void testTypeResolverRequiresNonNullFuncHints() {
    assertThrows(NullPointerException.class, () ->
        new TypeResolver(scopeStack, null, Map.of(), Map.of())
    );
  }

  @Test
  void testTypeResolverAllowsNullSchemas() {
    // functionSchemas和dataSchema可以为null（会被转换为空Map）
    var resolver = new TypeResolver(scopeStack, Map.of(), null, null);
    assertNotNull(resolver);

    var expr = new CoreModel.IntE();
    expr.value = 123;
    assertEquals('I', resolver.inferType(expr));
  }

  @Test
  void testNumericPromotionDoubleWins() {
    var left = new CoreModel.LongE();
    left.value = 100L;

    var right = new CoreModel.DoubleE();
    right.value = 2.5;

    var target = new CoreModel.Name();
    target.name = "*";

    var call = new CoreModel.Call();
    call.target = target;
    call.args = List.of(left, right);

    // long * double => double
    assertEquals('D', resolver.inferType(call));
  }

  @Test
  void testComparisonOperators() {
    var left = new CoreModel.IntE();
    left.value = 10;
    var right = new CoreModel.IntE();
    right.value = 20;

    String[] compOps = {"<", ">", "<=", ">=", "==", "!=", "equals"};

    for (String op : compOps) {
      var target = new CoreModel.Name();
      target.name = op;

      var call = new CoreModel.Call();
      call.target = target;
      call.args = List.of(left, right);

      assertEquals('Z', resolver.inferType(call), "Operator " + op + " should return boolean");
    }
  }

  @Test
  void testNumericBinaryOperators() {
    var left = new CoreModel.IntE();
    left.value = 5;
    var right = new CoreModel.IntE();
    right.value = 3;

    String[] numOps = {"+", "-", "*", "/", "times", "divided by"};

    for (String op : numOps) {
      var target = new CoreModel.Name();
      target.name = op;

      var call = new CoreModel.Call();
      call.target = target;
      call.args = List.of(left, right);

      Character result = resolver.inferType(call);
      assertNotNull(result, "Operator " + op + " should return a numeric type");
      assertTrue(result == 'I' || result == 'J' || result == 'D',
          "Operator " + op + " should return I, J, or D");
    }
  }
}
