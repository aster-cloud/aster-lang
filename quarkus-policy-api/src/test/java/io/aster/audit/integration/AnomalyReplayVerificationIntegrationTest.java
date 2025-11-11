package io.aster.audit.integration;

import io.aster.audit.dto.AnomalyReportDTO;
import io.aster.audit.entity.AnomalyActionEntity;
import io.aster.audit.entity.AnomalyReportEntity;
import io.aster.audit.rest.model.VerificationResult;
import io.aster.audit.service.AnomalyActionExecutor;
import io.aster.audit.service.AnomalyWorkflowService;
import io.aster.audit.service.PolicyAnalyticsService;
import io.aster.policy.entity.PolicyVersion;
import io.aster.workflow.WorkflowSchedulerService;
import io.aster.workflow.WorkflowStateEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anomaly Replay 验证链路集成测试（Phase 3.8）
 *
 * 端到端验证完整的异常响应自动化流程：
 * 1. 异常检测（PolicyAnalyticsService）→ 捕获 sampleWorkflowId
 * 2. 持久化异常报告（AnomalyReportEntity）
 * 3. 提交验证动作（AnomalyWorkflowService）→ 构建 payload
 * 4. 执行 Replay 验证（AnomalyActionExecutor）→ 调用 replayWorkflow()
 * 5. 记录验证结果（写入 verification_result）
 *
 * 注意：由于完整的 workflow 执行需要实际的 policy 编译和运行环境，
 * 这个集成测试主要验证数据流完整性和各组件集成正确性。
 * 实际的 workflow replay 执行在 E2E 测试中验证。
 */
@QuarkusTest
class AnomalyReplayVerificationIntegrationTest {

    @Inject
    PolicyAnalyticsService analyticsService;

    @Inject
    AnomalyWorkflowService workflowService;

    @Inject
    AnomalyActionExecutor actionExecutor;

    @Inject
    WorkflowSchedulerService schedulerService;

    private Long testVersionId;
    private String testPolicyId;

    @BeforeEach
    @Transactional
    void setUp() {
        // 创建测试策略版本
        testPolicyId = "test-policy-" + UUID.randomUUID();
        PolicyVersion version = new PolicyVersion();
        version.policyId = testPolicyId;
        version.version = Instant.now().toEpochMilli();
        version.moduleName = "test.module";
        version.functionName = "testFunction";
        version.content = "test content";
        version.active = true;
        version.createdAt = Instant.now();
        version.persist();

        testVersionId = version.id;
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // 清理测试数据（按依赖顺序）
        // 1. 清理 anomaly_reports（会级联删除 anomaly_actions）
        AnomalyReportEntity.delete("policyId = ?1", testPolicyId);

        // 3. 清理 workflow_state
        WorkflowStateEntity.delete("policyVersionId = ?1", testVersionId);

        // 4. 清理 policy_versions
        PolicyVersion.deleteById(testVersionId);
    }

