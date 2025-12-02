package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.dto.creditcard.ApplicantInfo;
import com.wontlost.aster.finance.dto.creditcard.ApprovalDecision;
import com.wontlost.aster.finance.dto.creditcard.CreditCardOffer;
import com.wontlost.aster.finance.dto.creditcard.FinancialHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreditCardPolicyEngine 單元測試（對齊 creditcard.aster DSL 規則）
 */
class CreditCardPolicyEngineTest {

    private CreditCardPolicyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CreditCardPolicyEngine();
    }

    @Test
    void 應批准高品質申請並返回最終額度() {
        ApplicantInfo applicant = applicant("prime", 30, 120_000, 805, 2, 1_500, "Full-time", 6);
        FinancialHistory history = history(0, 0, 25, 12, 1);
        CreditCardOffer offer = offer("Premium", 20_000, true, 95);

        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);

        assertThat(decision.approved()).isTrue();
        assertThat(decision.reason()).isEqualTo("Approved");
        assertThat(decision.approvedLimit()).isBetween(500, 50_000);
        assertThat(decision.creditLine()).isEqualTo(decision.approvedLimit());
        assertThat(decision.interestRateAPR()).isGreaterThan(0);
        assertThat(decision.requiresDeposit()).isTrue();
        assertThat(decision.depositAmount()).isEqualTo(decision.approvedLimit());
    }

    @Test
    void 信用分不足時必須拒絕() {
        ApplicantInfo applicant = applicant("low-score", 40, 90_000, 500, 3, 1_200, "Full-time", 5);
        FinancialHistory history = history(0, 1, 30, 24, 2);
        CreditCardOffer offer = offer("Standard", 15_000, true, 49);

        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.approvedLimit()).isZero();
        assertThat(decision.reason()).isEqualTo("Credit score too low");
    }

    @Test
    void 高負債比時會以建議文案拒絕() {
        ApplicantInfo applicant = applicant("high-dti", 35, 24_000, 680, 1, 1_800, "Self-employed", 2);
        FinancialHistory history = history(0, 2, 40, 18, 1);
        CreditCardOffer offer = offer("Standard", 25_000, false, 75);

        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("Debt-to-income ratio too high");
    }

    @Test
    void 有破產紀錄時直接拒絕() {
        ApplicantInfo applicant = applicant("bankruptcy", 45, 100_000, 710, 4, 1_400, "Full-time", 10);
        FinancialHistory history = history(1, 0, 20, 48, 0);
        CreditCardOffer offer = offer("Premium", 30_000, true, 95);

        ApprovalDecision decision = engine.evaluateCreditCardApplication(applicant, history, offer);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("Bankruptcy on record");
    }

    private ApplicantInfo applicant(
        String id,
        int age,
        int annualIncome,
        int creditScore,
        int existingCards,
        int monthlyRent,
        String employmentStatus,
        int yearsAtJob
    ) {
        return new ApplicantInfo(id, age, annualIncome, creditScore, existingCards, monthlyRent, employmentStatus, yearsAtJob);
    }

    private FinancialHistory history(int bankruptcy, int latePayments, int utilization, int accountAge, int inquiries) {
        return new FinancialHistory(bankruptcy, latePayments, utilization, accountAge, inquiries);
    }

    private CreditCardOffer offer(String productType, int requestedLimit, boolean hasRewards, int annualFee) {
        return new CreditCardOffer(productType, requestedLimit, hasRewards, annualFee);
    }
}
