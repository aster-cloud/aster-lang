package aster.truffle.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowScheduler 单元测试（Phase 2.0）
 *
 * 测试覆盖：
 * 1. 顺序执行（线性链）
 * 2. Fail-fast 失败传播
 * 3. 全局超时控制
 * 4. 并发就绪任务调度
 * 5. 取消任务、空调度及存取器
 */
public class WorkflowSchedulerTest {

  private AsyncTaskRegistry registry;
  private WorkflowScheduler scheduler;
  private DependencyGraph graph;

  @BeforeEach
  public void setUp() {
    registry = new AsyncTaskRegistry();
    scheduler = new WorkflowScheduler(registry);
    graph = new DependencyGraph();
  }

  private void registerTask(String taskId, Runnable taskBody, Set<String> deps) {
    registry.registerTask(taskId, taskBody);
    graph.addTask(taskId, deps == null ? Collections.emptySet() : deps);
  }

  /**
   * 测试：任务应严格按照依赖顺序执行（A→B→C）。
   */
  @Test
  public void testSequentialExecution() {
    AtomicInteger orderCounter = new AtomicInteger(0);
    List<Integer> observedOrder = new ArrayList<>();

    registerTask("A", () -> {
      observedOrder.add(orderCounter.incrementAndGet());
      registry.setResult("A", "alpha");
    }, Collections.emptySet());

    registerTask("B", () -> {
      observedOrder.add(orderCounter.incrementAndGet());
      registry.setResult("B", "beta");
    }, Set.of("A"));

    registerTask("C", () -> {
      observedOrder.add(orderCounter.incrementAndGet());
      registry.setResult("C", "gamma");
    }, Set.of("B"));

    scheduler.registerWorkflow(graph);
    scheduler.executeUntilComplete(5_000);

    assertEquals(List.of(1, 2, 3), observedOrder, "执行顺序必须符合拓扑排序");
    assertEquals("alpha", registry.getResult("A"));
    assertEquals("beta", registry.getResult("B"));
    assertEquals("gamma", registry.getResult("C"));
  }

  /**
   * 测试：任务失败应立即取消依赖链并在下一次调度时提前抛错。
   */
  @Test
  public void testFailFast() {
    registerTask("A", () -> registry.setResult("A", "ok"), Collections.emptySet());
    registerTask("B", () -> { throw new RuntimeException("boom"); }, Set.of("A"));
    registerTask("C", () -> registry.setResult("C", "skip"), Set.of("B"));

    scheduler.registerWorkflow(graph);
    RuntimeException first = assertThrows(RuntimeException.class, () -> scheduler.executeUntilComplete(5_000));
    assertTrue(first.getMessage().contains("Task failed during execution"), "应提示执行失败");

    assertTrue(registry.isCompleted("A"), "前置任务应完成");
    assertTrue(registry.isFailed("B"), "失败任务状态应为 FAILED");
    assertTrue(registry.isCancelled("C"), "依赖链应被取消");

    RuntimeException second = assertThrows(RuntimeException.class, () -> scheduler.executeNext());
    assertTrue(second.getMessage().contains("cancelling dependent tasks"), "再次调度应立即 fail-fast");
  }

  /**
   * 测试：超时控制在依赖无法满足时应抛出异常并中断工作流。
   */
  @Test
  public void testTimeout() {
    registerTask("blocked", () -> registry.setResult("blocked", "never"), Set.of("missing-dep"));

    scheduler.registerWorkflow(graph);
    RuntimeException timeout = assertThrows(RuntimeException.class, () -> scheduler.executeUntilComplete(30));
    assertTrue(timeout.getMessage().contains("timeout"), "应返回包含 timeout 的错误信息");

    assertEquals(AsyncTaskRegistry.TaskStatus.PENDING, registry.getStatus("blocked"),
        "超时后任务仍处于待执行状态");
  }

  /**
   * 测试：多个就绪任务同时存在时，调度器应快速耗尽就绪队列（模拟并发场景）。
   */
  @Test
  public void testConcurrentExecution() {
    CopyOnWriteArrayList<String> finished = new CopyOnWriteArrayList<>();

    for (int i = 0; i < 5; i++) {
      String taskId = "task-" + i;
      registerTask(taskId, () -> {
        finished.add(taskId);
        registry.setResult(taskId, taskId.toUpperCase());
      }, Collections.emptySet());
    }

    assertEquals(5, graph.getReadyTasks().size(), "所有任务应立即就绪");

    scheduler.registerWorkflow(graph);
    scheduler.executeUntilComplete(5_000);

    assertEquals(5, finished.size(), "所有并发就绪任务必须被调度完成");
    finished.forEach(id -> assertTrue(registry.isCompleted(id), "所有任务应进入 COMPLETED"));
  }

  /**
   * 测试：已取消的任务应被跳过，同时仍需解锁其依赖的后续任务。
   */
  @Test
  public void testCancelledTaskSkipped() {
    registerTask("root", () -> registry.setResult("root", "unused"), Collections.emptySet());
    registerTask("child", () -> registry.setResult("child", "ok"), Set.of("root"));

    registry.cancelTask("root");

    scheduler.registerWorkflow(graph);
    scheduler.executeNext();  // root 被取消，child 应被解锁

    List<String> ready = graph.getReadyTasks();
    assertTrue(ready.contains("child"), "父任务取消后子任务仍需可执行");
  }

  /**
   * 测试：未注册工作流即调用 executeUntilComplete 应抛出明确异常。
   */
  @Test
  public void testNoWorkflowRegistered() {
    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
      scheduler.executeUntilComplete(10);
    });
    assertTrue(thrown.getMessage().contains("No workflow registered"));
  }

  /**
   * 测试：未注册工作流时执行 executeNext 应为无操作。
   */
  @Test
  public void testExecuteNextWithoutWorkflow() {
    assertDoesNotThrow(() -> scheduler.executeNext());
  }

  /**
   * 测试：getGraph() / getRegistry() 返回已注册对象。
   */
  @Test
  public void testGetters() {
    scheduler.registerWorkflow(graph);
    assertSame(graph, scheduler.getGraph());
    assertSame(registry, scheduler.getRegistry());
  }
}
