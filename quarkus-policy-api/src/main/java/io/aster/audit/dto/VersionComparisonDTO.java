package io.aster.audit.dto;

/**
 * 版本对比 DTO（Phase 3.3）
 *
 * 对比两个策略版本的性能指标，支持版本回滚决策。
 */
public class VersionComparisonDTO {

    /** 版本 A ID */
    public Long versionAId;

    /** 版本 B ID */
    public Long versionBId;

    /** 版本 A workflow 数量 */
    public int versionAWorkflowCount;

    /** 版本 B workflow 数量 */
    public int versionBWorkflowCount;

    /** 版本 A 成功率（0-100）*/
    public double versionASuccessRate;

    /** 版本 B 成功率（0-100）*/
    public double versionBSuccessRate;

    /** 版本 A 平均执行时长（毫秒）- 允许 NULL 表示无完成记录 */
    public Double versionAAvgDurationMs;

    /** 版本 B 平均执行时长（毫秒）- 允许 NULL 表示无完成记录 */
    public Double versionBAvgDurationMs;

    /** 胜出版本（A, B, TIE）*/
    public String winner;

    /** 建议措施 */
    public String recommendation;
}
