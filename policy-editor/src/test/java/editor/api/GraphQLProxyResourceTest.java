package editor.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.specification.RequestSpecification;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 验证 GraphQL 代理资源在鉴权场景下的转发行为与响应一致性。
 */
@QuarkusTest
@TestProfile(GraphQLProxyResourceTest.WireMockProfile.class)
public class GraphQLProxyResourceTest {

    private static final WireMockServer backend = new WireMockServer(
            WireMockConfiguration.options().dynamicPort()
    );

    static {
        backend.start();
    }

    @BeforeEach
    void resetBackend() {
        backend.resetAll();
    }

    @AfterAll
    static void shutdownBackend() {
        if (backend.isRunning()) {
            backend.stop();
        }
    }

    /**
     * 场景一：带授权信息的请求应成功转发并返回后端响应。
     */
    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void authenticatedRequestIsForwarded() {
        String requestBody = "{\"query\":\"query { viewer { id } }\"}";
        String backendBody = "{\"data\":{\"viewer\":{\"id\":\"demo\"}}}";
        stubBackend(200, backendBody);

        performGraphQLPost(Map.of("Authorization", "Bearer token-123"), requestBody, 200, backendBody);

        CapturedRequest captured = awaitSingleRequest();
        assertEquals("POST", captured.method(), "应以 POST 发起 GraphQL 代理请求");
        assertEquals("/graphql", captured.path(), "路径应保持为 /graphql");
        assertEquals(requestBody, captured.body(), "请求体应原样转发");
        assertEquals("Bearer token-123", captured.header("Authorization"), "Authorization 头必须转发");
    }

    /**
     * 场景二：白名单头部（Authorization/Cookie/X-*）应全部透传。
     */
    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void forwardsWhitelistedHeaders() {
        String backendBody = "{\"data\":{\"ok\":true}}";
        stubBackend(200, backendBody);
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer complex-token",
                "Cookie", "SESSION=abcdef; theme=dark",
                "X-Tenant", "tenant-a",
                "X-User-Id", "user-42",
                "X-Correlation-Id", "corr-001"
        );

        performGraphQLPost(headers, "{\"query\":\"query { ping }\"}", 200, backendBody);

