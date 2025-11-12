package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.dto.loan.ApplicantProfile;
import com.wontlost.aster.finance.dto.loan.LoanDecision;
import com.wontlost.aster.finance.entities.Customer;
import com.wontlost.aster.finance.entities.LoanApplication;
import com.wontlost.aster.finance.entities.LoanPurpose;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Currency;
import com.wontlost.aster.finance.types.Money;
import com.wontlost.aster.finance.types.RiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * 贷款策略引擎 - 核心业务规则实现
 * 包含贷款审批、利率计算、风险评估三大功能
 */
public class LoanPolicyEngine {

    /**
     * 最低信用分数要求
     */
    private static final int MIN_CREDIT_SCORE = 620;

    /**
     * 最大债务收入比 (43% 是 Qualified Mortgage 标准)
     */
    private static final BigDecimal MAX_DTI = new BigDecimal("0.43");

    /**
     * 优秀信用分数（800+）对应的年利率
     */
    private static final BigDecimal EXCELLENT_RATE = new BigDecimal("0.05");

    /**
     * 良好信用分数（740-799）对应的年利率
     */
    private static final BigDecimal GOOD_RATE = new BigDecimal("0.07");

    /**
     * 一般信用分数（<740）对应的年利率
     */
    private static final BigDecimal FAIR_RATE = new BigDecimal("0.10");

    /**
     * 使用共享 DTO 执行贷款资格评估。
     */
    public LoanDecision evaluateLoanEligibility(
        com.wontlost.aster.finance.dto.loan.LoanApplication application,
        ApplicantProfile applicant
    ) {
        Objects.requireNonNull(application, "LoanApplication DTO cannot be null");
        Objects.requireNonNull(applicant, "ApplicantProfile DTO cannot be null");

        LoanApplication domainApplication = mapDtoToDomain(application, applicant);
        ApprovalDecision decision = approveLoan(domainApplication);
        if (!decision.isApproved()) {
            return new LoanDecision(false, decision.getReason(), 0, 0, 0);
        }

        int interestRateBps = toBasisPoints(calculateInterestRate(domainApplication.customer().creditScore()));
        return new LoanDecision(
            true,
            decision.getReason(),
            application.amount(),
            interestRateBps,
            application.termMonths()
        );
    }

    /**
     * 贷款审批决策
     */
    public static class ApprovalDecision {
        private final boolean approved;
        private final String reason;

        public ApprovalDecision(boolean approved, String reason) {
            this.approved = approved;
            this.reason = reason;
        }

        public boolean isApproved() {
            return approved;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApprovalDecision that = (ApprovalDecision) o;
            return approved == that.approved && Objects.equals(reason, that.reason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(approved, reason);
        }

        @Override
        public String toString() {
            return "ApprovalDecision{approved=" + approved + ", reason='" + reason + "'}";
        }
    }

    /**
     * 贷款审批
     *
     * @param application 贷款申请
     * @return 审批决策（批准/拒绝 + 原因）
     */
    public ApprovalDecision approveLoan(LoanApplication application) {
        Objects.requireNonNull(application, "Loan application cannot be null");

        Customer customer = application.customer();

        // 规则 1: 验证年龄 >= 18（已在 Customer 构造函数中验证，这里再次确认）
        if (customer.age() < Customer.MIN_AGE) {
            return new ApprovalDecision(false,
                String.format("Customer age (%d) below minimum requirement (%d)",
                    customer.age(), Customer.MIN_AGE));
        }

        // 规则 2: 验证信用分数 >= 620
        if (customer.creditScore().value() < MIN_CREDIT_SCORE) {
            return new ApprovalDecision(false,
                String.format("Credit score (%d) below minimum requirement (%d)",
                    customer.creditScore().value(), MIN_CREDIT_SCORE));
        }

        // 规则 3: 验证 DTI <= 43%
        BigDecimal dti = application.debtToIncomeRatio();
        if (dti.compareTo(MAX_DTI) > 0) {
            return new ApprovalDecision(false,
                String.format("Debt-to-income ratio (%.2f%%) exceeds maximum allowed (%.2f%%)",
                    dti.multiply(new BigDecimal("100")).doubleValue(),
                    MAX_DTI.multiply(new BigDecimal("100")).doubleValue()));
        }

        // 所有规则通过，批准贷款
        return new ApprovalDecision(true, "Application meets all requirements");
    }

