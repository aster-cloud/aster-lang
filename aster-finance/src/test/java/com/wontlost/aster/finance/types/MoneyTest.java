package com.wontlost.aster.finance.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateMoneyWithBigDecimal() {
        Money money = new Money(new BigDecimal("100.50"), Currency.USD);

        assertThat(money.amount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldCreateMoneyWithDouble() {
        Money money = new Money(100.50, Currency.USD);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldCreateMoneyWithLong() {
        Money money = new Money(100L, Currency.USD);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(money.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldRoundToCorrectDecimalPlaces() {
        Money usd = new Money(new BigDecimal("100.123"), Currency.USD);
        Money jpy = new Money(new BigDecimal("100.123"), Currency.JPY);

        assertThat(usd.amount()).isEqualByComparingTo(new BigDecimal("100.12"));
        assertThat(jpy.amount()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void shouldAddSameCurrency() {
        Money m1 = new Money(100.00, Currency.USD);
        Money m2 = new Money(50.50, Currency.USD);

        Money result = m1.add(m2);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money usd = new Money(100.00, Currency.USD);
        Money eur = new Money(50.00, Currency.EUR);

        assertThatThrownBy(() -> usd.add(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class)
            .hasMessageContaining("Cannot operate on different currencies");
    }

    @Test
    void shouldSubtractSameCurrency() {
        Money m1 = new Money(100.00, Currency.USD);
        Money m2 = new Money(30.50, Currency.USD);

        Money result = m1.subtract(m2);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("69.50"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
        Money usd = new Money(100.00, Currency.USD);
        Money eur = new Money(50.00, Currency.EUR);

        assertThatThrownBy(() -> usd.subtract(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class);
    }

    @Test
    void shouldMultiplyByDouble() {
        Money money = new Money(100.00, Currency.USD);

        Money result = money.multiply(1.5);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldMultiplyByBigDecimal() {
        Money money = new Money(100.00, Currency.USD);

        Money result = money.multiply(new BigDecimal("2.5"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldDivideByDouble() {
        Money money = new Money(100.00, Currency.USD);

        Money result = money.divide(2.0);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldDivideByBigDecimal() {
        Money money = new Money(100.00, Currency.USD);

        Money result = money.divide(new BigDecimal("4.0"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldThrowExceptionWhenDividingByZero() {
        Money money = new Money(100.00, Currency.USD);

        assertThatThrownBy(() -> money.divide(0.0))
            .isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("Cannot divide by zero");

        assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
            .isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("Cannot divide by zero");
    }

    @Test
    void shouldCalculateAbsoluteValue() {
        Money negative = new Money(-100.00, Currency.USD);

        Money result = negative.abs();

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.isPositive()).isTrue();
    }

    @Test
    void shouldNegate() {
        Money positive = new Money(100.00, Currency.USD);

        Money result = positive.negate();

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("-100.00"));
        assertThat(result.isNegative()).isTrue();
    }

    @Test
    void shouldCompareSameCurrency() {
        Money m1 = new Money(100.00, Currency.USD);
        Money m2 = new Money(50.00, Currency.USD);
        Money m3 = new Money(100.00, Currency.USD);

        assertThat(m1.compareTo(m2)).isPositive();
        assertThat(m2.compareTo(m1)).isNegative();
        assertThat(m1.compareTo(m3)).isZero();
    }

    @Test
    void shouldThrowExceptionWhenComparingDifferentCurrencies() {
        Money usd = new Money(100.00, Currency.USD);
        Money eur = new Money(100.00, Currency.EUR);

        assertThatThrownBy(() -> usd.compareTo(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class);
    }

    @Test
    void shouldCheckIfPositive() {
        assertThat(new Money(100.00, Currency.USD).isPositive()).isTrue();
        assertThat(new Money(0.00, Currency.USD).isPositive()).isFalse();
        assertThat(new Money(-100.00, Currency.USD).isPositive()).isFalse();
    }

    @Test
    void shouldCheckIfNegative() {
        assertThat(new Money(-100.00, Currency.USD).isNegative()).isTrue();
        assertThat(new Money(0.00, Currency.USD).isNegative()).isFalse();
        assertThat(new Money(100.00, Currency.USD).isNegative()).isFalse();
    }

    @Test
    void shouldCheckIfZero() {
        assertThat(new Money(0.00, Currency.USD).isZero()).isTrue();
        assertThat(new Money(0.01, Currency.USD).isZero()).isFalse();
        assertThat(new Money(-0.01, Currency.USD).isZero()).isFalse();
    }

    @Test
    void shouldFormatCorrectly() {
        assertThat(new Money(100.50, Currency.USD).format()).isEqualTo("$100.50");
        assertThat(new Money(100.50, Currency.EUR).format()).isEqualTo("€100.50");
        assertThat(new Money(100.50, Currency.JPY).format()).isEqualTo("¥101");
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        Money money = new Money(100.50, Currency.USD);

        String json = objectMapper.writeValueAsString(money);

        assertThat(json).contains("\"amount\"");
        assertThat(json).contains("100.5");
        assertThat(json).contains("\"currency\"");
        assertThat(json).contains("\"USD\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"amount\":100.50,\"currency\":\"USD\"}";

        Money money = objectMapper.readValue(json, Money.class);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNull() {
        assertThatThrownBy(() -> new Money(null, Currency.USD))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Amount cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenCurrencyIsNull() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Currency cannot be null");
    }
}
