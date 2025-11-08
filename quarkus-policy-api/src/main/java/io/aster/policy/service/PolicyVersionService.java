package io.aster.policy.service;

import io.aster.policy.entity.PolicyVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * 策略版本服务
 *
 * 提供策略版本管理功能：
 * - 创建新版本（自动停用旧版本）
 * - 查询版本历史
 * - 回滚到指定版本
 */
@ApplicationScoped
public class PolicyVersionService {

    /**
     * 创建新版本
     *
     * 自动停用旧的活跃版本，确保每个 policyId 只有一个活跃版本。
     *
     * @param policyId     策略ID
     * @param moduleName   模块名
     * @param functionName 函数名
     * @param content      策略内容
     * @param createdBy    创建者
     * @param notes        备注
     * @return 新创建的版本
     */
    @Transactional
    public PolicyVersion createVersion(
        String policyId,
        String moduleName,
        String functionName,
        String content,
        String createdBy,
        String notes
    ) {
        // 停用旧的活跃版本
        PolicyVersion.deactivateAllVersions(policyId);

        // 创建新版本
        PolicyVersion newVersion = new PolicyVersion(
            policyId,
            moduleName,
            functionName,
            content,
            createdBy,
            notes
        );

        newVersion.persist();

        return newVersion;
    }

    /**
     * 获取活跃版本
     *
     * @param policyId 策略ID
     * @return 活跃版本，如果不存在返回 null
     */
    public PolicyVersion getActiveVersion(String policyId) {
        return PolicyVersion.findActiveVersion(policyId);
    }

    /**
     * 获取所有版本（按版本号降序）
     *
     * @param policyId 策略ID
     * @return 版本列表
     */
    public List<PolicyVersion> getAllVersions(String policyId) {
        return PolicyVersion.findAllVersions(policyId);
    }

    /**
     * 获取指定版本
     *
     * @param policyId 策略ID
     * @param version  版本号
     * @return 版本实体，如果不存在返回 null
     */
    public PolicyVersion getVersion(String policyId, Long version) {
        return PolicyVersion.findByVersion(policyId, version);
    }

    /**
     * 回滚到指定版本
     *
     * 停用当前活跃版本，激活指定版本。
     *
     * @param policyId 策略ID
     * @param version  目标版本号
     * @return 回滚后的活跃版本
     * @throws IllegalArgumentException 如果目标版本不存在
     */
    @Transactional
    public PolicyVersion rollbackToVersion(String policyId, Long version) {
        // 查找目标版本
        PolicyVersion targetVersion = PolicyVersion.findByVersion(policyId, version);

        if (targetVersion == null) {
            throw new IllegalArgumentException(
                String.format("版本不存在: policyId=%s, version=%d", policyId, version)
            );
        }

        // 停用所有活跃版本
        PolicyVersion.deactivateAllVersions(policyId);

        // 激活目标版本
        targetVersion.active = true;
        targetVersion.persist();

        return targetVersion;
    }

    /**
     * 删除策略的所有版本
     *
     * 注意：此操作不可逆，仅用于测试或管理目的。
     *
     * @param policyId 策略ID
     * @return 删除的版本数量
     */
    @Transactional
    public long deleteAllVersions(String policyId) {
        return PolicyVersion.delete("policyId = ?1", policyId);
    }
}
