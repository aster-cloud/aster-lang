package com.wontlost.aster.finance.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyTest {

    @Test
    void shouldHaveCorrectSymbol() {
        assertThat(Currency.USD.getSymbol()).isEqualTo("$");
        assertThat(Currency.EUR.getSymbol()).isEqualTo("€");
        assertThat(Currency.GBP.getSymbol()).isEqualTo("£");
        assertThat(Currency.JPY.getSymbol()).isEqualTo("¥");
        assertThat(Currency.CNY.getSymbol()).isEqualTo("¥");
    }

    @Test
    void shouldHaveCorrectDecimalPlaces() {
        assertThat(Currency.USD.getDecimalPlaces()).isEqualTo(2);
        assertThat(Currency.EUR.getDecimalPlaces()).isEqualTo(2);
        assertThat(Currency.GBP.getDecimalPlaces()).isEqualTo(2);
        assertThat(Currency.JPY.getDecimalPlaces()).isEqualTo(0);
        assertThat(Currency.CNY.getDecimalPlaces()).isEqualTo(2);
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(Currency.USD.getDisplayName()).isEqualTo("美元");
        assertThat(Currency.EUR.getDisplayName()).isEqualTo("欧元");
        assertThat(Currency.GBP.getDisplayName()).isEqualTo("英镑");
        assertThat(Currency.JPY.getDisplayName()).isEqualTo("日元");
        assertThat(Currency.CNY.getDisplayName()).isEqualTo("人民币");
    }

    @Test
    void shouldFormatWithDecimalPlaces() {
        assertThat(Currency.USD.format(123.45)).isEqualTo("$123.45");
        assertThat(Currency.EUR.format(123.45)).isEqualTo("€123.45");
        assertThat(Currency.GBP.format(123.45)).isEqualTo("£123.45");
        assertThat(Currency.CNY.format(123.45)).isEqualTo("¥123.45");
    }

    @Test
    void shouldFormatWithoutDecimalPlaces() {
        assertThat(Currency.JPY.format(123.45)).isEqualTo("¥123");
        assertThat(Currency.JPY.format(123.99)).isEqualTo("¥124");
    }

    @Test
    void shouldFormatNegativeValues() {
        assertThat(Currency.USD.format(-123.45)).isEqualTo("$-123.45");
        assertThat(Currency.JPY.format(-123.45)).isEqualTo("¥-123");
    }

    @Test
    void shouldFormatZero() {
        assertThat(Currency.USD.format(0.0)).isEqualTo("$0.00");
        assertThat(Currency.JPY.format(0.0)).isEqualTo("¥0");
    }
}
