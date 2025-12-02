package editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * 提供 GraphQL WireMock 桩服务，避免测试访问真实 GraphQL API。
 */
public class GraphQLTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.oidc.enabled", "false",
            "quarkus.http.auth.permission.sync-test.paths", "/*",
            "quarkus.http.auth.permission.sync-test.policy", "permit"
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(GraphQLWireMockResource.class));
    }

    /**
     * 启动 WireMock 服务并将 policy.api.graphql.url 指向桩实现。
     */
    public static class GraphQLWireMockResource implements QuarkusTestResourceLifecycleManager {
        private WireMockServer server;
        private final ConcurrentHashMap<String, ObjectNode> policies = new ConcurrentHashMap<>();

        @Override
        public Map<String, String> start() {
            policies.clear();
            GraphQLResponseTransformer transformer = new GraphQLResponseTransformer(policies);
            server = new WireMockServer(
                WireMockConfiguration.options()
                    .dynamicPort()
                    .extensions(transformer)
            );
            server.start();
            server.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(GraphQLResponseTransformer.NAME)));
            return Map.of(
                "policy.api.graphql.url", server.baseUrl() + "/graphql",
                "policy.api.graphql.compression", "false",
                "quarkus.http.auth.permission.sync-test.paths", "/*",
                "quarkus.http.auth.permission.sync-test.policy", "permit",
                "quarkus.oidc.enabled", "false"
            );
        }

        @Override
        public void stop() {
            if (server != null) {
                server.stop();
            }
        }
    }

    /**
     * 根据 GraphQL 请求内容返回内存中的策略数据。
     */
    static final class GraphQLResponseTransformer implements ResponseDefinitionTransformerV2 {
        static final String NAME = "policy-editor-graphql-stub";

        private final ObjectMapper mapper = new ObjectMapper();
        private final ConcurrentHashMap<String, ObjectNode> policies;

        GraphQLResponseTransformer(ConcurrentHashMap<String, ObjectNode> policies) {
            this.policies = policies;
        }

        @Override
        public ResponseDefinition transform(ServeEvent event) {
            try {
                JsonNode payload = mapper.readTree(event.getRequest().getBodyAsString());
                String query = payload.path("query").asText("");
                JsonNode variables = payload.path("variables");

                ObjectNode data = mapper.createObjectNode();
                if (query.contains("listPolicies")) {
                    data.set("listPolicies", snapshot());
                } else if (query.contains("getPolicy")) {
                    String id = variables.path("id").asText("");
                    ObjectNode policy = policies.get(id);
                    data.set("getPolicy", policy == null ? mapper.nullNode() : policy.deepCopy());
                } else if (query.contains("createPolicy")) {
                    ObjectNode policy = toPolicyNode(variables.path("input"), null);
                    policies.put(policy.path("id").asText(), policy);
                    data.set("createPolicy", policy.deepCopy());
                } else if (query.contains("updatePolicy")) {
                    String id = variables.path("id").asText("");
                    ObjectNode policy = toPolicyNode(variables.path("input"), id);
                    policies.put(policy.path("id").asText(), policy);
                    data.set("updatePolicy", policy.deepCopy());
                } else if (query.contains("deletePolicy")) {
                    String id = variables.path("id").asText("");
                    boolean removed = policies.remove(id) != null;
                    data.put("deletePolicy", removed);
                } else {
                    return error("Unsupported GraphQL query.");
                }

                return success(data);
            } catch (Exception e) {
                return error(e.getMessage());
            }
        }

        private ArrayNode snapshot() {
            ArrayNode array = mapper.createArrayNode();
            policies.values().forEach(node -> array.add(node.deepCopy()));
            return array;
        }

        private ObjectNode toPolicyNode(JsonNode input, String fallbackId) {
            ObjectNode policy = mapper.createObjectNode();
            String id = input.path("id").asText("");
            if (id == null || id.isBlank()) {
                id = fallbackId != null && !fallbackId.isBlank()
                    ? fallbackId
                    : "test-" + System.nanoTime();
            }
            policy.put("id", id);
            policy.put("name", input.path("name").asText("test-policy"));
            policy.set("allow", normalizeRuleSet(input.path("allow")));
            policy.set("deny", normalizeRuleSet(input.path("deny")));
            return policy;
        }

        private ObjectNode normalizeRuleSet(JsonNode node) {
            ObjectNode result = mapper.createObjectNode();
            ArrayNode rules = mapper.createArrayNode();
            JsonNode rulesNode = node.path("rules");
            if (rulesNode.isArray()) {
                for (JsonNode ruleNode : rulesNode) {
                    ObjectNode rule = mapper.createObjectNode();
                    rule.put("resourceType", ruleNode.path("resourceType").asText(""));
                    ArrayNode patterns = mapper.createArrayNode();
                    JsonNode patternsNode = ruleNode.path("patterns");
                    if (patternsNode.isArray()) {
                        for (JsonNode p : patternsNode) {
                            patterns.add(p.asText(""));
                        }
                    }
                    rule.set("patterns", patterns);
                    rules.add(rule);
                }
            }
            result.set("rules", rules);
            return result;
        }

        private ResponseDefinition success(ObjectNode data) {
            ObjectNode root = mapper.createObjectNode();
            root.set("data", data);
            return new ResponseDefinitionBuilder()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(root.toString())
                .build();
        }

        private ResponseDefinition error(String message) {
            ObjectNode root = mapper.createObjectNode();
            ArrayNode errors = mapper.createArrayNode();
            ObjectNode error = mapper.createObjectNode();
            error.put("message", message == null ? "GraphQL stub error" : message);
            errors.add(error);
            root.set("errors", errors);
            return new ResponseDefinitionBuilder()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody(root.toString())
                .build();
        }

        @Override
        public String getName() {
            return NAME;
        }

    }
}
