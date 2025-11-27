package editor.util;

/**
 * 策略名称解析工具类。
 * 解析策略名称（如 "module.submodule.functionName"）为模块和函数部分。
 */
public final class PolicyNameParser {

    private PolicyNameParser() {
        // 工具类禁止实例化
    }

    /**
     * 解析结果。
     */
    public static class ParseResult {
        private final String moduleName;
        private final String functionName;

        public ParseResult(String moduleName, String functionName) {
            this.moduleName = moduleName;
            this.functionName = functionName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getFunctionName() {
            return functionName;
        }
    }

    /**
     * 解析策略名称为模块和函数部分。
     *
     * @param policyName 策略名称
     * @return 解析结果，null 表示输入无效
     */
    public static ParseResult parse(String policyName) {
        if (policyName == null || policyName.isBlank()) {
            return null;
        }
        String trimmed = policyName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.contains(".")) {
            int lastDot = trimmed.lastIndexOf('.');
            String modulePart = trimmed.substring(0, lastDot).trim();
            String funcPart = trimmed.substring(lastDot + 1).trim();
            // 边界检查：确保分割后的部分非空
            if (modulePart.isEmpty()) {
                modulePart = "default";
            }
            if (funcPart.isEmpty()) {
                funcPart = "evaluate";
            }
            return new ParseResult(modulePart, funcPart);
        } else {
            return new ParseResult(trimmed, "evaluate");
        }
    }
}
