package com.wontlost.aster.finance.types;

/**
 * 货币枚举 - 支持主要国际货币
 * 符合 ISO 4217 标准
 */
public enum Currency {
    USD("$", 2, "美元"),
    EUR("€", 2, "欧元"),
    GBP("£", 2, "英镑"),
    JPY("¥", 0, "日元"),
    CNY("¥", 2, "人民币");

    private final String symbol;
    private final int decimalPlaces;
    private final String displayName;

    Currency(String symbol, int decimalPlaces, String displayName) {
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.displayName = displayName;
    }

    /**
     * 获取货币符号
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * 获取小数位数（日元为 0，其他为 2）
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 格式化金额显示
     * 例如：USD 123.45 -> "$123.45"
     */
    public String format(double amount) {
        if (decimalPlaces == 0) {
            return symbol + String.format("%.0f", amount);
        } else {
            return symbol + String.format("%." + decimalPlaces + "f", amount);
        }
    }
}
