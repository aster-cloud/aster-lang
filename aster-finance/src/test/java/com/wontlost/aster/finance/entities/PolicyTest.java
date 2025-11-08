package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Policy 实体单元测试
 */
class PolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void shouldCreateValidPolicy() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("maxLoanAmount", 500_000.0);
        rules.put("minCreditScore", 650);

        Policy policy = Policy.builder()
            .id("P001")
            .name("Standard Loan Policy")
            .version("1.0.0")
            .rules(rules)
            .createdAt(LocalDateTime.now())
            .build();

        assertThat(policy.id()).isEqualTo("P001");
        assertThat(policy.name()).isEqualTo("Standard Loan Policy");
        assertThat(policy.version()).isEqualTo("1.0.0");
        assertThat(policy.ruleCount()).isEqualTo(2);
    }

    @Test
    void shouldGetRuleWithCorrectType() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .addRule("minCreditScore", 650)
            .addRule("requiresManualReview", true)
            .build();

        Double maxLoanAmount = policy.getRule("maxLoanAmount", Double.class);
        Integer minCreditScore = policy.getRule("minCreditScore", Integer.class);
        Boolean requiresManualReview = policy.getRule("requiresManualReview", Boolean.class);

        assertThat(maxLoanAmount).isEqualTo(500_000.0);
        assertThat(minCreditScore).isEqualTo(650);
        assertThat(requiresManualReview).isTrue();
    }

    @Test
    void shouldReturnNullForNonExistentRule() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .build();

        Integer nonExistent = policy.getRule("nonExistent", Integer.class);

        assertThat(nonExistent).isNull();
    }

    @Test
    void shouldGetRuleOrDefaultValue() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("minCreditScore", 650)
            .build();

        Integer existing = policy.getRuleOrDefault("minCreditScore", Integer.class, 600);
        Integer nonExistent = policy.getRuleOrDefault("maxDTI", Integer.class, 43);

        assertThat(existing).isEqualTo(650);
        assertThat(nonExistent).isEqualTo(43);
    }

    @Test
    void shouldCheckIfRuleExists() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .build();

        assertThat(policy.hasRule("maxLoanAmount")).isTrue();
        assertThat(policy.hasRule("nonExistent")).isFalse();
    }

    @Test
    void shouldCreateNewVersionWithUpdatedVersionNumber() {
        Policy original = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .version("1.0.0")
            .addRule("maxLoanAmount", 500_000.0)
            .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
            .build();

        Policy newVersion = original.withVersion("1.1.0");

        assertThat(newVersion.id()).isEqualTo(original.id());
        assertThat(newVersion.name()).isEqualTo(original.name());
        assertThat(newVersion.version()).isEqualTo("1.1.0");
        assertThat(newVersion.rules()).isEqualTo(original.rules());
        assertThat(newVersion.createdAt()).isNotEqualTo(original.createdAt());
    }

    @Test
    void shouldCreateCopyWithAddedRule() {
        Policy original = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .build();

        Policy updated = original.withRule("minCreditScore", 650);

        assertThat(original.ruleCount()).isEqualTo(1);
        assertThat(updated.ruleCount()).isEqualTo(2);
        assertThat(updated.getRule("minCreditScore", Integer.class)).isEqualTo(650);
    }

    @Test
    void shouldCreateCopyWithUpdatedRule() {
        Policy original = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .build();

        Policy updated = original.withRule("maxLoanAmount", 600_000.0);

        assertThat(original.getRule("maxLoanAmount", Double.class)).isEqualTo(500_000.0);
        assertThat(updated.getRule("maxLoanAmount", Double.class)).isEqualTo(600_000.0);
    }

    @Test
    void shouldCreateCopyWithoutRule() {
        Policy original = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)
            .addRule("minCreditScore", 650)
            .build();

        Policy updated = original.withoutRule("minCreditScore");

        assertThat(original.ruleCount()).isEqualTo(2);
        assertThat(updated.ruleCount()).isEqualTo(1);
        assertThat(updated.hasRule("minCreditScore")).isFalse();
        assertThat(updated.hasRule("maxLoanAmount")).isTrue();
    }

    @Test
    void shouldEnsureRulesAreImmutable() {
        Map<String, Object> mutableRules = new HashMap<>();
        mutableRules.put("maxLoanAmount", 500_000.0);

        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .rules(mutableRules)
            .build();

        // 修改原始 Map 不应影响 Policy
        mutableRules.put("minCreditScore", 650);

        assertThat(policy.ruleCount()).isEqualTo(1);
        assertThat(policy.hasRule("minCreditScore")).isFalse();

        // 尝试修改返回的 Map 应抛出异常
        assertThatThrownBy(() ->
            policy.rules().put("newRule", "value")
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() ->
            Policy.builder()
                .id(null)
                .name("Test Policy")
                .version("1.0.0")
                .build()
        ).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Policy ID cannot be null");
    }

    @Test
    void shouldRejectBlankId() {
        assertThatThrownBy(() ->
            Policy.builder()
                .id("   ")
                .name("Test Policy")
                .version("1.0.0")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Policy ID cannot be blank");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() ->
            Policy.builder()
                .id("P001")
                .name("   ")
                .version("1.0.0")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Policy name cannot be blank");
    }

    @Test
    void shouldRejectBlankVersion() {
        assertThatThrownBy(() ->
            Policy.builder()
                .id("P001")
                .name("Test Policy")
                .version("   ")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Policy version cannot be blank");
    }

    @Test
    void shouldUseDefaultVersion() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .build();

        assertThat(policy.version()).isEqualTo("1.0.0");
    }

    @Test
    void shouldThrowClassCastExceptionForWrongType() {
        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("maxLoanAmount", 500_000.0)  // Double
            .build();

        assertThatThrownBy(() ->
            policy.getRule("maxLoanAmount", Integer.class)  // 尝试转换为 Integer
        ).isInstanceOf(ClassCastException.class);
    }

    @Test
    void shouldSerializeAndDeserializeToJson() throws Exception {
        Policy original = Policy.builder()
            .id("P001")
            .name("Standard Loan Policy")
            .version("1.0.0")
            .addRule("maxLoanAmount", 500_000.0)
            .addRule("minCreditScore", 650)
            .addRule("requiresManualReview", true)
            .createdAt(LocalDateTime.of(2025, 1, 15, 10, 30))
            .build();

        String json = objectMapper.writeValueAsString(original);
        Policy deserialized = objectMapper.readValue(json, Policy.class);

        assertThat(deserialized.id()).isEqualTo(original.id());
        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.version()).isEqualTo(original.version());
        assertThat(deserialized.ruleCount()).isEqualTo(original.ruleCount());
        assertThat(deserialized.createdAt()).isEqualTo(original.createdAt());

        // 验证规则内容
        assertThat(deserialized.getRule("maxLoanAmount", Double.class)).isEqualTo(500_000.0);
        assertThat(deserialized.getRule("minCreditScore", Integer.class)).isEqualTo(650);
        assertThat(deserialized.getRule("requiresManualReview", Boolean.class)).isTrue();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 15, 10, 30);
        Map<String, Object> rules = new HashMap<>();
        rules.put("maxLoanAmount", 500_000.0);

        Policy policy1 = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .version("1.0.0")
            .rules(rules)
            .createdAt(timestamp)
            .build();

        Policy policy2 = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .version("1.0.0")
            .rules(rules)
            .createdAt(timestamp)
            .build();

        assertThat(policy1).isEqualTo(policy2);
        assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
    }

    @Test
    void shouldSupportComplexRuleTypes() {
        Map<String, Integer> scoringMatrix = new HashMap<>();
        scoringMatrix.put("excellent", 800);
        scoringMatrix.put("good", 700);

        Policy policy = Policy.builder()
            .id("P001")
            .name("Test Policy")
            .addRule("scoringMatrix", scoringMatrix)
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Integer> retrieved = policy.getRule("scoringMatrix", Map.class);

        assertThat(retrieved).containsEntry("excellent", 800);
        assertThat(retrieved).containsEntry("good", 700);
    }
}
