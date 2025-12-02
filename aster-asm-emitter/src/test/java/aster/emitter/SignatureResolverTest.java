package aster.emitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignatureResolver 单元测试
 */
class SignatureResolverTest {
    private SignatureResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SignatureResolver(false); // diagOverload = false
    }

    @Test
    void testResolveSimpleMethod() {
        // String.length() -> ()I
        String sig = resolver.resolveMethodSignature("java/lang/String", "length", List.of(), "I");
        assertNotNull(sig);
        assertEquals("()I", sig);
    }

    @Test
    void testResolveMethodWithIntParameter() {
        // String.charAt(int) -> (I)C
        String sig = resolver.resolveMethodSignature("java/lang/String", "charAt", List.of("I"), "C");
        assertNotNull(sig);
        assertEquals("(I)C", sig);
    }

    @Test
    void testResolveMethodWithStringParameter() {
        // String.equals(Object) -> (Ljava/lang/Object;)Z
        // But we pass String argument, should still resolve
        String sig = resolver.resolveMethodSignature("java/lang/String", "equals",
                List.of("Ljava/lang/String;"), "Z");
        assertNotNull(sig);
        assertTrue(sig.endsWith(")Z"));
    }

    @Test
    void testResolveMethodWithMultipleParameters() {
        // String.substring(int, int) -> (II)Ljava/lang/String;
        String sig = resolver.resolveMethodSignature("java/lang/String", "substring",
                List.of("I", "I"), "Ljava/lang/String;");
        assertNotNull(sig);
        assertEquals("(II)Ljava/lang/String;", sig);
    }

    @Test
    void testResolveOverloadedMethod() {
        // String.substring(int) -> (I)Ljava/lang/String;
        String sig1 = resolver.resolveMethodSignature("java/lang/String", "substring",
                List.of("I"), "Ljava/lang/String;");
        assertNotNull(sig1);
        assertEquals("(I)Ljava/lang/String;", sig1);

        // String.substring(int, int) -> (II)Ljava/lang/String;
        String sig2 = resolver.resolveMethodSignature("java/lang/String", "substring",
                List.of("I", "I"), "Ljava/lang/String;");
        assertNotNull(sig2);
        assertEquals("(II)Ljava/lang/String;", sig2);
    }

    @Test
    void testResolvePrimitiveTypePromotion() {
        // Math.max(int, int) -> (II)I
        // Test that int argument matches int parameter (exact match, score 30)
        String sig = resolver.resolveMethodSignature("java/lang/Math", "max",
                List.of("I", "I"), "I");
        assertNotNull(sig);
        assertEquals("(II)I", sig);
    }

    @Test
    void testResolveNonExistentMethod() {
        // 不存在的方法应返回 null
        String sig = resolver.resolveMethodSignature("java/lang/String", "nonExistentMethod",
                List.of(), "V");
        assertNull(sig);
    }

    @Test
    void testResolveNonExistentClass() {
        // 不存在的类应返回 null
        String sig = resolver.resolveMethodSignature("java/lang/NonExistentClass", "someMethod",
                List.of(), "V");
        assertNull(sig);
    }

    @Test
    void testCachingBehavior() {
        // 第一次调用
        String sig1 = resolver.resolveMethodSignature("java/lang/String", "length", List.of(), "I");
        assertNotNull(sig1);

        // 第二次调用应该从缓存返回（测试缓存机制）
        String sig2 = resolver.resolveMethodSignature("java/lang/String", "length", List.of(), "I");
        assertNotNull(sig2);
        assertEquals(sig1, sig2);
    }

    @Test
    void testVarargsMethod() {
        // String.format(String, Object...) -> (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        // 传入 2 个参数：format 字符串 + 1 个 varargs 参数
        String sig = resolver.resolveMethodSignature("java/lang/String", "format",
                List.of("Ljava/lang/String;", "Ljava/lang/Object;"), "Ljava/lang/String;");
        assertNotNull(sig);
        assertTrue(sig.contains("Ljava/lang/String;"));
        assertTrue(sig.contains("[Ljava/lang/Object;")); // varargs 参数是数组
    }

    @Test
    void testBooleanParameterMatching() {
        // Boolean.valueOf(boolean) -> (Z)Ljava/lang/Boolean;
        String sig = resolver.resolveMethodSignature("java/lang/Boolean", "valueOf",
                List.of("Z"), "Ljava/lang/Boolean;");
        assertNotNull(sig);
        assertEquals("(Z)Ljava/lang/Boolean;", sig);
    }

    @Test
    void testLongParameterMatching() {
        // Long.valueOf(long) -> (J)Ljava/lang/Long;
        String sig = resolver.resolveMethodSignature("java/lang/Long", "valueOf",
                List.of("J"), "Ljava/lang/Long;");
        assertNotNull(sig);
        assertEquals("(J)Ljava/lang/Long;", sig);
    }

    @Test
    void testDoubleParameterMatching() {
        // Math.abs(double) -> (D)D
        String sig = resolver.resolveMethodSignature("java/lang/Math", "abs",
                List.of("D"), "D");
        assertNotNull(sig);
        assertEquals("(D)D", sig);
    }

    @Test
    void testListMethod() {
        // List.size() -> ()I
        String sig = resolver.resolveMethodSignature("java/util/List", "size", List.of(), "I");
        assertNotNull(sig);
        assertEquals("()I", sig);
    }

    @Test
    void testMapMethod() {
        // Map.get(Object) -> (Ljava/lang/Object;)Ljava/lang/Object;
        String sig = resolver.resolveMethodSignature("java/util/Map", "get",
                List.of("Ljava/lang/Object;"), "Ljava/lang/Object;");
        assertNotNull(sig);
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", sig);
    }
}
