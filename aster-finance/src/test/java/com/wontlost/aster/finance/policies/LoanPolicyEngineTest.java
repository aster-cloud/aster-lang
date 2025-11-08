package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.entities.Customer;
import com.wontlost.aster.finance.entities.LoanApplication;
import com.wontlost.aster.finance.entities.LoanPurpose;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Currency;
import com.wontlost.aster.finance.types.Money;
import com.wontlost.aster.finance.types.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * LoanPolicyEngine 单元测试
 */
class LoanPolicyEngineTest {

    private LoanPolicyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LoanPolicyEngine();
    }

    // ========== approveLoan Tests ==========

    @Test
    void shouldApproveQualifiedApplication() {
        Customer customer = Customer.builder()
            .id("C001")
            .name("Qualified Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))  // Good credit
            .annualIncome(new Money(80_000, Currency.USD))  // $80K/year
            .build();

        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(customer)
            .requestedAmount(new Money(200_000, Currency.USD))  // DTI ~32%
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.ApprovalDecision decision = engine.approveLoan(application);

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).contains("meets all requirements");
    }

    @Test
    void shouldRejectLowCreditScore() {
        Customer customer = Customer.builder()
            .id("C002")
            .name("Low Credit Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(580))  // Below 620 threshold
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        LoanApplication application = LoanApplication.builder()
            .id("LA002")
            .customer(customer)
            .requestedAmount(new Money(100_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.ApprovalDecision decision = engine.approveLoan(application);

        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).contains("Credit score");
        assertThat(decision.getReason()).contains("580");
        assertThat(decision.getReason()).contains("620");
    }

    @Test
    void shouldRejectHighDTI() {
        Customer customer = Customer.builder()
            .id("C003")
            .name("High DTI Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))  // Good credit
            .annualIncome(new Money(60_000, Currency.USD))  // $5K/month
            .build();

        LoanApplication application = LoanApplication.builder()
            .id("LA003")
            .customer(customer)
            .requestedAmount(new Money(500_000, Currency.USD))  // DTI ~54% > 43%
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.ApprovalDecision decision = engine.approveLoan(application);

        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).contains("Debt-to-income ratio");
        assertThat(decision.getReason()).contains("43");
    }

    @Test
    void shouldAcceptBoundaryDTI() {
        Customer customer = Customer.builder()
            .id("C004")
            .name("Boundary DTI Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(100_000, Currency.USD))  // $8,333/month
            .build();

        // Loan amount that produces DTI exactly at 43% threshold
        // At 7% interest for 360 months: $538,602 → $3,583/month → 43.00% DTI
        LoanApplication application = LoanApplication.builder()
            .id("LA004")
            .customer(customer)
            .requestedAmount(new Money(538_602, Currency.USD))  // DTI exactly 43%
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.ApprovalDecision decision = engine.approveLoan(application);

        // Should be approved (DTI <= 43%)
        assertThat(decision.isApproved()).isTrue();
    }

    @Test
    void shouldRejectNullApplication() {
        assertThatThrownBy(() -> engine.approveLoan(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Loan application cannot be null");
    }

    // ========== calculateInterestRate Tests ==========

    @Test
    void shouldReturnLowestRateForExcellentCredit() {
        CreditScore excellentScore = new CreditScore(820);

        BigDecimal rate = engine.calculateInterestRate(excellentScore);

        assertThat(rate).isEqualByComparingTo("0.05");  // 5%
    }

    @Test
    void shouldReturnMediumRateForGoodCredit() {
        CreditScore goodScore = new CreditScore(750);

        BigDecimal rate = engine.calculateInterestRate(goodScore);

        // 750 is Very Good (>=740), gets 7%
        assertThat(rate).isEqualByComparingTo("0.07");  // 7%
    }

    @Test
    void shouldReturnMediumRateForVeryGoodCredit() {
        CreditScore veryGoodScore = new CreditScore(780);

        BigDecimal rate = engine.calculateInterestRate(veryGoodScore);

        assertThat(rate).isEqualByComparingTo("0.07");  // 7%
    }

    @Test
    void shouldReturnHighestRateForFairCredit() {
        CreditScore fairScore = new CreditScore(680);

        BigDecimal rate = engine.calculateInterestRate(fairScore);

        assertThat(rate).isEqualByComparingTo("0.10");  // 10%
    }

    @Test
    void shouldReturnHighestRateForPoorCredit() {
        CreditScore poorScore = new CreditScore(580);

        BigDecimal rate = engine.calculateInterestRate(poorScore);

        assertThat(rate).isEqualByComparingTo("0.10");  // 10%
    }

    @Test
    void shouldRejectNullCreditScoreInRateCalculation() {
        assertThatThrownBy(() -> engine.calculateInterestRate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Credit score cannot be null");
    }

    // ========== assessRisk Tests ==========

    @Test
    void shouldAssessLowRiskForHighScore() {
        CreditScore highScore = new CreditScore(800);

        RiskLevel risk = engine.assessRisk(highScore);

        assertThat(risk).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldAssessMediumRiskForModerateScore() {
        CreditScore moderateScore = new CreditScore(700);

        RiskLevel risk = engine.assessRisk(moderateScore);

        assertThat(risk).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void shouldAssessHighRiskForLowScore() {
        CreditScore lowScore = new CreditScore(620);

        RiskLevel risk = engine.assessRisk(lowScore);

        assertThat(risk).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shouldAssessCriticalRiskForVeryLowScore() {
        CreditScore veryLowScore = new CreditScore(550);

        RiskLevel risk = engine.assessRisk(veryLowScore);

        assertThat(risk).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldHandleBoundaryScoresForRisk() {
        assertThat(engine.assessRisk(new CreditScore(750))).isEqualTo(RiskLevel.LOW);
        assertThat(engine.assessRisk(new CreditScore(749))).isEqualTo(RiskLevel.MEDIUM);
        assertThat(engine.assessRisk(new CreditScore(670))).isEqualTo(RiskLevel.MEDIUM);
        assertThat(engine.assessRisk(new CreditScore(669))).isEqualTo(RiskLevel.HIGH);
        assertThat(engine.assessRisk(new CreditScore(580))).isEqualTo(RiskLevel.HIGH);
        assertThat(engine.assessRisk(new CreditScore(579))).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldRejectNullCreditScoreInRiskAssessment() {
        assertThatThrownBy(() -> engine.assessRisk(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Credit score cannot be null");
    }

    // ========== evaluate (Comprehensive) Tests ==========

    @Test
    void shouldProvideComprehensiveEvaluation() {
        Customer customer = Customer.builder()
            .id("C005")
            .name("Test Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(820))  // Excellent
            .annualIncome(new Money(120_000, Currency.USD))
            .build();

        LoanApplication application = LoanApplication.builder()
            .id("LA005")
            .customer(customer)
            .requestedAmount(new Money(300_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.LoanEvaluation evaluation = engine.evaluate(application);

        assertThat(evaluation.getDecision().isApproved()).isTrue();
        assertThat(evaluation.getInterestRate()).isEqualByComparingTo("0.05");  // Excellent rate
        assertThat(evaluation.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldProvideEvaluationForRejectedApplication() {
        Customer customer = Customer.builder()
            .id("C006")
            .name("Rejected Customer")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(550))  // Poor
            .annualIncome(new Money(40_000, Currency.USD))
            .build();

        LoanApplication application = LoanApplication.builder()
            .id("LA006")
            .customer(customer)
            .requestedAmount(new Money(100_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.PERSONAL)
            .submittedAt(LocalDateTime.now())
            .build();

        LoanPolicyEngine.LoanEvaluation evaluation = engine.evaluate(application);

        assertThat(evaluation.getDecision().isApproved()).isFalse();
        assertThat(evaluation.getInterestRate()).isEqualByComparingTo("0.10");  // Poor rate
        assertThat(evaluation.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldRejectNullApplicationInEvaluation() {
        assertThatThrownBy(() -> engine.evaluate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Loan application cannot be null");
    }

    // ========== Decision/Evaluation Equality Tests ==========

    @Test
    void shouldImplementEqualityForApprovalDecision() {
        LoanPolicyEngine.ApprovalDecision d1 = new LoanPolicyEngine.ApprovalDecision(true, "Approved");
        LoanPolicyEngine.ApprovalDecision d2 = new LoanPolicyEngine.ApprovalDecision(true, "Approved");
        LoanPolicyEngine.ApprovalDecision d3 = new LoanPolicyEngine.ApprovalDecision(false, "Rejected");

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        assertThat(d1).isNotEqualTo(d3);
    }

    @Test
    void shouldImplementEqualityForLoanEvaluation() {
        LoanPolicyEngine.ApprovalDecision decision = new LoanPolicyEngine.ApprovalDecision(true, "Approved");
        LoanPolicyEngine.LoanEvaluation e1 = new LoanPolicyEngine.LoanEvaluation(
            decision, new BigDecimal("0.05"), RiskLevel.LOW
        );
        LoanPolicyEngine.LoanEvaluation e2 = new LoanPolicyEngine.LoanEvaluation(
            decision, new BigDecimal("0.05"), RiskLevel.LOW
        );

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }
}
