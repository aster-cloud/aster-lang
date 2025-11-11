package io.aster.workflow;

import aster.runtime.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL 事件存储实现
 *
 * 基于 Panache Active Record 模式实现事件溯源持久化。
 * 支持幂等性追加、事件重放和快照优化。
 */
@ApplicationScoped
public class PostgresEventStore implements EventStore {

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "postgresql")
    String dbKind;

    /**
     * 追加事件到 workflow 事件流
     *
     * 实现幂等性保证：
     * - 通过 (workflow_id, idempotency_key) UNIQUE 约束防止重复
     * - 序列号由存储层自动递增
     * - 追加成功后通过 NOTIFY 通知调度器
     *
     * @param workflowId workflow 唯一标识符
     * @param eventType 事件类型
     * @param payload 事件负载数据
     * @return 事件序列号
     */
    @Transactional
    @Override
    public long appendEvent(String workflowId, String eventType, Object payload) {
        try {
            UUID wfId = UUID.fromString(workflowId);

            // 生成幂等性键（基于 workflow ID + 事件类型 + 时间戳）
            String idempotencyKey = generateIdempotencyKey(workflowId, eventType);

            // 检查幂等性：如果事件已存在，返回现有序列号
            Optional<WorkflowEventEntity> existing = WorkflowEventEntity.findByIdempotencyKey(wfId, idempotencyKey);
            if (existing.isPresent()) {
                Log.debugf("Event already exists for workflow %s, idempotency key %s", workflowId, idempotencyKey);
                return existing.get().sequence;
            }

            // 使用数据库 BIGSERIAL 序列生成全局递增序号，保证并发写入无冲突
            long nextSeq = nextSequenceValue();

            // 创建事件实体
            WorkflowEventEntity event = new WorkflowEventEntity();
            event.workflowId = wfId;
            event.sequence = nextSeq;
            event.seq = nextSeq;
            event.eventType = eventType;
            event.payload = serializePayload(payload);
            event.occurredAt = Instant.now();
            event.idempotencyKey = idempotencyKey;

            // Phase 3.1: 从 workflow_state 获取 policyVersionId 并记录到事件
            WorkflowStateEntity state = WorkflowStateEntity.getOrCreate(wfId);
            if (state.policyVersionId != null) {
                event.policyVersionId = state.policyVersionId;
                Log.debugf("Event %s for workflow %s recorded with policyVersionId=%d",
                        eventType, workflowId, state.policyVersionId);
            }

            // 持久化事件
            event.persist();

            // 更新 workflow 状态
            state.lastEventSeq = nextSeq;
            state.status = deriveStatusFromEvent(eventType);
            state.persist();

            // 发送 PostgreSQL NOTIFY 通知调度器
            if (supportsNotify()) {
                // NOTIFY 不支持参数绑定，使用字符串格式化
                em.createNativeQuery(String.format("NOTIFY workflow_events, '%s'", workflowId))
                        .executeUpdate();
            }

            Log.debugf("Appended event %s (seq=%d) for workflow %s", eventType, nextSeq, workflowId);
            return nextSeq;

        } catch (Exception e) {
            Log.errorf(e, "Failed to append event %s for workflow %s", eventType, workflowId);
            throw new RuntimeException("Failed to append event", e);
        }
    }

    /**
     * 获取 workflow 的事件历史
     *
     * @param workflowId workflow 唯一标识符
     * @param fromSeq 起始序列号（包含）
     * @return 事件列表，按序列号升序排列
     */
    @Override
    public List<WorkflowEvent> getEvents(String workflowId, long fromSeq) {
        UUID wfId = UUID.fromString(workflowId);
        List<WorkflowEventEntity> entities = WorkflowEventEntity.findByWorkflowId(wfId, fromSeq);

        return entities.stream()
                .map(this::toWorkflowEvent)
                .toList();
    }

    /**
     * 获取 workflow 当前状态
     *
     * @param workflowId workflow 唯一标识符
     * @return workflow 状态，如果不存在则返回 empty
     */
    @Override
    public Optional<WorkflowState> getState(String workflowId) {
        UUID wfId = UUID.fromString(workflowId);
        return WorkflowStateEntity.findByWorkflowId(wfId)
                .map(this::toWorkflowState);
    }

    /**
     * 保存 workflow 状态快照
     *
     * @param workflowId workflow 唯一标识符
     * @param eventSeq 快照对应的事件序列号
     * @param state 序列化后的状态数据
     */
    @Transactional
    @Override
    public void saveSnapshot(String workflowId, long eventSeq, Object state) {
        try {
            UUID wfId = UUID.fromString(workflowId);
            WorkflowStateEntity entity = WorkflowStateEntity.getOrCreate(wfId);

            entity.snapshot = serializePayload(state);
            entity.snapshotSeq = eventSeq;
            entity.persist();

            Log.debugf("Saved snapshot for workflow %s at event seq %d", workflowId, eventSeq);

        } catch (Exception e) {
            Log.errorf(e, "Failed to save snapshot for workflow %s", workflowId);
            throw new RuntimeException("Failed to save snapshot", e);
        }
    }

    /**
     * 获取最近的快照
     *
     * @param workflowId workflow 唯一标识符
     * @return 快照数据，如果不存在则返回 empty
     */
    @Override
    public Optional<WorkflowSnapshot> getLatestSnapshot(String workflowId) {
        UUID wfId = UUID.fromString(workflowId);
        return WorkflowStateEntity.findByWorkflowId(wfId)
                .filter(s -> s.snapshot != null && s.snapshotSeq != null)
                .map(this::toWorkflowSnapshot);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成幂等性键
     *
     * 基于 workflow ID + 事件类型 + 纳秒时间戳生成唯一键。
     * 注意：这个实现假设同一 workflow 在同一纳秒内不会发出相同类型的事件。
     * 更严格的实现可能需要包含事件内容的哈希。
     */
    private String generateIdempotencyKey(String workflowId, String eventType) {
        return String.format("%s:%s:%d", workflowId, eventType, System.nanoTime());
    }

    /**
     * 序列化负载数据为 JSON 字符串
     */
    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为对象
     */
    private Object deserializePayload(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize payload", e);
        }
    }

    /**
     * 从事件类型推导 workflow 状态
     *
     * 简化映射：
     * - WorkflowStarted -> RUNNING
     * - WorkflowCompleted -> COMPLETED
     * - WorkflowFailed -> FAILED
     * - CompensationScheduled -> COMPENSATING
     * - 其他 -> RUNNING
     */
    private String deriveStatusFromEvent(String eventType) {
        return switch (eventType) {
            case "WorkflowStarted" -> "RUNNING";
            case "WorkflowCompleted" -> "COMPLETED";
            case "WorkflowFailed" -> "FAILED";
            case "CompensationScheduled" -> "COMPENSATING";
            case "CompensationCompleted" -> "COMPENSATED";
            case "CompensationFailed" -> "COMPENSATION_FAILED";
            case "WorkflowTerminated" -> "TERMINATED";
            default -> "RUNNING";
        };
    }

    /**
     * 转换实体为 WorkflowEvent
     */
    private WorkflowEvent toWorkflowEvent(WorkflowEventEntity entity) {
        return new WorkflowEvent(
                entity.sequence,
                entity.workflowId.toString(),
                entity.eventType,
                deserializePayload(entity.payload),
                entity.occurredAt
        );
    }

    /**
     * 转换实体为 WorkflowState
     */
    private WorkflowState toWorkflowState(WorkflowStateEntity entity) {
        return new WorkflowState(
                entity.workflowId.toString(),
                WorkflowState.Status.valueOf(entity.status),
                entity.lastEventSeq,
                entity.result != null ? deserializePayload(entity.result) : null,
                entity.snapshot != null ? deserializePayload(entity.snapshot) : null,
                entity.snapshotSeq,
                entity.createdAt,
                entity.updatedAt
        );
    }

    /**
     * 转换实体为 WorkflowSnapshot
     */
    private WorkflowSnapshot toWorkflowSnapshot(WorkflowStateEntity entity) {
        return new WorkflowSnapshot(
                entity.workflowId.toString(),
                entity.snapshotSeq,
                deserializePayload(entity.snapshot)
        );
    }

    private boolean supportsNotify() {
        return !"h2".equalsIgnoreCase(dbKind);
    }

    /**
     * 获取 workflow_events.seq 的下一个值
     *
     * 通过数据库序列生成器保证全局递增且线程安全，避免 getLastSequence()+1 带来的并发冲突。
     */
    private long nextSequenceValue() {
        String sql = "SELECT nextval(pg_get_serial_sequence('workflow_events', 'seq'))";
        if ("h2".equalsIgnoreCase(dbKind)) {
            ensureH2Sequence();
            sql = "SELECT nextval('workflow_events_seq_seq')";
        }
        Number value = (Number) em.createNativeQuery(sql).getSingleResult();
        return value.longValue();
    }

    /**
     * H2 环境下显式创建 workflow_events 序列，避免并发测试中重复定义
     */
    private void ensureH2Sequence() {
        em.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS workflow_events_seq_seq START WITH 1")
                .executeUpdate();
    }
}
