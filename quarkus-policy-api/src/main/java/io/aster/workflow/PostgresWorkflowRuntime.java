package io.aster.workflow;

import aster.runtime.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
            WorkflowStateEntity.getOrCreate(wfUuid);

            // 追加 WorkflowStarted 事件
            Map<String, Object> startPayload = Map.of(
                    "metadata", serializeMetadata(metadata),
                    "idempotencyKey", idempotencyKey != null ? idempotencyKey : ""
            );
            eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_STARTED, startPayload);

            // 记录指标
            metrics.recordWorkflowStarted();

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
}
