package com.wontlost.aster.finance.entities;

/**
 * 贷款用途枚举 - 避免字符串错误
 */
public enum LoanPurpose {
    HOME("购房贷款"),
    AUTO("购车贷款"),
    PERSONAL("个人消费贷款"),
    BUSINESS("商业贷款"),
    EDUCATION("教育贷款"),
    MEDICAL("医疗贷款"),
    DEBT_CONSOLIDATION("债务整合贷款");

    private final String displayName;

    LoanPurpose(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
