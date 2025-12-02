package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Currency;
import com.wontlost.aster.finance.types.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * LoanApplication 实体单元测试
 */
class LoanApplicationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(60_000, Currency.USD))  // $5,000/月
            .build();
    }

    @Test
    void shouldCreateValidLoanApplication() {
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)  // 30 years
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        assertThat(application.id()).isEqualTo("LA001");
        assertThat(application.customer()).isEqualTo(testCustomer);
        assertThat(application.requestedAmount().amount()).isEqualByComparingTo("200000.00");
        assertThat(application.termMonths()).isEqualTo(360);
        assertThat(application.purpose()).isEqualTo(LoanPurpose.HOME);
    }

    @Test
    void shouldCalculateDebtToIncomeRatioWithDefaultRate() {
        // 年收入 $60,000 → 月收入 $5,000
        // 贷款 $200,000, 360个月, 5% 年利率
        // 月还款额约 $1,073.64
        // DTI = $1,073.64 / $5,000 ≈ 0.2147 (21.47%)
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        BigDecimal dti = application.debtToIncomeRatio();

        assertThat(dti).isGreaterThan(new BigDecimal("0.20"));
        assertThat(dti).isLessThan(new BigDecimal("0.25"));
    }

    @Test
    void shouldCalculateDebtToIncomeRatioWithCustomRate() {
        // 年收入 $60,000 → 月收入 $5,000
        // 贷款 $200,000, 360个月, 3% 年利率
        // 月还款额约 $843.21
        // DTI = $843.21 / $5,000 ≈ 0.1686 (16.86%)
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        BigDecimal dti = application.debtToIncomeRatio(new BigDecimal("0.03"));

        assertThat(dti).isGreaterThan(new BigDecimal("0.15"));
        assertThat(dti).isLessThan(new BigDecimal("0.20"));
    }

    @Test
    void shouldCalculateDebtToIncomeRatioWithZeroInterestRate() {
        // 年收入 $60,000 → 月收入 $5,000
        // 贷款 $60,000, 60个月, 0% 年利率
        // 月还款额 = $60,000 / 60 = $1,000
        // DTI = $1,000 / $5,000 = 0.20 (20%)
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(60_000, Currency.USD))
            .termMonths(60)
            .purpose(LoanPurpose.AUTO)
            .submittedAt(LocalDateTime.now())
            .build();

        BigDecimal dti = application.debtToIncomeRatio(BigDecimal.ZERO);

        assertThat(dti).isEqualByComparingTo("0.2000");
    }

    @Test
    void shouldIdentifyHighRiskDTI() {
        // 高 DTI：年收入 $60,000 ($5,000/月)，贷款 $500,000，360个月，5%年利率
        // 月还款额约 $2,684.11，DTI ≈ 53.68% > 43%
        LoanApplication highRisk = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(500_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        assertThat(highRisk.isHighRiskDTI()).isTrue();

        // 低 DTI：年收入 $60,000，贷款 $100,000
        // 月还款额约 $536.82，DTI ≈ 10.74% < 43%
        LoanApplication lowRisk = LoanApplication.builder()
            .id("LA002")
            .customer(testCustomer)
            .requestedAmount(new Money(100_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        assertThat(lowRisk.isHighRiskDTI()).isFalse();
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(-10_000, Currency.USD))
                .termMonths(60)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Requested amount must be positive");
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(0, Currency.USD))
                .termMonths(60)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Requested amount must be positive");
    }

    @Test
    void shouldRejectTermBelowMinimum() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(10_000, Currency.USD))
                .termMonths(0)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Term months must be between");
    }

    @Test
    void shouldRejectTermAboveMaximum() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(200_000, Currency.USD))
                .termMonths(361)  // > 360
                .purpose(LoanPurpose.HOME)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Term months must be between");
    }

    @Test
    void shouldAcceptMinimumTerm() {
        assertThatCode(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(1_000, Currency.USD))
                .termMonths(1)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptMaximumTerm() {
        assertThatCode(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(testCustomer)
                .requestedAmount(new Money(200_000, Currency.USD))
                .termMonths(360)
                .purpose(LoanPurpose.HOME)
                .submittedAt(LocalDateTime.now())
                .build()
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullCustomer() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("LA001")
                .customer(null)
                .requestedAmount(new Money(10_000, Currency.USD))
                .termMonths(60)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Customer cannot be null");
    }

    @Test
    void shouldRejectBlankId() {
        assertThatThrownBy(() ->
            LoanApplication.builder()
                .id("   ")
                .customer(testCustomer)
                .requestedAmount(new Money(10_000, Currency.USD))
                .termMonths(60)
                .purpose(LoanPurpose.PERSONAL)
                .submittedAt(LocalDateTime.now())
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Loan application ID cannot be blank");
    }

    @Test
    void shouldRejectNegativeInterestRate() {
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(100_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() ->
            application.debtToIncomeRatio(new BigDecimal("-0.05"))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Annual interest rate must be non-negative");
    }

    @Test
    void shouldRejectNullInterestRate() {
        LoanApplication application = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(100_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() ->
            application.debtToIncomeRatio(null)
        ).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Annual interest rate cannot be null");
    }

    @Test
    void shouldSerializeAndDeserializeToJson() throws Exception {
        LoanApplication original = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(LocalDateTime.of(2025, 1, 15, 10, 30))
            .build();

        String json = objectMapper.writeValueAsString(original);
        LoanApplication deserialized = objectMapper.readValue(json, LoanApplication.class);

        assertThat(deserialized.id()).isEqualTo(original.id());
        assertThat(deserialized.customer()).isEqualTo(original.customer());
        assertThat(deserialized.requestedAmount()).isEqualTo(original.requestedAmount());
        assertThat(deserialized.termMonths()).isEqualTo(original.termMonths());
        assertThat(deserialized.purpose()).isEqualTo(original.purpose());
        assertThat(deserialized.submittedAt()).isEqualTo(original.submittedAt());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 15, 10, 30);

        LoanApplication app1 = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(timestamp)
            .build();

        LoanApplication app2 = LoanApplication.builder()
            .id("LA001")
            .customer(testCustomer)
            .requestedAmount(new Money(200_000, Currency.USD))
            .termMonths(360)
            .purpose(LoanPurpose.HOME)
            .submittedAt(timestamp)
            .build();

        assertThat(app1).isEqualTo(app2);
        assertThat(app1.hashCode()).isEqualTo(app2.hashCode());
    }
}
