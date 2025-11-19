package com.wontlost.aster.policy;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PolicySerializerTest {
    private final PolicySerializer serializer = new PolicySerializer();

    // 简单策略对象用于测试
    static class TestPolicy {
        public String id;
        public String name;
        public String version;
        public Map<String, Object> rules;
        public LocalDateTime createdAt;

        public TestPolicy() {}

        public TestPolicy(String id, String name, String version, Map<String, Object> rules, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.rules = rules;
            this.createdAt = createdAt;
        }
    }

    @Test
    void shouldSerializeToJson() {
        TestPolicy policy = new TestPolicy(
            "loan-approval",
            "Loan Approval Policy",
            "v1",
            Map.of("minCreditScore", 620, "maxDTI", 4.0),
            LocalDateTime.of(2025, 11, 8, 10, 0)
        );

        String json = serializer.toJson(policy);

        assertThat(json)
            .contains("\"id\" : \"loan-approval\"")
            .contains("\"name\" : \"Loan Approval Policy\"")
            .contains("\"version\" : \"v1\"")
            .contains("\"minCreditScore\" : 620")
            .contains("\"maxDTI\" : 4.0");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
            {
              "id" : "loan-approval",
              "name" : "Loan Approval Policy",
              "version" : "v1",
              "rules" : {
                "minCreditScore" : 620,
                "maxDTI" : 4.0
              },
              "createdAt" : "2025-11-08T10:00:00"
            }
            """;

        TestPolicy policy = serializer.fromJson(json, TestPolicy.class);

        assertThat(policy.id).isEqualTo("loan-approval");
        assertThat(policy.name).isEqualTo("Loan Approval Policy");
        assertThat(policy.version).isEqualTo("v1");
        assertThat(policy.rules).containsEntry("minCreditScore", 620);
        assertThat(policy.createdAt).isEqualTo(LocalDateTime.of(2025, 11, 8, 10, 0));
    }

    @Test
    void shouldSupportJsonRoundTrip() throws Exception {
        TestPolicy original = new TestPolicy(
            "test-policy",
            "Test Policy",
            "v2",
            Map.of("rule1", "value1", "rule2", 123),
            LocalDateTime.of(2025, 11, 8, 12, 30)
        );

        // JSON round-trip
        String json = serializer.toJson(original);
        TestPolicy restored = serializer.fromJson(json, TestPolicy.class);

        assertThat(restored.id).isEqualTo(original.id);
        assertThat(restored.name).isEqualTo(original.name);
        assertThat(restored.version).isEqualTo(original.version);
        assertThat(restored.rules).isEqualTo(original.rules);
        assertThat(restored.createdAt).isEqualTo(original.createdAt);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldConvertCoreIRJsonToCNL() throws Exception {
        // This test verifies that a valid Core IR JSON can be converted to CNL format
        // We use fromCNL to first generate valid Core IR JSON, then convert it back
        String asterCNL = """
            This module is test.minimal.

            To greet with name: Text, produce Text:
              Return "Hello".
            """;

        // Step 1: CNL → Core IR JSON (via compile)
        Map<String, Object> coreIR = serializer.fromCNL(asterCNL, Map.class);

        // Step 2: Core IR JSON → CNL format (via json-to-cnl)
        String regeneratedCNL = serializer.toCNL(coreIR);

        // Verify output contains module name and function
        assertThat(regeneratedCNL)
            .contains("test.minimal")
            .contains("greet");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldConvertFromCNL_withValidAsterSource() {
        // This test uses actual Aster CNL source code
        String asterCNL = """
            This module is test.minimal.

            To greet with name: Text, produce Text:
              Return "Hello".
            """;

        // fromCNL compiles Aster source to Core IR JSON, then deserializes
        // The resulting JSON structure depends on the Core IR format
        // For now, we just verify it doesn't throw an exception
        assertThatCode(() -> {
            Map<String, Object> result = serializer.fromCNL(asterCNL, Map.class);
            assertThat(result).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleSerializationError() {
        // 测试循环引用对象（会导致序列化失败）
        Object circularRef = new Object() {
            public Object self = this;
        };

        assertThatThrownBy(() -> serializer.toJson(circularRef))
            .isInstanceOf(PolicySerializer.PolicySerializationException.class)
            .hasMessageContaining("Failed to serialize policy to JSON");
    }

    @Test
    void shouldHandleDeserializationError() {
        String invalidJson = "{ invalid json }";

        assertThatThrownBy(() -> serializer.fromJson(invalidJson, TestPolicy.class))
            .isInstanceOf(PolicySerializer.PolicySerializationException.class)
            .hasMessageContaining("Failed to deserialize policy from JSON");
    }

    @Test
    void shouldHandleEmptyPolicy() throws Exception {
        TestPolicy empty = new TestPolicy();
        String json = serializer.toJson(empty);
        TestPolicy restored = serializer.fromJson(json, TestPolicy.class);

        assertThat(restored).isNotNull();
        assertThat(restored.id).isNull();
        assertThat(restored.name).isNull();
    }

    @Test
    void shouldPreserveJsonStructure() throws Exception {
        String originalJson = """
            {
              "id" : "preserve-test",
              "name" : "Preserve Test",
              "version" : "v1",
              "rules" : {
                "nested" : {
                  "key" : "value"
                }
              },
              "createdAt" : "2025-11-08T15:00:00"
            }
            """;

        TestPolicy policy = serializer.fromJson(originalJson, TestPolicy.class);
        String serializedJson = serializer.toJson(policy);

        // 使用 JSONAssert 比较 JSON 结构（忽略空白和顺序）
        JSONAssert.assertEquals(originalJson, serializedJson, JSONCompareMode.LENIENT);
    }
}
