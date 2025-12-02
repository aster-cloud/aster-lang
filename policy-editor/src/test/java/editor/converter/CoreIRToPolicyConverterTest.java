package editor.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import editor.converter.CoreIRToPolicyConverter.ConversionException;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CoreIRToPolicyConverterTest {
    private CoreIRToPolicyConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CoreIRToPolicyConverter();
    }

    @Test
    void testExtractSimpleAllowRule() throws Exception {
        String json = """
            {
              "version": "1.0",
              "module": {
                "kind": "Module",
                "name": "demo.policy",
                "decls": [
                  {
                    "kind": "Func",
                    "name": "evaluateLoan",
                    "typeParams": [],
                    "params": [],
                    "ret": { "kind": "TypeName", "name": "Text" },
                    "effects": [],
                    "effectCaps": ["Http", "Sql"],
                    "effectCapsExplicit": true,
                    "body": {
                      "kind": "Block",
                      "statements": [
                        {
                          "kind": "Let",
                          "name": "resp",
                          "expr": {
                            "kind": "Call",
                            "target": { "kind": "Name", "name": "Http.get" },
                            "args": [
                              { "kind": "String", "value": "https://api.example.com/rates" }
                            ]
                          }
                        },
                        {
                          "kind": "Return",
                          "expr": {
                            "kind": "Call",
                            "target": { "kind": "Name", "name": "Db.insert" },
                            "args": [
                              { "kind": "String", "value": "loan_rules" },
                              { "kind": "Name", "name": "resp" }
                            ]
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        PolicyRuleSet rules = converter.extractAllowRules(json);
        Map<String, List<String>> map = rules.getRules();
        assertEquals(List.of("evaluateLoan"), map.get("execution"));
        assertEquals(List.of("Http.get", "https://api.example.com/rates"), map.get("http"));
        assertEquals(List.of("Db.insert", "loan_rules"), map.get("database"));
    }

    @Test
    void testExtractComplexRules() throws Exception {
        String json = """
            {
              "version": "1.0",
              "module": {
                "kind": "Module",
                "name": "demo.policy",
                "decls": [
                  {
                    "kind": "Func",
                    "name": "checkRisk",
                    "typeParams": [],
                    "params": [],
                    "ret": { "kind": "TypeName", "name": "Bool" },
                    "effects": [],
                    "effectCaps": ["Http"],
                    "effectCapsExplicit": true,
                    "body": {
                      "kind": "Block",
                      "statements": [
                        {
                          "kind": "Return",
                          "expr": {
                            "kind": "Call",
                            "target": { "kind": "Name", "name": "Http.post" },
                            "args": [
                              { "kind": "String", "value": "https://fraud.service/check" }
                            ]
                          }
                        }
                      ]
                    }
                  },
                  {
                    "kind": "Func",
                    "name": "enforceLimits",
                    "typeParams": [],
                    "params": [],
                    "ret": { "kind": "TypeName", "name": "Text" },
                    "effects": [],
                    "effectCaps": [],
                    "effectCapsExplicit": false,
                    "body": {
                      "kind": "Block",
                      "statements": [
                        {
                          "kind": "Return",
                          "expr": {
                            "kind": "Call",
                            "target": { "kind": "Name", "name": "deny.database" },
                            "args": [
                              { "kind": "String", "value": "write" }
                            ]
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        PolicyRuleSet allow = converter.extractAllowRules(json);
        PolicyRuleSet deny = converter.extractDenyRules(json);

        // 注意：effectCaps 声明能力，但不代表实际调用，因此函数名不包含在资源列表中
        // 资源列表只包含实际的 Call 节点：调用目标和参数
        assertEquals(List.of("Http.post", "https://fraud.service/check"), allow.getRules().get("http"));
        assertEquals(List.of("checkRisk", "enforceLimits"), allow.getRules().get("execution"));
        assertEquals(List.of("database"), deny.getRules().keySet().stream().toList());
        assertEquals(List.of("write"), deny.getRules().get("database"));
    }

    @Test
    void testInvalidJSON() {
        assertThrows(ConversionException.class, () -> converter.extractAllowRules("not-json"));
    }

    @Test
    void testMissingModule() {
        String json = """
            {
              "version": "1.0"
            }
            """;
        assertThrows(ConversionException.class, () -> converter.extractAllowRules(json));
    }

    @Test
    @Disabled("需要 aster-convert CLI 可执行文件在 PATH 中，暂时禁用此测试")
    void testRoundTripConversion() throws Exception {
        String cnl = """
            This module is test.policy.

            To evaluatePolicy with input: Text, produce Text. It performs io [Http, Sql]:
              Let resp be Http.get("https://api.example.com/policy").
              Return Db.insert("policies", resp).
            """;

        Policy policy = converter.convertCNLToPolicy(cnl, "test-id", "round-trip");
        Map<String, List<String>> allow = policy.getAllow().getRules();
        assertEquals(List.of("evaluatePolicy"), allow.get("execution"));
        assertEquals(List.of("Http.get", "https://api.example.com/policy"), allow.get("http"));
        assertEquals(List.of("Db.insert", "policies"), allow.get("database"));
    }
}