    /**
     * 测试完整的异常检测 → payload 构建链路
     *
     * 验证：
     * 1. 异常检测能正确捕获 sampleWorkflowId
     * 2. 异常持久化后 sampleWorkflowId 正确存储
     * 3. 提交验证动作时 payload 正确构建
     */
    @Test
    @Transactional
    void testEndToEnd_AnomalyDetection_To_PayloadBuilding() {
        // 1. 准备测试数据：创建高失败率场景
        Instant now = Instant.now();
        UUID expectedSampleWorkflowId = null;

        for (int i = 0; i < 10; i++) {
            WorkflowStateEntity workflow = new WorkflowStateEntity();
            workflow.workflowId = UUID.randomUUID();
            workflow.policyVersionId = testVersionId;
            workflow.startedAt = now.minus(i, ChronoUnit.HOURS);
            workflow.status = (i % 2 == 0) ? "COMPLETED" : "FAILED";
            workflow.durationMs = 1000L;
            workflow.persist();

            // 记录最新的失败 workflow（应该被捕获为 sampleWorkflowId）
            if ("FAILED".equals(workflow.status) && expectedSampleWorkflowId == null) {
                expectedSampleWorkflowId = workflow.workflowId;
            }
        }

        // 2. 执行异常检测
        List<AnomalyReportDTO> anomalies = analyticsService.detectAnomalies(0.3, 30);

        // 验证检测到异常
        AnomalyReportDTO detectedAnomaly = anomalies.stream()
            .filter(a -> testPolicyId.equals(a.policyId))
            .findFirst()
            .orElse(null);

        assertNotNull(detectedAnomaly, "应该检测到异常");
        assertNotNull(detectedAnomaly.sampleWorkflowId, "应该捕获 sampleWorkflowId");
        assertEquals(expectedSampleWorkflowId, detectedAnomaly.sampleWorkflowId,
            "sampleWorkflowId 应该是最新的失败 workflow");

        // 3. 持久化异常报告
        AnomalyReportEntity anomalyEntity = new AnomalyReportEntity();
        anomalyEntity.anomalyType = detectedAnomaly.anomalyType;
        anomalyEntity.versionId = detectedAnomaly.versionId;
        anomalyEntity.policyId = detectedAnomaly.policyId;
        anomalyEntity.metricValue = detectedAnomaly.metricValue;
        anomalyEntity.threshold = detectedAnomaly.threshold;
        anomalyEntity.severity = detectedAnomaly.severity;
        anomalyEntity.description = detectedAnomaly.description;
        anomalyEntity.recommendation = detectedAnomaly.recommendation;
        anomalyEntity.detectedAt = detectedAnomaly.detectedAt;
        anomalyEntity.sampleWorkflowId = detectedAnomaly.sampleWorkflowId;  // Phase 3.8
        anomalyEntity.status = "PENDING";
        anomalyEntity.persist();

        Long anomalyId = anomalyEntity.id;

        // 验证持久化成功
        AnomalyReportEntity persistedAnomaly = AnomalyReportEntity.findById(anomalyId);
        assertEquals(expectedSampleWorkflowId, persistedAnomaly.sampleWorkflowId,
            "持久化后 sampleWorkflowId 应保持一致");

        // 4. 提交验证动作
        Long actionId = workflowService.submitVerificationAction(anomalyId)
            .await().indefinitely();

        // 验证 payload 构建成功
        AnomalyActionEntity action = AnomalyActionEntity.findById(actionId);
        assertNotNull(action.payload, "payload 应该被构建");
        assertTrue(action.payload.contains("workflowId"), "payload 应包含 workflowId");
        assertTrue(action.payload.contains(expectedSampleWorkflowId.toString()),
            "payload 应包含正确的 workflow ID");

        // 验证异常状态更新
        persistedAnomaly = AnomalyReportEntity.findById(anomalyId);
        assertEquals("VERIFYING", persistedAnomaly.status, "状态应更新为 VERIFYING");
    }

    /**
     * 测试降级场景：无 sampleWorkflowId 时的优雅处理
     *
     * 验证：
     * 1. 缺少 sampleWorkflowId 时仍能提交验证动作
     * 2. payload 为 null
     * 3. 不阻塞整个流程
     */
    @Test
    @Transactional
    void testEndToEnd_GracefulDegradation_WithoutSampleWorkflowId() {
        // 1. 创建异常报告但不设置 sampleWorkflowId
        AnomalyReportEntity anomaly = new AnomalyReportEntity();
        anomaly.anomalyType = "HIGH_FAILURE_RATE";
        anomaly.versionId = testVersionId;
        anomaly.policyId = testPolicyId;
        anomaly.metricValue = 0.5;
        anomaly.threshold = 0.3;
        anomaly.severity = "CRITICAL";
        anomaly.description = "Test anomaly";
        anomaly.recommendation = "Test recommendation";
        anomaly.detectedAt = Instant.now();
        anomaly.status = "PENDING";
        anomaly.sampleWorkflowId = null;  // 缺少 sampleWorkflowId
        anomaly.persist();

        Long anomalyId = anomaly.id;

        // 2. 提交验证动作应该成功（优雅降级）
        Long actionId = assertDoesNotThrow(() -> {
            return workflowService.submitVerificationAction(anomalyId)
                .await().indefinitely();
        }, "缺少 sampleWorkflowId 时应优雅降级");

        // 3. 验证 payload 为 null
        AnomalyActionEntity action = AnomalyActionEntity.findById(actionId);
        assertNull(action.payload, "缺少 sampleWorkflowId 时 payload 应为 null");

        // 4. 验证状态仍然更新
        anomaly = AnomalyReportEntity.findById(anomalyId);
        assertEquals("VERIFYING", anomaly.status, "即使缺少 sampleWorkflowId 也应更新状态");
    }

