package editor.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wontlost.aster.policy.PolicySerializer;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * 將 Core IR JSON 轉換為 Policy/PolicyRuleSet 的輔助工具。
 * <p>
 * 解析流程：
 * <ol>
 *   <li>必要時將 CNL 轉為 Core IR JSON（透過 PolicySerializer）。</li>
 *   <li>校驗 JSON 結構：version/module/decls。</li>
 *   <li>遍歷函數宣告，根據 effectCaps 與 Call 節點推導 allow/deny。</li>
 * </ol>
 */
@ApplicationScoped
public class CoreIRToPolicyConverter {
    private static final Map<String, String> CAPABILITY_RESOURCE_MAP = Map.ofEntries(
            Map.entry("Http", "http"),
            Map.entry("Sql", "database"),
            Map.entry("Files", "filesystem"),
            Map.entry("Secrets", "secrets"),
            Map.entry("AiModel", "ai-model"),
            Map.entry("Time", "time"),
            Map.entry("Cpu", "cpu")
    );

    private final ObjectMapper objectMapper;
    private final PolicySerializer policySerializer;

    public CoreIRToPolicyConverter() {
        this(new ObjectMapper(), new PolicySerializer());
    }

    CoreIRToPolicyConverter(ObjectMapper objectMapper, PolicySerializer serializer) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper 不能为空");
        this.policySerializer = Objects.requireNonNull(serializer, "policySerializer 不能为空");
    }

    /**
     * 從 Core IR JSON 中提取 allow 規則。
     */
    public PolicyRuleSet extractAllowRules(String coreIrSource) throws ConversionException {
        JsonNode module = parseModule(coreIrSource);
        return extractAllowRules(module);
    }

    /**
     * 從 Core IR JSON 中提取 deny 規則。
     */
    public PolicyRuleSet extractDenyRules(String coreIrSource) throws ConversionException {
        JsonNode module = parseModule(coreIrSource);
        return extractDenyRules(module);
    }

    /**
     * 將 CNL 轉換為完整 Policy 對象。
     */
    public Policy convertCNLToPolicy(String cnl, String policyId, String policyName) throws ConversionException {
        if (policyId == null || policyId.isBlank()) {
            throw new ConversionException("policyId 不能为空");
        }
        if (policyName == null || policyName.isBlank()) {
            throw new ConversionException("policyName 不能为空");
        }
        JsonNode module = parseModule(cnl);
        PolicyRuleSet allow = extractAllowRules(module);
        PolicyRuleSet deny = extractDenyRules(module);
        return new Policy(policyId, policyName, allow, deny, cnl);
    }

    private JsonNode parseModule(String source) throws ConversionException {
        if (source == null || source.isBlank()) {
            throw new ConversionException("Core IR 內容不能为空");
        }
        String json = toCoreIrJson(source);
        final JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ConversionException("無法解析 Core IR JSON", e);
        }
        JsonNode version = root.path("version");
        if (!"1.0".equals(version.asText(null))) {
            throw new ConversionException("Core IR 版本不受支援: " + version.asText());
        }
        JsonNode module = root.path("module");
        if (!module.isObject()) {
            throw new ConversionException("Core IR 缺少 module 節點");
        }
        if (!"Module".equals(module.path("kind").asText(null))) {
            throw new ConversionException("module.kind 必須為 Module");
        }
        if (!module.path("decls").isArray()) {
            throw new ConversionException("module.decls 必須為陣列");
        }
        return module;
    }

    private String toCoreIrJson(String source) throws ConversionException {
        String trimmed = source.stripLeading();
        if (trimmed.startsWith("{")) {
            return source;
        }
        try {
            Object ir = policySerializer.fromCNL(source, Object.class);
            return policySerializer.toJson(ir);
        } catch (RuntimeException ex) {
            throw new ConversionException("CNL 轉 Core IR 失敗", ex);
        }
    }

    private PolicyRuleSet extractAllowRules(JsonNode module) {
        Map<String, Set<String>> accumulator = new LinkedHashMap<>();
        for (JsonNode decl : module.path("decls")) {
            if (!"Func".equals(decl.path("kind").asText())) {
                continue;
            }
            String funcName = decl.path("name").asText(null);
            if (funcName == null || funcName.isBlank()) {
                continue;
            }
            addPattern(accumulator, "execution", funcName);
            collectCapabilities(decl.path("effectCaps"), funcName, accumulator);
            JsonNode body = decl.path("body");
            walkCalls(body, call -> processAllowCall(call, accumulator));
        }
        return new PolicyRuleSet(toListMap(accumulator));
    }

    private PolicyRuleSet extractDenyRules(JsonNode module) {
        Map<String, Set<String>> accumulator = new LinkedHashMap<>();
        for (JsonNode decl : module.path("decls")) {
            if (!"Func".equals(decl.path("kind").asText())) {
                continue;
            }
            walkCalls(decl.path("body"), call -> processDenyCall(call, accumulator));
        }
        return new PolicyRuleSet(toListMap(accumulator));
    }

    private void collectCapabilities(JsonNode effectCapsNode, String funcName, Map<String, Set<String>> accumulator) {
        // 注意：effectCaps 只表明函数声明了哪些能力，不代表实际调用
        // 实际的资源访问模式应该从 Call 节点中提取，而非从 effectCaps
        // 因此这里不再将 funcName 添加到资源列表中
    }

    private void processAllowCall(JsonNode callNode, Map<String, Set<String>> accumulator) {
        String targetName = callNode.path("target").path("name").asText(null);
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        if (isDenyTarget(targetName)) {
            // 避免 deny 語句被計入 allow
            return;
        }
        String resource = resolveResourceFromCall(targetName);
        if (resource == null) {
            return;
        }
        addPattern(accumulator, resource, targetName);
        if (requiresLiteralPattern(resource)) {
            extractFirstStringArg(callNode).ifPresent(value -> addPattern(accumulator, resource, value));
        }
    }

    private void processDenyCall(JsonNode callNode, Map<String, Set<String>> accumulator) {
        String targetName = callNode.path("target").path("name").asText(null);
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        DenyDescriptor descriptor = resolveDenyDescriptor(targetName, callNode);
        if (descriptor == null) {
            return;
        }
        addPattern(accumulator, descriptor.resource(), descriptor.pattern());
    }

    private void walkCalls(JsonNode root, java.util.function.Consumer<JsonNode> consumer) {
        if (root == null || root.isMissingNode()) {
            return;
        }
        Queue<JsonNode> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            JsonNode current = queue.remove();
            if (current.isObject()) {
                if ("Call".equals(current.path("kind").asText(null))) {
                    consumer.accept(current);
                }
                current.fields().forEachRemaining(entry -> queue.add(entry.getValue()));
            } else if (current.isArray()) {
                current.forEach(queue::add);
            }
        }
    }

    private boolean isDenyTarget(String targetName) {
        String lower = targetName.toLowerCase(Locale.ROOT);
        return lower.startsWith("deny") || lower.contains(".deny");
    }

    private String resolveResourceFromCall(String targetName) {
        String normalized = targetName.trim();
        int dotIndex = normalized.indexOf('.');
        String prefix = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return switch (lowered) {
            case "http", "fetch" -> "http";
            case "db", "sql", "database" -> "database";
            case "files", "fs", "file", "filesystem" -> "filesystem";
            case "secrets", "secret" -> "secrets";
            case "aimodel", "ai" -> "ai-model";
            case "time", "clock" -> "time";
            case "cpu" -> "cpu";
            default -> null;
        };
    }

    private boolean requiresLiteralPattern(String resource) {
        return "http".equals(resource) || "database".equals(resource) || "filesystem".equals(resource);
    }

    private Optional<String> extractFirstStringArg(JsonNode callNode) {
        JsonNode argsNode = callNode.path("args");
        if (!argsNode.isArray()) {
            return Optional.empty();
        }
        for (JsonNode arg : argsNode) {
            if ("String".equals(arg.path("kind").asText(null))) {
                String value = arg.path("value").asText(null);
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    private DenyDescriptor resolveDenyDescriptor(String targetName, JsonNode callNode) {
        String lower = targetName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("deny.")) {
            String resource = targetName.substring(targetName.indexOf('.') + 1);
            String pattern = extractFirstStringArg(callNode).orElse("*");
            return new DenyDescriptor(resource, pattern);
        }
        if (lower.equals("deny")) {
            String pattern = extractFirstStringArg(callNode).orElse("*");
            return new DenyDescriptor("execution", pattern);
        }
        int idx = lower.indexOf(".deny");
        if (idx >= 0) {
            String resource = targetName.substring(idx + 5);
            if (resource.isBlank() && idx > 0) {
                resource = targetName.substring(0, idx);
            }
            if (resource.isBlank()) {
                resource = "execution";
            }
            String pattern = extractFirstStringArg(callNode).orElse("*");
            return new DenyDescriptor(resource, pattern);
        }
        return null;
    }

    private void addPattern(Map<String, Set<String>> accumulator, String resource, String pattern) {
        if (resource == null || resource.isBlank() || pattern == null || pattern.isBlank()) {
            return;
        }
        accumulator.computeIfAbsent(resource, ignored -> new LinkedHashSet<>()).add(pattern);
    }

    private Map<String, List<String>> toListMap(Map<String, Set<String>> accumulator) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        accumulator.forEach((key, value) -> {
            if (!value.isEmpty()) {
                result.put(key, new ArrayList<>(value));
            }
        });
        return result;
    }

    private record DenyDescriptor(String resource, String pattern) {}

    public static class ConversionException extends Exception {
        private static final long serialVersionUID = 1L;

        public ConversionException(String message) {
            super(message);
        }

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
