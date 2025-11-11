package io.aster.audit.service;

import io.aster.audit.entity.AnomalyActionEntity;
import io.aster.audit.entity.AnomalyReportEntity;
import io.aster.audit.rest.model.VerificationResult;
import io.aster.policy.entity.PolicyVersion;
import io.aster.policy.service.PolicyVersionService;
import io.aster.workflow.WorkflowSchedulerService;
import io.aster.workflow.WorkflowStateEntity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.io.StringReader;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * 异常响应动作执行器（Phase 3.7）
 *
 * 采用 orchestration 模式调用现有服务：
 * - Replay 验证：调用 WorkflowSchedulerService.processWorkflow
 * - 自动回滚：调用 PolicyVersionService.rollbackToVersion
 * - 不重新实现业务逻辑，仅作为编排层
 */
@ApplicationScoped
public class AnomalyActionExecutor {

    private static final Logger LOG = Logger.getLogger(AnomalyActionExecutor.class);

    @Inject
    WorkflowSchedulerService workflowScheduler;

    @Inject
    PolicyVersionService policyVersionService;

    @Inject
    AnomalyWorkflowService anomalyWorkflowService;

    /**
     * 执行 Replay 验证
     *
     * 流程：
     * 1. 从 payload 提取 workflowId
     * 2. 查询 WorkflowStateEntity，检查 clockTimes
     * 3. 调用 WorkflowSchedulerService.processWorkflow 触发 Replay
     * 4. 比对原始执行与 Replay 结果
     * 5. 构建 VerificationResult
     * 6. 调用 AnomalyWorkflowService.recordVerificationResult
     *
     * @param action 异常动作实体
     * @return 验证结果
     */
    @Transactional
    public Uni<VerificationResult> executeReplayVerification(AnomalyActionEntity action) {
        try {
            // 1. 从 payload 提取 workflowId
            UUID workflowId = extractWorkflowId(action.payload);
            if (workflowId == null) {
                return Uni.createFrom().failure(
                    new IllegalArgumentException("Payload 缺少 workflowId")
                );
            }

            // 2. 查询原始 WorkflowStateEntity
            WorkflowStateEntity originalWorkflow = WorkflowStateEntity.findByWorkflowId(workflowId)
                .orElse(null);

            if (originalWorkflow == null) {
                return Uni.createFrom().failure(
                    new IllegalArgumentException("Workflow 不存在: workflowId=" + workflowId)
                );
            }

            // 检查是否有 clockTimes（确定性时间记录）
            if (originalWorkflow.clockTimes == null || originalWorkflow.clockTimes.isBlank()) {
                LOG.warnf("Workflow %s 没有 clockTimes，降级为普通执行", workflowId);
                // 对于没有 clockTimes 的旧 workflow，返回无法验证的结果
                VerificationResult result = new VerificationResult(
                    false,  // replaySucceeded
                    null,   // anomalyReproduced (无法判断)
                    workflowId.toString(),
                    Instant.now(),
                    originalWorkflow.durationMs,
                    null    // replayDurationMs (未执行)
                );
                return Uni.createFrom().item(result);
            }

            // 3. 记录原始执行结果
            String originalStatus = originalWorkflow.status;
            Long originalDurationMs = originalWorkflow.durationMs;
            String originalErrorMessage = originalWorkflow.errorMessage;

            LOG.infof("开始 Replay 验证: workflowId=%s, originalStatus=%s, originalDurationMs=%d",
                workflowId, originalStatus, originalDurationMs);

            // 4. Phase 3.8: 调用 WorkflowSchedulerService.replayWorkflow() 触发 Replay
            return workflowScheduler.replayWorkflow(workflowId)
                .onItem().transformToUni(replayWorkflow -> {
                    LOG.infof("Replay 完成: workflowId=%s, replayStatus=%s, replayDurationMs=%d",
                        workflowId, replayWorkflow.status, replayWorkflow.durationMs);

                    // 5. 比对结果
                    boolean anomalyReproduced = compareResults(
                        originalStatus, replayWorkflow.status,
                        originalErrorMessage, replayWorkflow.errorMessage,
                        originalDurationMs, replayWorkflow.durationMs
                    );

                    // 6. 构建验证结果
                    VerificationResult result = new VerificationResult(
                        true,  // replaySucceeded
                        anomalyReproduced,
                        workflowId.toString(),
                        Instant.now(),
                        originalDurationMs,
                        replayWorkflow.durationMs
                    );

                    // 7. 记录验证结果到 anomaly_reports（响应式）
                    return anomalyWorkflowService.recordVerificationResult(action.anomalyId, result)
                        .replaceWith(result);
                })
                .onFailure(IllegalStateException.class).recoverWithUni(e -> {
                    // Clock_times 缺失，无法 replay
                    LOG.warnf("Replay 验证失败（clock_times 缺失）: %s", e.getMessage());
                    VerificationResult result = new VerificationResult(
                        false,  // replaySucceeded
                        null,   // anomalyReproduced (无法判断)
                        workflowId.toString(),
                        Instant.now(),
                        originalDurationMs,
                        null    // replayDurationMs (未执行)
                    );
                    // 即使失败也要记录结果（响应式）
                    return anomalyWorkflowService.recordVerificationResult(action.anomalyId, result)
                        .replaceWith(result);
                })
                .onFailure(TimeoutException.class).recoverWithUni(e -> {
                    // Replay 超时
                    LOG.errorf("Replay 验证超时: workflowId=%s", workflowId);
                    VerificationResult result = new VerificationResult(
                        false,  // replaySucceeded
                        null,   // anomalyReproduced (无法判断)
                        workflowId.toString(),
                        Instant.now(),
                        originalDurationMs,
                        null    // replayDurationMs (未执行)
                    );
                    // 即使超时也要记录结果（响应式）
                    return anomalyWorkflowService.recordVerificationResult(action.anomalyId, result)
                        .replaceWith(result);
                })
                .onFailure().invoke(e -> {
                    LOG.errorf(e, "Replay 验证失败: action=%d, workflowId=%s", action.id, workflowId);
                });

        } catch (Exception e) {
            LOG.errorf(e, "Replay 验证准备失败: action=%d", action.id);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * 执行自动回滚
     *
     * 流程：
     * 1. 从 payload 提取 targetVersion
     * 2. 查询异常报告获取 policyId
     * 3. 调用 PolicyVersionService.rollbackToVersion
     * 4. 发布 AuditEvent.anomalyAutoRollback
     *
     * @param action 异常动作实体
     * @return 回滚是否成功
     */
    @Transactional
    public Uni<Boolean> executeAutoRollback(AnomalyActionEntity action) {
        try {
            // 1. 查询异常报告
            AnomalyReportEntity anomaly = AnomalyReportEntity.findById(action.anomalyId);
            if (anomaly == null) {
                return Uni.createFrom().failure(
                    new IllegalArgumentException("异常报告不存在: anomalyId=" + action.anomalyId)
                );
            }

            // 2. 从 payload 提取 targetVersion
            Long targetVersion = extractTargetVersion(action.payload);
            if (targetVersion == null) {
                return Uni.createFrom().failure(
                    new IllegalArgumentException("Payload 缺少 targetVersion")
                );
            }

            // 3. 获取当前活跃版本
            PolicyVersion currentVersion = policyVersionService.getActiveVersion(anomaly.policyId);
            Long fromVersion = currentVersion != null ? currentVersion.version : null;

            // 4. 调用 PolicyVersionService.rollbackToVersion
            policyVersionService.rollbackToVersion(anomaly.policyId, targetVersion);

            LOG.infof("异常 %d 触发自动回滚: %s from %d to %d",
                action.anomalyId, anomaly.policyId, fromVersion, targetVersion);

            return Uni.createFrom().item(true);

        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "自动回滚失败: action=%d", action.id);
            return Uni.createFrom().failure(e);
        } catch (Exception e) {
            LOG.errorf(e, "自动回滚执行异常: action=%d", action.id);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * 比对原始执行与 Replay 结果（Phase 3.8 增强）
     *
     * 判断异常是否在 Replay 中重现：
     * - 如果原始和 Replay 都失败（status=FAILED）→ 异常重现
     * - 如果原始失败但 Replay 成功 → 异常未重现（可能是瞬态问题）
     * - 其他情况 → 异常未重现
     *
     * Phase 3.8 增强：
     * - 比对错误消息（如果都失败，记录错误消息是否一致）
     * - 比对持续时间（允许 10% 误差）
     *
     * @param originalStatus       原始执行状态
     * @param replayStatus         Replay 执行状态
     * @param originalErrorMessage 原始错误消息
     * @param replayErrorMessage   Replay 错误消息
     * @param originalDurationMs   原始持续时间
     * @param replayDurationMs     Replay 持续时间
     * @return true 表示异常重现
     */
    private boolean compareResults(String originalStatus, String replayStatus,
                                    String originalErrorMessage, String replayErrorMessage,
                                    Long originalDurationMs, Long replayDurationMs) {
        // 1. 状态比对（主要判断标准）
        if ("FAILED".equals(originalStatus) && "FAILED".equals(replayStatus)) {
            // 2. 错误消息比对（辅助判断）
            if (originalErrorMessage != null && replayErrorMessage != null) {
                if (!originalErrorMessage.equals(replayErrorMessage)) {
                    LOG.warnf("异常重现但错误消息不同 - 原始: %s, Replay: %s",
                        originalErrorMessage, replayErrorMessage);
                }
            }

            // 3. 持续时间比对（允许 10% 误差）
            if (originalDurationMs != null && replayDurationMs != null) {
                long diff = Math.abs(replayDurationMs - originalDurationMs);
                double diffPercent = (double) diff / originalDurationMs * 100;
                if (diffPercent > 10) {
                    LOG.warnf("持续时间差异较大 - 原始: %dms, Replay: %dms, 差异: %.1f%%",
                        originalDurationMs, replayDurationMs, diffPercent);
                }
            }

            return true;  // 异常重现
        }

        return false;  // 异常未重现或无法判断
    }

    /**
     * 从 JSON payload 提取 workflowId
     */
    private UUID extractWorkflowId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonReader reader = Json.createReader(new StringReader(payload));
            JsonObject json = reader.readObject();
            String workflowIdStr = json.getString("workflowId", null);
            return workflowIdStr != null ? UUID.fromString(workflowIdStr) : null;
        } catch (Exception e) {
            LOG.warnf("解析 payload 失败: %s", e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON payload 提取 targetVersion
     */
    private Long extractTargetVersion(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonReader reader = Json.createReader(new StringReader(payload));
            JsonObject json = reader.readObject();
            return json.getJsonNumber("targetVersion").longValue();
        } catch (Exception e) {
            LOG.warnf("解析 payload 失败: %s", e.getMessage());
            return null;
        }
    }
}
