package aster.truffle.runtime;

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

  /**
   * 构造工作流调度器
   *
   * @param registry 异步任务注册表
   */
  public WorkflowScheduler(AsyncTaskRegistry registry) {
    if (registry == null) {
      throw new IllegalArgumentException("registry cannot be null");
    }
    this.registry = registry;
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
   * 获取关联的任务注册表
   *
   * @return 任务注册表
   */
  public AsyncTaskRegistry getRegistry() {
    return registry;
  }
}
