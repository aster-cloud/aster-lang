package io.aster.policy.service;

import io.aster.policy.entity.PolicyVersion;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PolicyVersionService 集成测试
 *
 * 使用 H2 内存数据库验证策略版本控制功能：
 * - Flyway 自动迁移
 * - 版本创建与停用
 * - 版本查询
 * - 版本回滚
 */
@QuarkusTest
class PolicyVersionServiceTest {

    @Inject
    PolicyVersionService versionService;

    @BeforeEach
    @Transactional
    void cleanup() {
        // 清理测试数据
        PolicyVersion.deleteAll();
    }

    @Test
    void testFlywayMigrationExecuted() {
        // 验证 Flyway 迁移已执行（表已创建）
        // 如果表不存在，下面的查询会抛出异常
        long count = PolicyVersion.count();
        assertEquals(0, count, "初始状态下应该没有版本记录");
    }

    @Test
    @Transactional
    void testCreateVersion() {
        // 创建版本
        PolicyVersion version = versionService.createVersion(
            "policy-001",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Aster policy code",
            "test-user",
            "Initial version"
        );

        // 验证版本创建成功
        assertNotNull(version);
        assertNotNull(version.id);
        assertEquals("policy-001", version.policyId);
        assertEquals("aster.finance.loan", version.moduleName);
        assertEquals("evaluateLoanEligibility", version.functionName);
        assertTrue(version.active, "新版本应该是活跃的");
        assertNotNull(version.version, "版本号应该被设置");
        assertNotNull(version.createdAt, "创建时间应该被设置");
    }

    @Test
    @Transactional
    void testCreateVersionDeactivatesOldVersion() {
        // 创建第一个版本
        PolicyVersion v1 = versionService.createVersion(
            "policy-002",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 1",
            "test-user",
            "Version 1"
        );

        assertTrue(v1.active, "版本 1 应该是活跃的");

        // 模拟时间流逝，确保版本号不同
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 创建第二个版本
        PolicyVersion v2 = versionService.createVersion(
            "policy-002",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 2",
            "test-user",
            "Version 2"
        );

        // 重新查询版本 1
        PolicyVersion v1Updated = PolicyVersion.findById(v1.id);

        // 验证版本 1 被停用
        assertFalse(v1Updated.active, "旧版本应该被停用");
        assertTrue(v2.active, "新版本应该是活跃的");
        assertNotEquals(v1.version, v2.version, "版本号应该不同");
    }

