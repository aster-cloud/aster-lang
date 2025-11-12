package com.wontlost.aster.finance.types;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 覆盖 Money 值对象的算术、比较与格式化语义，确保算术守护逻辑稳定。
 */
class MoneyTest {

    @Test
    void addAndSubtractRequireSameCurrency() {
        Money base = new Money(new BigDecimal("10.10"), Currency.USD);
        Money delta = new Money(new BigDecimal("2.20"), Currency.USD);

        assertThat(base.add(delta).amount()).isEqualByComparingTo("12.30");
        assertThat(base.subtract(delta).amount()).isEqualByComparingTo("7.90");
    }

    @Test
    void operationsRejectMismatchedCurrency() {
        Money usd = new Money(10, Currency.USD);
        Money eur = new Money(5, Currency.EUR);

        assertThatThrownBy(() -> usd.add(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class);
        assertThatThrownBy(() -> usd.subtract(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class);
        assertThatThrownBy(() -> usd.compareTo(eur))
            .isInstanceOf(Money.CurrencyMismatchException.class);
    }

    @Test
    void multiplyDivideAndFlagsWorkTogether() {
        Money money = new Money(new BigDecimal("10.00"), Currency.USD);
        Money multiplied = money.multiply(1.333);
        Money divided = money.divide(3);
        Money negative = money.negate();

        assertThat(multiplied.amount()).isEqualByComparingTo("13.33");
        assertThat(divided.amount()).isEqualByComparingTo("3.33");
        assertThat(money.compareTo(divided)).isGreaterThan(0);
        assertThat(money.isPositive()).isTrue();
        assertThat(negative.isNegative()).isTrue();
        assertThat(negative.abs().amount()).isEqualByComparingTo("10.00");
        assertThat(money.format()).isEqualTo("$10.00");
        assertThat(money.toString()).isEqualTo("$10.00");
    }

    @Test
    void divideByZeroIsGuarded() {
        Money money = new Money(5, Currency.USD);
        assertThatThrownBy(() -> money.divide(0))
            .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void roundingFollowsCurrencyPrecision() {
        Money jpy = new Money(new BigDecimal("1234.9"), Currency.JPY);
        assertThat(jpy.amount()).isEqualByComparingTo("1235"); // 日元无小数
        assertThat(jpy.toString()).isEqualTo("¥1235");
    }
}
