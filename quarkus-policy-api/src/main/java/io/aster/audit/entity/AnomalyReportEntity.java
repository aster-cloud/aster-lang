package io.aster.audit.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 异常检测报告实体（Phase 3.4）
 *
 * 用于存储定时任务生成的异常检测结果，支持异步化异常检测。
 * 减少 API 实时计算开销，提升响应速度。
 */
@Entity
@Table(name = "anomaly_reports")
public class AnomalyReportEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "anomaly_type", nullable = false, length = 64)
    public String anomalyType;

    @Column(name = "version_id")
    public Long versionId;

    @Column(name = "policy_id", nullable = false, length = 255)
    public String policyId;

    @Column(name = "metric_value")
    public Double metricValue;

    @Column(name = "threshold")
    public Double threshold;

    @Column(name = "severity", nullable = false, length = 16)
    public String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    public String recommendation;

    @Column(name = "detected_at", nullable = false)
    public Instant detectedAt;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    // ==================== Panache Active Record 查询方法 ====================

    /**
     * 查询最近 N 天的异常报告
     *
     * @param days 天数
     * @return 异常报告列表，按检测时间降序排列
     */
    public static List<AnomalyReportEntity> findRecent(int days) {
        return find("detected_at >= ?1 ORDER BY detected_at DESC",
            Instant.now().minus(days, ChronoUnit.DAYS))
            .list();
    }

    /**
     * 按异常类型查询
     *
     * @param type 异常类型（HIGH_FAILURE_RATE, ZOMBIE_VERSION, PERFORMANCE_DEGRADATION）
     * @param days 天数
     * @return 指定类型的异常报告列表
     */
    public static List<AnomalyReportEntity> findByType(String type, int days) {
        return find("anomaly_type = ?1 AND detected_at >= ?2 ORDER BY detected_at DESC",
            type, Instant.now().minus(days, ChronoUnit.DAYS))
            .list();
    }

    /**
     * 统计 CRITICAL 严重程度的异常数量
     *
     * @param days 天数
     * @return CRITICAL 异常数量
     */
    public static long countCritical(int days) {
        return count("severity = 'CRITICAL' AND detected_at >= ?1",
            Instant.now().minus(days, ChronoUnit.DAYS));
    }

    /**
     * 删除超过 N 天的历史记录（定时清理任务）
     *
     * @param days 保留天数
     * @return 删除的记录数
     */
    public static long deleteOlderThan(int days) {
        return delete("detected_at < ?1", Instant.now().minus(days, ChronoUnit.DAYS));
    }

    /**
     * 按版本 ID 查询异常历史
     *
     * @param versionId 策略版本 ID
     * @param days      天数
     * @return 指定版本的异常报告列表
     */
    public static List<AnomalyReportEntity> findByVersion(Long versionId, int days) {
        return find("version_id = ?1 AND detected_at >= ?2 ORDER BY detected_at DESC",
            versionId, Instant.now().minus(days, ChronoUnit.DAYS))
            .list();
    }
}
