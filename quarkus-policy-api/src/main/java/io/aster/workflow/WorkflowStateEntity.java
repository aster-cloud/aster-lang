package io.aster.workflow;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String result;

    @JdbcTypeCode(SqlTypes.JSON)
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
     * 租户 ID（Phase 3.2）
     * 支持多租户场景的 workflow 过滤
     */
    @Column(name = "tenant_id", length = 64)
    public String tenantId;

    /**
     * 策略版本 ID（Phase 3.1）
     * NULL 表示版本化功能上线前的 workflow
     */
    @Column(name = "policy_version_id")
    public Long policyVersionId;

    /**
     * 策略版本激活时间（Phase 3.1）
     * 用于审计和版本使用时间线分析
     */
    @Column(name = "policy_activated_at")
    public Instant policyActivatedAt;

    /**
     * Workflow 开始时间（Phase 3.3）
     * 用于性能统计和执行时长计算
     */
    @Column(name = "started_at")
    public Instant startedAt;

    /**
     * Workflow 完成时间（Phase 3.3）
     * 用于性能统计和时间线分析
     */
    @Column(name = "completed_at")
    public Instant completedAt;

    /**
     * 执行时长（毫秒）（Phase 3.3）
     * 冗余字段，通过 completed_at - started_at 计算得出
     * 用于快速查询和统计，避免实时计算
     */
    @Column(name = "duration_ms")
    public Long durationMs;

    /**
     * 错误信息（Phase 3.3）
     * 仅在 status=FAILED 时有值，用于失败模式分析
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    /**
     * 确定性决策快照（Phase 0 Task 1.3）
     * JSONB 结构：{ "clockTimes": [...], "uuids": [...], "randoms": {"source": [...] } }
     * 兼容旧结构 recordedTimes/replayIndex/replayMode/version
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "clock_times", columnDefinition = "jsonb")
    public String clockTimes;

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

    /**
     * 计算并更新执行时长（Phase 3.3）
     *
     * 应在 workflow 完成时调用，确保 duration_ms 与时间戳一致
     */
    public void updateDuration() {
        if (this.startedAt != null && this.completedAt != null) {
            this.durationMs = Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }

    /**
     * 标记 workflow 开始（Phase 3.3）
     */
    public void markStarted() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    /**
     * 标记 workflow 完成（Phase 3.3）
     *
     * @param finalStatus 最终状态（COMPLETED, FAILED, etc.）
     */
    public void markCompleted(String finalStatus) {
        this.completedAt = Instant.now();
        this.status = finalStatus;
        updateDuration();
    }
}