    /**
     * 计算年利率
     *
     * 利率分级规则：
     * - Excellent (800+): 5%
     * - Very Good (740-799): 7%
     * - Good/Fair/Poor (<740): 10%
     *
     * @param creditScore 客户信用分数
     * @return 年利率（例如 0.05 表示 5%）
     */
    public BigDecimal calculateInterestRate(CreditScore creditScore) {
        Objects.requireNonNull(creditScore, "Credit score cannot be null");

        if (creditScore.isExcellent()) {
            return EXCELLENT_RATE;
        } else if (creditScore.isVeryGood()) {
            return GOOD_RATE;
        } else {
            return FAIR_RATE;
        }
    }

    /**
     * 评估风险等级
     *
     * 风险分级规则：
     * - 750+: LOW
     * - 670-749: MEDIUM
     * - 580-669: HIGH
     * - <580: CRITICAL
     *
     * @param creditScore 客户信用分数
     * @return 风险等级
     */
    public RiskLevel assessRisk(CreditScore creditScore) {
        Objects.requireNonNull(creditScore, "Credit score cannot be null");

        int score = creditScore.value();

        if (score >= 750) {
            return RiskLevel.LOW;
        } else if (score >= 670) {
            return RiskLevel.MEDIUM;
        } else if (score >= 580) {
            return RiskLevel.HIGH;
        } else {
            return RiskLevel.CRITICAL;
        }
    }

    /**
     * 综合评估 - 返回审批决策、利率和风险等级
     */
    public static class LoanEvaluation {
        private final ApprovalDecision decision;
        private final BigDecimal interestRate;
        private final RiskLevel riskLevel;

        public LoanEvaluation(ApprovalDecision decision, BigDecimal interestRate, RiskLevel riskLevel) {
            this.decision = decision;
            this.interestRate = interestRate;
            this.riskLevel = riskLevel;
        }

        public ApprovalDecision getDecision() {
            return decision;
        }

        public BigDecimal getInterestRate() {
            return interestRate;
        }

        public RiskLevel getRiskLevel() {
            return riskLevel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoanEvaluation that = (LoanEvaluation) o;
            return Objects.equals(decision, that.decision) &&
                   Objects.equals(interestRate, that.interestRate) &&
                   riskLevel == that.riskLevel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(decision, interestRate, riskLevel);
        }

        @Override
        public String toString() {
            return "LoanEvaluation{" +
                   "decision=" + decision +
                   ", interestRate=" + interestRate +
                   ", riskLevel=" + riskLevel +
                   '}';
        }
    }

    /**
     * 综合评估贷款申请
     *
     * @param application 贷款申请
     * @return 包含审批决策、利率和风险等级的评估结果
     */
    public LoanEvaluation evaluate(LoanApplication application) {
        Objects.requireNonNull(application, "Loan application cannot be null");

        ApprovalDecision decision = approveLoan(application);
        BigDecimal interestRate = calculateInterestRate(application.customer().creditScore());
        RiskLevel riskLevel = assessRisk(application.customer().creditScore());

        return new LoanEvaluation(decision, interestRate, riskLevel);
    }

    private LoanApplication mapDtoToDomain(
        com.wontlost.aster.finance.dto.loan.LoanApplication application,
        ApplicantProfile profile
    ) {
        Customer customer = buildCustomer(application, profile);
        return LoanApplication.builder()
            .id(application.applicantId())
            .customer(customer)
            .requestedAmount(new Money(application.amount(), Currency.USD))
            .termMonths(application.termMonths())
            .purpose(resolvePurpose(application.purpose()))
            .submittedAt(LocalDateTime.now())
            .build();
    }

    private Customer buildCustomer(
        com.wontlost.aster.finance.dto.loan.LoanApplication application,
        ApplicantProfile profile
    ) {
        LocalDate birthDate = deriveBirthDate(profile.age());
        return Customer.builder()
            .id(application.applicantId())
            .name(application.applicantId())
            .dateOfBirth(birthDate)
            .creditScore(new CreditScore(profile.creditScore()))
            .annualIncome(new Money(profile.annualIncome(), Currency.USD))
            .build();
    }

    private LocalDate deriveBirthDate(int age) {
        if (age <= 0) {
            return LocalDate.now();
        }
        return LocalDate.now().minusYears(age);
    }

    private LoanPurpose resolvePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return LoanPurpose.PERSONAL;
        }
        return Arrays.stream(LoanPurpose.values())
            .filter(candidate -> candidate.name().equalsIgnoreCase(purpose))
            .findFirst()
            .orElse(LoanPurpose.PERSONAL);
    }

    private int toBasisPoints(BigDecimal rate) {
        return rate.movePointRight(4).setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
