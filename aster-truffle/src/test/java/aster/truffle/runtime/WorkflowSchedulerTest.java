package aster.truffle.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WorkflowScheduler 单元测试（Phase 2.5）
 *
 * 覆盖要点：
 * 1. executeUntilComplete 应正确驱动 AsyncTaskRegistry
 * 2. 失败需 wrap 为统一的 Workflow execution failed 错误
 * 3. executeNext 仍可单步执行（兼容 start/await 语义）
 * 4. 基础构造与存取器行为
 */
public class WorkflowSchedulerTest {

  private AsyncTaskRegistry registry;
  private WorkflowScheduler scheduler;

  @BeforeEach
  public void setUp() {
    registry = new AsyncTaskRegistry();
    scheduler = new WorkflowScheduler(registry);
  }

  private void registerTask(String taskId, Runnable taskBody, Set<String> deps) {
    Set<String> normalized = deps == null ? Collections.emptySet() : deps;
    registry.registerTaskWithDependencies(taskId, () -> {
      taskBody.run();
      return null;
    }, normalized);
  }

  @Test
  public void testExecuteUntilCompleteRunsAllTasks() {
    AtomicInteger order = new AtomicInteger(0);
    List<Integer> observed = new ArrayList<>();

    registerTask("A", () -> {
      observed.add(order.incrementAndGet());
      registry.setResult("A", "alpha");
    }, Collections.emptySet());

    registerTask("B", () -> {
      observed.add(order.incrementAndGet());
      registry.setResult("B", "beta");
    }, Set.of("A"));

    registerTask("C", () -> {
      observed.add(order.incrementAndGet());
      registry.setResult("C", "gamma");
    }, Set.of("B"));

    scheduler.executeUntilComplete();

    assertEquals(List.of(1, 2, 3), observed, "执行顺序应符合拓扑排序");
    assertEquals("alpha", registry.getResult("A"));
    assertEquals("beta", registry.getResult("B"));
    assertEquals("gamma", registry.getResult("C"));
  }

  @Test
  public void testExecuteUntilCompleteWrapsFailure() {
    registerTask("A", () -> registry.setResult("A", "ok"), Collections.emptySet());
    registerTask("B", () -> {
      throw new RuntimeException("boom");
    }, Set.of("A"));
    registerTask("C", () -> registry.setResult("C", "skip"), Set.of("B"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> scheduler.executeUntilComplete());
    assertTrue(thrown.getMessage().contains("Workflow execution failed"));
    assertTrue(thrown.getCause() instanceof RuntimeException);

    assertTrue(registry.isCompleted("A"));
    assertTrue(registry.isFailed("B"));
    assertTrue(registry.isCancelled("C"));
  }

  @Test
  public void testExecuteNextDelegatesToRegistry() {
    registerTask("single", () -> registry.setResult("single", "done"), Collections.emptySet());
    assertDoesNotThrow(() -> scheduler.executeNext());
    assertTrue(registry.isCompleted("single"));
  }

  @Test
  public void testConstructorRejectsNullRegistry() {
    assertThrows(IllegalArgumentException.class, () -> new WorkflowScheduler(null));
  }

  @Test
  public void testGetRegistry() {
    assertSame(registry, scheduler.getRegistry());
  }
}
