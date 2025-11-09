package aster.truffle.nodes;

import aster.truffle.AsterLanguage;
import aster.truffle.AsterContext;
import aster.truffle.runtime.AsyncTaskRegistry;
import aster.truffle.runtime.DependencyGraph;
import aster.truffle.runtime.WorkflowScheduler;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WorkflowNode - 工作流编排节点
 *
 * Phase 2.0 实现：工作流编排的语法集成
 * - 接收任务列表和依赖声明
 * - 使用 StartNode 创建异步任务
 * - 构建 DependencyGraph 并使用 WorkflowScheduler 执行
 * - 收集并返回所有任务结果
 * - 支持全局超时控制
 *
 * Phase 2.1 升级路径：
 * - 编译器支持（从 AST 自动生成 WorkflowNode）
 * - 优化的依赖图序列化
 */
@NodeInfo(shortName = "workflow", description = "工作流编排节点")
public final class WorkflowNode extends Node {
  private final Env env;
  @Children private final Node[] taskExprs;  // 任务表达式
  private final String[] taskNames;
  private final Map<String, Set<String>> dependencies;  // name -> dep names
  private final long timeoutMs;

  /**
   * 构造工作流节点
   *
   * @param env 环境对象（用于变量绑定）
   * @param taskExprs 任务表达式数组
   * @param taskNames 任务名称数组（与 taskExprs 一一对应）
   * @param dependencies 依赖关系映射（任务名 -> 依赖的任务名集合）
   * @param timeoutMs 工作流全局超时时间（毫秒）
   */
  public WorkflowNode(Env env, Node[] taskExprs, String[] taskNames,
                      Map<String, Set<String>> dependencies, long timeoutMs) {
    if (taskExprs == null || taskNames == null) {
      throw new IllegalArgumentException("taskExprs and taskNames cannot be null");
    }
    if (taskExprs.length != taskNames.length) {
      throw new IllegalArgumentException(
          "taskExprs and taskNames must have the same length"
      );
    }
    this.env = env;
    this.taskExprs = taskExprs;
    this.taskNames = taskNames;
    this.dependencies = (dependencies == null) ? Collections.emptyMap() : dependencies;
    this.timeoutMs = timeoutMs;
  }

  /**
   * 执行工作流
   *
   * Phase 2.0: 协作式调度
   * 1. 为所有任务创建 StartNode 并获取 task_id
   * 2. 构建依赖图（将任务名映射为 task_id）
   * 3. 使用 WorkflowScheduler 执行工作流
   * 4. 收集并返回所有任务结果
   *
   * @param frame 当前执行帧
   * @return 结果数组（按 taskNames 顺序）
   */
  public Object execute(VirtualFrame frame) {
    Profiler.inc("workflow");

    // 获取上下文
    AsterContext context = AsterLanguage.getContext();
    AsyncTaskRegistry registry = context.getAsyncRegistry();
    WorkflowScheduler scheduler = new WorkflowScheduler(registry);
    DependencyGraph graph = new DependencyGraph();

    // 1. 创建所有任务并记录 name -> taskId 映射
    Map<String, String> nameToId = new HashMap<>();
    for (int i = 0; i < taskExprs.length; i++) {
      // 为每个任务表达式创建 StartNode
      StartNode startNode = new StartNode(env, taskNames[i], taskExprs[i]);
      // 执行 StartNode 并获取 task_id
      String taskId = (String) startNode.execute(frame);
      nameToId.put(taskNames[i], taskId);
    }

    // 2. 构建依赖图（将任务名转换为 task_id）
    for (String name : taskNames) {
      String taskId = nameToId.get(name);
      Set<String> depNames = dependencies.get(name);

      // 将依赖任务名转换为 task_id
      Set<String> depIds;
      if (depNames == null || depNames.isEmpty()) {
        depIds = Collections.emptySet();
      } else {
        depIds = depNames.stream()
            .map(nameToId::get)
            .collect(Collectors.toSet());
      }

      // 添加任务到依赖图
      graph.addTask(taskId, depIds);
    }

    // 3. 执行工作流
    scheduler.registerWorkflow(graph);
    scheduler.executeUntilComplete(timeoutMs);

    // 4. 收集结果
    Object[] results = new Object[taskNames.length];
    for (int i = 0; i < taskNames.length; i++) {
      String taskId = nameToId.get(taskNames[i]);
      results[i] = registry.getResult(taskId);
    }

    return results;
  }
}
