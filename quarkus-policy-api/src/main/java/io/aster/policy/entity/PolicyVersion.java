package io.aster.policy.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

/**
 * 策略版本实体
 *
 * 实现不可变部署：每次策略更新都创建新版本，旧版本标记为非活跃。
 * 使用 timestamp 作为版本号确保唯一性和排序。
 */
@RegisterForReflection
@Entity
@Table(name = "policy_versions", indexes = {
    @Index(name = "idx_policy_id_active", columnList = "policy_id,active"),
    @Index(name = "idx_policy_id_version", columnList = "policy_id,version")
})
public class PolicyVersion extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 策略唯一标识符（业务ID，跨版本不变）
     */
    @Column(name = "policy_id", nullable = false, length = 100)
    public String policyId;

    /**
     * 版本号（使用 timestamp 确保唯一性和时间顺序）
     */
    @Column(name = "version", nullable = false)
    public Long version;

    /**
     * 策略模块名称（如 aster.finance.loan）
     */
    @Column(name = "module_name", nullable = false, length = 200)
    public String moduleName;

    /**
     * 策略函数名称（如 evaluateLoanEligibility）
     */
    @Column(name = "function_name", nullable = false, length = 200)
    public String functionName;

    /**
     * 策略内容（Aster CNL 代码或 JSON 配置）
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    /**
     * 是否为活跃版本（每个 policyId 只有一个活跃版本）
     */
    @Column(name = "active", nullable = false)
    public Boolean active;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /**
     * 创建者
     */
    @Column(name = "created_by", length = 100)
    public String createdBy;

    /**
     * 备注信息（版本变更说明）
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    /**
     * 编译产物 SHA256 校验和（Phase 3.1）
     */
    @Column(name = "artifact_sha256", length = 64)
    public String artifactSha256;

    /**
     * 编译产物存储路径（Phase 3.1）
     */
    @Column(name = "artifact_uri", columnDefinition = "TEXT")
    public String artifactUri;

    /**
     * Runtime 构建版本（Phase 3.1）
     */
    @Column(name = "runtime_build", length = 50)
    public String runtimeBuild;

    /**
     * 版本激活时间（Phase 3.1）
     * 用于审计时间线分析
     */
    @Column(name = "activated_at")
    public Instant activatedAt;

    // 无参构造函数（JPA 要求）
    public PolicyVersion() {
    }

    /**
     * 创建新版本
     *
     * @param policyId     策略ID
     * @param moduleName   模块名
     * @param functionName 函数名
     * @param content      策略内容
     * @param createdBy    创建者
     * @param notes        备注
     */
    public PolicyVersion(
        String policyId,
        String moduleName,
        String functionName,
        String content,
        String createdBy,
        String notes
    ) {
        this.policyId = policyId;
        this.version = Instant.now().toEpochMilli(); // 使用当前时间戳作为版本号
        this.moduleName = moduleName;
        this.functionName = functionName;
        this.content = content;
        this.active = true;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
        this.notes = notes;
    }

    /**
     * 停用当前版本
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 查找指定策略的活跃版本
     *
     * @param policyId 策略ID
     * @return 活跃版本，如果不存在返回 null
     */
    public static PolicyVersion findActiveVersion(String policyId) {
        return find("policyId = ?1 and active = true", policyId).firstResult();
    }

    /**
     * 查找指定策略的所有版本（按版本号降序）
     *
     * @param policyId 策略ID
     * @return 版本列表
     */
    public static java.util.List<PolicyVersion> findAllVersions(String policyId) {
        return find("policyId = ?1 order by version desc", policyId).list();
    }

    /**
     * 查找指定策略的特定版本
     *
     * @param policyId 策略ID
     * @param version  版本号
     * @return 版本实体，如果不存在返回 null
     */
    public static PolicyVersion findByVersion(String policyId, Long version) {
        return find("policyId = ?1 and version = ?2", policyId, version).firstResult();
    }

    /**
     * 停用指定策略的所有活跃版本
     *
     * @param policyId 策略ID
     * @return 停用的版本数量
     */
    public static long deactivateAllVersions(String policyId) {
        // 使用 stream() 逐个停用，确保实体状态正确
        List<PolicyVersion> activeVersions = find("policyId = ?1 and active = true", policyId).list();
        activeVersions.forEach(v -> v.active = false);
        return activeVersions.size();
    }
}
