package com.wontlost.aster.finance.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 信用评分类型 - 符合 FICO 标准（300-850）
 * 不可变值对象，确保评分有效性
 */
public record CreditScore(@JsonValue int value) {
    /**
     * FICO 标准评分范围
     */
    public static final int MIN_SCORE = 300;
    public static final int MAX_SCORE = 850;

    /**
     * 评分等级阈值
     */
    private static final int EXCELLENT_THRESHOLD = 800;
    private static final int VERY_GOOD_THRESHOLD = 740;
    private static final int GOOD_THRESHOLD = 670;
    private static final int FAIR_THRESHOLD = 580;

    /**
     * 构造函数 - 验证评分范围
     * @throws IllegalArgumentException 评分超出范围时抛出
     */
    @JsonCreator
    public CreditScore {
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new IllegalArgumentException(
                String.format("Credit score must be between %d and %d, got: %d",
                    MIN_SCORE, MAX_SCORE, value)
            );
        }
    }

    /**
     * 是否为优秀评分（≥800）
     */
    public boolean isExcellent() {
        return value >= EXCELLENT_THRESHOLD;
    }

    /**
     * 是否为非常好（≥740）
     */
    public boolean isVeryGood() {
        return value >= VERY_GOOD_THRESHOLD;
    }

    /**
     * 是否为良好（≥670）
     */
    public boolean isGood() {
        return value >= GOOD_THRESHOLD;
    }

    /**
     * 是否为一般（≥580）
     */
    public boolean isFair() {
        return value >= FAIR_THRESHOLD;
    }

    /**
     * 是否为较差（<580）
     */
    public boolean isPoor() {
        return value < FAIR_THRESHOLD;
    }

    /**
     * 获取评分等级描述
     */
    public String getRating() {
        if (isExcellent()) {
            return "优秀";
        } else if (isVeryGood()) {
            return "非常好";
        } else if (isGood()) {
            return "良好";
        } else if (isFair()) {
            return "一般";
        } else {
            return "较差";
        }
    }

    /**
     * 获取英文评分等级
     */
    public String getRatingEnglish() {
        if (isExcellent()) {
            return "Excellent";
        } else if (isVeryGood()) {
            return "Very Good";
        } else if (isGood()) {
            return "Good";
        } else if (isFair()) {
            return "Fair";
        } else {
            return "Poor";
        }
    }

    /**
     * 计算与目标评分的差距
     */
    public int gapTo(CreditScore target) {
        return target.value - this.value;
    }

    /**
     * 比较评分
     */
    public int compareTo(CreditScore other) {
        return Integer.compare(this.value, other.value);
    }

    /**
     * 是否高于指定评分
     */
    public boolean isHigherThan(CreditScore other) {
        return this.value > other.value;
    }

    /**
     * 是否低于指定评分
     */
    public boolean isLowerThan(CreditScore other) {
        return this.value < other.value;
    }

    /**
     * 根据信用评分推荐风险等级
     */
    public RiskLevel recommendRiskLevel() {
        if (isExcellent() || isVeryGood()) {
            return RiskLevel.LOW;
        } else if (isGood()) {
            return RiskLevel.MEDIUM;
        } else if (isFair()) {
            return RiskLevel.HIGH;
        } else {
            return RiskLevel.CRITICAL;
        }
    }

    @Override
    public String toString() {
        return value + " (" + getRating() + ")";
    }
}
