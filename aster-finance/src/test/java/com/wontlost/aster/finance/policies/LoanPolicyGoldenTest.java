package com.wontlost.aster.finance.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wontlost.aster.finance.entities.Customer;
import com.wontlost.aster.finance.entities.LoanApplication;
import com.wontlost.aster.finance.entities.LoanPurpose;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Currency;
import com.wontlost.aster.finance.types.Money;
import com.wontlost.aster.finance.types.RiskLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Golden Tests for LoanPolicyEngine
 * 使用固定的测试用例验证策略输出的稳定性
 */
class LoanPolicyGoldenTest {

    private final LoanPolicyEngine engine = new LoanPolicyEngine();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void shouldMatchExcellentApprovalCase() throws IOException {
        verifyGoldenTestCase("loan-approval-excellent.json");
    }

    @Test
    void shouldMatchGoodApprovalCase() throws IOException {
        verifyGoldenTestCase("loan-approval-good.json");
    }

    @Test
    void shouldMatchFairApprovalCase() throws IOException {
        verifyGoldenTestCase("loan-approval-fair.json");
    }

    @Test
    void shouldMatchLowCreditRejectionCase() throws IOException {
        verifyGoldenTestCase("loan-rejection-lowcredit.json");
    }

    @Test
    void shouldMatchHighDTIRejectionCase() throws IOException {
        verifyGoldenTestCase("loan-rejection-highdti.json");
    }

    private void verifyGoldenTestCase(String filename) throws IOException {
        // Load golden test case
        InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("golden/" + filename);
        assertThat(inputStream).isNotNull();

        JsonNode testCase = objectMapper.readTree(inputStream);

        // Parse customer data
        JsonNode customerNode = testCase.get("customer");
        Customer customer = Customer.builder()
            .id(customerNode.get("id").asText())
            .name(customerNode.get("name").asText())
            .dateOfBirth(LocalDate.parse(customerNode.get("dateOfBirth").asText()))
            .creditScore(new CreditScore(customerNode.get("creditScore").asInt()))
            .annualIncome(new Money(
                customerNode.get("annualIncome").get("amount").asDouble(),
                Currency.valueOf(customerNode.get("annualIncome").get("currency").asText())
            ))
            .build();

        // Parse loan application data
        JsonNode loanNode = testCase.get("loanApplication");
        LoanApplication application = LoanApplication.builder()
            .id(loanNode.get("id").asText())
            .customer(customer)
            .requestedAmount(new Money(
                loanNode.get("requestedAmount").get("amount").asDouble(),
                Currency.valueOf(loanNode.get("requestedAmount").get("currency").asText())
            ))
            .termMonths(loanNode.get("termMonths").asInt())
            .purpose(LoanPurpose.valueOf(loanNode.get("purpose").asText()))
            .submittedAt(LocalDateTime.now())
            .build();

        // Execute policy evaluation
        LoanPolicyEngine.LoanEvaluation evaluation = engine.evaluate(application);

        // Verify against expected results
        JsonNode expected = testCase.get("expectedEvaluation");

        assertThat(evaluation.getDecision().isApproved())
            .as("Approval decision for " + filename)
            .isEqualTo(expected.get("approved").asBoolean());

        if (expected.has("reason")) {
            assertThat(evaluation.getDecision().getReason())
                .as("Approval reason for " + filename)
                .isEqualTo(expected.get("reason").asText());
        } else if (expected.has("reasonContains")) {
            assertThat(evaluation.getDecision().getReason())
                .as("Approval reason for " + filename)
                .contains(expected.get("reasonContains").asText());
        }

        assertThat(evaluation.getInterestRate())
            .as("Interest rate for " + filename)
            .isEqualByComparingTo(new BigDecimal(expected.get("interestRate").asText()));

        assertThat(evaluation.getRiskLevel())
            .as("Risk level for " + filename)
            .isEqualTo(RiskLevel.valueOf(expected.get("riskLevel").asText()));
    }
}
