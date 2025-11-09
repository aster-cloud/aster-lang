package aster.truffle.runtime;

import java.util.*;

/**
 * 依赖图管理器 - 管理任务之间的依赖关系和调度顺序
 *
 * Phase 2.0 实现：依赖图构建和拓扑排序
 * - 支持任务依赖声明和注册
 * - 循环依赖检测（DFS 算法）
 * - 就绪任务查询（O(1) 性能）
 * - 依赖计数优化（避免重复遍历）
 */
public final class DependencyGraph {
  // 任务节点存储：task_id -> TaskNode
  private final Map<String, TaskNode> nodes = new HashMap<>();

  // 就绪任务队列（依赖已满足的任务）
  private final Set<String> readyQueue = new LinkedHashSet<>();

  /**
   * 任务节点 - 封装任务的依赖关系和状态
   */
  static class TaskNode {
    final String taskId;
    final Set<String> dependencies;  // 依赖的任务 ID 集合
    int remainingDeps;                // 剩余未完成的依赖数量

    TaskNode(String taskId, Set<String> dependencies) {
      this.taskId = taskId;
      this.dependencies = new HashSet<>(dependencies);
      this.remainingDeps = dependencies.size();
    }
  }

  /**
   * 添加任务到依赖图
   *
   * @param taskId 任务唯一标识符
   * @param dependencies 依赖的任务 ID 集合（可为空）
   * @throws IllegalArgumentException 如果检测到循环依赖
   */
  public void addTask(String taskId, Set<String> dependencies) {
    if (taskId == null) {
      throw new IllegalArgumentException("taskId cannot be null");
    }

    if (nodes.containsKey(taskId)) {
      throw new IllegalArgumentException("Task already exists: " + taskId);
    }

    Set<String> deps = (dependencies == null) ? Collections.emptySet() : dependencies;

    // 循环依赖检测
    if (hasCycle(taskId, deps)) {
      throw new IllegalArgumentException("Circular dependency detected for task: " + taskId);
    }

    // 创建任务节点
    TaskNode node = new TaskNode(taskId, deps);
    nodes.put(taskId, node);

    // 无依赖任务直接加入就绪队列
    if (deps.isEmpty()) {
      readyQueue.add(taskId);
    }
  }

  /**
   * 标记任务已完成，更新依赖计数
   *
   * @param taskId 已完成的任务 ID
   */
  public void markCompleted(String taskId) {
    if (taskId == null) {
      return;
    }

    // 从就绪队列移除
    readyQueue.remove(taskId);

    // 遍历所有节点，减少依赖此任务的节点的计数
    for (TaskNode node : nodes.values()) {
      if (node.dependencies.contains(taskId)) {
        node.remainingDeps--;

        // 依赖全部满足，加入就绪队列
        if (node.remainingDeps == 0) {
          readyQueue.add(node.taskId);
        }
      }
    }
  }

  /**
   * 获取所有就绪任务（依赖已满足）
   *
   * @return 就绪任务 ID 列表
   */
  public List<String> getReadyTasks() {
    return new ArrayList<>(readyQueue);
  }

  /**
   * 检查所有任务是否已完成
   *
   * @return true 如果所有任务的依赖都已满足且就绪队列为空
   */
  public boolean allCompleted() {
    // 所有任务的 remainingDeps 都为 0，且就绪队列为空
    return readyQueue.isEmpty() &&
           nodes.values().stream().allMatch(node -> node.remainingDeps == 0);
  }

  /**
   * 获取依赖指定任务的所有后续任务
   *
   * @param taskId 任务 ID
   * @return 依赖此任务的后续任务 ID 集合
   */
  public Set<String> getDependents(String taskId) {
    Set<String> dependents = new HashSet<>();
    for (TaskNode node : nodes.values()) {
      if (node.dependencies.contains(taskId)) {
        dependents.add(node.taskId);
      }
    }
    return dependents;
  }

  /**
   * 循环依赖检测 - 使用 DFS 算法
   *
   * @param newTaskId 新任务 ID
   * @param newDeps 新任务的依赖集合
   * @return true 如果检测到循环依赖
   */
  private boolean hasCycle(String newTaskId, Set<String> newDeps) {
    // 构建临时图：包含现有节点 + 新节点
    Map<String, Set<String>> tempGraph = new HashMap<>();

    // 添加现有节点的依赖关系
    for (TaskNode node : nodes.values()) {
      tempGraph.put(node.taskId, new HashSet<>(node.dependencies));
    }

    // 添加新节点
    tempGraph.put(newTaskId, new HashSet<>(newDeps));

    // DFS 循环检测
    Set<String> visited = new HashSet<>();
    Set<String> recursionStack = new HashSet<>();

    // 从新节点开始 DFS
    return hasCycleDFS(newTaskId, tempGraph, visited, recursionStack);
  }

  /**
   * DFS 循环检测辅助方法
   *
   * @param taskId 当前访问的任务 ID
   * @param graph 依赖图
   * @param visited 已访问节点集合
   * @param recursionStack 递归栈（检测回边）
   * @return true 如果检测到循环
   */
  private boolean hasCycleDFS(String taskId, Map<String, Set<String>> graph,
                               Set<String> visited, Set<String> recursionStack) {
    // 标记当前节点为正在访问
    visited.add(taskId);
    recursionStack.add(taskId);

    // 访问所有依赖节点
    Set<String> dependencies = graph.get(taskId);
    if (dependencies != null) {
      for (String dep : dependencies) {
        // 如果依赖节点不存在，跳过（可能是外部任务）
        if (!graph.containsKey(dep)) {
          continue;
        }

        // 如果依赖节点未访问，递归访问
        if (!visited.contains(dep)) {
          if (hasCycleDFS(dep, graph, visited, recursionStack)) {
            return true;  // 检测到循环
          }
        }
        // 如果依赖节点在递归栈中，说明存在回边（循环）
        else if (recursionStack.contains(dep)) {
          return true;
        }
      }
    }

    // 回溯：从递归栈移除
    recursionStack.remove(taskId);
    return false;
  }

  /**
   * 获取任务总数
   *
   * @return 任务数量
   */
  public int getTaskCount() {
    return nodes.size();
  }

  /**
   * 获取就绪任务数量
   *
   * @return 就绪队列大小
   */
  public int getReadyCount() {
    return readyQueue.size();
  }
}
