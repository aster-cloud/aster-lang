package editor.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 简单 GraphQL 客户端，向本服务的 /graphql 发送查询。
 */
public class GraphQLClient {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint;
    private final int timeoutMillis;
    private final boolean compression;
    private final int cacheTtlMillis;
    private final Map<String, CacheEntry> cache;

    public GraphQLClient(String endpoint) {
        this(endpoint, 5000, true, 0);
    }

    public GraphQLClient(String endpoint, int timeoutMillis, boolean compression, int cacheTtlMillis) {
        this.endpoint = endpoint;
        this.timeoutMillis = timeoutMillis;
        this.compression = compression;
        this.cacheTtlMillis = cacheTtlMillis;
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > 128; // 简单 LRU 上限
            }
        };
        HttpClient.Builder b = HttpClient.newBuilder();
        if (timeoutMillis > 0) b.connectTimeout(Duration.ofMillis(timeoutMillis));
        this.client = b.build();
    }

    public JsonNode execute(String query) {
        return execute(query, Map.of(), Map.of());
    }

    public JsonNode execute(String query, Map<String, Object> variables) {
        return execute(query, variables, Map.of());
    }

    public JsonNode execute(String query, Map<String, Object> variables, Map<String, String> additionalHeaders) {
        try {
            Objects.requireNonNull(query, "query 不能为空");

            ObjectNode body = mapper.createObjectNode();
            body.put("query", query);
            if (variables != null && !variables.isEmpty()) {
                body.set("variables", mapper.valueToTree(variables));
            }
            String bodyText = body.toString();

            long now = System.currentTimeMillis();
            if (cacheTtlMillis > 0) {
                CacheEntry cached = cache.get(bodyText);
                if (cached != null && now - cached.ts <= cacheTtlMillis) {
                    return cached.node;
                }
            }

            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyText));
            if (timeoutMillis > 0) {
                rb.timeout(Duration.ofMillis(timeoutMillis));
            }
            if (compression) {
                rb.header("Accept-Encoding", "gzip");
            }
            if (additionalHeaders != null) {
                for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
                    if (header.getKey() != null && header.getValue() != null) {
                        rb.header(header.getKey(), header.getValue());
                    }
                }
            }

            HttpRequest req = rb.build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("GraphQL HTTP错误: " + resp.statusCode() + "\n" + node);
            }
            if (node.has("errors")) {
                throw new RuntimeException("GraphQL 执行错误: " + node.get("errors").toString());
            }
            if (cacheTtlMillis > 0) {
                cache.put(bodyText, new CacheEntry(now, node));
            }
            return node;
        } catch (Exception e) {
            throw new RuntimeException("GraphQL 调用失败: " + e.getMessage(), e);
        }
    }

    private static class CacheEntry {
        final long ts; final JsonNode node;
        CacheEntry(long ts, JsonNode node) { this.ts = ts; this.node = node; }
    }
}
