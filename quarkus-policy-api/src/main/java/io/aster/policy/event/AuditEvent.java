package io.aster.policy.event;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计事件 - 不可变事件对象
 *
 * 用于在业务操作完成后发布，由 AuditEventListener 异步处理并持久化。
 */
public record AuditEvent(
    EventType eventType,
    Instant timestamp,
    String tenantId,
    String policyModule,
    String policyFunction,
    String policyId,
    Long fromVersion,
    Long toVersion,
    String performedBy,
    boolean success,
    Long executionTimeMs,
    String errorMessage,
    Map<String, Object> metadata
) {
    /**
     * 创建策略评估事件。
     */
    public static AuditEvent policyEvaluation(
        String tenantId,
        String policyModule,
        String policyFunction,
        String performedBy,
        boolean success,
        Long executionTimeMs,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        return new AuditEvent(
            EventType.POLICY_EVALUATION,
            Instant.now(),
            tenantId,
            policyModule,
            policyFunction,
            null,
            null,
            null,
            performedBy,
            success,
            executionTimeMs,
            errorMessage,
            sanitizeMetadata(metadata)
        );
    }

    /**
     * 创建回滚事件。
     */
    public static AuditEvent rollback(
        String tenantId,
        String policyModule,
        String policyId,
        Long fromVersion,
        Long targetVersion,
        String performedBy,
        String reason
    ) {
        return new AuditEvent(
            EventType.POLICY_ROLLBACK,
            Instant.now(),
            tenantId,
            policyModule,
            null,
            policyId,
            fromVersion,
            targetVersion,
            performedBy,
            true,
            null,
            null,
            buildRollbackMetadata(policyId, fromVersion, targetVersion, reason)
        );
    }

    /**
     * 创建部署事件。
     */
    public static AuditEvent deployment(
        String tenantId,
        String policyModule,
        String policyId,
        String policyFunction,
        String performedBy,
        Long version
    ) {
        Map<String, Object> metadata = version == null
            ? Collections.emptyMap()
            : Map.of("version", version);
        return new AuditEvent(
            EventType.POLICY_CREATED,
            Instant.now(),
            tenantId,
            policyModule,
            policyFunction,
            policyId,
            null,
            version,
            performedBy,
            true,
            null,
            null,
            metadata
        );
    }

    /**
     * 创建订单提交事件。
     */
    public static AuditEvent orderSubmission(
        String tenantId,
        String workflowModule,
        String workflowFunction,
        String orderId,
        String workflowId,
        String performedBy,
        boolean success,
        Long executionTimeMs,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = new HashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        enriched.put("workflowId", workflowId);
        enriched.put("orderId", orderId);

        return new AuditEvent(
            EventType.ORDER_SUBMITTED,
            Instant.now(),
            tenantId,
            workflowModule,
            workflowFunction,
            orderId,
            null,
            null,
            performedBy,
            success,
            executionTimeMs,
            errorMessage,
            sanitizeMetadata(enriched)
        );
    }

    /**
     * 创建订单状态查询事件。
     */
    public static AuditEvent orderStatusQuery(
        String tenantId,
        String workflowModule,
        String workflowFunction,
        String orderId,
        String workflowId,
        String performedBy,
        String status,
        boolean success,
        Long executionTimeMs,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = new HashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        enriched.put("workflowId", workflowId);
        enriched.put("orderId", orderId);
        enriched.put("status", status);

        return new AuditEvent(
            EventType.ORDER_STATUS_QUERIED,
            Instant.now(),
            tenantId,
            workflowModule,
            workflowFunction,
            orderId,
            null,
            null,
            performedBy,
            success,
            executionTimeMs,
            errorMessage,
            sanitizeMetadata(enriched)
        );
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new HashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(sanitized);
    }

    private static Map<String, Object> buildRollbackMetadata(
        String policyId,
        Long fromVersion,
        Long targetVersion,
        String reason
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (policyId != null && !policyId.isBlank()) {
            metadata.put("policyId", policyId);
        }
        if (fromVersion != null) {
            metadata.put("fromVersion", fromVersion);
        }
        if (targetVersion != null) {
            metadata.put("targetVersion", targetVersion);
        }
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        return metadata.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
}
