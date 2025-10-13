package editor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import editor.graphql.GraphQLClient;
import editor.model.Policy;
import editor.model.PolicyRuleSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 策略管理服务，使用 GraphQL API 执行 CRUD 操作。
 */
@ApplicationScoped
public class PolicyService {

    private static final String POLICY_SELECTION = """
        id
        name
        allow { rules { resourceType patterns } }
        deny { rules { resourceType patterns } }
        """;

    private static final String LIST_POLICIES_QUERY = """
        query ListPolicies {
          listPolicies {
            %s
          }
        }
        """.formatted(POLICY_SELECTION);

    private static final String GET_POLICY_QUERY = """
        query GetPolicy($id: String!) {
          getPolicy(id: $id) {
            %s
          }
        }
        """.formatted(POLICY_SELECTION);

    private static final String CREATE_POLICY_MUTATION = """
        mutation CreatePolicy($input: PolicyInput!) {
          createPolicy(input: $input) {
            %s
          }
        }
        """.formatted(POLICY_SELECTION);

    private static final String UPDATE_POLICY_MUTATION = """
        mutation UpdatePolicy($id: String!, $input: PolicyInput!) {
          updatePolicy(id: $id, input: $input) {
            %s
          }
        }
        """.formatted(POLICY_SELECTION);

    private static final String DELETE_POLICY_MUTATION = """
        mutation DeletePolicy($id: String!) {
          deletePolicy(id: $id)
        }
        """;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AuditService auditService;

    @Inject
    HistoryService historyService;

    @Inject
    AuthService authService;

    @Inject
    RequestContextService requestContext;

    @ConfigProperty(name = "policy.api.graphql.url")
    String graphqlEndpoint;

    @ConfigProperty(name = "policy.api.graphql.timeout", defaultValue = "5000")
    int graphqlTimeoutMillis;

    @ConfigProperty(name = "policy.api.graphql.compression", defaultValue = "true")
    boolean graphqlCompression;

    @ConfigProperty(name = "policy.api.graphql.cache-ttl", defaultValue = "0")
    int graphqlCacheTtlMillis;

    private volatile GraphQLClient graphQLClient;

