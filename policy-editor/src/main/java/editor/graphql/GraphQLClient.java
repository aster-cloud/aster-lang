package editor.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
        try {
            long now = System.currentTimeMillis();
            if (cacheTtlMillis > 0) {
                CacheEntry ce = cache.get(query);
                if (ce != null && now - ce.ts <= cacheTtlMillis) {
                    return ce.node;
                }
            }
            ObjectNode body = mapper.createObjectNode();
            body.put("query", query);
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(timeoutMillis > 0 ? Duration.ofMillis(timeoutMillis) : null)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (compression) {
                rb.header("Accept-Encoding", "gzip");
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
                cache.put(query, new CacheEntry(now, node));
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