    /**
     * 测试 Replay 验证流程 - clock_times 缺失场景
     *
     * 验证：
     * 1. 创建验证动作
     * 2. 执行验证时检测到 clock_times 缺失
     * 3. 返回 replaySucceeded=false 的结果
     */
    @Test
    @Transactional
    void testEndToEnd_ReplayVerification_MissingClockTimes() {
        // 1. 创建 workflow（无 clock_times）
        UUID workflowId = UUID.randomUUID();
        WorkflowStateEntity workflow = new WorkflowStateEntity();
        workflow.workflowId = workflowId;
        workflow.policyVersionId = testVersionId;
        workflow.startedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        workflow.status = "FAILED";
        workflow.errorMessage = "Test error";
        workflow.durationMs = 1000L;
        workflow.clockTimes = null;  // 缺少 clock_times
        workflow.persist();

        // 2. 创建异常报告
        AnomalyReportEntity anomaly = new AnomalyReportEntity();
        anomaly.anomalyType = "HIGH_FAILURE_RATE";
        anomaly.versionId = testVersionId;
        anomaly.policyId = testPolicyId;
        anomaly.metricValue = 0.5;
        anomaly.threshold = 0.3;
        anomaly.severity = "CRITICAL";
        anomaly.description = "Test anomaly";
        anomaly.recommendation = "Test recommendation";
        anomaly.detectedAt = Instant.now();
        anomaly.status = "PENDING";
        anomaly.sampleWorkflowId = workflowId;
        anomaly.persist();

        // 3. 创建验证动作
        AnomalyActionEntity action = new AnomalyActionEntity();
        action.anomalyId = anomaly.id;
        action.actionType = "VERIFY_REPLAY";
        action.status = "PENDING";
        action.payload = String.format("{\"workflowId\":\"%s\"}", workflowId);
        action.createdAt = Instant.now();
        action.persist();

        // 4. 执行 Replay 验证
        VerificationResult result = actionExecutor.executeReplayVerification(action)
            .await().indefinitely();

        // 5. 验证结果（应该优雅降级）
        assertNotNull(result, "应该返回验证结果");
        assertFalse(result.replaySucceeded(), "缺少 clock_times 时 replay 应该失败");
        assertNull(result.anomalyReproduced(), "无法执行 replay 时应为 null");
        assertEquals(workflowId.toString(), result.workflowId());
        assertNull(result.replayDurationMs(), "未执行 replay 时 duration 应为 null");
    }

    /**
     * 测试验证结果记录流程
     *
     * 验证：
     * 1. 验证结果正确序列化为 JSON
     * 2. 写入 anomaly_reports.verification_result
     * 3. 状态更新为 VERIFIED
     */
    @Test
    @Transactional
    void testEndToEnd_VerificationResultRecording() {
        // 1. 创建异常报告
        AnomalyReportEntity anomaly = new AnomalyReportEntity();
        anomaly.anomalyType = "HIGH_FAILURE_RATE";
        anomaly.versionId = testVersionId;
        anomaly.policyId = testPolicyId;
        anomaly.metricValue = 0.5;
        anomaly.threshold = 0.3;
        anomaly.severity = "CRITICAL";
        anomaly.description = "Test anomaly";
        anomaly.recommendation = "Test recommendation";
        anomaly.detectedAt = Instant.now();
        anomaly.status = "VERIFYING";
        anomaly.persist();

        Long anomalyId = anomaly.id;

        // 2. 构建验证结果
        UUID testWorkflowId = UUID.randomUUID();
        VerificationResult result = new VerificationResult(
            true,   // replaySucceeded
            true,   // anomalyReproduced
            testWorkflowId.toString(),
            Instant.now(),
            1000L,  // originalDurationMs
            1050L   // replayDurationMs
        );

        // 3. 记录验证结果
        Boolean success = workflowService.recordVerificationResult(anomalyId, result)
            .await().indefinitely();

        assertTrue(success, "记录验证结果应成功");

        // 4. 验证结果写入成功
        anomaly = AnomalyReportEntity.findById(anomalyId);
        assertEquals("VERIFIED", anomaly.status, "状态应更新为 VERIFIED");
        assertNotNull(anomaly.verificationResult, "验证结果应被写入");
        assertTrue(anomaly.verificationResult.contains(testWorkflowId.toString()),
            "验证结果应包含 workflow ID");
        assertTrue(anomaly.verificationResult.contains("\"replaySucceeded\":true"),
            "验证结果应包含 replaySucceeded");
        assertTrue(anomaly.verificationResult.contains("\"anomalyReproduced\":true"),
            "验证结果应包含 anomalyReproduced");
    }

}
