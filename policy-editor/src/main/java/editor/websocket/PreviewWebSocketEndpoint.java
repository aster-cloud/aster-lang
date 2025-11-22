package editor.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Preview WebSocket 端点：提供实时策略评估预览功能。
 *
 * 接收来自前端的策略评估请求，转发到 policy-api 服务进行评估，
 * 并将结果返回给前端以实现实时预览效果。
 */
@ServerEndpoint("/ws/preview")
@ApplicationScoped
public class PreviewWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(PreviewWebSocketEndpoint.class);

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "policy.api.graphql.url")
    String policyApiUrl;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        LOG.infov("Preview WebSocket connected: {0}", session.getId());

        // 发送连接成功消息
        sendMessage(session, createStatusMessage("connected", "Preview WebSocket connected"));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // 解析请求
            JsonNode request = objectMapper.readTree(message);
            String policyModule = request.path("policyModule").asText();
            String policyFunction = request.path("policyFunction").asText();
            JsonNode context = request.path("context");

            if (policyModule.isEmpty() || policyFunction.isEmpty()) {
                sendError(session, "Missing policyModule or policyFunction");
                return;
            }

            LOG.infov("Preview request: {0}.{1}", policyModule, policyFunction);

            // 调用 policy-api 评估服务
            evaluatePolicyAsync(session, policyModule, policyFunction, context);

        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to parse preview request from session %s", session.getId());
            sendError(session, "Invalid JSON format: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session.getId());
        LOG.infov("Preview WebSocket closed: {0} reason={1}", session.getId(), reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "Preview WebSocket error for session %s", session != null ? session.getId() : "unknown");
        if (session != null) {
            sessions.remove(session.getId());
            sendError(session, "WebSocket error: " + throwable.getMessage());
        }
    }

    /**
     * 异步评估策略并返回结果
     */
    private void evaluatePolicyAsync(Session session, String policyModule, String policyFunction, JsonNode context) {
        // 在单独的线程中调用 REST API，避免阻塞 WebSocket 线程
        Thread.ofVirtual().start(() -> {
            try {
                // 构建 REST API 请求
                String apiEndpoint = resolveApiEndpoint();
                Map<String, Object> requestBody = Map.of(
                    "policyModule", policyModule,
                    "policyFunction", policyFunction,
                    "context", context
                );

                String requestJson = objectMapper.writeValueAsString(requestBody);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint + "/api/policies/evaluate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(10))
                    .build();

                LOG.debugv("Sending request to policy-api: {0}", apiEndpoint);

                // 发送请求
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // 解析响应并转发到前端
                    JsonNode responseJson = objectMapper.readTree(response.body());
                    sendMessage(session, responseJson.toString());
                    LOG.debugv("Preview evaluation completed for session {0}", session.getId());
                } else {
                    LOG.warnf("Policy-api returned status %d: %s", response.statusCode(), response.body());
                    sendError(session, "Evaluation failed with status " + response.statusCode());
                }

            } catch (IOException e) {
                LOG.errorf(e, "Failed to connect to policy-api for session %s", session.getId());
                sendError(session, "Cannot connect to policy-api: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.errorf(e, "Evaluation interrupted for session %s", session.getId());
                sendError(session, "Evaluation interrupted");
            } catch (Exception e) {
                LOG.errorf(e, "Unexpected error during evaluation for session %s", session.getId());
                sendError(session, "Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * 解析 policy-api 端点地址
     * 从配置的 GraphQL URL 中提取 base URL（去掉 /graphql 后缀）
     */
    private String resolveApiEndpoint() {
        if (policyApiUrl.endsWith("/graphql")) {
            return policyApiUrl.substring(0, policyApiUrl.length() - "/graphql".length());
        }
        return policyApiUrl;
    }

    /**
     * 发送消息到前端
     */
    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send message to session %s", session.getId());
            }
        }
    }

    /**
     * 发送错误消息到前端
     */
    private void sendError(Session session, String errorMessage) {
        try {
            String errorJson = objectMapper.writeValueAsString(Map.of(
                "error", errorMessage,
                "timestamp", System.currentTimeMillis()
            ));
            sendMessage(session, errorJson);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize error message for session %s", session.getId());
        }
    }

    /**
     * 创建状态消息
     */
    private String createStatusMessage(String status, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "status", status,
                "message", message,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (JsonProcessingException e) {
            LOG.error("Failed to create status message", e);
            return "{\"status\":\"error\",\"message\":\"Failed to create status message\"}";
        }
    }
}
