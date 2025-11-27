package editor.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PolicyRule 单元测试
 * 验证数据类的构造、验证、equals/hashCode 契约
 */
class PolicyRuleTest {

    @Test
    void testConstructor() {
        PolicyRule rule = new PolicyRule("http", List.of("*.example.com", "api.service/*"));

        assertEquals("http", rule.getResourceType());
        assertEquals(List.of("*.example.com", "api.service/*"), rule.getPatterns());
    }

    @Test
    void testConstructorWithEmptyPatterns() {
        PolicyRule rule = new PolicyRule("database", List.of());

        assertEquals("database", rule.getResourceType());
        assertEquals(List.of(), rule.getPatterns());
    }

    @Test
    void testConstructor_NullResourceType() {
        assertThrows(NullPointerException.class, () ->
            new PolicyRule(null, List.of("pattern"))
        );
    }

    @Test
    void testConstructor_NullPatterns() {
        assertThrows(NullPointerException.class, () ->
            new PolicyRule("http", null)
        );
    }

    @Test
    void testEqualsAndHashCode() {
        PolicyRule rule1 = new PolicyRule("http", List.of("*.example.com"));
        PolicyRule rule2 = new PolicyRule("http", List.of("*.example.com"));
        PolicyRule rule3 = new PolicyRule("database", List.of("*.example.com"));
        PolicyRule rule4 = new PolicyRule("http", List.of("api.service/*"));

        // Reflexive
        assertEquals(rule1, rule1);
        assertEquals(rule1.hashCode(), rule1.hashCode());

        // Symmetric
        assertEquals(rule1, rule2);
        assertEquals(rule2, rule1);
        assertEquals(rule1.hashCode(), rule2.hashCode());

        // Different resourceType
        assertNotEquals(rule1, rule3);

        // Different patterns
        assertNotEquals(rule1, rule4);

        // Null comparison
        assertNotEquals(rule1, null);

        // Different class comparison
        assertNotEquals(rule1, new Object());
    }

    @Test
    void testToString() {
        PolicyRule rule = new PolicyRule("http", List.of("*.example.com", "api.service/*"));

        String result = rule.toString();

        assertNotNull(result);
        assertTrue(result.contains("PolicyRule"));
        assertTrue(result.contains("http"));
        assertTrue(result.contains("*.example.com"));
        assertTrue(result.contains("api.service/*"));
    }

    @Test
    void testGetters() {
        PolicyRule rule = new PolicyRule("execution", List.of("function1", "function2", "function3"));

        assertEquals("execution", rule.getResourceType());

        List<String> patterns = rule.getPatterns();
        assertEquals(3, patterns.size());
        assertEquals("function1", patterns.get(0));
        assertEquals("function2", patterns.get(1));
        assertEquals("function3", patterns.get(2));
    }
}
