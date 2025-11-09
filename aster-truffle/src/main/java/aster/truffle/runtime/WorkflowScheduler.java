package aster.truffle.runtime;

import java.util.List;
import java.util.Set;

/**
 * 工作流调度器 - 管理依赖图并协调任务执行
 *
 * Phase 2.0 实现：协作式调度
 * - 基于依赖图的拓扑排序调度任务
 * - 顺序执行就绪任务（单线程协作式）
 * - fail-fast 错误处理
 * - 全局超时控制
 *
 * Phase 2.2 升级路径：
 * - 引入 System Threads 实现真正的并行调度
 * - 资源池管理（线程池、连接池）
 */
public final class WorkflowScheduler {
  private final AsyncTaskRegistry registry;
  private DependencyGraph graph;

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

  /**
   * 注册工作流依赖图
   *
   * @param graph 依赖图对象
   */
  public void registerWorkflow(DependencyGraph graph) {
    if (graph == null) {
      throw new IllegalArgumentException("graph cannot be null");
    }
    this.graph = graph;
  }

  /**
   * 执行下一个就绪任务
   *
   * Phase 2.0: 顺序执行第一个就绪任务（协作式调度）
   * - 从依赖图获取就绪任务列表
   * - 检查任务状态（是否失败）
   * - 调用 registry.executeNext() 执行任务
   * - 更新依赖图（标记任务完成）
   */
  public void executeNext() {
    if (graph == null) {
      return;
    }

    // 获取所有就绪任务
    List<String> readyTasks = graph.getReadyTasks();
    if (readyTasks.isEmpty()) {
      return;
    }

    // Phase 2.0: 顺序执行第一个就绪任务
    // Phase 2.2 升级点：可以并行执行多个就绪任务
    String taskId = readyTasks.get(0);

    // 检查任务是否已失败（可能在之前的调度中失败）
    if (registry.isFailed(taskId)) {
      // fail-fast: 取消所有依赖此任务的后续任务
      cancelDependentTasks(taskId);
      throw new RuntimeException("Task failed: " + taskId + ", cancelling dependent tasks");
    }

    // 检查任务是否已取消
    if (registry.isCancelled(taskId)) {
      // 已取消的任务跳过执行，但需要从依赖图移除
      graph.markCompleted(taskId);
      return;
    }

    // 执行任务（调用 AsyncTaskRegistry 的执行逻辑）
    registry.executeNext();

    // 检查执行结果并更新依赖图
    if (registry.isCompleted(taskId)) {
      // 任务成功完成，标记依赖图
      graph.markCompleted(taskId);
    } else if (registry.isFailed(taskId)) {
      // 任务执行失败，取消依赖链
      cancelDependentTasks(taskId);
      throw new RuntimeException("Task failed during execution: " + taskId);
    }
  }

  /**
   * 执行工作流直到所有任务完成或超时
   *
   * @param timeoutMs 超时时间（毫秒）
   * @throws RuntimeException 如果超时或任务失败
   */
  public void executeUntilComplete(long timeoutMs) {
    if (graph == null) {
      throw new IllegalStateException("No workflow registered");
    }

    long startTime = System.currentTimeMillis();

    // 轮询执行直到所有任务完成
    while (!graph.allCompleted()) {
      // 检查全局超时
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > timeoutMs) {
        // 超时：取消所有未完成任务
        cancelAllPendingTasks();
        throw new RuntimeException(
            String.format("Workflow timeout after %d ms (limit: %d ms)", elapsed, timeoutMs)
        );
      }

      // 执行下一个就绪任务
      executeNext();

      // 让出 CPU，避免忙等
      Thread.yield();
    }
  }

  /**
   * 取消所有依赖失败任务的后续任务（递归）
   *
   * @param failedTaskId 失败的任务 ID
   */
  private void cancelDependentTasks(String failedTaskId) {
    // 获取所有直接依赖此任务的后续任务
    Set<String> dependents = graph.getDependents(failedTaskId);

    for (String dependentId : dependents) {
      // 取消任务
      registry.cancelTask(dependentId);

      // 递归取消后续任务的依赖链
      cancelDependentTasks(dependentId);
    }
  }

  /**
   * 取消所有待执行任务（超时时使用）
   */
  private void cancelAllPendingTasks() {
    List<String> readyTasks = graph.getReadyTasks();
    for (String taskId : readyTasks) {
      registry.cancelTask(taskId);
    }
  }

  /**
   * 获取当前工作流的依赖图
   *
   * @return 依赖图对象，如果未注册返回 null
   */
  public DependencyGraph getGraph() {
    return graph;
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
