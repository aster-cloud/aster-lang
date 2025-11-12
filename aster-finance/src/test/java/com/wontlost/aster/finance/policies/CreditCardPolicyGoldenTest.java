package com.wontlost.aster.finance.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wontlost.aster.finance.dto.creditcard.ApplicantInfo;
import com.wontlost.aster.finance.dto.creditcard.ApprovalDecision;
import com.wontlost.aster.finance.dto.creditcard.CreditCardOffer;
import com.wontlost.aster.finance.dto.creditcard.FinancialHistory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreditCardPolicyEngine Golden 測試 —— 驗證 DSL 對齊資料輸出
 */
class CreditCardPolicyGoldenTest {

    private final CreditCardPolicyEngine engine = new CreditCardPolicyEngine();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void prime申請應匹配黃金案例() throws IOException {
        verifyGolden("creditcard-approval-prime.json");
    }

    @Test
    void balanced申請應匹配黃金案例() throws IOException {
        verifyGolden("creditcard-approval-balanced.json");
    }

    @Test
    void 低分拒絕應匹配黃金案例() throws IOException {
        verifyGolden("creditcard-rejection-lowscore.json");
    }

    @Test
    void 高DTI拒絕應匹配黃金案例() throws IOException {
        verifyGolden("creditcard-rejection-highdti.json");
    }

    private void verifyGolden(String fileName) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("golden/" + fileName);
        assertThat(input).as("Golden file " + fileName + " must exist").isNotNull();

        JsonNode root = mapper.readTree(input);
        ApplicantInfo applicant = toApplicant(root.get("applicant"));
        FinancialHistory history = toHistory(root.get("history"));
        CreditCardOffer offer = toOffer(root.get("offer"));
        JsonNode expected = root.get("expectedDecision");

        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);

        assertThat(decision.approved()).isEqualTo(expected.get("approved").asBoolean());
        if (expected.has("approvedLimit")) {
            assertThat(decision.approvedLimit()).isEqualTo(expected.get("approvedLimit").asInt());
            assertThat(decision.creditLine()).isEqualTo(decision.approvedLimit());
        }
        if (expected.has("interestRateAPR")) {
            assertThat(decision.interestRateAPR()).isEqualTo(expected.get("interestRateAPR").asInt());
        }
        if (expected.has("monthlyFee")) {
            assertThat(decision.monthlyFee()).isEqualTo(expected.get("monthlyFee").asInt());
        }
        if (expected.has("requiresDeposit")) {
            assertThat(decision.requiresDeposit()).isEqualTo(expected.get("requiresDeposit").asBoolean());
            assertThat(decision.depositAmount()).isEqualTo(expected.get("depositAmount").asInt());
        }
        if (expected.has("reason")) {
            assertThat(decision.reason()).isEqualTo(expected.get("reason").asText());
        } else if (expected.has("reasonContains")) {
            assertThat(decision.reason()).contains(expected.get("reasonContains").asText());
        }
    }

    private ApplicantInfo toApplicant(JsonNode node) {
        return new ApplicantInfo(
            node.get("applicantId").asText(),
            node.get("age").asInt(),
            node.get("annualIncome").asInt(),
            node.get("creditScore").asInt(),
            node.get("existingCreditCards").asInt(),
            node.get("monthlyRent").asInt(),
            node.get("employmentStatus").asText(),
            node.get("yearsAtCurrentJob").asInt()
        );
    }

    private FinancialHistory toHistory(JsonNode node) {
        return new FinancialHistory(
            node.get("bankruptcyCount").asInt(),
            node.get("latePayments").asInt(),
            node.get("utilization").asInt(),
            node.get("accountAge").asInt(),
            node.get("hardInquiries").asInt()
        );
    }

    private CreditCardOffer toOffer(JsonNode node) {
        return new CreditCardOffer(
            node.get("productType").asText(),
            node.get("requestedLimit").asInt(),
            node.get("hasRewards").asBoolean(),
            node.get("annualFee").asInt()
        );
    }
}
