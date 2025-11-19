package aster.truffle.runtime;

import io.aster.workflow.DeterminismContext;

/**
 * 工作流调度器 - 仅负责驱动 AsyncTaskRegistry
 *
 * Phase 2.5 目标：
 * - 彻底移除 Scheduler 内部的依赖图副本
 * - 依赖感知调度完全交由 AsyncTaskRegistry 负责
 * - Scheduler 仅负责 fail-fast 包装与向后兼容 API
 */
public final class WorkflowScheduler {
  private final AsyncTaskRegistry registry;
  private final String workflowId;
  private final PostgresEventStore eventStore;
  private final DeterminismContext determinismContext;

  public WorkflowScheduler(AsyncTaskRegistry registry) {
    this(registry, null, null, registry != null ? registry.getDeterminismContext() : new DeterminismContext());
  }

  /**
   * 构造工作流调度器
   *
   * @param registry 异步任务注册表
   * @param workflowId workflow 唯一标识符
   * @param eventStore PostgreSQL 事件存储
   */
  public WorkflowScheduler(AsyncTaskRegistry registry, String workflowId, PostgresEventStore eventStore) {
    this(registry, workflowId, eventStore, registry != null ? registry.getDeterminismContext() : new DeterminismContext());
  }

  /**
   * 构造工作流调度器（重放模式需要注入 DeterminismContext）
   *
   * 注意：传入的 determinismContext 应与 registry 的 DeterminismContext 一致。
   * 如果不一致，将使用 registry 的实例以确保行为一致。
   */
  public WorkflowScheduler(AsyncTaskRegistry registry, String workflowId, PostgresEventStore eventStore,
      DeterminismContext determinismContext) {
    if (registry == null) {
      throw new IllegalArgumentException("registry cannot be null");
    }
    this.registry = registry;
    this.workflowId = workflowId;
    this.eventStore = eventStore;

    // 使用 registry 的 DeterminismContext 以确保一致性
    DeterminismContext registryContext = registry.getDeterminismContext();
    if (determinismContext != null && registryContext != determinismContext) {
      io.quarkus.logging.Log.warnf(
          "Provided DeterminismContext differs from registry's context. Using registry's context for consistency.");
    }
    this.determinismContext = registryContext;

    if (workflowId != null) {
      registry.setWorkflowId(workflowId);
    }
    if (eventStore != null) {
      registry.setEventStore(eventStore);
    }
  }

  /** 向后兼容：单步执行（Start/Await 仍可能调用此 API） */
  @Deprecated
  public void executeNext() {
    registry.executeNext();
  }

  /**
   * 执行所有工作流任务直至完成（依赖 AsyncTaskRegistry 的并发调度）
   */
  public void executeUntilComplete() {
    try {
      registry.executeUntilComplete();
    } catch (RuntimeException e) {
      throw new RuntimeException("Workflow execution failed", e);
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }

  /**
   * 调度 workflow 重试（带延迟）
   *
   * @param workflowId workflow 唯一标识符
   * @param delayMs 延迟时间（毫秒）
   * @param attemptNumber 重试次数
   * @param failureReason 失败原因
   */
  public void scheduleRetry(String workflowId, long delayMs, int attemptNumber, String failureReason) {
    registry.scheduleRetry(workflowId, delayMs, attemptNumber, failureReason);
  }

  /**
   * 获取关联的任务注册表
   *
   * @return 任务注册表
   */
  public AsyncTaskRegistry getRegistry() {
    return registry;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public PostgresEventStore getEventStore() {
    return eventStore;
  }

  public DeterminismContext getDeterminismContext() {
    return determinismContext;
  }
}
