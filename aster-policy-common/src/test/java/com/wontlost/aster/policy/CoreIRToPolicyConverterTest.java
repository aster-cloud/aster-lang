package com.wontlost.aster.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wontlost.aster.policy.CoreIRToPolicyConverter.ConversionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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

        Map<String, List<String>> rules = converter.extractAllowRules(json);

        // Verify execution rule
        assertTrue(rules.containsKey("execution"));
        assertEquals(List.of("evaluateLoan"), rules.get("execution"));

        // Verify http rules (function name + URL pattern)
        assertTrue(rules.containsKey("http"));
        assertTrue(rules.get("http").contains("Http.get"));
        assertTrue(rules.get("http").contains("https://api.example.com/rates"));

        // Verify database rules
        assertTrue(rules.containsKey("database"));
        assertTrue(rules.get("database").contains("Db.insert"));
        assertTrue(rules.get("database").contains("loan_rules"));
    }

    @Test
    void testExtractDenyRules() throws Exception {
        String json = """
            {
              "version": "1.0",
              "module": {
                "kind": "Module",
                "name": "demo.policy",
                "decls": [
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

        Map<String, List<String>> deny = converter.extractDenyRules(json);

        assertTrue(deny.containsKey("database"));
        assertEquals(List.of("write"), deny.get("database"));
    }

    @Test
    void testInvalidJSON() {
        assertThrows(ConversionException.class, () ->
            converter.extractAllowRules("not-json"));
    }

    @Test
    void testMissingModule() {
        String json = """
            {
              "version": "1.0"
            }
            """;
        assertThrows(ConversionException.class, () ->
            converter.extractAllowRules(json));
    }

    @Test
    void testIntegrationWithPolicySerializer() throws Exception {
        String cnl = """
            This module is demo.policy.

            To evaluateRisk with request: Text, produce Bool. It performs io [Http]:
              Let response be Http.post("https://fraud.service/check").
              Return response.
            """;

        // Convert CNL to Core IR JSON using PolicySerializer
        PolicySerializer serializer = new PolicySerializer();
        Object coreIR = serializer.fromCNL(cnl, Object.class);
        String coreIrJson = serializer.toJson(coreIR);

        // Extract rules from Core IR
        Map<String, List<String>> rules = converter.extractAllowRules(coreIrJson);

        // Verify extracted rules
        assertTrue(rules.containsKey("execution"));
        assertEquals(List.of("evaluateRisk"), rules.get("execution"));

        assertTrue(rules.containsKey("http"));
        assertTrue(rules.get("http").contains("Http.post"));
        assertTrue(rules.get("http").contains("https://fraud.service/check"));
    }
}