    @Test
    @Transactional
    void testOnlyOneActiveVersionPerPolicy() {
        // 创建多个版本
        for (int i = 1; i <= 3; i++) {
            versionService.createVersion(
                "policy-003",
                "aster.finance.loan",
                "evaluateLoanEligibility",
                "// Version " + i,
                "test-user",
                "Version " + i
            );

            // 模拟时间流逝
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 验证只有一个活跃版本
        List<PolicyVersion> allVersions = PolicyVersion.findAllVersions("policy-003");
        assertEquals(3, allVersions.size(), "应该有 3 个版本");

        long activeCount = allVersions.stream()
            .filter(v -> v.active)
            .count();

        assertEquals(1, activeCount, "应该只有 1 个活跃版本");
    }

    @Test
    @Transactional
    void testGetActiveVersion() {
        // 创建版本
        PolicyVersion created = versionService.createVersion(
            "policy-004",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Active version",
            "test-user",
            "Active version"
        );

        // 查询活跃版本
        PolicyVersion active = versionService.getActiveVersion("policy-004");

        assertNotNull(active, "应该能查询到活跃版本");
        assertEquals(created.id, active.id, "应该是同一个版本");
        assertEquals(created.version, active.version, "版本号应该相同");
    }

    @Test
    @Transactional
    void testGetAllVersions() {
        // 创建多个版本
        for (int i = 1; i <= 3; i++) {
            versionService.createVersion(
                "policy-005",
                "aster.finance.loan",
                "evaluateLoanEligibility",
                "// Version " + i,
                "test-user",
                "Version " + i
            );

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 查询所有版本
        List<PolicyVersion> versions = versionService.getAllVersions("policy-005");

        assertEquals(3, versions.size(), "应该有 3 个版本");

        // 验证按版本号降序排列
        for (int i = 0; i < versions.size() - 1; i++) {
            assertTrue(
                versions.get(i).version > versions.get(i + 1).version,
                "版本应该按版本号降序排列"
            );
        }
    }

    @Test
    @Transactional
    void testGetVersion() {
        // 创建版本
        PolicyVersion created = versionService.createVersion(
            "policy-006",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Specific version",
            "test-user",
            "Specific version"
        );

        // 查询指定版本
        PolicyVersion found = versionService.getVersion("policy-006", created.version);

        assertNotNull(found, "应该能查询到指定版本");
        assertEquals(created.id, found.id, "应该是同一个版本");
    }

    @Test
    @Transactional
    void testRollbackToVersion() {
        // 创建多个版本
        PolicyVersion v1 = versionService.createVersion(
            "policy-007",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 1",
            "test-user",
            "Version 1"
        );

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PolicyVersion v2 = versionService.createVersion(
            "policy-007",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 2",
            "test-user",
            "Version 2"
        );

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PolicyVersion v3 = versionService.createVersion(
            "policy-007",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 3",
            "test-user",
            "Version 3"
        );

        // 回滚到版本 1
        PolicyVersion rolledBack = versionService.rollbackToVersion("policy-007", v1.version);

        // 验证回滚成功
        assertEquals(v1.id, rolledBack.id, "应该回滚到版本 1");
        assertTrue(rolledBack.active, "回滚的版本应该是活跃的");

        // 验证其他版本被停用
        PolicyVersion v2Updated = PolicyVersion.findById(v2.id);
        PolicyVersion v3Updated = PolicyVersion.findById(v3.id);

        assertFalse(v2Updated.active, "版本 2 应该被停用");
        assertFalse(v3Updated.active, "版本 3 应该被停用");

        // 验证只有一个活跃版本
        PolicyVersion active = versionService.getActiveVersion("policy-007");
        assertEquals(v1.version, active.version, "活跃版本应该是版本 1");
    }

    @Test
    @Transactional
    void testRollbackToNonExistentVersion() {
        // 创建版本
        versionService.createVersion(
            "policy-008",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 1",
            "test-user",
            "Version 1"
        );

        // 尝试回滚到不存在的版本
        assertThrows(
            IllegalArgumentException.class,
            () -> versionService.rollbackToVersion("policy-008", 999999L),
            "回滚到不存在的版本应该抛出异常"
        );
    }

    @Test
    @Transactional
    void testDeleteAllVersions() {
        // 创建多个版本
        for (int i = 1; i <= 3; i++) {
            versionService.createVersion(
                "policy-009",
                "aster.finance.loan",
                "evaluateLoanEligibility",
                "// Version " + i,
                "test-user",
                "Version " + i
            );

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 删除所有版本
        long deletedCount = versionService.deleteAllVersions("policy-009");

        assertEquals(3, deletedCount, "应该删除 3 个版本");

        // 验证删除成功
        List<PolicyVersion> versions = PolicyVersion.findAllVersions("policy-009");
        assertEquals(0, versions.size(), "不应该有版本记录");
    }

    @Test
    @Transactional
    void testTimestampVersionUniqueness() {
        // 快速创建多个版本，验证版本号唯一性
        PolicyVersion v1 = versionService.createVersion(
            "policy-010",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 1",
            "test-user",
            "Version 1"
        );

        // 等待至少 1ms 确保时间戳不同
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PolicyVersion v2 = versionService.createVersion(
            "policy-010",
            "aster.finance.loan",
            "evaluateLoanEligibility",
            "// Version 2",
            "test-user",
            "Version 2"
        );

        // 验证版本号不同
        assertNotEquals(v1.version, v2.version, "版本号应该不同");
        assertTrue(v2.version > v1.version, "后创建的版本号应该更大");
    }
}
