package io.aster.policy.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wontlost.aster.policy.PIIRedactor;
import io.aster.policy.entity.AuditLog;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 审计事件监听器 - 异步处理审计事件并持久化。
 */
@ApplicationScoped
public class AuditEventListener {

    private static final int MAX_METADATA_SIZE = 4096;
    private final PIIRedactor piiRedactor = new PIIRedactor();
    @Inject
    ObjectMapper objectMapper;

    /**
     * 监听审计事件并持久化。
     *
     * 使用 @ObservesAsync 实现异步处理，避免阻塞主业务流程。
     * 注意：异步事件不支持 TransactionPhase，需要在新事务中处理。
     */
    @Transactional
    public void onAuditEvent(@ObservesAsync AuditEvent event) {
        try {
            AuditLog log = new AuditLog();
            log.eventType = event.eventType().name();
            log.timestamp = event.timestamp();
            log.tenantId = redact(event.tenantId());
            log.policyModule = redact(event.policyModule());
            log.policyFunction = redact(event.policyFunction());
            log.policyId = redact(event.policyId());
            log.fromVersion = event.fromVersion();
            log.toVersion = event.toVersion();
            log.performedBy = redact(event.performedBy());
            log.success = event.success();
            log.executionTimeMs = event.executionTimeMs();
            log.errorMessage = event.errorMessage();
            log.reason = extractReason(event.metadata());
            log.metadata = serializeMetadata(event.metadata());

            log.persist();

            Log.debugf(
                "Audit event persisted: type=%s, tenant=%s, module=%s",
                event.eventType(),
                event.tenantId(),
                event.policyModule()
            );
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to persist audit event: type=%s, tenant=%s",
                event.eventType(),
                event.tenantId()
            );
        }
    }

    private String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return piiRedactor.redact(value);
    }

    private String extractReason(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object reason = metadata.get("reason");
        return reason != null ? redact(reason.toString()) : null;
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Map<String, Object> redactedMetadata = new HashMap<>();
        metadata.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (value instanceof String stringValue) {
                redactedMetadata.put(key, redact(stringValue));
            } else {
                redactedMetadata.put(key, value);
            }
        });
        if (redactedMetadata.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(redactedMetadata);
            if (json.length() > MAX_METADATA_SIZE) {
                Log.warnf(
                    "Metadata too large (%d bytes), truncating to %d bytes",
                    json.length(),
                    MAX_METADATA_SIZE
                );
                int safeLength = Math.max(0, MAX_METADATA_SIZE - 13);
                json = json.substring(0, safeLength) + "...truncated";
            }
            return json;
        } catch (Exception e) {
            Log.errorf(e, "Failed to serialize metadata");
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}
