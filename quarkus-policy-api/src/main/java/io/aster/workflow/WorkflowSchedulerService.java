package io.aster.workflow;

import aster.runtime.workflow.WorkflowEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * PostgreSQL-backed Workflow 调度器服务
 *
 * 核心特性：
 * - LISTEN/NOTIFY 事件驱动调度（低延迟）
 * - SELECT FOR UPDATE SKIP LOCKED 分布式锁（避免冲突）
 * - 定时器轮询（处理 orphan timers）
 * - 事件重放恢复状态
 */
@ApplicationScoped
public class WorkflowSchedulerService {

    @Inject
    PostgresEventStore eventStore;

    @Inject
    PostgresWorkflowRuntime workflowRuntime;

    @Inject
    EntityManager entityManager;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SagaCompensationExecutor compensationExecutor;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "postgresql")
    String dbKind;

    private ExecutorService executorService;
    private ScheduledExecutorService timerPollingService;
    private volatile boolean running = false;

    // Worker 标识符
    private final String workerId = UUID.randomUUID().toString().substring(0, 8);

    /**
     * 服务启动
     */
    void onStart(@Observes StartupEvent ev) {
        Log.infof("Starting WorkflowSchedulerService (worker=%s)", workerId);

        // 创建工作线程池（可配置）
        int workerCount = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(workerCount);

        // 启动定时器轮询服务
        timerPollingService = Executors.newScheduledThreadPool(1);
        timerPollingService.scheduleAtFixedRate(
                this::pollTimers,
                0,
                5,
                TimeUnit.SECONDS
        );

        // 启动就绪 workflow 轮询（LISTEN/NOTIFY 的简化替代）
        // 实际生产环境应使用 PostgreSQL LISTEN/NOTIFY
        timerPollingService.scheduleAtFixedRate(
                this::pollReadyWorkflows,
                0,
                1,
                TimeUnit.SECONDS
        );

        running = true;
        Log.info("WorkflowSchedulerService started");
    }

    /**
     * 服务关闭
     */
    void onStop(@Observes ShutdownEvent ev) {
        Log.info("Stopping WorkflowSchedulerService");
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        if (timerPollingService != null) {
            timerPollingService.shutdown();
            try {
                if (!timerPollingService.awaitTermination(5, TimeUnit.SECONDS)) {
                    timerPollingService.shutdownNow();
                }
            } catch (InterruptedException e) {
                timerPollingService.shutdownNow();
            }
        }

        Log.info("WorkflowSchedulerService stopped");
    }

    /**
     * 轮询就绪的 workflow
     *
     * 使用 SELECT FOR UPDATE SKIP LOCKED 避免多 worker 冲突。
     * 实际生产环境应使用 LISTEN/NOTIFY 代替轮询。
     */
    private void pollReadyWorkflows() {
        if (!running) return;

        try {
            List<WorkflowStateEntity> readyWorkflows = findReadyWorkflows(10);

            for (WorkflowStateEntity state : readyWorkflows) {
                executorService.submit(() -> processWorkflow(state.workflowId.toString()));
            }

        } catch (Exception e) {
            Log.errorf(e, "Error polling ready workflows");
        }
    }

    /**
     * 查找就绪的 workflow（使用 SKIP LOCKED）
     *
     * @param limit 最大返回数量
     * @return 就绪的 workflow 列表
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public List<WorkflowStateEntity> findReadyWorkflows(int limit) {
        // 使用原生 SQL 查询以支持 PostgreSQL 的 FOR UPDATE SKIP LOCKED 语法
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM workflow_state " +
                "WHERE status IN ('READY', 'RUNNING') " +
                "AND (lock_expires_at IS NULL OR lock_expires_at < :now) " +
                "ORDER BY created_at"
        );
        if (supportsSkipLocked()) {
            sql.append(" FOR UPDATE SKIP LOCKED");
        }
        sql.append(" LIMIT :limit");

        return entityManager.createNativeQuery(sql.toString(), WorkflowStateEntity.class)
        .setParameter("now", Instant.now())
        .setParameter("limit", limit)
        .getResultList();
    }

    private boolean supportsSkipLocked() {
        return !"h2".equalsIgnoreCase(dbKind);
    }

    /**
     * 处理单个 workflow
     *
     * @param workflowId workflow 唯一标识符
     */
    @Transactional
    public void processWorkflow(String workflowId) {
        // Phase 3.6: 设置 ThreadLocal 上下文传递 workflowId
        PostgresWorkflowRuntime.setCurrentWorkflowId(workflowId);
        try {
            UUID wfUuid = UUID.fromString(workflowId);

            // 查找并锁定 workflow 状态
            Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(wfUuid);
            if (stateOpt.isEmpty()) {
                Log.warnf("Workflow %s not found", workflowId);
                return;
            }

            WorkflowStateEntity state = stateOpt.get();

            // Phase 3.6: 加载并激活 replay 模式
            if (state.clockTimes != null && !state.clockTimes.isBlank()) {
                ClockTimesSnapshot snapshot = deserializeClockTimes(state.clockTimes);
                if (snapshot != null && !snapshot.recordedTimes.isEmpty()) {
                    Object clockObj = workflowRuntime.getClock();
                    if (clockObj instanceof ReplayDeterministicClock) {
                        ReplayDeterministicClock clock = (ReplayDeterministicClock) clockObj;
                        clock.enterReplayMode(snapshot.recordedTimes);
                        Log.infof("Workflow %s entered replay mode with %d recorded times",
                                workflowId, snapshot.recordedTimes.size());
                    }
                }
            }

            // 检查是否已被其他 worker 处理
            if (state.lockOwner != null && state.lockExpiresAt != null &&
                state.lockExpiresAt.isAfter(Instant.now())) {
                Log.debugf("Workflow %s is locked by another worker", workflowId);
                return;
            }

            String previousStatus = state.status;

            // 加锁
            state.lockOwner = workerId;
            state.lockExpiresAt = Instant.now().plusSeconds(60);
            if ("COMPENSATING".equals(previousStatus) || "COMPENSATED".equals(previousStatus)
                    || "COMPENSATION_FAILED".equals(previousStatus)) {
                // 保持补偿相关状态，避免被并发 worker 误改回 RUNNING
                state.status = previousStatus;
            } else {
                state.status = "RUNNING";
            }
            state.persist();

            Log.infof("Processing workflow %s (worker=%s)", workflowId, workerId);

            // 获取事件历史
            List<WorkflowEvent> events = eventStore.getEvents(workflowId, 0);

            // 检查 workflow 状态
            boolean completed = events.stream()
                    .anyMatch(e -> "WorkflowCompleted".equals(e.getEventType()));
            boolean failed = events.stream()
                    .anyMatch(e -> "WorkflowFailed".equals(e.getEventType()));
            boolean stepFailed = events.stream()
                    .anyMatch(e -> "StepFailed".equals(e.getEventType()));
            boolean compensating = "COMPENSATING".equals(previousStatus);
            boolean compensationFailed = compensationExecutor.hasCompensationFailure(events);
            boolean allCompensationsCompleted = compensationExecutor.areAllCompensationsCompleted(events);

            // 处理 step 失败 -> 触发补偿
            if (stepFailed && !compensating && !compensationFailed) {
                Optional<WorkflowEvent> failedEvent = events.stream()
                        .filter(e -> "StepFailed".equals(e.getEventType()))
                        .findFirst();

                if (failedEvent.isPresent()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) failedEvent.get().getPayload();
                    String failedStepId = (String) payload.get("stepId");
                    String errorMessage = (String) payload.getOrDefault("error", "Unknown error");

                    compensationExecutor.handleStepFailure(
                            workflowId,
                            failedStepId,
                            new RuntimeException(errorMessage)
                    );

                    state.status = "COMPENSATING";
                    state.persist();
                    Log.infof("Triggered compensation for workflow %s", workflowId);
                }
            }

            // 处理补偿完成
            if (allCompensationsCompleted) {
                state.status = "COMPENSATED";
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();

                workflowRuntime.completeWorkflow(workflowId, null);
                Log.infof("All compensations completed for workflow %s", workflowId);
                return;
            }

            // 处理补偿失败
            if (compensationFailed) {
                state.markCompleted("COMPENSATION_FAILED");
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();

                workflowRuntime.failWorkflow(workflowId,
                        new RuntimeException("Compensation failed, requires manual intervention"));
                Log.errorf("Compensation failed for workflow %s, requires manual intervention", workflowId);
                return;
            }

            // 处理正常完成
            if (completed) {
                state.markCompleted("COMPLETED");
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();

                // 通知 runtime 完成
                Object result = events.stream()
                        .filter(e -> "WorkflowCompleted".equals(e.getEventType()))
                        .map(WorkflowEvent::getPayload)
                        .findFirst()
                        .orElse(null);
                workflowRuntime.completeWorkflow(workflowId, result);

                Log.infof("Workflow %s completed", workflowId);

            } else if (failed) {
                state.markCompleted("FAILED");
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();

                // 通知 runtime 失败
                workflowRuntime.failWorkflow(workflowId, new RuntimeException("Workflow failed"));

                Log.infof("Workflow %s failed", workflowId);

            } else {
                // 仍在执行中，释放锁等待下次调度
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();

                Log.debugf("Workflow %s still running, will retry later", workflowId);
            }

        } catch (Exception e) {
            Log.errorf(e, "Error processing workflow %s", workflowId);
            // 释放锁以便重试
            try {
                releaseWorkflowLock(workflowId);
            } catch (Exception ex) {
                Log.errorf(ex, "Failed to release lock for workflow %s", workflowId);
            }
        } finally {
            // Phase 3.6: 清理 ThreadLocal 上下文防止内存泄漏
            PostgresWorkflowRuntime.clearCurrentWorkflowId();
        }
    }

    /**
     * 反序列化 clock_times JSONB 字符串（Phase 3.6）
     *
     * 从持久化的 JSONB 数据恢复 ClockTimesSnapshot。
     * 反序列化失败时返回 null，不阻塞 workflow 处理。
     *
     * @param clockTimesJson JSONB 字符串
     * @return ClockTimesSnapshot 实例，失败时返回 null
     */
    private ClockTimesSnapshot deserializeClockTimes(String clockTimesJson) {
        if (clockTimesJson == null || clockTimesJson.isBlank()) {
            return null;
        }
        try {
            // 创建独立的 ObjectMapper 并注册 JavaTimeModule
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            return mapper.readValue(clockTimesJson, ClockTimesSnapshot.class);
        } catch (Exception e) {
            Log.warnf(e, "Failed to deserialize clock_times, workflow will proceed without replay mode");
            return null; // 反序列化失败降级为非 replay 模式
        }
    }

    /**
     * 轮询待触发的定时器
     */
    @Transactional
    public void pollTimers() {
        if (!running) return;

        try {
            List<WorkflowTimerEntity> firedTimers = WorkflowTimerEntity.findFiredTimers(100);

            for (WorkflowTimerEntity timer : firedTimers) {
                try {
                    // 追加 TimerFired 事件
                    eventStore.appendEvent(
                            timer.workflowId.toString(),
                            WorkflowEvent.Type.TIMER_FIRED,
                            timer.payload
                    );

                    // 标记定时器已触发
                    timer.status = "FIRED";
                    timer.persist();

                    Log.debugf("Timer %s fired for workflow %s", timer.timerId, timer.workflowId);

                } catch (Exception e) {
                    Log.errorf(e, "Error firing timer %s", timer.timerId);
                }
            }

        } catch (Exception e) {
            Log.errorf(e, "Error polling timers");
        }
    }

    /**
     * 释放 workflow 锁
     *
     * @param workflowId workflow 唯一标识符
     */
    @Transactional
    public void releaseWorkflowLock(String workflowId) {
        UUID wfUuid = UUID.fromString(workflowId);
        Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(wfUuid);
        if (stateOpt.isPresent()) {
            WorkflowStateEntity state = stateOpt.get();
            if (workerId.equals(state.lockOwner)) {
                state.lockOwner = null;
                state.lockExpiresAt = null;
                state.persist();
            }
        }
    }

    /**
     * 获取结果 future（用于 ExecutionHandle）
     *
     * @param workflowId workflow 唯一标识符
     * @return 结果 future
     */
    public CompletableFuture<Object> getResultFuture(String workflowId) {
        return workflowRuntime.getResultFuture(workflowId);
    }
}
