package aster.truffle.nodes;

import aster.truffle.AsterLanguage;
import aster.truffle.AsterContext;
import aster.truffle.runtime.AsyncTaskRegistry;
import aster.truffle.runtime.WorkflowScheduler;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

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

    AsterContext context = AsterLanguage.getContext();
    if (!context.isEffectAllowed("Async")) {
      throw new RuntimeException("workflow requires Async effect");
    }
    AsyncTaskRegistry registry = context.getAsyncRegistry();
    WorkflowScheduler scheduler = new WorkflowScheduler(registry);

    Map<String, String> nameToId = new LinkedHashMap<>();
    MaterializedFrame[] capturedFrames = new MaterializedFrame[taskExprs.length];
    Set<String>[] effectSnapshots = new Set[taskExprs.length];

    // 1. 为所有任务生成 task_id 并捕获执行上下文
    for (int i = 0; i < taskExprs.length; i++) {
      Profiler.inc("start");
      MaterializedFrame materializedFrame = frame.materialize();
      Set<String> capturedEffects = context.getAllowedEffects();
      String taskId = context.generateTaskId();

      nameToId.put(taskNames[i], taskId);
      if (taskNames[i] != null) {
        env.set(taskNames[i], taskId);
      }
      capturedFrames[i] = materializedFrame;
      effectSnapshots[i] = capturedEffects;
    }

    // 2. 注册任务并声明依赖关系
    for (int i = 0; i < taskExprs.length; i++) {
      Node expr = taskExprs[i];
      String stepName = taskNames[i];
      String taskId = nameToId.get(stepName);
      MaterializedFrame materializedFrame = capturedFrames[i];
      Set<String> capturedEffects = effectSnapshots[i];

      Callable<Object> callable = () -> {
        Set<String> previousEffects = context.getAllowedEffects();
        try {
          context.setAllowedEffects(capturedEffects);
          return Exec.exec(expr, materializedFrame);
        } catch (RuntimeException ex) {
          throw ex;
        } catch (Throwable t) {
          throw new RuntimeException("workflow step failed: " + stepName, t);
        } finally {
          context.setAllowedEffects(previousEffects);
        }
      };

      Set<String> depIds = resolveDependencyIds(stepName, nameToId);
      registry.registerTaskWithDependencies(taskId, callable, depIds);
    }

    // 3. 执行工作流
    scheduler.executeUntilComplete();

    // 4. 收集结果
    Object[] results = new Object[taskNames.length];
    for (int i = 0; i < taskNames.length; i++) {
      String taskId = nameToId.get(taskNames[i]);
      results[i] = registry.getResult(taskId);
    }

    return results;
  }

  private Set<String> resolveDependencyIds(String name, Map<String, String> nameToId) {
    Set<String> depNames = dependencies.get(name);
    if (depNames == null || depNames.isEmpty()) {
      return Collections.emptySet();
    }
    LinkedHashSet<String> ids = new LinkedHashSet<>();
    for (String dep : depNames) {
      String taskId = nameToId.get(dep);
      if (taskId == null) {
        throw new RuntimeException("Unknown workflow dependency: " + dep);
      }
      ids.add(taskId);
    }
    return ids;
  }
}
