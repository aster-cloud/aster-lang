package io.aster.audit.service;

import io.aster.audit.entity.AnomalyActionEntity;
import io.aster.audit.entity.AnomalyReportEntity;
import io.aster.audit.rest.model.VerificationResult;
import io.aster.policy.event.AuditEvent;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.transaction.Transactional;
import java.time.Instant;

/**
 * 异常工作流服务（Phase 3.7）
 *
 * 采用 orchestration 模式负责异常状态机编排：
 * - 仅负责状态迁移和事件发布
 * - 不实现具体业务逻辑（Replay/Rollback 由 AnomalyActionExecutor 调用现有服务）
 * - 通过 Event<AuditEvent> 集成到现有审计体系
 */
@ApplicationScoped
public class AnomalyWorkflowService {

    @Inject
    Event<AuditEvent> auditEvent;

    /**
     * 提交验证动作到队列
     *
     * 状态迁移：PENDING → VERIFYING
     * 创建 anomaly_actions 记录（actionType=VERIFY_REPLAY, status=PENDING）
     *
     * @param anomalyId 异常报告 ID
     * @return 创建的动作 ID
     */
    @Transactional
    public Uni<Long> submitVerificationAction(Long anomalyId) {
        // 查询异常实体
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(anomalyId);
        if (anomaly == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("异常报告不存在: anomalyId=" + anomalyId)
            );
        }

        // 更新异常状态为 VERIFYING
        anomaly.status = "VERIFYING";
        anomaly.persist();

        // Phase 3.8: 构建 payload（从 sampleWorkflowId 提取）
        String payload = null;
        if (anomaly.sampleWorkflowId != null) {
            payload = Json.createObjectBuilder()
                .add("workflowId", anomaly.sampleWorkflowId.toString())
                .build()
                .toString();
            Log.infof("提交验证动作: anomalyId=%d, workflowId=%s", anomalyId, anomaly.sampleWorkflowId);
        } else {
            Log.warnf("异常报告 %d 缺少 sampleWorkflowId，验证动作将创建但可能无法执行 Replay", anomalyId);
        }

        // 创建验证动作
        AnomalyActionEntity action = new AnomalyActionEntity();
        action.anomalyId = anomalyId;
        action.actionType = "VERIFY_REPLAY";
        action.status = "PENDING";
        action.payload = payload;  // Phase 3.8: 填充 payload
        action.createdAt = Instant.now();
        action.persist();

        // 发布审计事件
        auditEvent.fireAsync(AuditEvent.anomalyVerification(
            anomaly.policyId,
            anomalyId,
            "VERIFY_REPLAY"
        ));

        return Uni.createFrom().item(action.id);
    }

    /**
     * 更新异常状态
     *
     * 支持的状态转换：
     * - VERIFYING → VERIFIED
     * - VERIFIED → RESOLVED
     * - VERIFIED → DISMISSED
     * - PENDING → DISMISSED
     *
     * @param anomalyId 异常报告 ID
     * @param newStatus 新状态
     * @param notes     处置备注
     * @return 成功标志
     */
    @Transactional
    public Uni<Boolean> updateStatus(Long anomalyId, String newStatus, String notes) {
        // 查询异常实体
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(anomalyId);
        if (anomaly == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("异常报告不存在: anomalyId=" + anomalyId)
            );
        }

        String oldStatus = anomaly.status;

        // 更新状态
        anomaly.status = newStatus;
        if (notes != null && !notes.isBlank()) {
            anomaly.resolutionNotes = notes;
        }

        // 如果状态为 RESOLVED 或 DISMISSED，记录解决时间
        if ("RESOLVED".equals(newStatus) || "DISMISSED".equals(newStatus)) {
            anomaly.resolvedAt = Instant.now();
        }

        anomaly.persist();

        // 发布审计事件
        auditEvent.fireAsync(AuditEvent.anomalyStatusChange(
            anomaly.policyId,
            anomalyId,
            oldStatus,
            newStatus,
            notes
        ));

        return Uni.createFrom().item(true);
    }

    /**
     * 记录验证结果
     *
     * 将 VerificationResult 序列化为 JSONB 并写入 verification_result 字段
     *
     * @param anomalyId          异常报告 ID
     * @param verificationResult 验证结果对象
     * @return 成功标志
     */
    @Transactional
    public Uni<Boolean> recordVerificationResult(Long anomalyId, VerificationResult verificationResult) {
        // 查询异常实体
        AnomalyReportEntity anomaly = AnomalyReportEntity.findById(anomalyId);
        if (anomaly == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("异常报告不存在: anomalyId=" + anomalyId)
            );
        }

        // 序列化验证结果为 JSON 字符串
        String jsonResult = verificationResult.toJson().toString();
        anomaly.verificationResult = jsonResult;

        // 状态迁移：VERIFYING → VERIFIED
        anomaly.status = "VERIFIED";
        anomaly.persist();

        return Uni.createFrom().item(true);
    }
}