    /**
     * 获取所有策略。
     */
    public List<Policy> getAllPolicies() {
        JsonNode listNode = executeGraphQL(LIST_POLICIES_QUERY, Map.of()).path("listPolicies");
        List<Policy> policies = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode node : listNode) {
                policies.add(parsePolicy(node));
            }
        }
        return policies;
    }

    /**
     * 根据 ID 获取策略。
     */
    public Optional<Policy> getPolicyById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> variables = Map.of("id", id);
        JsonNode node = executeGraphQL(GET_POLICY_QUERY, variables).path("getPolicy");
        if (node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(parsePolicy(node));
    }

    /**
     * 创建策略（带审计与历史记录）。
     */
    public Policy createPolicy(Policy policy) {
        Policy created = doCreatePolicy(policy);
        historyService.snapshot(created);
        recordAudit("create", created.getId(), created.getName());
        return created;
    }

    /**
     * 更新策略（带审计与历史记录）。
     */
    public Optional<Policy> updatePolicy(String id, Policy policy) {
        Optional<Policy> updated = doUpdatePolicy(id, policy);
        updated.ifPresent(p -> {
            historyService.snapshot(p);
            recordAudit("update", p.getId(), p.getName());
        });
        return updated;
    }

    /**
     * 删除策略。
     */
    public boolean deletePolicy(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Map<String, Object> variables = Map.of("id", id);
        boolean deleted = executeGraphQL(DELETE_POLICY_MUTATION, variables)
            .path("deletePolicy").asBoolean(false);
        if (deleted) {
            recordAudit("delete", id, "");
        }
        return deleted;
    }

    /**
     * 导出所有策略为 ZIP。
     */
    public Path exportZip(Path targetZip) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            for (Policy policy : getAllPolicies()) {
                ZipEntry entry = new ZipEntry(policy.getId() + ".json");
                zos.putNextEntry(entry);
                byte[] data = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(policy);
                zos.write(data);
                zos.closeEntry();
            }
            recordAudit("export", "all", targetZip.toString());
            return targetZip;
        } catch (IOException e) {
            throw new RuntimeException("导出ZIP失败", e);
        }
    }

    /**
     * 从 ZIP 输入流导入策略。
     */
    public void importZip(InputStream in) {
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                Policy policy = objectMapper.readValue(baos.toByteArray(), Policy.class);
                upsertPolicySilently(policy);
                zis.closeEntry();
            }
            recordAudit("import", "all", "zip");
        } catch (IOException e) {
            throw new RuntimeException("导入ZIP失败", e);
        }
    }

    /**
     * 同步：从远端目录拉取。
     */
    public void syncPull(String remoteDir) {
        syncPullWithResult(remoteDir);
    }

    /**
     * 同步：推送到远端目录。
     */
    public void syncPush(String remoteDir) {
        syncPushWithResult(remoteDir);
    }

    public static class SyncResult {
        public int created;
        public int updated;
        public int skipped;
    }

    /**
     * 同步：从远端目录拉取并返回统计。
     */
    public SyncResult syncPullWithResult(String remoteDir) {
        Path src = Paths.get(remoteDir);
        SyncResult result = new SyncResult();
        if (!Files.exists(src)) {
            return result;
        }
        try {
            Files.walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        Policy incoming = objectMapper.readValue(path.toFile(), Policy.class);
                        Optional<Policy> existing = getPolicyById(incoming.getId());
                        if (existing.isPresent()) {
                            if (existing.get().equals(incoming)) {
                                result.skipped++;
                            } else {
                                doUpdatePolicy(incoming.getId(), incoming).ifPresent(p -> result.updated++);
                            }
                        } else {
                            doCreatePolicy(incoming);
                            result.created++;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            recordAudit("pull", remoteDir, summary(result));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("同步拉取失败", e);
        }
    }

    /**
     * 同步：推送到远端目录并返回统计。
     */
    public SyncResult syncPushWithResult(String remoteDir) {
        Path dst = Paths.get(remoteDir);
        SyncResult result = new SyncResult();
        try {
            Files.createDirectories(dst);
            for (Policy policy : getAllPolicies()) {
                Path out = dst.resolve(policy.getId() + ".json");
                String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(policy);
                if (!Files.exists(out)) {
                    Files.writeString(out, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    result.created++;
                } else {
                    String existing = Files.readString(out);
                    if (existing.equals(json)) {
                        result.skipped++;
                    } else {
                        Files.writeString(out, json, StandardCharsets.UTF_8,
                            StandardOpenOption.TRUNCATE_EXISTING);
                        result.updated++;
                    }
                }
            }
            recordAudit("push", remoteDir, summary(result));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("同步推送失败", e);
        }
    }

    private String summary(SyncResult result) {
        return "created=%d updated=%d skipped=%d".formatted(result.created, result.updated, result.skipped);
    }

    private Policy doCreatePolicy(Policy policy) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("input", toPolicyInput(policy));
        JsonNode node = executeGraphQL(CREATE_POLICY_MUTATION, variables).path("createPolicy");
        return ensurePolicy(node);
    }

    private Optional<Policy> doUpdatePolicy(String id, Policy policy) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("id", id);
        variables.put("input", toPolicyInput(policy));
        JsonNode node = executeGraphQL(UPDATE_POLICY_MUTATION, variables).path("updatePolicy");
        if (node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(parsePolicy(node));
    }

    private void upsertPolicySilently(Policy policy) {
        doUpdatePolicy(policy.getId(), policy).orElseGet(() -> doCreatePolicy(policy));
    }

    private Policy ensurePolicy(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new IllegalStateException("GraphQL 未返回策略数据");
        }
        return parsePolicy(node);
    }

    private Policy parsePolicy(JsonNode node) {
        String id = node.path("id").asText();
        String name = node.path("name").asText();
        PolicyRuleSet allow = parseRuleSet(node.path("allow"));
        PolicyRuleSet deny = parseRuleSet(node.path("deny"));
        return new Policy(id, name, allow, deny);
    }

    private PolicyRuleSet parseRuleSet(JsonNode node) {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        JsonNode rulesNode = node.path("rules");
        if (rulesNode.isArray()) {
            for (JsonNode ruleNode : rulesNode) {
                String resourceType = ruleNode.path("resourceType").asText();
                List<String> patterns = new ArrayList<>();
                JsonNode patternsNode = ruleNode.path("patterns");
                if (patternsNode.isArray()) {
                    for (JsonNode p : patternsNode) {
                        patterns.add(p.asText());
                    }
                }
                rules.put(resourceType, patterns);
            }
        }
        return new PolicyRuleSet(rules);
    }

    private Map<String, Object> toPolicyInput(Policy policy) {
        Map<String, Object> input = new LinkedHashMap<>();
        if (policy.getId() != null && !policy.getId().isBlank()) {
            input.put("id", policy.getId());
        }
        input.put("name", policy.getName());
        input.put("allow", toRuleSetInput(policy.getAllow()));
        input.put("deny", toRuleSetInput(policy.getDeny()));
        return input;
    }

    private Map<String, Object> toRuleSetInput(PolicyRuleSet ruleSet) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        ruleSet.getRules().forEach((resourceType, patterns) -> {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("resourceType", resourceType);
            rule.put("patterns", patterns);
            rules.add(rule);
        });
        result.put("rules", rules);
        return result;
    }

    private JsonNode executeGraphQL(String query, Map<String, Object> variables) {
        return client().execute(query, variables == null ? Map.of() : variables, tenantHeaders())
            .path("data");
    }

    private GraphQLClient client() {
        GraphQLClient local = graphQLClient;
        if (local == null) {
            synchronized (this) {
                if (graphQLClient == null) {
                    graphQLClient = new GraphQLClient(
                        graphqlEndpoint,
                        graphqlTimeoutMillis,
                        graphqlCompression,
                        graphqlCacheTtlMillis
                    );
                }
                local = graphQLClient;
            }
        }
        return local;
    }

    private Map<String, String> tenantHeaders() {
        String tenant = requestContext.tenant();
        if (tenant == null || tenant.isBlank()) {
            return Map.of();
        }
        return Map.of("X-Tenant-Id", tenant);
    }

    private void recordAudit(String action, String targetId, String targetName) {
        String actor = authService.currentUser();
        auditService.recordCtx(actor, action, targetId, targetName,
            requestContext.tenant(), requestContext.ip(), requestContext.userAgent());
    }
}
