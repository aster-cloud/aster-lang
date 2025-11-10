package io.aster.audit.dto;

import java.time.Instant;

/**
 * 版本使用统计 DTO（Phase 3.3）
 *
 * 提供按时间粒度聚合的版本使用量、成功率、失败率和平均执行时间。
 */
public class VersionUsageStatsDTO {

    /** 策略版本 ID */
    public Long versionId;

    /** 时间桶（聚合时间粒度的起点）*/
    public Instant timeBucket;

    /** 总 workflow 数量 */
    public int totalCount;

    /** 已完成 workflow 数量 */
    public int completedCount;

    /** 失败 workflow 数量 */
    public int failedCount;

    /** 运行中 workflow 数量 */
    public int runningCount;

    /** 成功率（0-100）*/
    public double successRate;

    /** 平均执行时长（毫秒）- 允许 NULL 表示无完成记录 */
    public Double avgDurationMs;

    /** 租户 ID（用于多租户过滤）*/
    public String tenantId;
}
