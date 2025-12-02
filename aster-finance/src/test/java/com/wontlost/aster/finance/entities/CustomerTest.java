package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Currency;
import com.wontlost.aster.finance.types.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Customer 实体单元测试
 */
class CustomerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void shouldCreateValidCustomer() {
        Customer customer = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        assertThat(customer.id()).isEqualTo("C001");
        assertThat(customer.name()).isEqualTo("张三");
        assertThat(customer.creditScore().value()).isEqualTo(750);
        assertThat(customer.annualIncome().amount()).isEqualByComparingTo("80000.00");
    }

    @Test
    void shouldCalculateAgeCorrectly() {
        Customer customer = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.now().minusYears(30))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        assertThat(customer.age()).isEqualTo(30);
    }

    @Test
    void shouldIdentifyHighIncomeCustomer() {
        Customer highIncome = Customer.builder()
            .id("C001")
            .name("高收入客户")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(150_000, Currency.USD))
            .build();

        assertThat(highIncome.isHighIncome()).isTrue();

        Customer normalIncome = Customer.builder()
            .id("C002")
            .name("普通收入客户")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        assertThat(normalIncome.isHighIncome()).isFalse();
    }

    @Test
    void shouldIdentifyPremiumCustomer() {
        Customer premium = Customer.builder()
            .id("C001")
            .name("优质客户")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(820))  // Excellent
            .annualIncome(new Money(150_000, Currency.USD))  // High income
            .build();

        assertThat(premium.isPremiumCustomer()).isTrue();

        Customer notPremium = Customer.builder()
            .id("C002")
            .name("非优质客户")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .creditScore(new CreditScore(700))  // Not excellent
            .annualIncome(new Money(150_000, Currency.USD))
            .build();

        assertThat(notPremium.isPremiumCustomer()).isFalse();
    }

    @Test
    void shouldRejectUnderageCustomer() {
        assertThatThrownBy(() ->
            Customer.builder()
                .id("C001")
                .name("未成年客户")
                .dateOfBirth(LocalDate.now().minusYears(17))  // 17 years old
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(50_000, Currency.USD))
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be at least 18 years old");
    }

    @Test
    void shouldAcceptExactly18YearsOld() {
        assertThatCode(() ->
            Customer.builder()
                .id("C001")
                .name("18岁客户")
                .dateOfBirth(LocalDate.now().minusYears(18))
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(50_000, Currency.USD))
                .build()
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNegativeIncome() {
        assertThatThrownBy(() ->
            Customer.builder()
                .id("C001")
                .name("负收入客户")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(-10_000, Currency.USD))
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Annual income must be positive");
    }

    @Test
    void shouldRejectZeroIncome() {
        assertThatThrownBy(() ->
            Customer.builder()
                .id("C001")
                .name("零收入客户")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(0, Currency.USD))
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Annual income must be positive");
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() ->
            Customer.builder()
                .id(null)
                .name("客户")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(80_000, Currency.USD))
                .build()
        ).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Customer ID cannot be null");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() ->
            Customer.builder()
                .id("C001")
                .name("   ")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .creditScore(new CreditScore(750))
                .annualIncome(new Money(80_000, Currency.USD))
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer name cannot be blank");
    }

    @Test
    void shouldSerializeAndDeserializeToJson() throws Exception {
        Customer original = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        String json = objectMapper.writeValueAsString(original);
        Customer deserialized = objectMapper.readValue(json, Customer.class);

        assertThat(deserialized.id()).isEqualTo(original.id());
        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.dateOfBirth()).isEqualTo(original.dateOfBirth());
        assertThat(deserialized.creditScore()).isEqualTo(original.creditScore());
        assertThat(deserialized.annualIncome()).isEqualTo(original.annualIncome());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Customer customer1 = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        Customer customer2 = Customer.builder()
            .id("C001")
            .name("张三")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .creditScore(new CreditScore(750))
            .annualIncome(new Money(80_000, Currency.USD))
            .build();

        assertThat(customer1).isEqualTo(customer2);
        assertThat(customer1.hashCode()).isEqualTo(customer2.hashCode());
    }
}
