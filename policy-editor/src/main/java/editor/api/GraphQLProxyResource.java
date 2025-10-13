package editor.api;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

/**
 * GraphQL 反向代理，将请求转发到后端 quarkus-policy-api 服务。
 * 这样前端（或其他客户端）可以直接向 policy-editor 的 /graphql 发送查询/变更，
 * 由本服务转发到实际 GraphQL 端点（默认 http://localhost:8080/graphql）。
 *
 * 修复说明：现在会转发关键的鉴权和租户头部，确保后端API能够正确识别用户身份。
 */
@ApplicationScoped
@Path("/graphql")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GraphQLProxyResource {

    @Inject
    Config config;

    /**
     * 需要转发到后端的HTTP头部白名单
     */
    private static final List<String> FORWARDED_HEADERS = Arrays.asList(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "X-Tenant",           // 租户标识
            "X-User-Id",          // 用户ID（如果使用）
            "X-Correlation-Id"    // 追踪ID（可选）
    );

    /**
     * 共享的 HttpClient 实例，支持连接池和超时配置。
     * 作为单例在应用启动时创建，避免每次请求都创建新的客户端。
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    @POST
    public Uni<Response> proxy(@Context HttpHeaders headers, JsonObject body) {
        String target = config.getOptionalValue("policy.api.graphql.url", String.class)
                .orElse("http://localhost:8080/graphql");
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(target))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.encode()));

        // 转发白名单中的头部
        for (String headerName : FORWARDED_HEADERS) {
            String headerValue = headers.getHeaderString(headerName);
            if (headerValue != null && !headerValue.isEmpty()) {
                requestBuilder.header(headerName, headerValue);
            }
        }

        HttpRequest req = requestBuilder.build();

        return Uni.createFrom().item(() -> {
                    try {
                        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onItem().transform(resp -> Response.status(resp.statusCode())
                        .entity(resp.body())
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }
}