        CapturedRequest captured = awaitSingleRequest();
        headers.forEach((name, value) ->
                assertEquals(value, captured.header(name), "应透传头部 " + name));
    }

    /**
     * 场景三：缺失 Authorization 头时按后端策略返回（此处模拟 401）。
     */
    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void missingAuthorizationReliesOnBackendPolicy() {
        String backendBody = "{\"errors\":[{\"message\":\"unauthorized\"}]}";
        stubBackend(401, backendBody);

        performGraphQLPost(Collections.emptyMap(), "{\"query\":\"query { viewer { id } }\"}", 401, backendBody);

        CapturedRequest captured = awaitSingleRequest();
        assertNull(captured.header("Authorization"), "未携带 Authorization 时不应凭空生成头部");
    }

    /**
     * 场景四：不同租户请求互不污染，确保租户隔离。
     */
    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void tenantIsolationKeepsHeadersPerRequest() {
        stubBackend(200, "{\"data\":{\"tenant\":\"alpha\"}}");
        performGraphQLPost(Map.of(
                "Authorization", "Bearer tenant-alpha",
                "X-Tenant", "tenant-alpha"
        ), "{\"query\":\"mutation { updateTenant(id:\\\"alpha\\\") }\"}", 200, "{\"data\":{\"tenant\":\"alpha\"}}");
        CapturedRequest first = awaitSingleRequest();
        assertEquals("tenant-alpha", first.header("X-Tenant"), "首次请求需带上 tenant-alpha");

        backend.resetAll();
        stubBackend(200, "{\"data\":{\"tenant\":\"beta\"}}");
        performGraphQLPost(Map.of(
                "Authorization", "Bearer tenant-beta",
                "X-Tenant", "tenant-beta"
        ), "{\"query\":\"mutation { updateTenant(id:\\\"beta\\\") }\"}", 200, "{\"data\":{\"tenant\":\"beta\"}}");
        CapturedRequest second = awaitSingleRequest();
        assertEquals("tenant-beta", second.header("X-Tenant"), "第二次请求需带上 tenant-beta");

        assertTrue(Objects.equals("tenant-alpha", first.header("X-Tenant"))
                && Objects.equals("tenant-beta", second.header("X-Tenant")),
                "不同租户的头部不应串台");
    }

    /**
     * 场景五：将后端响应状态码与 JSON 内容完整转发。
     */
    @Test
    @TestSecurity(user = "alice", roles = {"admin"})
    void propagatesBackendStatusAndBody() {
        String backendBody = "{\"errors\":[{\"message\":\"downstream unavailable\"}]}";
        stubBackend(502, backendBody);

        performGraphQLPost(Map.of("Authorization", "Bearer token-xyz"), "{\"query\":\"query { health }\"}", 502, backendBody);

        CapturedRequest captured = awaitSingleRequest();
        assertEquals("Bearer token-xyz", captured.header("Authorization"), "失败时也需保留鉴权头");
    }

    /**
     * 统一的 GraphQL POST 调用封装，便于维持断言一致性。
     */
    private void performGraphQLPost(Map<String, String> headers, String body, int expectedStatus, String expectedJson) {
        RequestSpecification spec = given()
                .contentType("application/json")
                .body(body);
        headers.forEach(spec::header);

        var response = spec.when().post("/graphql");
        if (response.statusCode() != expectedStatus) {
            int recorded = backend.getAllServeEvents().size();
            fail("代理接口返回状态码 " + response.statusCode() + "，期望 " + expectedStatus
                    + "，响应体：" + response.getBody().asString()
                    + "，下游捕获请求数：" + recorded);
        }
        response.then()
                .contentType("application/json")
                .body(equalTo(expectedJson));
    }

    private void stubBackend(int status, String body) {
        backend.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private CapturedRequest awaitSingleRequest() {
        return awaitRequests(1).get(0);
    }

    private List<CapturedRequest> awaitRequests(int expectedCount) {
        for (int attempt = 0; attempt < 20; attempt++) {
            List<ServeEvent> events = backend.getAllServeEvents();
            if (events.size() >= expectedCount) {
                return events.stream()
                        .map(GraphQLProxyResourceTest::toCaptured)
                        .toList();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("等待下游请求时线程被中断", e);
            }
        }
        fail("在超时时间内未捕获到期望数量的下游请求：" + expectedCount);
        return List.of();
    }

    private static CapturedRequest toCaptured(ServeEvent event) {
        var request = event.getRequest();
        return new CapturedRequest(
                request.getMethod().toString(),
                request.getUrl(),
                request.getBodyAsString(),
                flattenHeaders(event)
        );
    }

    private static Map<String, String> flattenHeaders(ServeEvent event) {
        var request = event.getRequest();
        Map<String, String> flattened = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : request.getHeaders().keys()) {
            flattened.put(name, request.getHeader(name));
        }
        return Map.copyOf(flattened);
    }

    /**
     * 保存一次下游调用的细节，便于在断言中取用。
     */
    static final class CapturedRequest {
        private final String method;
        private final String path;
        private final String body;
        private final Map<String, String> headers;

        CapturedRequest(String method, String path, String body, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.headers = headers;
        }

        String method() {
            return method;
        }

        String path() {
            return path;
        }

        String body() {
            return body;
        }

        String header(String name) {
            return headers.get(name);
        }
    }

    /**
     * 提供测试专用配置，将 GraphQL 代理指向 WireMock 服务。
     */
    public static class WireMockProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("policy.api.graphql.url", backend.baseUrl() + "/graphql"),
                    Map.entry("quarkus.http.auth.permission.graphql-test.paths", "/*"),
                    Map.entry("quarkus.http.auth.permission.graphql-test.policy", "permit"),
                    Map.entry("quarkus.oidc.enabled", "false")
            );
        }
    }
}
