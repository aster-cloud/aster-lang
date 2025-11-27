package editor.converter;

import editor.model.Policy;
import editor.model.PolicyRuleSet;
import editor.util.PolicyNameParser;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * 將 Policy allow/deny 規則轉換為 CNL 代碼。
 * <p>
 * 這是 CoreIRToPolicyConverter 的逆向操作：
 * <ul>
 *   <li>CoreIRToPolicyConverter: CNL → Core IR → Policy</li>
 *   <li>PolicyToCNLConverter: Policy → CNL</li>
 * </ul>
 * <p>
 * 設計原則：優先保留現有 CNL，僅當 policy.cnl 為 null 時從規則生成。
 */
@ApplicationScoped
public class PolicyToCNLConverter {

    /**
     * 資源類型到 Capability 名稱的反向映射。
     * 與 CoreIRToPolicyConverter.CAPABILITY_RESOURCE_MAP 互補。
     */
    private static final Map<String, String> RESOURCE_CAPABILITY_MAP = Map.ofEntries(
            Map.entry("http", "Http"),
            Map.entry("database", "Sql"),
            Map.entry("filesystem", "Files"),
            Map.entry("secrets", "Secrets"),
            Map.entry("ai-model", "AiModel"),
            Map.entry("time", "Time"),
            Map.entry("cpu", "Cpu")
    );

    /**
     * 已知的函數調用前綴集合。
     * 包含大小寫形式，用於識別函數模式（如 Http.get, fetch.get, Db.query）。
     * 與 CoreIRToPolicyConverter.resolveResourceFromCall 中的前綴保持一致。
     */
    private static final Set<String> FUNCTION_PREFIXES = Set.of(
            // 大寫形式（CNL 標準）
            "Http", "Sql", "Db", "Database", "Files", "Fs", "File", "Filesystem",
            "Secrets", "Secret", "AiModel", "Ai", "Time", "Clock", "Cpu",
            // 小寫形式（CoreIRToPolicyConverter 接受的別名）
            "http", "fetch", "sql", "db", "database", "files", "fs", "file", "filesystem",
            "secrets", "secret", "aimodel", "ai", "time", "clock", "cpu"
    );

    /**
     * 將 Policy 轉換為 CNL 代碼。
     * <p>
     * 如果 policy 已有 CNL（非 null），直接返回現有 CNL 以保留原始格式。
     * 否則從 allow/deny 規則生成 CNL 代碼。
     *
     * @param policy 策略對象
     * @return CNL 代碼字符串
     * @throws NullPointerException 如果 policy 為 null
     */
    public String convertToCNL(Policy policy) {
        Objects.requireNonNull(policy, "policy 不能为空");

        // 優先保留現有 CNL
        if (policy.getCnl() != null) {
            return policy.getCnl();
        }

        // 從規則生成 CNL
        return generateCNLFromRules(policy);
    }

    /**
     * 從 Policy 的 allow/deny 規則生成 CNL 代碼。
     */
    private String generateCNLFromRules(Policy policy) {
        List<String> lines = new ArrayList<>();

        // 生成模塊聲明
        String moduleName = extractModuleName(policy.getName());
        lines.add("module " + moduleName);
        lines.add("");

        // 生成 allow 規則
        generateRuleStatements(policy.getAllow(), "allow", lines);

        // 生成 deny 規則
        generateRuleStatements(policy.getDeny(), "deny", lines);

        // 移除尾部空行
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return String.join("\n", lines);
    }

    /**
     * 從策略名稱提取模塊名。
     */
    private String extractModuleName(String policyName) {
        PolicyNameParser.ParseResult result = PolicyNameParser.parse(policyName);
        if (result == null) {
            return "default";
        }
        return result.getModuleName();
    }

    /**
     * 生成規則語句（allow 或 deny）。
     * 使用 TreeMap 確保輸出順序穩定可重複。
     */
    private void generateRuleStatements(PolicyRuleSet ruleSet, String ruleType, List<String> lines) {
        if (ruleSet == null || ruleSet.getRules() == null || ruleSet.getRules().isEmpty()) {
            return;
        }

        // 過濾 null/空白鍵後放入 TreeMap，確保輸出穩定可重複
        // 注意：TreeMap 不允許 null 鍵，必須先過濾
        Map<String, List<String>> sortedRules = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : ruleSet.getRules().entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank()) {
                sortedRules.put(key, entry.getValue());
            }
        }

        for (Map.Entry<String, List<String>> entry : sortedRules.entrySet()) {
            String resource = entry.getKey();
            List<String> patterns = entry.getValue();

            if (patterns == null || patterns.isEmpty()) {
                continue;
            }

            // 跳過 execution 類型（不生成 CNL 規則）
            if ("execution".equals(resource)) {
                continue;
            }

            String capability = resolveCapability(resource);

            for (String pattern : patterns) {
                if (pattern == null || pattern.isBlank()) {
                    continue;
                }
                // 跳過函數名模式（如 Http.get, fetch.get, Sql.query）
                if (isFunctionPattern(pattern)) {
                    continue;
                }
                lines.add(String.format("rule %s %s to \"%s\"", ruleType, capability, escapeString(pattern)));
            }
        }
    }

    /**
     * 解析資源類型對應的 Capability 名稱。
     * 未知資源類型使用首字母大寫。
     */
    private String resolveCapability(String resource) {
        String capability = RESOURCE_CAPABILITY_MAP.get(resource.toLowerCase(Locale.ROOT));
        if (capability != null) {
            return capability;
        }
        // 未知類型：首字母大寫
        if (resource.isEmpty()) {
            return "Unknown";
        }
        return Character.toUpperCase(resource.charAt(0)) + resource.substring(1);
    }

    /**
     * 判斷模式是否為函數名（如 Http.get, fetch.get, Sql.query, db.insert）。
     * 函數名模式不應生成 CNL 規則。
     * <p>
     * 檢測邏輯：前綴必須是已知的 Capability 名稱或別名（大小寫均可）。
     */
    private boolean isFunctionPattern(String pattern) {
        // 函數名通常包含點號
        if (!pattern.contains(".")) {
            return false;
        }
        // URL 或路徑不是函數名
        if (pattern.contains("://") || pattern.startsWith("/")) {
            return false;
        }
        // 通配符不是函數名
        if (pattern.contains("*")) {
            return false;
        }
        // 提取點號前的前綴，檢查是否為已知函數前綴
        int dotIndex = pattern.indexOf('.');
        if (dotIndex > 0 && dotIndex < pattern.length() - 1) {
            String prefix = pattern.substring(0, dotIndex);
            return FUNCTION_PREFIXES.contains(prefix);
        }
        return false;
    }

    /**
     * 轉義字符串中的特殊字符。
     */
    private String escapeString(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
