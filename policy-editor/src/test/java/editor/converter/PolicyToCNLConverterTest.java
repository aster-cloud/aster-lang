package editor.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import editor.model.Policy;
import editor.model.PolicyRuleSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PolicyToCNLConverter 单元测试。
 * 覆盖 Policy → CNL 转换的核心场景。
 */
class PolicyToCNLConverterTest {

    private PolicyToCNLConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PolicyToCNLConverter();
    }

    @Test
    @DisplayName("优先返回现有 CNL（非空时直接返回）")
    void testPreserveExistingCnl() {
        String existingCnl = "module test.existing\n\nrule allow Http to \"*\"";
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-1", "test.existing.evaluate", allow, deny, existingCnl);

        String result = converter.convertToCNL(policy);

        assertEquals(existingCnl, result, "应直接返回现有 CNL，不重新生成");
    }

    @Test
    @DisplayName("空规则生成空模块声明")
    void testEmptyRulesGenerateModuleOnly() {
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-2", "empty.module.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertEquals("module empty.module", result, "空规则应只生成模块声明");
    }

    @Test
    @DisplayName("allow 规则转换：http → Http, database → Sql")
    void testAllowRulesConversion() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("https://api.example.com"),
            "database", List.of("SELECT")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-3", "demo.policy.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("module demo.policy"), "应包含模块声明");
        assertTrue(result.contains("rule allow Http to \"https://api.example.com\""),
            "http 应映射为 Http");
        assertTrue(result.contains("rule allow Sql to \"SELECT\""),
            "database 应映射为 Sql");
    }

    @Test
    @DisplayName("deny 规则转换正确")
    void testDenyRulesConversion() {
        PolicyRuleSet allow = new PolicyRuleSet(null);
        PolicyRuleSet deny = new PolicyRuleSet(Map.of(
            "filesystem", List.of("/etc/passwd"),
            "secrets", List.of("*")
        ));
        Policy policy = new Policy("id-4", "secure.policy.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("module secure.policy"), "应包含模块声明");
        assertTrue(result.contains("rule deny Files to \"/etc/passwd\""),
            "filesystem 应映射为 Files");
        assertTrue(result.contains("rule deny Secrets to \"*\""),
            "secrets 应映射为 Secrets");
    }

    @Test
    @DisplayName("未知资源类型首字母大写")
    void testUnknownResourceCapitalized() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "customresource", List.of("pattern1")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-5", "custom.policy.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Customresource to \"pattern1\""),
            "未知资源应首字母大写");
    }

    @Test
    @DisplayName("多个 patterns 生成多条规则")
    void testMultiplePatterns() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("https://api1.com", "https://api2.com", "https://api3.com")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-6", "multi.pattern.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"https://api1.com\""));
        assertTrue(result.contains("rule allow Http to \"https://api2.com\""));
        assertTrue(result.contains("rule allow Http to \"https://api3.com\""));
    }

    @Test
    @DisplayName("跳过 execution 类型规则")
    void testSkipExecutionRules() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "execution", List.of("evaluatePolicy", "checkRisk"),
            "http", List.of("*")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-7", "exec.skip.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"*\""), "http 规则应保留");
        assertTrue(!result.contains("execution"), "execution 规则不应生成 CNL");
        assertTrue(!result.contains("evaluatePolicy"), "函数名不应出现在 CNL 中");
    }

    @Test
    @DisplayName("跳过函数名模式（如 Http.get）")
    void testSkipFunctionPatterns() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("Http.get", "Http.post", "https://real.url.com")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-8", "func.skip.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"https://real.url.com\""),
            "真实 URL 应保留");
        assertTrue(!result.contains("Http.get"), "函数名模式不应生成规则");
        assertTrue(!result.contains("Http.post"), "函数名模式不应生成规则");
    }

    @Test
    @DisplayName("特殊字符转义")
    void testSpecialCharacterEscaping() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("https://api.com/path?a=1&b=\"test\"")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-9", "escape.test.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"https://api.com/path?a=1&b=\\\"test\\\"\""),
            "双引号应被转义");
    }

    @Test
    @DisplayName("null policy 抛出异常")
    void testNullPolicyThrowsException() {
        assertThrows(NullPointerException.class, () -> converter.convertToCNL(null));
    }

    @Test
    @DisplayName("无点号策略名称使用完整名称作为模块")
    void testNoDotPolicyName() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of("http", List.of("*")));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-10", "standalone", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.startsWith("module standalone"), "无点号名称应作为模块名");
    }

    @Test
    @DisplayName("所有已知资源类型映射正确")
    void testAllKnownResourceMappings() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("url1"),
            "database", List.of("url2"),
            "filesystem", List.of("url3"),
            "secrets", List.of("url4"),
            "ai-model", List.of("url5"),
            "time", List.of("url6"),
            "cpu", List.of("url7")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-11", "all.mappings.test", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("Http"), "http → Http");
        assertTrue(result.contains("Sql"), "database → Sql");
        assertTrue(result.contains("Files"), "filesystem → Files");
        assertTrue(result.contains("Secrets"), "secrets → Secrets");
        assertTrue(result.contains("AiModel"), "ai-model → AiModel");
        assertTrue(result.contains("Time"), "time → Time");
        assertTrue(result.contains("Cpu"), "cpu → Cpu");
    }

    // ===== Codex 审查后补充的边界测试 =====

    @Test
    @DisplayName("跳过小写函数名模式（如 fetch.get, db.query, sql.insert）")
    void testSkipLowercaseFunctionPatterns() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("fetch.get", "fetch.post", "https://real.api.com"),
            "database", List.of("db.query", "sql.insert", "SELECT * FROM users")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-lowercase", "lowercase.func.test", allow, deny, null);

        String result = converter.convertToCNL(policy);

        // 真实 URL/SQL 应保留
        assertTrue(result.contains("rule allow Http to \"https://real.api.com\""),
            "真实 URL 应保留");
        assertTrue(result.contains("rule allow Sql to \"SELECT * FROM users\""),
            "真实 SQL 应保留");

        // 小写函数名模式不应生成规则
        assertTrue(!result.contains("fetch.get"), "fetch.get 不应生成规则");
        assertTrue(!result.contains("fetch.post"), "fetch.post 不应生成规则");
        assertTrue(!result.contains("db.query"), "db.query 不应生成规则");
        assertTrue(!result.contains("sql.insert"), "sql.insert 不应生成规则");
    }

    @Test
    @DisplayName("输出顺序稳定可重复")
    void testOutputStability() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("url1", "url2"),
            "database", List.of("sql1"),
            "filesystem", List.of("path1")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(Map.of(
            "secrets", List.of("*")
        ));
        Policy policy = new Policy("id-stable", "stable.test.evaluate", allow, deny, null);

        // 多次调用应产生完全相同的输出
        String result1 = converter.convertToCNL(policy);
        String result2 = converter.convertToCNL(policy);
        String result3 = converter.convertToCNL(policy);

        assertEquals(result1, result2, "多次调用输出应一致 (1 vs 2)");
        assertEquals(result2, result3, "多次调用输出应一致 (2 vs 3)");

        // 验证输出按字母顺序排列（database < filesystem < http）
        int dbIndex = result1.indexOf("Sql");
        int fsIndex = result1.indexOf("Files");
        int httpIndex = result1.indexOf("Http");
        assertTrue(dbIndex < fsIndex, "database 规则应在 filesystem 之前");
        assertTrue(fsIndex < httpIndex, "filesystem 规则应在 http 之前");
    }

    @Test
    @DisplayName("空白资源类型被安全跳过")
    void testBlankResourceSkipped() {
        // 使用 HashMap 允许空白键（测试边界情况）
        java.util.HashMap<String, List<String>> rulesMap = new java.util.HashMap<>();
        rulesMap.put("http", List.of("url1"));
        rulesMap.put("", List.of("should-skip"));
        rulesMap.put("  ", List.of("also-skip"));

        PolicyRuleSet allow = new PolicyRuleSet(rulesMap);
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-blank", "blank.resource.test", allow, deny, null);

        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"url1\""), "有效规则应保留");
        assertTrue(!result.contains("should-skip"), "空白资源规则应被跳过");
        assertTrue(!result.contains("also-skip"), "空白资源规则应被跳过");
    }

    @Test
    @DisplayName("null 资源键被安全跳过（不抛出 NPE）")
    void testNullResourceKeySkipped() {
        // 使用 HashMap 允许 null 键（TreeMap 不允许，需先过滤）
        java.util.HashMap<String, List<String>> rulesMap = new java.util.HashMap<>();
        rulesMap.put("http", List.of("url1"));
        rulesMap.put(null, List.of("null-key-should-skip"));

        PolicyRuleSet allow = new PolicyRuleSet(rulesMap);
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-null-key", "null.key.test", allow, deny, null);

        // 不应抛出 NullPointerException
        String result = converter.convertToCNL(policy);

        assertTrue(result.contains("rule allow Http to \"url1\""), "有效规则应保留");
        assertTrue(!result.contains("null-key-should-skip"), "null 键资源规则应被跳过");
    }

    @Test
    @DisplayName("自定义域名模式不被误判为函数（如 Salesforce.Api）")
    void testCustomDomainPatternsPreserved() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("Salesforce.Api", "CRM.Service", "api.example.com")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(null);
        Policy policy = new Policy("id-domain", "domain.test.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        // 非标准前缀的点号模式应保留（不在 FUNCTION_PREFIXES 中）
        assertTrue(result.contains("rule allow Http to \"Salesforce.Api\""),
            "Salesforce.Api 应保留（非标准前缀）");
        assertTrue(result.contains("rule allow Http to \"CRM.Service\""),
            "CRM.Service 应保留（非标准前缀）");
        assertTrue(result.contains("rule allow Http to \"api.example.com\""),
            "api.example.com 应保留（小写非函数前缀）");
    }

    @Test
    @DisplayName("全文断言：验证完整 CNL 输出格式")
    void testFullOutputFormat() {
        PolicyRuleSet allow = new PolicyRuleSet(Map.of(
            "http", List.of("https://api.example.com")
        ));
        PolicyRuleSet deny = new PolicyRuleSet(Map.of(
            "database", List.of("DROP")
        ));
        Policy policy = new Policy("id-full", "test.full.evaluate", allow, deny, null);

        String result = converter.convertToCNL(policy);

        // 注意：allow 规则先于 deny 规则输出
        String expected = """
            module test.full

            rule allow Http to "https://api.example.com"
            rule deny Sql to "DROP\"""".strip();

        assertEquals(expected, result, "完整 CNL 输出格式应正确");
    }
}
