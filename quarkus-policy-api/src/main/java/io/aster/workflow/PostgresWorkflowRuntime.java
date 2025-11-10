package io.aster.workflow;

import aster.runtime.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aster.policy.entity.PolicyVersion;
import io.aster.policy.service.PolicyVersionService;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL 持久化 Workflow 运行时
 *
 * 实现 durable execution，支持：
 * - 幂等性保证（通过 UNIQUE 约束）
 * - 确定性时间（通过 ReplayDeterministicClock）
 * - 事件重放（从事件流恢复状态）
 * - 高可用（通过 PostgreSQL 持久化）
 */
@ApplicationScoped
public class PostgresWorkflowRuntime implements WorkflowRuntime {

    @Inject
    PostgresEventStore eventStore;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WorkflowMetrics metrics;

    @Inject
    PolicyVersionService policyVersionService;

    @ConfigProperty(name = "workflow.result-futures.ttl-hours", defaultValue = "24")
    int ttlHours;

    // 内存中维护的结果 future，用于返回给调用方
    // 实际生产环境可能需要分布式缓存（如 Redis）或轮询机制
    private final Map<String, CompletableFuture<Object>> resultFutures = new ConcurrentHashMap<>();

    /**
     * 调度 workflow 执行
     *
     * 实现幂等性：
     * - 如果 idempotencyKey 已存在，返回现有执行句柄
     * - 如果不存在，追加 WorkflowStarted 事件并创建新句柄
     *
     * @param workflowId workflow 唯一标识符
     * @param idempotencyKey 幂等性键（可选）
     * @param metadata workflow 元数据
     * @return 执行句柄
     */
    @Transactional
    @Override
    public ExecutionHandle schedule(String workflowId, String idempotencyKey, WorkflowMetadata metadata) {
        try {
            UUID wfUuid = UUID.fromString(workflowId);

            // 幂等性检查
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                Optional<WorkflowEventEntity> existing = WorkflowEventEntity.findByIdempotencyKey(wfUuid, idempotencyKey);
                if (existing.isPresent()) {
                    Log.debugf("Workflow %s already started with idempotency key %s", workflowId, idempotencyKey);

                    // 检查 workflow 状态
                    Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(wfUuid);
                    if (stateOpt.isPresent()) {
                        WorkflowStateEntity state = stateOpt.get();
                        if ("COMPLETED".equals(state.status) || "FAILED".equals(state.status)) {
                            // 已完成，返回结果
                            return new CompletedExecutionHandle(workflowId, state.result);
                        }
                    }

                    // 仍在执行中，返回待完成句柄
                    CompletableFuture<Object> future = resultFutures.computeIfAbsent(
                            workflowId,
                            id -> new CompletableFuture<>()
                    );
                    return new PendingExecutionHandle(workflowId, future);
                }
            }

            // 创建 workflow 状态（如果不存在）
            WorkflowStateEntity state = WorkflowStateEntity.getOrCreate(wfUuid);

            // Phase 3.4: 标记 workflow 开始时间
            state.markStarted();

            // Phase 3.1: 注入策略版本信息
            enrichPolicyVersion(metadata, state);
            state.persist(); // 保存版本信息

            boolean alreadyStarted = WorkflowEventEntity.hasEvent(wfUuid, WorkflowEvent.Type.WORKFLOW_STARTED);
            if (!alreadyStarted) {
                Map<String, Object> startPayload = Map.of(
                        "metadata", serializeMetadata(metadata),
                        "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""
                );
                eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_STARTED, startPayload);
                metrics.recordWorkflowStarted();
            } else {
                Log.debugf("Workflow %s already recorded WorkflowStarted event, skipping duplicate append", workflowId);
            }

            Log.infof("Scheduled workflow %s with idempotency key %s", workflowId, idempotencyKey);

            // 创建结果 future
            CompletableFuture<Object> future = resultFutures.computeIfAbsent(
                    workflowId,
                    id -> new CompletableFuture<>()
            );

