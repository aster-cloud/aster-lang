package editor.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import editor.model.Policy;
import editor.model.PolicyRuleSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CNL 往返测试：验证 CNL 内容在 Policy 对象中正确保存和访问。
 */
class PolicyCnlRoundTripTest {

    @Test
    @DisplayName("Policy 5参数构造函数正确保存 CNL")
    void testPolicyConstructorPreservesCnl() {
        String cnl = "module demo.policy\n\nrule allow Http to \"https://api.example.com\"";
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("https://api.example.com")));
        PolicyRuleSet deny = new PolicyRuleSet(null);

        Policy policy = new Policy("test-id", "test-policy", allow, deny, cnl);

        assertEquals("test-id", policy.getId());
        assertEquals("test-policy", policy.getName());
        assertEquals(cnl, policy.getCnl());
        assertNotNull(policy.getAllow());
        assertNotNull(policy.getDeny());
    }

    @Test
    @DisplayName("null CNL 正确处理")
    void testNullCnlHandling() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("execution", List.of("main")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("null-cnl-id", "null-cnl-policy", allow, deny, null);

        assertNull(policy.getCnl());
        assertEquals("null-cnl-id", policy.getId());
        assertEquals("null-cnl-policy", policy.getName());
    }

    @Test
    @DisplayName("空字符串 CNL 正确处理")
    void testEmptyCnlHandling() {
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("empty-cnl-id", "empty-cnl-policy", allow, deny, "");

        assertEquals("", policy.getCnl());
    }

    @Test
    @DisplayName("包含特殊字符的 CNL 正确处理")
    void testSpecialCharactersCnl() {
        String cnl = """
            module test.special

            -- 注释：中文字符测试
            rule allow Http to "https://example.com/path?param=value&other=123"
            rule deny Sql to "SELECT * FROM users WHERE id = 'escaped\\'value'"
            """;
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("https://example.com")));
        PolicyRuleSet deny = new PolicyRuleSet(Map.of("database", List.of("SELECT")));
        Policy policy = new Policy("special-id", "special-policy", allow, deny, cnl);

        assertEquals(cnl, policy.getCnl());
    }

    @Test
    @DisplayName("多行 CNL 内容正确保留换行符")
    void testMultilineCnlPreservesNewlines() {
        String cnl = "line1\nline2\nline3\n\nline5";
        Policy policy = new Policy("multiline-id", "multiline-policy",
            new PolicyRuleSet(null), new PolicyRuleSet(null), cnl);

        assertEquals(cnl, policy.getCnl());
        assertEquals(5, policy.getCnl().split("\n").length);
    }

    @Test
    @DisplayName("Policy equals() 方法考虑 CNL 字段")
    void testPolicyEqualsIncludesCnl() {
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(null);

        Policy p1 = new Policy("id", "name", allow, deny, "cnl-content");
        Policy p2 = new Policy("id", "name", allow, deny, "cnl-content");
        Policy p3 = new Policy("id", "name", allow, deny, "different-cnl");
        Policy p4 = new Policy("id", "name", allow, deny, null);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());

        // CNL 不同时应不相等
        org.junit.jupiter.api.Assertions.assertNotEquals(p1, p3);
        org.junit.jupiter.api.Assertions.assertNotEquals(p1, p4);
    }

    @Test
    @DisplayName("向后兼容：4参数构造函数 CNL 为 null")
    void testBackwardCompatibility4ParamConstructor() {
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(null);

        Policy policy = new Policy("id", "name", allow, deny);

        assertNull(policy.getCnl());
    }

    @Test
    @DisplayName("Policy withName() 保留 CNL")
    void testWithNamePreservesCnl() {
        String cnl = "module test\nrule allow all";
        Policy original = new Policy("id", "original-name",
            new PolicyRuleSet(null), new PolicyRuleSet(null), cnl);

        Policy renamed = original.withName("new-name");

        assertEquals("new-name", renamed.getName());
        assertEquals(cnl, renamed.getCnl());
        assertEquals(original.getId(), renamed.getId());
    }
}
