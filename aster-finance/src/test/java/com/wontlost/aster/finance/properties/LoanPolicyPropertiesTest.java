package com.wontlost.aster.finance.properties;

import com.wontlost.aster.finance.policies.LoanPolicyEngine;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.RiskLevel;
import net.jqwik.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for LoanPolicyEngine
 * 验证策略逻辑的代数性质
 */
class LoanPolicyPropertiesTest {

    private final LoanPolicyEngine engine = new LoanPolicyEngine();

    @Provide
    Arbitrary<CreditScore> creditScores() {
        return Arbitraries.integers()
            .between(CreditScore.MIN_SCORE, CreditScore.MAX_SCORE)
            .map(CreditScore::new);
    }

    /**
     * 属性：信用分数越高，利率越低（或相等）
     */
    @Property
    void higherCreditScoreShouldHaveLowerOrEqualRate(
        @ForAll("creditScores") CreditScore lower,
        @ForAll("creditScores") CreditScore higher
    ) {
        Assume.that(lower.value() < higher.value());

        BigDecimal lowerRate = engine.calculateInterestRate(lower);
        BigDecimal higherRate = engine.calculateInterestRate(higher);

        assertThat(higherRate.compareTo(lowerRate)).isLessThanOrEqualTo(0);
    }

    /**
     * 属性：信用分数越高，风险等级越低（或相等）
     */
    @Property
    void higherCreditScoreShouldHaveLowerOrEqualRisk(
        @ForAll("creditScores") CreditScore lower,
        @ForAll("creditScores") CreditScore higher
    ) {
        Assume.that(lower.value() < higher.value());

        RiskLevel lowerRisk = engine.assessRisk(lower);
        RiskLevel higherRisk = engine.assessRisk(higher);

        // Higher credit score → lower or equal risk severity
        assertThat(higherRisk.getSeverityLevel()).isLessThanOrEqualTo(lowerRisk.getSeverityLevel());
    }

    /**
     * 属性：利率必须在合理范围内 (0%, 20%]
     */
    @Property
    void interestRateShouldBeInReasonableRange(@ForAll("creditScores") CreditScore score) {
        BigDecimal rate = engine.calculateInterestRate(score);

        assertThat(rate).isGreaterThan(BigDecimal.ZERO);
        assertThat(rate).isLessThanOrEqualTo(new BigDecimal("0.20"));  // Max 20%
    }

    /**
     * 属性：风险评估必须返回非 null 值
     */
    @Property
    void riskAssessmentShouldNeverReturnNull(@ForAll("creditScores") CreditScore score) {
        RiskLevel risk = engine.assessRisk(score);

        assertThat(risk).isNotNull();
    }

    /**
     * 属性：相同的信用分数必须返回相同的利率（幂等性）
     */
    @Property
    void interestRateCalculationShouldBeIdempotent(@ForAll("creditScores") CreditScore score) {
        BigDecimal rate1 = engine.calculateInterestRate(score);
        BigDecimal rate2 = engine.calculateInterestRate(score);

        assertThat(rate1).isEqualByComparingTo(rate2);
    }

    /**
     * 属性：相同的信用分数必须返回相同的风险等级（幂等性）
     */
    @Property
    void riskAssessmentShouldBeIdempotent(@ForAll("creditScores") CreditScore score) {
        RiskLevel risk1 = engine.assessRisk(score);
        RiskLevel risk2 = engine.assessRisk(score);

        assertThat(risk1).isEqualTo(risk2);
    }

    @Provide
    Arbitrary<CreditScore> excellentCreditScores() {
        return Arbitraries.integers()
            .between(800, CreditScore.MAX_SCORE)
            .map(CreditScore::new);
    }

    /**
     * 属性：Excellent 信用分数应该获得最低利率
     */
    @Property
    void excellentCreditShouldGetLowestRate(@ForAll("excellentCreditScores") CreditScore score) {
        BigDecimal rate = engine.calculateInterestRate(score);

        // Excellent credit (800+) should get 5% rate
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.05"));
    }

    /**
     * 属性：Excellent 信用分数应该被评为低风险
     */
    @Property
    void excellentCreditShouldBeLowRisk(@ForAll("excellentCreditScores") CreditScore score) {
        RiskLevel risk = engine.assessRisk(score);

        assertThat(risk).isEqualTo(RiskLevel.LOW);
    }

    /**
     * 属性：Poor 信用分数应该获得最高利率
     */
    @Property
    void poorCreditShouldGetHighestRate(@ForAll("creditScores") CreditScore score) {
        Assume.that(score.isPoor());

        BigDecimal rate = engine.calculateInterestRate(score);

        // Poor credit should get 10% rate
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.10"));
    }

    /**
     * 属性：Poor 信用分数应该被评为高风险或极高风险
     */
    @Property
    void poorCreditShouldBeHighOrCriticalRisk(@ForAll("creditScores") CreditScore score) {
        Assume.that(score.isPoor());

        RiskLevel risk = engine.assessRisk(score);

        assertThat(risk).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
    }

    /**
     * 属性：利率的离散值有限（只有 3 种：5%, 7%, 10%）
     */
    @Property
    void interestRateShouldHaveLimitedValues(@ForAll("creditScores") CreditScore score) {
        BigDecimal rate = engine.calculateInterestRate(score);

        assertThat(rate).isIn(
            new BigDecimal("0.05"),  // Excellent
            new BigDecimal("0.07"),  // Good/VeryGood
            new BigDecimal("0.10")   // Fair/Poor
        );
    }

    /**
     * 属性：风险等级的离散值有限（4 种：LOW, MEDIUM, HIGH, CRITICAL）
     */
    @Property
    void riskLevelShouldHaveLimitedValues(@ForAll("creditScores") CreditScore score) {
        RiskLevel risk = engine.assessRisk(score);

        assertThat(risk).isIn(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);
    }
}
