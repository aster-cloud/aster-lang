package aster.runtime.workflow;

import io.aster.workflow.DeterminismContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现的 Workflow 运行时
 *
 * 用于 Phase 2.0 向后兼容和单元测试。
 * 不持久化状态，所有数据存储在内存中。
 */
public class InMemoryWorkflowRuntime implements WorkflowRuntime {

    private final Map<String, WorkflowExecutionState> executions = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyKeys = new ConcurrentHashMap<>();
    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final DeterminismContext context = new DeterminismContext();

    /**
     * 调度 workflow 执行
     *
     * @param workflowId workflow 唯一标识符
     * @param idempotencyKey 幂等性键（可选）
     * @param metadata workflow 元数据
     * @return 执行句柄
     */
    @Override
    public ExecutionHandle schedule(String workflowId, String idempotencyKey, WorkflowMetadata metadata) {
        // 幂等性检查
        if (idempotencyKey != null && idempotencyKeys.containsKey(idempotencyKey)) {
            String existingWorkflowId = idempotencyKeys.get(idempotencyKey);
            WorkflowExecutionState state = executions.get(existingWorkflowId);
            if (state != null) {
                return state.handle;
            }
        }

        // 创建执行句柄
        CompletableFuture<Object> resultFuture = new CompletableFuture<>();
        InMemoryExecutionHandle handle = new InMemoryExecutionHandle(workflowId, resultFuture);

        // 记录执行状态
        WorkflowExecutionState state = new WorkflowExecutionState(handle, metadata);
        executions.put(workflowId, state);

        // 记录幂等性键
        if (idempotencyKey != null) {
            idempotencyKeys.put(idempotencyKey, workflowId);
        }

        // 追加 WorkflowStarted 事件
        eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_STARTED, metadata);

        return handle;
    }

    /**
     * 获取确定性上下文（Phase 0 Task 1.5）
     *
     * @return 确定性上下文实例
     */
    public DeterminismContext getDeterminismContext() {
        return context;
    }

    /**
     * 兼容旧接口：返回 DeterminismContext 内部的时钟
     *
     * @return 确定性时钟实例
     */
    @Deprecated
    @Override
    public DeterministicClock getClock() {
        return context.clock();
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
     */
    @Override
    public void shutdown() {
        executions.values().forEach(state -> {
            if (!state.handle.getResult().isDone()) {
                state.handle.cancel();
            }
        });
        executions.clear();
        idempotencyKeys.clear();
    }

    /**
     * 完成 workflow 执行
     *
     * @param workflowId workflow 唯一标识符
     * @param result 执行结果
     */
    public void completeWorkflow(String workflowId, Object result) {
        WorkflowExecutionState state = executions.get(workflowId);
        if (state != null) {
            state.handle.complete(result);
            eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_COMPLETED, result);
        }
    }

    /**
     * 使 workflow 执行失败
     *
     * @param workflowId workflow 唯一标识符
     * @param error 失败原因
     */
    public void failWorkflow(String workflowId, Throwable error) {
        WorkflowExecutionState state = executions.get(workflowId);
        if (state != null) {
            state.handle.fail(error);
            eventStore.appendEvent(workflowId, WorkflowEvent.Type.WORKFLOW_FAILED, error.getMessage());
        }
    }

    // ==================== 内部类 ====================

    /**
     * Workflow 执行状态
     */
    private static class WorkflowExecutionState {
        final InMemoryExecutionHandle handle;
        final WorkflowMetadata metadata;

        WorkflowExecutionState(InMemoryExecutionHandle handle, WorkflowMetadata metadata) {
            this.handle = handle;
            this.metadata = metadata;
        }
    }

    /**
     * 内存执行句柄
     */
    private static class InMemoryExecutionHandle implements ExecutionHandle {
        private final String workflowId;
        private final CompletableFuture<Object> resultFuture;

        InMemoryExecutionHandle(String workflowId, CompletableFuture<Object> resultFuture) {
            this.workflowId = workflowId;
            this.resultFuture = resultFuture;
        }

        @Override
        public String getWorkflowId() {
            return workflowId;
        }

        @Override
        public CompletableFuture<Object> getResult() {
            return resultFuture;
        }

        @Override
        public void cancel() {
            resultFuture.cancel(true);
        }

        void complete(Object result) {
            resultFuture.complete(result);
        }

        void fail(Throwable error) {
            resultFuture.completeExceptionally(error);
        }
    }
}
