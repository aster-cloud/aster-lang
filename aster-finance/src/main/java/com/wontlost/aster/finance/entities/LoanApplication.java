package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wontlost.aster.finance.types.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 贷款申请实体 - 包含客户、金额、期限等信息
 */
public record LoanApplication(
    @JsonProperty("id") String id,
    @JsonProperty("customer") Customer customer,
    @JsonProperty("requestedAmount") Money requestedAmount,
    @JsonProperty("termMonths") int termMonths,
    @JsonProperty("purpose") LoanPurpose purpose,
    @JsonProperty("submittedAt") LocalDateTime submittedAt
) {
    /**
     * 最小贷款期限（月）
     */
    public static final int MIN_TERM_MONTHS = 1;

    /**
     * 最大贷款期限（月）
     */
    public static final int MAX_TERM_MONTHS = 360;  // 30 years

    /**
     * 构造函数 - 验证参数
     */
    @JsonCreator
    public LoanApplication {
        Objects.requireNonNull(id, "Loan application ID cannot be null");
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");
        Objects.requireNonNull(purpose, "Loan purpose cannot be null");
        Objects.requireNonNull(submittedAt, "Submitted time cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Loan application ID cannot be blank");
        }

        // 验证贷款金额为正数
        if (!requestedAmount.isPositive()) {
            throw new IllegalArgumentException("Requested amount must be positive");
        }

        // 验证期限范围
        if (termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException(
                String.format("Term months must be between %d and %d, got: %d",
                    MIN_TERM_MONTHS, MAX_TERM_MONTHS, termMonths)
            );
        }
    }

    /**
     * 计算债务收入比 (Debt-to-Income Ratio)
     * DTI = 月还款额 / 月收入
     *
     * 使用简化的等额本息计算公式：
     * 月还款额 = 本金 × [月利率 × (1 + 月利率)^期数] / [(1 + 月利率)^期数 - 1]
     *
     * 假设年利率为 5%（实际应从策略中获取）
     *
     * @return DTI 比率（例如 0.35 表示 35%）
     */
    public BigDecimal debtToIncomeRatio() {
        return debtToIncomeRatio(new BigDecimal("0.05"));  // 默认 5% 年利率
    }

    /**
     * 使用指定年利率计算债务收入比
     *
     * @param annualInterestRate 年利率（例如 0.05 表示 5%）
     * @return DTI 比率
     */
    public BigDecimal debtToIncomeRatio(BigDecimal annualInterestRate) {
        Objects.requireNonNull(annualInterestRate, "Annual interest rate cannot be null");

        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual interest rate must be non-negative");
        }

        // 月收入 = 年收入 / 12
        BigDecimal monthlyIncome = customer.annualIncome().amount()
            .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Monthly income cannot be zero");
        }

        // 如果利率为 0，月还款额 = 本金 / 期数
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal monthlyPayment = requestedAmount.amount()
                .divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
            return monthlyPayment.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
        }

        // 月利率 = 年利率 / 12
        BigDecimal monthlyRate = annualInterestRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

        // (1 + 月利率)^期数
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowN = onePlusRate.pow(termMonths);

        // 月还款额 = 本金 × [月利率 × (1 + 月利率)^期数] / [(1 + 月利率)^期数 - 1]
        BigDecimal numerator = requestedAmount.amount()
            .multiply(monthlyRate)
            .multiply(onePlusRatePowN);

        BigDecimal denominator = onePlusRatePowN.subtract(BigDecimal.ONE);

        BigDecimal monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        // DTI = 月还款额 / 月收入
        return monthlyPayment.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
    }

    /**
     * 判断是否为高风险申请（DTI > 43%）
     * 43% 是美国 Qualified Mortgage 标准的上限
     */
    public boolean isHighRiskDTI() {
        return debtToIncomeRatio().compareTo(new BigDecimal("0.43")) > 0;
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private String id;
        private Customer customer;
        private Money requestedAmount;
        private int termMonths;
        private LoanPurpose purpose;
        private LocalDateTime submittedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Builder requestedAmount(Money requestedAmount) {
            this.requestedAmount = requestedAmount;
            return this;
        }

        public Builder termMonths(int termMonths) {
            this.termMonths = termMonths;
            return this;
        }

        public Builder purpose(LoanPurpose purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public LoanApplication build() {
            return new LoanApplication(id, customer, requestedAmount, termMonths, purpose, submittedAt);
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
