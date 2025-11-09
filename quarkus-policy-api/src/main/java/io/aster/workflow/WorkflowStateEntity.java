package io.aster.workflow;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workflow 状态实体 - 使用 Panache Active Record 模式
 *
 * 持久化 workflow 当前执行状态，支持快速查询和调度。
 */
@Entity
@Table(name = "workflow_state")
public class WorkflowStateEntity extends PanacheEntityBase {

    @Id
    @Column(name = "workflow_id")
    public UUID workflowId;

    @Column(nullable = false, length = 32)
    public String status;

    @Column(name = "last_event_seq", nullable = false)
    public Long lastEventSeq = 0L;

    @Column(columnDefinition = "jsonb")
    public String result;

    @Column(columnDefinition = "jsonb")
    public String snapshot;

    @Column(name = "snapshot_seq")
    public Long snapshotSeq;

    @Column(name = "lock_owner", length = 64)
    public String lockOwner;

    @Column(name = "lock_expires_at")
    public Instant lockExpiresAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    /**
     * 根据 workflow ID 查询状态
     *
     * @param workflowId workflow 唯一标识符
     * @return 状态实例，如果不存在则返回 empty
     */
    public static Optional<WorkflowStateEntity> findByWorkflowId(UUID workflowId) {
        return findByIdOptional(workflowId);
    }

    /**
     * 查询指定状态的 workflow 列表
     *
     * @param status 状态类型（READY, RUNNING, etc.）
     * @return workflow 状态列表
     */
    public static List<WorkflowStateEntity> findByStatus(String status) {
        return find("status", status).list();
    }

    /**
     * 查询就绪可调度的 workflow（支持 SELECT FOR UPDATE SKIP LOCKED）
     *
     * @param limit 最大返回数量
     * @return 就绪的 workflow 状态列表
     */
    public static List<WorkflowStateEntity> findReadyForScheduling(int limit) {
        return find("status IN ('READY', 'RUNNING') AND (lockExpiresAt IS NULL OR lockExpiresAt < ?1)",
                Instant.now())
                .page(0, limit)
                .list();
    }

    /**
     * 统计指定状态的 workflow 数量
     *
     * @param status 状态类型
     * @return workflow 数量
     */
    public static long countByStatus(String status) {
        return count("status", status);
    }

    /**
     * 获取或创建 workflow 状态
     *
     * @param workflowId workflow 唯一标识符
     * @return workflow 状态实例
     */
    public static WorkflowStateEntity getOrCreate(UUID workflowId) {
        Optional<WorkflowStateEntity> existing = findByIdOptional(workflowId);
        if (existing.isPresent()) {
            return existing.get();
        }

        var state = new WorkflowStateEntity();
        state.workflowId = workflowId;
        state.status = "READY";
        state.lastEventSeq = 0L;
        state.createdAt = Instant.now();
        state.updatedAt = Instant.now();
        state.persist();
        return state;
    }

    /**
     * 更新时间戳
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
