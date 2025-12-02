package aster.emitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试ScopeStack的作用域管理和变量追踪功能
 */
class ScopeStackTest {

  private ScopeStack stack;

  @BeforeEach
  void setUp() {
    stack = new ScopeStack();
  }

  @Test
  void testInitialState() {
    // 新建的ScopeStack应该有一个根作用域（深度0）
    assertEquals(0, stack.depth());
    assertTrue(stack.snapshotAll().isEmpty());
  }

  @Test
  void testDeclareAndLookupByName() {
    // 在根作用域声明变量
    var local = stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    assertEquals("x", local.name());
    assertEquals(1, local.slot());
    assertEquals("I", local.descriptor());
    assertEquals(ScopeStack.JvmKind.INT, local.kind());
    assertEquals(0, local.depth());

    // 按名称查找应该成功
    var found = stack.lookup("x");
    assertTrue(found.isPresent());
    assertEquals(local, found.get());
  }

  @Test
  void testDeclareAndLookupBySlot() {
    // 声明变量
    stack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);

    // 按槽位查找应该成功
    var found = stack.lookup(2);
    assertTrue(found.isPresent());
    assertEquals("y", found.get().name());
    assertEquals(ScopeStack.JvmKind.LONG, found.get().kind());
  }

  @Test
  void testGetTypeByName() {
    stack.declare("intVar", 1, "I", ScopeStack.JvmKind.INT);
    stack.declare("longVar", 2, "J", ScopeStack.JvmKind.LONG);
    stack.declare("doubleVar", 3, "D", ScopeStack.JvmKind.DOUBLE);
    stack.declare("boolVar", 4, "Z", ScopeStack.JvmKind.BOOLEAN);

    assertEquals('I', stack.getType("intVar"));
    assertEquals('J', stack.getType("longVar"));
    assertEquals('D', stack.getType("doubleVar"));
    assertEquals('Z', stack.getType("boolVar"));
    assertNull(stack.getType("nonexistent"));
  }

  @Test
  void testGetTypeBySlot() {
    stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);
    stack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);

    assertEquals('I', stack.getType(1));
    assertEquals('J', stack.getType(2));
    assertNull(stack.getType(99));
  }

  @Test
  void testGetDescriptor() {
    stack.declare("obj", 1, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

    assertEquals("Ljava/lang/String;", stack.getDescriptor("obj"));
    assertEquals("Ljava/lang/String;", stack.getDescriptor(1));
    assertNull(stack.getDescriptor("nonexistent"));
    assertNull(stack.getDescriptor(99));
  }

  @Test
  void testPushScope() {
    assertEquals(0, stack.depth());

    stack.pushScope();
    assertEquals(1, stack.depth());

    stack.pushScope();
    assertEquals(2, stack.depth());
  }

  @Test
  void testPopScope() {
    stack.pushScope();
    stack.pushScope();
    assertEquals(2, stack.depth());

    stack.popScope();
    assertEquals(1, stack.depth());

    stack.popScope();
    assertEquals(0, stack.depth());
  }

  @Test
  void testCannotPopRootScope() {
    assertEquals(0, stack.depth());

    // 尝试弹出根作用域应该抛出异常
    assertThrows(IllegalStateException.class, () -> stack.popScope());
  }

  @Test
  void testScopeIsolation() {
    // 在根作用域声明x
    stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    // 进入新作用域
    stack.pushScope();

    // 在新作用域也能看到外层的x
    assertTrue(stack.lookup("x").isPresent());
    assertEquals(1, stack.lookup("x").get().slot());

    // 在新作用域声明同名变量y
    stack.declare("y", 2, "J", ScopeStack.JvmKind.LONG);
    assertTrue(stack.lookup("y").isPresent());

    // 弹出作用域
    stack.popScope();

    // y应该消失
    assertFalse(stack.lookup("y").isPresent());
    assertNull(stack.getType(2));

    // x仍然可见
    assertTrue(stack.lookup("x").isPresent());
  }

  @Test
  void testVariableShadowing() {
    // 在根作用域声明x为int
    stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);
    assertEquals('I', stack.getType("x"));

    // 进入新作用域
    stack.pushScope();

    // 在新作用域声明同名x为long
    stack.declare("x", 2, "J", ScopeStack.JvmKind.LONG);

    // 应该看到新作用域的x（遮蔽了外层的x）
    assertEquals('J', stack.getType("x"));
    assertEquals(2, stack.lookup("x").get().slot());

    // 弹出作用域
    stack.popScope();

    // 应该恢复到根作用域的x
    assertEquals('I', stack.getType("x"));
    assertEquals(1, stack.lookup("x").get().slot());
  }

  @Test
  void testRedeclarationInSameScope() {
    // 第一次声明x
    var first = stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);
    assertEquals(1, first.slot());
    assertEquals('I', stack.getType("x"));

    // 在同一作用域重新声明x（不同槽位、不同类型）
    var second = stack.declare("x", 3, "J", ScopeStack.JvmKind.LONG);
    assertEquals(3, second.slot());
    assertEquals('J', stack.getType("x"));

    // 旧的槽位1应该被释放
    assertFalse(stack.lookup(1).isPresent());

    // 新的槽位3应该绑定到x
    assertTrue(stack.lookup(3).isPresent());
    assertEquals("x", stack.lookup(3).get().name());
  }

  @Test
  void testMultipleVariablesInScope() {
    stack.declare("a", 1, "I", ScopeStack.JvmKind.INT);
    stack.declare("b", 2, "J", ScopeStack.JvmKind.LONG);
    stack.declare("c", 3, "D", ScopeStack.JvmKind.DOUBLE);

    var all = stack.snapshotAll();
    assertEquals(3, all.size());

    // 验证所有变量都可见
    assertTrue(stack.lookup("a").isPresent());
    assertTrue(stack.lookup("b").isPresent());
    assertTrue(stack.lookup("c").isPresent());
  }

  @Test
  void testNestedScopesWithMultipleVariables() {
    // 根作用域: a, b
    stack.declare("a", 1, "I", ScopeStack.JvmKind.INT);
    stack.declare("b", 2, "J", ScopeStack.JvmKind.LONG);

    stack.pushScope();
    // 作用域1: c, d
    stack.declare("c", 3, "D", ScopeStack.JvmKind.DOUBLE);
    stack.declare("d", 4, "Z", ScopeStack.JvmKind.BOOLEAN);

    stack.pushScope();
    // 作用域2: e
    stack.declare("e", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT);

    // 所有5个变量应该可见
    assertEquals(5, stack.snapshotAll().size());

    // 弹出作用域2
    stack.popScope();
    assertEquals(4, stack.snapshotAll().size());
    assertFalse(stack.lookup("e").isPresent());

    // 弹出作用域1
    stack.popScope();
    assertEquals(2, stack.snapshotAll().size());
    assertFalse(stack.lookup("c").isPresent());
    assertFalse(stack.lookup("d").isPresent());

    // a和b仍然可见
    assertTrue(stack.lookup("a").isPresent());
    assertTrue(stack.lookup("b").isPresent());
  }

  @Test
  void testSnapshotCurrentScope() {
    stack.declare("a", 1, "I", ScopeStack.JvmKind.INT);

    stack.pushScope();
    stack.declare("b", 2, "J", ScopeStack.JvmKind.LONG);
    stack.declare("c", 3, "D", ScopeStack.JvmKind.DOUBLE);

    var current = stack.snapshotCurrentScope();
    assertEquals(2, current.size());
    assertTrue(current.stream().anyMatch(l -> l.name().equals("b")));
    assertTrue(current.stream().anyMatch(l -> l.name().equals("c")));
    assertFalse(current.stream().anyMatch(l -> l.name().equals("a")));
  }

  @Test
  void testDump() {
    stack.declare("x", 1, "I", ScopeStack.JvmKind.INT);

    String dump = stack.dump();
    assertNotNull(dump);
    assertTrue(dump.contains("x@1:I"));
    assertTrue(dump.contains("depth=0"));
  }

  @Test
  void testJvmKindPrimitive() {
    assertTrue(ScopeStack.JvmKind.INT.isPrimitive());
    assertTrue(ScopeStack.JvmKind.LONG.isPrimitive());
    assertTrue(ScopeStack.JvmKind.DOUBLE.isPrimitive());
    assertTrue(ScopeStack.JvmKind.BOOLEAN.isPrimitive());
    assertFalse(ScopeStack.JvmKind.OBJECT.isPrimitive());
    assertFalse(ScopeStack.JvmKind.UNKNOWN.isPrimitive());
  }

  @Test
  void testJvmKindDescriptorHint() {
    assertEquals("I", ScopeStack.JvmKind.INT.descriptorHint());
    assertEquals("J", ScopeStack.JvmKind.LONG.descriptorHint());
    assertEquals("D", ScopeStack.JvmKind.DOUBLE.descriptorHint());
    assertEquals("Z", ScopeStack.JvmKind.BOOLEAN.descriptorHint());
    assertEquals("Ljava/lang/Object;", ScopeStack.JvmKind.OBJECT.descriptorHint());
    assertEquals("", ScopeStack.JvmKind.UNKNOWN.descriptorHint());
  }

  @Test
  void testTypedLocalRecord() {
    var local = new ScopeStack.TypedLocal("test", 5, "Ljava/lang/String;", ScopeStack.JvmKind.OBJECT, 2);

    assertEquals("test", local.name());
    assertEquals(5, local.slot());
    assertEquals("Ljava/lang/String;", local.descriptor());
    assertEquals(ScopeStack.JvmKind.OBJECT, local.kind());
    assertEquals(2, local.depth());
  }

  @Test
  void testDeclareRequiresNonNullName() {
    assertThrows(NullPointerException.class, () ->
        stack.declare(null, 1, "I", ScopeStack.JvmKind.INT)
    );
  }

  @Test
  void testDeclareRequiresNonNullDescriptor() {
    assertThrows(NullPointerException.class, () ->
        stack.declare("x", 1, null, ScopeStack.JvmKind.INT)
    );
  }

  @Test
  void testDeclareRequiresNonNullKind() {
    assertThrows(NullPointerException.class, () ->
        stack.declare("x", 1, "I", null)
    );
  }

  @Test
  void testGetAllVariables() {
    stack.declare("a", 1, "I", ScopeStack.JvmKind.INT);
    stack.pushScope();
    stack.declare("b", 2, "J", ScopeStack.JvmKind.LONG);

    var all = stack.getAllVariables();
    assertEquals(2, all.size());

    // getAllVariables应该与snapshotAll返回相同结果
    assertEquals(stack.snapshotAll(), all);
  }
}
