package io.aster.audit.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;

/**
 * 异常响应动作实体（Phase 3.7）
 *
 * 采用 Outbox 模式解耦异常检测与响应执行。
 * 检测任务（AnomalyDetectionScheduler）只负责写入动作队列，
 * 独立的消费器（AnomalyActionScheduler）异步处理动作。
 */
@Entity
@Table(name = "anomaly_actions")
public class AnomalyActionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 关联的异常报告 ID
     */
    @Column(name = "anomaly_id", nullable = false)
    public Long anomalyId;

    /**
     * 动作类型：VERIFY_REPLAY, AUTO_ROLLBACK
     */
    @Column(name = "action_type", nullable = false, length = 32)
    public String actionType;

    /**
     * 动作状态：PENDING, RUNNING, DONE, FAILED
     */
    @Column(name = "status", nullable = false, length = 16)
    public String status = "PENDING";

    /**
     * 动作参数 JSON
     * 结构：{ "workflowId": "wf-123", "targetVersion": 5 }
     * VERIFY_REPLAY 需要 workflowId，AUTO_ROLLBACK 需要 targetVersion
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    public String payload;

    /**
     * 执行失败时的错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    /**
     * 动作创建时间（提交到队列）
     */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    /**
     * 开始执行时间
     */
    @Column(name = "started_at")
    public Instant startedAt;

    /**
     * 完成时间（成功或失败）
     */
    @Column(name = "completed_at")
    public Instant completedAt;

    // ==================== Panache Active Record 查询方法 ====================

    /**
     * 查询待处理的动作（PENDING 状态），按创建时间升序排列
     *
     * @param limit 最大返回数量
     * @return 待处理动作列表
     */
    public static List<AnomalyActionEntity> findPendingActions(int limit) {
        return find("status = 'PENDING' ORDER BY created_at ASC")
            .page(0, limit)
            .list();
    }

    /**
     * 查询指定异常的所有动作历史
     *
     * @param anomalyId 异常报告 ID
     * @return 动作列表，按创建时间降序排列
     */
    public static List<AnomalyActionEntity> findByAnomalyId(Long anomalyId) {
        return find("anomaly_id = ?1 ORDER BY created_at DESC", anomalyId).list();
    }

    /**
     * 查询正在执行的动作（RUNNING 状态）
     *
     * @return 正在执行的动作列表
     */
    public static List<AnomalyActionEntity> findRunningActions() {
        return find("status = 'RUNNING' ORDER BY started_at ASC").list();
    }

    /**
     * 统计待处理动作数量
     *
     * @return 待处理动作数
     */
    public static long countPending() {
        return count("status = 'PENDING'");
    }
}
