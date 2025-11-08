package com.wontlost.aster.finance.types;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 货币金额类型 - 不可变值对象
 * 使用 BigDecimal 确保精度，支持货币运算
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public record Money(
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") Currency currency
) {
    /**
     * 构造函数 - 验证参数并规范化金额
     */
    @JsonCreator
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        // 规范化金额：根据货币的小数位数进行四舍五入
        amount = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    /**
     * 便捷构造函数 - 从 double 创建
     */
    public Money(double amount, Currency currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    /**
     * 便捷构造函数 - 从 long 创建（整数金额）
     */
    public Money(long amount, Currency currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    /**
     * 加法 - 相同货币才能相加
     * @throws CurrencyMismatchException 货币不一致时抛出
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 减法 - 相同货币才能相减
     * @throws CurrencyMismatchException 货币不一致时抛出
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * 乘法 - 乘以数值
     */
    public Money multiply(double multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * 乘法 - 乘以 BigDecimal
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * 除法 - 除以数值
     * @throws ArithmeticException 除数为 0 时抛出
     */
    public Money divide(double divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), currency.getDecimalPlaces(), RoundingMode.HALF_UP), this.currency);
    }

    /**
     * 除法 - 除以 BigDecimal
     * @throws ArithmeticException 除数为 0 时抛出
     */
    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, currency.getDecimalPlaces(), RoundingMode.HALF_UP), this.currency);
    }

    /**
     * 取绝对值
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    /**
     * 取反
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    /**
     * 比较大小
     * @return 负数表示小于，0 表示相等，正数表示大于
     * @throws CurrencyMismatchException 货币不一致时抛出
     */
    public int compareTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    /**
     * 是否为正数
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 是否为负数
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 是否为零
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 验证货币一致性
     */
    private void validateSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new CurrencyMismatchException(
                String.format("Cannot operate on different currencies: %s and %s",
                    this.currency, other.currency)
            );
        }
    }

    /**
     * 格式化显示
     * 例如：USD 123.45 -> "$123.45"
     */
    public String format() {
        return currency.format(amount.doubleValue());
    }

    @Override
    public String toString() {
        return format();
    }

    /**
     * 货币不一致异常
     */
    public static class CurrencyMismatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CurrencyMismatchException(String message) {
            super(message);
        }
    }
}
