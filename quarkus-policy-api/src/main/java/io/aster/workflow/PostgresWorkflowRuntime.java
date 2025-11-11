package io.aster.workflow;

import aster.runtime.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.List;
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
     * Clock 缓存（Phase 3.6）
     * 维护 workflowId → ReplayDeterministicClock 映射，确保同一 workflow 使用同一 clock 实例
     */
    private final Map<String, ReplayDeterministicClock> clocks = new ConcurrentHashMap<>();

    /**
     * ThreadLocal 传递当前 workflowId（Phase 3.6）
     * 用于在不改变 WorkflowRuntime 接口签名的情况下传递上下文
     */
    private static final ThreadLocal<String> currentWorkflowId = new ThreadLocal<>();

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
     * 设置当前线程的 workflowId 上下文（Phase 3.6）
     *
     * @param workflowId workflow 唯一标识符
     */
    public static void setCurrentWorkflowId(String workflowId) {
        currentWorkflowId.set(workflowId);
    }

    /**
     * 清除当前线程的 workflowId 上下文（Phase 3.6）
     */
    public static void clearCurrentWorkflowId() {
        currentWorkflowId.remove();
    }

    /**
     * 获取确定性时钟（Phase 3.6 改造）
     *
     * 从 ThreadLocal 获取 workflowId，返回对应的缓存 clock 实例。
     * 如果没有设置 workflowId，降级为非缓存模式（创建新实例）。
     *
     * @return 确定性时钟实例
     */
    @Override
    public DeterministicClock getClock() {
        String workflowId = currentWorkflowId.get();
        if (workflowId == null) {
            // 降级为非缓存模式
            return new ReplayDeterministicClock();
        }
        return clocks.computeIfAbsent(workflowId, id -> new ReplayDeterministicClock());
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
     * 关闭运行时（Phase 3.6 改造）
     *
     * 取消所有待完成的 future，清理所有 clock 缓存。
     */
    @Override
    public void shutdown() {
        Log.info("Shutting down PostgresWorkflowRuntime");
        resultFutures.values().forEach(future -> future.cancel(true));
        resultFutures.clear();
        // Phase 3.6: 清理所有 clock 缓存
        clocks.clear();
    }

    /**
     * 完成 workflow 执行（Phase 3.6 改造）
     *
     * 由调度器在 workflow 完成时调用，设置结果并完成 future。
     * 同时持久化 clock_times 并清理 clock 缓存。
     *
     * @param workflowId workflow 唯一标识符
     * @param result 执行结果
     */
    public void completeWorkflow(String workflowId, Object result) {
        // Phase 3.6: 持久化 clock_times
        ReplayDeterministicClock clock = clocks.remove(workflowId);
        if (clock != null) {
            try {
                Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));
                if (stateOpt.isPresent()) {
                    WorkflowStateEntity state = stateOpt.get();
                    state.clockTimes = serializeClockTimes(clock);
                    state.persist();
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to persist clock_times for workflow %s, continuing", workflowId);
            }
        }

        // 原有 resultFutures 处理逻辑
        CompletableFuture<Object> future = resultFutures.remove(workflowId);
        if (future != null) {
            future.complete(result);
        }
    }

    /**
     * 使 workflow 执行失败（Phase 3.6 改造）
     *
     * 由调度器在 workflow 失败时调用，设置异常并完成 future。
     * 同时持久化 clock_times 并清理 clock 缓存。
     *
     * @param workflowId workflow 唯一标识符
     * @param error 失败原因
     */
    public void failWorkflow(String workflowId, Throwable error) {
        // Phase 3.6: 持久化 clock_times（与 completeWorkflow 相同逻辑）
        ReplayDeterministicClock clock = clocks.remove(workflowId);
        if (clock != null) {
            try {
                Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));
                if (stateOpt.isPresent()) {
                    WorkflowStateEntity state = stateOpt.get();
                    state.clockTimes = serializeClockTimes(clock);
                    state.persist();
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to persist clock_times for workflow %s, continuing", workflowId);
            }
        }

        // 原有 resultFutures 处理逻辑
        CompletableFuture<Object> future = resultFutures.remove(workflowId);
        if (future != null) {
            future.completeExceptionally(error);
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
     * 定时清理过期的 resultFutures 和 clock 缓存（Phase 3.6 改造）
     *
     * 防止内存泄漏：清理超过 TTL 的 workflow 结果 future 和 clock 实例。
     * 默认每小时执行一次，TTL 默认 24 小时。
     */
    @Scheduled(every = "1h")
    void cleanupExpiredFutures() {
        Instant threshold = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
        int removedFutures = 0;
        int removedClocks = 0;

        // 清理 resultFutures
        for (Map.Entry<String, CompletableFuture<Object>> entry : resultFutures.entrySet()) {
            String workflowId = entry.getKey();
            try {
                Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));

                // 清理条件：workflow 不存在或已超过 TTL
                if (stateOpt.isEmpty() || stateOpt.get().updatedAt.isBefore(threshold)) {
                    resultFutures.remove(workflowId);
                    removedFutures++;
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to check workflow %s during cleanup, skipping", workflowId);
            }
        }

        // Phase 3.6: 清理 clock 缓存
        for (Map.Entry<String, ReplayDeterministicClock> entry : clocks.entrySet()) {
            String workflowId = entry.getKey();
            try {
                Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));

                // 清理条件：workflow 不存在、已完成、已失败或已超过 TTL
                if (stateOpt.isEmpty() ||
                    "COMPLETED".equals(stateOpt.get().status) ||
                    "FAILED".equals(stateOpt.get().status) ||
                    stateOpt.get().updatedAt.isBefore(threshold)) {
                    clocks.remove(workflowId);
                    removedClocks++;
                }
            } catch (Exception e) {
                Log.warnf(e, "Failed to check workflow %s clock during cleanup, skipping", workflowId);
            }
        }

        if (removedFutures > 0 || removedClocks > 0) {
            Log.infof("Cleaned up %d expired workflow result futures and %d clock instances", removedFutures, removedClocks);
            if (removedFutures > 0) {
                metrics.recordResultFuturesCleaned(removedFutures);
            }
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
     * 序列化 clock_times 为 JSONB 字符串（Phase 3.6）
     *
     * 将 ReplayDeterministicClock 的时间决策序列持久化为 ClockTimesSnapshot。
     * 序列化失败时返回 null，不阻塞 workflow 完成。
     *
     * @param clock 确定性时钟实例
     * @return JSONB 字符串，失败时返回 null
     */
    private String serializeClockTimes(ReplayDeterministicClock clock) {
        if (clock == null) {
            return null;
        }
        try {
            List<Instant> recordedTimes = clock.getRecordedTimes();
            if (recordedTimes.isEmpty()) {
                return null; // 没有记录时间，无需持久化
            }

            // 创建快照，replayIndex 重置为 0，replayMode 设为 false
            ClockTimesSnapshot snapshot = new ClockTimesSnapshot(
                recordedTimes,
                0, // 持久化后总是从头重放
                false // 持久化后退出 replay 模式
            );

            // 创建独立的 ObjectMapper 并注册 JavaTimeModule
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            Log.errorf(e, "Failed to serialize clock times, skipping persistence");
            return null; // 序列化失败不影响 workflow 完成
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
