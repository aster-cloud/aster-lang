package com.wontlost.aster.finance.types;

/**
 * 风险等级枚举 - 用于贷款审批和风险评估
 */
public enum RiskLevel {
    LOW("低风险", 0, 1.0),
    MEDIUM("中等风险", 1, 1.5),
    HIGH("高风险", 2, 2.0),
    CRITICAL("极高风险", 3, 3.0);

    private final String displayName;
    private final int severityLevel;
    private final double riskMultiplier;

    RiskLevel(String displayName, int severityLevel, double riskMultiplier) {
        this.displayName = displayName;
        this.severityLevel = severityLevel;
        this.riskMultiplier = riskMultiplier;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取严重程度级别（0-3）
     */
    public int getSeverityLevel() {
        return severityLevel;
    }

    /**
     * 获取风险乘数（用于利率调整）
     */
    public double getRiskMultiplier() {
        return riskMultiplier;
    }

    /**
     * 是否为高风险（HIGH 或 CRITICAL）
     */
    public boolean isHighRisk() {
        return this == HIGH || this == CRITICAL;
    }

    /**
     * 是否为低风险
     */
    public boolean isLowRisk() {
        return this == LOW;
    }

    /**
     * 是否可接受（LOW 或 MEDIUM）
     */
    public boolean isAcceptable() {
        return this == LOW || this == MEDIUM;
    }

    /**
     * 根据严重程度比较风险等级
     */
    public boolean isMoreSevereThan(RiskLevel other) {
        return this.severityLevel > other.severityLevel;
    }

    /**
     * 根据严重程度比较风险等级
     */
    public boolean isLessSevereThan(RiskLevel other) {
        return this.severityLevel < other.severityLevel;
    }
}
