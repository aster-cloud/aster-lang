package io.aster.audit.service;

import io.aster.audit.entity.AnomalyActionEntity;
import io.aster.audit.entity.AnomalyReportEntity;
import io.aster.audit.rest.model.VerificationResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnomalyWorkflowService 单元测试（Phase 3.7）
 *
 * 验证异常状态机编排逻辑：
 * - 提交验证动作
 * - 更新异常状态
 * - 记录验证结果
 */
@QuarkusTest
class AnomalyWorkflowServiceTest {

    @Inject
    AnomalyWorkflowService service;

    private Long testAnomalyId;

    @BeforeEach
    @Transactional
    void setUp() {
        // 创建测试异常报告
        AnomalyReportEntity anomaly = new AnomalyReportEntity();
        anomaly.anomalyType = "HIGH_FAILURE_RATE";
        anomaly.versionId = 1L;
        anomaly.policyId = "test-policy";
        anomaly.severity = "CRITICAL";
        anomaly.status = "PENDING";
        anomaly.description = "Test anomaly";
        anomaly.recommendation = "Test recommendation";
        anomaly.metricValue = 0.5;
        anomaly.threshold = 0.1;
        anomaly.detectedAt = Instant.now();
        anomaly.persist();

        testAnomalyId = anomaly.id;
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // 清理测试数据
        AnomalyActionEntity.delete("anomalyId = ?1", testAnomalyId);
        AnomalyReportEntity.deleteById(testAnomalyId);
    }

    @Test
    void testSubmitVerificationAction_Success() {
        // 执行提交验证动作
        Long actionId = service.submitVerificationAction(testAnomalyId)
            .await().indefinitely();

        // 验证动作创建成功
        assertNotNull(actionId);
        assertTrue(actionId > 0);

        // 验证异常状态更新为 VERIFYING
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(testAnomalyId);
        assertEquals("VERIFYING", anomaly.status);

        // 验证动作实体创建成功
        AnomalyActionEntity action = AnomalyActionEntity.findById(actionId);
        assertNotNull(action);
        assertEquals(testAnomalyId, action.anomalyId);
        assertEquals("VERIFY_REPLAY", action.actionType);
        assertEquals("PENDING", action.status);
    }

    @Test
    void testSubmitVerificationAction_AnomalyNotFound() {
        // 测试异常不存在的情况
        Long invalidId = 999999L;

        assertThrows(IllegalArgumentException.class, () -> {
            service.submitVerificationAction(invalidId)
                .await().indefinitely();
        });
    }

    @Test
    @Transactional
    void testUpdateStatus_ToResolved() {
        // 先设置状态为 VERIFIED
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(testAnomalyId);
        anomaly.status = "VERIFIED";
        anomaly.persist();

        // 更新状态为 RESOLVED
        Boolean result = service.updateStatus(testAnomalyId, "RESOLVED", "问题已解决")
            .await().indefinitely();

        assertTrue(result);

        // 验证状态更新成功
        anomaly = AnomalyReportEntity.findById(testAnomalyId);
        assertEquals("RESOLVED", anomaly.status);
        assertEquals("问题已解决", anomaly.resolutionNotes);
        assertNotNull(anomaly.resolvedAt);
    }

    @Test
    @Transactional
    void testUpdateStatus_ToDismissed() {
        // 更新状态为 DISMISSED
        Boolean result = service.updateStatus(testAnomalyId, "DISMISSED", "误报")
            .await().indefinitely();

        assertTrue(result);

        // 验证状态更新成功
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(testAnomalyId);
        assertEquals("DISMISSED", anomaly.status);
        assertEquals("误报", anomaly.resolutionNotes);
        assertNotNull(anomaly.resolvedAt);
    }

    @Test
    @Transactional
    void testRecordVerificationResult_Success() {
        // 构建验证结果
        VerificationResult result = new VerificationResult(
            true,  // replaySucceeded
            true,  // anomalyReproduced
            "test-workflow-id",
            Instant.now(),
            1000L,  // originalDurationMs
            1200L   // replayDurationMs
        );

        // 记录验证结果
        Boolean success = service.recordVerificationResult(testAnomalyId, result)
            .await().indefinitely();

        assertTrue(success);

        // 验证结果写入成功
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(testAnomalyId);
        assertEquals("VERIFIED", anomaly.status);
        assertNotNull(anomaly.verificationResult);
        assertTrue(anomaly.verificationResult.contains("test-workflow-id"));
        assertTrue(anomaly.verificationResult.contains("\"replaySucceeded\":true"));
        assertTrue(anomaly.verificationResult.contains("\"anomalyReproduced\":true"));
    }
}
