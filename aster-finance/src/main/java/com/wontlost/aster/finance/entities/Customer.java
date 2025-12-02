package com.wontlost.aster.finance.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wontlost.aster.finance.types.CreditScore;
import com.wontlost.aster.finance.types.Money;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * 客户实体 - 包含客户基本信息和财务状况
 */
public record Customer(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("dateOfBirth") LocalDate dateOfBirth,
    @JsonProperty("creditScore") CreditScore creditScore,
    @JsonProperty("annualIncome") Money annualIncome
) {
    /**
     * 最小年龄要求（成年）
     */
    public static final int MIN_AGE = 18;

    /**
     * 构造函数 - 验证参数
     */
    @JsonCreator
    public Customer {
        Objects.requireNonNull(id, "Customer ID cannot be null");
        Objects.requireNonNull(name, "Customer name cannot be null");
        Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null");
        Objects.requireNonNull(creditScore, "Credit score cannot be null");
        Objects.requireNonNull(annualIncome, "Annual income cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be blank");
        }

        // 验证年龄
        int age = calculateAge(dateOfBirth);
        if (age < MIN_AGE) {
            throw new IllegalArgumentException(
                String.format("Customer must be at least %d years old, current age: %d", MIN_AGE, age)
            );
        }

        // 验证收入为正数
        if (!annualIncome.isPositive()) {
            throw new IllegalArgumentException("Annual income must be positive");
        }
    }

    /**
     * 计算年龄（基于出生日期）
     */
    public int age() {
        return calculateAge(this.dateOfBirth);
    }

    /**
     * 静态方法：计算年龄
     */
    private static int calculateAge(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * 是否为高收入客户（年收入 > $100,000）
     */
    public boolean isHighIncome() {
        return annualIncome.compareTo(new Money(100_000, annualIncome.currency())) > 0;
    }

    /**
     * 是否为优质客户（高收入 + 优秀信用评分）
     */
    public boolean isPremiumCustomer() {
        return isHighIncome() && creditScore.isExcellent();
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private String id;
        private String name;
        private LocalDate dateOfBirth;
        private CreditScore creditScore;
        private Money annualIncome;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder creditScore(CreditScore creditScore) {
            this.creditScore = creditScore;
            return this;
        }

        public Builder annualIncome(Money annualIncome) {
            this.annualIncome = annualIncome;
            return this;
        }

        public Customer build() {
            return new Customer(id, name, dateOfBirth, creditScore, annualIncome);
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