            return new PendingExecutionHandle(workflowId, future);

        } catch (Exception e) {
            Log.errorf(e, "Failed to schedule workflow %s", workflowId);
            throw new RuntimeException("Failed to schedule workflow", e);
        }
    }

    /**
     * 获取确定性时钟
     *
     * 每个 workflow 执行需要独立的时钟实例。
     * 调用方负责在重放时调用 enterReplayMode()。
     *
     * @return 确定性时钟实例
     */
    @Override
    public DeterministicClock getClock() {
        return new ReplayDeterministicClock();
    }

    /**
     * 获取事件存储
     *
     * @return 事件存储实例
     */
    @Override
    public EventStore getEventStore() {
        return eventStore;
    }

    /**
     * 关闭运行时
     *
     * 取消所有待完成的 future。
     */
    @Override
    public void shutdown() {
        Log.info("Shutting down PostgresWorkflowRuntime");
        resultFutures.values().forEach(future -> future.cancel(true));
        resultFutures.clear();
    }

    /**
     * 完成 workflow 执行
     *
     * 由调度器在 workflow 完成时调用，设置结果并完成 future。
     *
     * @param workflowId workflow 唯一标识符
     * @param result 执行结果
     */
    public void completeWorkflow(String workflowId, Object result) {
        CompletableFuture<Object> future = resultFutures.get(workflowId);
        if (future != null) {
            future.complete(result);
            resultFutures.remove(workflowId);
        }
    }

    /**
     * 使 workflow 执行失败
     *
     * 由调度器在 workflow 失败时调用，设置异常并完成 future。
     *
     * @param workflowId workflow 唯一标识符
     * @param error 失败原因
     */
    public void failWorkflow(String workflowId, Throwable error) {
        CompletableFuture<Object> future = resultFutures.get(workflowId);
        if (future != null) {
            future.completeExceptionally(error);
            resultFutures.remove(workflowId);
        }
    }

    /**
     * 获取结果 future（用于调度器）
     *
     * @param workflowId workflow 唯一标识符
     * @return 结果 future
     */
    public CompletableFuture<Object> getResultFuture(String workflowId) {
        return resultFutures.computeIfAbsent(workflowId, id -> new CompletableFuture<>());
    }

    /**
     * 定时清理过期的 resultFutures
     *
     * 防止内存泄漏：清理超过 TTL 的 workflow 结果 future。
     * 默认每小时执行一次，TTL 默认 24 小时。
     */
    @Scheduled(every = "1h")
    void cleanupExpiredFutures() {
        Instant threshold = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
        int removed = 0;

        for (Map.Entry<String, CompletableFuture<Object>> entry : resultFutures.entrySet()) {
            String workflowId = entry.getKey();
            try {
                Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));

                // 清理条件：workflow 不存在或已超过 TTL
                if (stateOpt.isEmpty() || stateOpt.get().updatedAt.isBefore(threshold)) {
                    resultFutures.remove(workflowId);
                    removed++;
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to check workflow %s during cleanup, skipping", workflowId);
            }
        }

        if (removed > 0) {
            Log.infof("Cleaned up %d expired workflow result futures", removed);
            metrics.recordResultFuturesCleaned(removed);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 序列化元数据为 JSON 字符串
     */
    private String serializeMetadata(WorkflowMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    /**
     * 注入策略版本信息（Phase 3.1）
     *
     * 如果元数据包含 policyId，自动查询当前活跃版本并注入到元数据中。
     * 用于审计追踪：记录每次 workflow 执行使用的具体策略版本。
     *
     * @param metadata workflow 元数据
     * @param state workflow 状态实体（用于记录版本信息）
     */
    private void enrichPolicyVersion(WorkflowMetadata metadata, WorkflowStateEntity state) {
        // 检查元数据是否包含 policyId
        String policyId = metadata.get(WorkflowMetadata.Keys.POLICY_ID, String.class);
        if (policyId == null || policyId.isEmpty()) {
            return; // 无策略信息，跳过
        }

        try {
            // 查询当前活跃版本
            PolicyVersion activeVersion = policyVersionService.getActiveVersion(policyId);

            if (activeVersion != null) {
                // 注入到元数据
                metadata.setPolicyVersion(policyId, activeVersion.version, activeVersion.id);

                // 记录到 workflow_state
                state.policyVersionId = activeVersion.id;
                state.policyActivatedAt = Instant.now();

                Log.debugf("Enriched workflow with policy version: %s v%d (id=%d)",
                        policyId, activeVersion.version, activeVersion.id);
            } else {
                Log.warnf("No active version found for policyId=%s, workflow will proceed without version tracking", policyId);
            }
        } catch (Exception e) {
            // 版本查询失败不影响 workflow 执行，仅记录警告
            Log.warnf(e, "Failed to enrich policy version for policyId=%s, workflow will proceed without version tracking", policyId);
        }
    }
}
