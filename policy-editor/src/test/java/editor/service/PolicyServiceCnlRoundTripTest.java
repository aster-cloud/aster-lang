package editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 服务层 CNL 往返测试：验证 parsePolicy() → Policy → toPolicyInput() 保持 CNL 一致。
 */
class PolicyServiceCnlRoundTripTest {

    private PolicyService policyService;
    private ObjectMapper objectMapper;
    private Method parsePolicyMethod;
    private Method toPolicyInputMethod;

    @BeforeEach
    void setUp() throws Exception {
        policyService = new PolicyService();
        objectMapper = new ObjectMapper();

        // 获取私有方法的访问权限
        parsePolicyMethod = PolicyService.class.getDeclaredMethod("parsePolicy", JsonNode.class);
        parsePolicyMethod.setAccessible(true);

        toPolicyInputMethod = PolicyService.class.getDeclaredMethod("toPolicyInput", Policy.class);
        toPolicyInputMethod.setAccessible(true);
    }

    @Test
    @DisplayName("parsePolicy → toPolicyInput 往返保留 CNL")
    void testParsePolicyToPolicyInputRoundTrip() throws Exception {
        String cnl = "module demo.policy\n\nrule allow Http to \"https://api.example.com\"";

        // 模拟 GraphQL 响应 JSON
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("id", "test-id");
        jsonNode.put("name", "test-policy");
        jsonNode.put("cnl", cnl);
        jsonNode.putObject("allow").putArray("rules");
        jsonNode.putObject("deny").putArray("rules");

        // parsePolicy: JsonNode → Policy
        Policy policy = (Policy) parsePolicyMethod.invoke(policyService, jsonNode);
        assertEquals(cnl, policy.getCnl());

        // toPolicyInput: Policy → Map
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);

        // 验证 CNL 往返一致
        assertEquals(cnl, input.get("cnl"));
        assertEquals("test-id", input.get("id"));
        assertEquals("test-policy", input.get("name"));
    }

    @Test
    @DisplayName("parsePolicy → toPolicyInput 往返保留 null CNL")
    void testNullCnlRoundTrip() throws Exception {
        // 模拟 GraphQL 响应 JSON（cnl 为 null）
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("id", "null-cnl-id");
        jsonNode.put("name", "null-cnl-policy");
        jsonNode.putNull("cnl");
        jsonNode.putObject("allow").putArray("rules");
        jsonNode.putObject("deny").putArray("rules");

        // parsePolicy: JsonNode → Policy
        Policy policy = (Policy) parsePolicyMethod.invoke(policyService, jsonNode);
        assertNull(policy.getCnl());

        // toPolicyInput: Policy → Map
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);

        // 验证 null CNL 被显式发送（P1 修复验证）
        assertTrue(input.containsKey("cnl"), "cnl 字段应始终存在");
        assertNull(input.get("cnl"), "cnl 值应为 null");
    }

    @Test
    @DisplayName("parsePolicy → toPolicyInput 往返保留缺失 CNL 字段")
    void testMissingCnlFieldRoundTrip() throws Exception {
        // 模拟旧版 GraphQL 响应（没有 cnl 字段）
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("id", "legacy-id");
        jsonNode.put("name", "legacy-policy");
        // 注意：不设置 cnl 字段，模拟旧数据
        jsonNode.putObject("allow").putArray("rules");
        jsonNode.putObject("deny").putArray("rules");

        // parsePolicy: JsonNode → Policy
        Policy policy = (Policy) parsePolicyMethod.invoke(policyService, jsonNode);
        assertNull(policy.getCnl());

        // toPolicyInput: Policy → Map
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);

        // 验证缺失字段处理为 null
        assertTrue(input.containsKey("cnl"));
        assertNull(input.get("cnl"));
    }

    @Test
    @DisplayName("parsePolicy → toPolicyInput 往返保留特殊字符 CNL")
    void testSpecialCharactersCnlRoundTrip() throws Exception {
        String cnl = "module test\n-- 中文注释\nrule allow Http to \"https://example.com?a=1&b=2\"";

        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("id", "special-id");
        jsonNode.put("name", "special-policy");
        jsonNode.put("cnl", cnl);
        jsonNode.putObject("allow").putArray("rules");
        jsonNode.putObject("deny").putArray("rules");

        Policy policy = (Policy) parsePolicyMethod.invoke(policyService, jsonNode);
        assertEquals(cnl, policy.getCnl());

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);
        assertEquals(cnl, input.get("cnl"));
    }

    @Test
    @DisplayName("toPolicyInput 显式清空 CNL（P1 修复验证）")
    void testExplicitCnlClearing() throws Exception {
        // 创建一个没有 CNL 的 Policy（模拟用户清空操作）
        Policy policy = new Policy("clear-id", "clear-policy",
            new PolicyRuleSet(null), new PolicyRuleSet(null), null);

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);

        // P1 修复：cnl 字段应始终存在，即使值为 null
        assertTrue(input.containsKey("cnl"), "cnl 字段应始终发送以支持显式清空");
        assertNull(input.get("cnl"));
    }

    @Test
    @DisplayName("完整往返：带规则的 Policy 保留 CNL")
    void testFullRoundTripWithRules() throws Exception {
        String cnl = "module full.test\nrule allow Http to \"*\"\nrule deny Sql to \"DROP\"";

        // 构建带规则的 JSON
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("id", "full-id");
        jsonNode.put("name", "full-policy");
        jsonNode.put("cnl", cnl);

        ObjectNode allowNode = jsonNode.putObject("allow");
        allowNode.putArray("rules")
            .addObject()
            .put("resourceType", "http")
            .putArray("patterns").add("*");

        ObjectNode denyNode = jsonNode.putObject("deny");
        denyNode.putArray("rules")
            .addObject()
            .put("resourceType", "database")
            .putArray("patterns").add("DROP");

        // 往返测试
        Policy policy = (Policy) parsePolicyMethod.invoke(policyService, jsonNode);
        assertEquals(cnl, policy.getCnl());
        assertEquals(List.of("*"), policy.getAllow().getRules().get("http"));
        assertEquals(List.of("DROP"), policy.getDeny().getRules().get("database"));

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) toPolicyInputMethod.invoke(policyService, policy);
        assertEquals(cnl, input.get("cnl"));
    }
}
