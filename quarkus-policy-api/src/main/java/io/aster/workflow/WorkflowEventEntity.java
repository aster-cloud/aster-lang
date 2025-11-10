package io.aster.workflow;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workflow 事件实体 - 使用 Panache Active Record 模式
 *
 * 持久化 workflow 执行事件到 PostgreSQL 分区表，支持事件溯源和状态重放。
 */
@Entity
@Table(name = "workflow_events")
public class WorkflowEventEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "workflow_id", nullable = false)
    public UUID workflowId;

    @Column(nullable = false)
    public Long sequence;

    @Column(name = "event_type", nullable = false, length = 64)
    public String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String payload;

    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;

    @Column(name = "idempotency_key", length = 255)
    public String idempotencyKey;

    /**
     * 查询指定 workflow 的事件历史
     *
     * @param workflowId workflow 唯一标识符
     * @param fromSeq 起始序列号（包含）
     * @return 事件列表，按序列号升序排列
     */
    public static List<WorkflowEventEntity> findByWorkflowId(UUID workflowId, long fromSeq) {
        return find("workflowId = ?1 AND sequence >= ?2 ORDER BY sequence", workflowId, fromSeq).list();
    }

    /**
     * 根据幂等性键查询事件
     *
     * @param workflowId workflow 唯一标识符
     * @param idempotencyKey 幂等性键
     * @return 事件实例，如果不存在则返回 empty
     */
    public static Optional<WorkflowEventEntity> findByIdempotencyKey(UUID workflowId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return find("workflowId = ?1 AND idempotencyKey = ?2", workflowId, idempotencyKey)
                .firstResultOptional();
    }

    /**
     * 获取 workflow 的最后事件序列号
     *
     * @param workflowId workflow 唯一标识符
     * @return 最后序列号，如果不存在则返回 0
     */
    public static long getLastSequence(UUID workflowId) {
        Long lastSeq = find("SELECT MAX(sequence) FROM WorkflowEventEntity WHERE workflowId = ?1", workflowId)
                .project(Long.class)
                .firstResult();
        return lastSeq != null ? lastSeq : 0L;
    }

    /**
     * 统计 workflow 的事件数量
     *
     * @param workflowId workflow 唯一标识符
     * @return 事件总数
     */
    public static long countByWorkflowId(UUID workflowId) {
        return count("workflowId", workflowId);
    }

    /**
     * 判斷指定類型事件是否已存在
     *
     * @param workflowId workflow 唯一標識符
     * @param eventType 事件類型
     * @return true 表示已存在
     */
    public static boolean hasEvent(UUID workflowId, String eventType) {
        return count("workflowId = ?1 AND eventType = ?2", workflowId, eventType) > 0;
    }
}
