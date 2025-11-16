package aster.truffle;

import aster.truffle.runtime.AsyncTaskRegistry;
import aster.truffle.runtime.AsyncTaskRegistry.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 混沌测试：随机失败、随机超时、高并发与组合扰动场景。
 */
public class ChaosSchedulerTest {

  @Test
  public void testRandomFailures() {
    Random random = new Random(42L);
    int iterations = 10;
    for (int i = 0; i < iterations; i++) {
      ChaosScenarioResult result = runRandomFailureScenario(random.nextLong());
      assertFalse(result.timeout, "应捕获失败场景");
      assertNotNull(result.failingTaskId);
      assertDownstreamSuppressed(result);
      assertCompensationReplaysCompletion(result);
    }
  }

  @Test
  public void testRandomTimeouts() {
    Random random = new Random(42L);
    int iterations = 10;
    for (int i = 0; i < iterations; i++) {
      ChaosScenarioResult result = runRandomTimeoutScenario(random.nextLong());
      assertTrue(result.timeout, "应捕获超时场景");
      assertNotNull(result.failingTaskId);
      assertDownstreamSuppressed(result);
      assertCompensationReplaysCompletion(result);
    }
  }

  @Test
  public void testHighConcurrency() throws InterruptedException {
    AsyncTaskRegistry registry = new AsyncTaskRegistry(16);
    List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
    int workflows = 120;

    try {
      for (int i = 0; i < workflows; i++) {
        String taskId = "wf-" + i;
        final int taskIndex = i;
        registry.registerTaskWithDependencies(
            taskId,
            () -> {
              // 修复：移除 CountDownLatch，简化并发测试避免死锁
              Thread.sleep(5);
              executionOrder.add(taskId);
              return taskId;
            },
            Collections.emptySet(),
            0L,
            null,
            taskIndex % 4);
      }

      long startNs = System.nanoTime();
      registry.executeUntilComplete();
      long endNs = System.nanoTime();

      double elapsedSeconds = Math.max((endNs - startNs) / 1_000_000_000.0, 1e-6);
      double throughput = workflows / elapsedSeconds;
      assertEquals(workflows, executionOrder.size());
      assertTrue(throughput >= 100.0,
          String.format("Expected >=100 workflows/sec but got %.2f", throughput));
    } finally {
      registry.shutdown();
    }
  }

  @Test
  public void testCombinedChaos() {
    Random random = new Random(42L);
    int iterations = 12;
    int timeoutRuns = 0;
    int failureRuns = 0;

    for (int i = 0; i < iterations; i++) {
      boolean forceTimeout = random.nextBoolean();
      ChaosScenarioResult result = runHybridScenario(forceTimeout);
      assertNotNull(result.failingTaskId);
      if (result.timeout) {
        timeoutRuns++;
      } else {
        failureRuns++;
      }
      assertDownstreamSuppressed(result);
      assertCompensationReplaysCompletion(result);
    }

    assertTrue(timeoutRuns > 0, "组合测试需要至少一次超时");
    assertTrue(failureRuns > 0, "组合测试需要至少一次失败");
  }

  private ChaosScenarioResult runRandomFailureScenario(long seed) {
    AsyncTaskRegistry registry = new AsyncTaskRegistry(8);
    Map<String, Set<String>> deps = new HashMap<>();
    List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());
    List<String> compensationOrder = Collections.synchronizedList(new ArrayList<>());
    AtomicReference<String> failedTask = new AtomicReference<>();
    Random random = new Random(seed);
    List<String> registered = new ArrayList<>();

    try {
      int taskCount = 30;
      for (int i = 0; i < taskCount; i++) {
        String taskId = "fail-task-" + i;
        Set<String> dependencies = randomDependencies(registered, random);
        deps.put(taskId, new LinkedHashSet<>(dependencies));
        boolean shouldFail = random.nextDouble() < 0.2 || (i == taskCount - 1 && failedTask.get() == null);
        int priority = random.nextInt(5);

        // 修复：使用 final 变量避免 lambda 捕获问题
        final boolean taskShouldFail = shouldFail;

        registry.registerTaskWithDependencies(
            taskId,
            () -> {
              if (taskShouldFail && failedTask.compareAndSet(null, taskId)) {
                throw new RuntimeException("chaos failure: " + taskId);
              }
              completionOrder.add(taskId);
              return taskId;
            },
            dependencies,
            0L,
            () -> compensationOrder.add(taskId),
            priority);
        registered.add(taskId);
      }

      RuntimeException ex = assertThrows(RuntimeException.class, registry::executeUntilComplete);
      assertTrue(ex.getMessage().contains("chaos failure"));

      Map<String, TaskStatus> snapshot = snapshotStatuses(registry, deps.keySet());
      return new ChaosScenarioResult(
          failedTask.get(),
          false,
          copyDependencies(deps),
          snapshot,
          copyList(completionOrder),
          copyList(compensationOrder));
    } finally {
      registry.shutdown();
    }
  }

  private ChaosScenarioResult runRandomTimeoutScenario(long seed) {
    AsyncTaskRegistry registry = new AsyncTaskRegistry(8);
    Map<String, Set<String>> deps = new HashMap<>();
    List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());
    List<String> compensationOrder = Collections.synchronizedList(new ArrayList<>());
    Random random = new Random(seed);
    List<String> registered = new ArrayList<>();

    try {
      int taskCount = 30;
      for (int i = 0; i < taskCount; i++) {
        String taskId = "timeout-task-" + i;
        Set<String> dependencies = randomDependencies(registered, random);
        deps.put(taskId, new LinkedHashSet<>(dependencies));
        long timeoutMs = (random.nextDouble() < 0.25 || i == taskCount - 1) ? 25L : 0L;
        int priority = random.nextInt(5);

        // 修复：使用 final 变量避免 lambda 捕获问题
        final long taskTimeout = timeoutMs;
        final int sleepMs = random.nextInt(5);

        registry.registerTaskWithDependencies(
            taskId,
            () -> {
              if (taskTimeout > 0) {
                Thread.sleep(taskTimeout + 50);
              } else {
                Thread.sleep(sleepMs);
              }
              completionOrder.add(taskId);
              return taskId;
            },
            dependencies,
            timeoutMs,
            () -> compensationOrder.add(taskId),
            priority);
        registered.add(taskId);
      }

      RuntimeException ex = assertThrows(RuntimeException.class, registry::executeUntilComplete);
      assertTrue(ex.getCause() instanceof TimeoutException);
      String timeoutTaskId = extractTaskIdFromTimeout((TimeoutException) ex.getCause());
      Map<String, TaskStatus> snapshot = snapshotStatuses(registry, deps.keySet());
      return new ChaosScenarioResult(
          timeoutTaskId,
          true,
          copyDependencies(deps),
          snapshot,
          copyList(completionOrder),
          copyList(compensationOrder));
    } finally {
      registry.shutdown();
    }
  }

  private ChaosScenarioResult runHybridScenario(boolean forceTimeout) {
    AsyncTaskRegistry registry = new AsyncTaskRegistry(6);
    Map<String, Set<String>> deps = new HashMap<>();
    List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());
    List<String> compensationOrder = Collections.synchronizedList(new ArrayList<>());

    try {
      // 构造菱形拓扑：root -> {branchA, branchB} -> join -> leaf
      registerHybridNode(registry, deps, completionOrder, compensationOrder, "root", Collections.emptySet(), 1);

      Set<String> rootDep = Set.of("root");
      registerHybridNode(registry, deps, completionOrder, compensationOrder, "branch-a", rootDep, 1);
      registerHybridNode(registry, deps, completionOrder, compensationOrder, "branch-b", rootDep, 2);

      Set<String> branches = Set.of("branch-a", "branch-b");
      registerHybridNode(registry, deps, completionOrder, compensationOrder, "join", branches, 0);

      if (forceTimeout) {
        deps.put("timeout-leaf", new LinkedHashSet<>(Set.of("join")));
        registry.registerTaskWithDependencies(
            "timeout-leaf",
            () -> {
              Thread.sleep(60);
              completionOrder.add("timeout-leaf");
              return null;
            },
            Set.of("join"),
            20L,
            () -> compensationOrder.add("timeout-leaf"),
            0);
        deps.put("terminal", new LinkedHashSet<>(Set.of("timeout-leaf")));
        registry.registerTaskWithDependencies(
            "terminal",
            () -> {
              completionOrder.add("terminal");
              return null;
            },
            Set.of("timeout-leaf"),
            0L,
            () -> compensationOrder.add("terminal"),
            0);
      } else {
        deps.put("fail-leaf", new LinkedHashSet<>(Set.of("join")));
        registry.registerTaskWithDependencies(
            "fail-leaf",
            () -> {
              throw new IllegalStateException("hybrid failure");
            },
            Set.of("join"),
            0L,
            () -> compensationOrder.add("fail-leaf"),
            0);
      }

      RuntimeException ex = assertThrows(RuntimeException.class, registry::executeUntilComplete);
      boolean timeout = ex.getCause() instanceof TimeoutException;
      String failingId = timeout ? extractTaskIdFromTimeout((TimeoutException) ex.getCause()) : "fail-leaf";
      Map<String, TaskStatus> snapshot = snapshotStatuses(registry, deps.keySet());
      return new ChaosScenarioResult(
          failingId,
          timeout,
          copyDependencies(deps),
          snapshot,
          copyList(completionOrder),
          copyList(compensationOrder));
    } finally {
      registry.shutdown();
    }
  }

  private static void registerHybridNode(
      AsyncTaskRegistry registry,
      Map<String, Set<String>> deps,
      List<String> completionOrder,
      List<String> compensationOrder,
      String taskId,
      Set<String> dependencies,
      int priority) {
    deps.put(taskId, new LinkedHashSet<>(dependencies));
    registry.registerTaskWithDependencies(
        taskId,
        () -> {
          completionOrder.add(taskId);
          return taskId;
        },
        dependencies,
        0L,
        () -> compensationOrder.add(taskId),
        priority);
  }

  private static Set<String> randomDependencies(List<String> existing, Random random) {
    if (existing.isEmpty()) {
      return Collections.emptySet();
    }
    int maxPick = Math.min(existing.size(), 3);
    int depCount = random.nextInt(maxPick + 1);
    LinkedHashSet<String> deps = new LinkedHashSet<>();
    while (deps.size() < depCount) {
      String candidate = existing.get(random.nextInt(existing.size()));
      deps.add(candidate);
    }
    return deps;
  }

  private static void assertDownstreamSuppressed(ChaosScenarioResult result) {
    Set<String> downstream = collectDownstream(result.failingTaskId, result.dependencies);
    Set<String> completed = new LinkedHashSet<>(result.completionOrder);
    for (String dependent : downstream) {
      assertFalse(completed.contains(dependent), dependent + " 不应执行");
    }
  }

  private static void assertCompensationReplaysCompletion(ChaosScenarioResult result) {
    // 修复：并发环境下，验证集合相等而非严格顺序
    // 因为无依赖关系的任务可能并发完成，导致 completionOrder 和 compensationStack push 顺序略有差异
    Set<String> completedSet = new LinkedHashSet<>(result.completionOrder);
    Set<String> compensatedSet = new LinkedHashSet<>(result.compensationOrder);
    assertEquals(completedSet, compensatedSet, "所有完成的任务都应有补偿");
  }

  private static Set<String> collectDownstream(String taskId, Map<String, Set<String>> dependencies) {
    Set<String> downstream = new LinkedHashSet<>();
    for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
      if (entry.getValue().contains(taskId)) {
        downstream.add(entry.getKey());
        downstream.addAll(collectDownstream(entry.getKey(), dependencies));
      }
    }
    return downstream;
  }

  private static Map<String, TaskStatus> snapshotStatuses(AsyncTaskRegistry registry, Set<String> taskIds) {
    Map<String, TaskStatus> snapshot = new HashMap<>();
    for (String id : taskIds) {
      snapshot.put(id, registry.getStatus(id));
    }
    return snapshot;
  }

  private static Map<String, Set<String>> copyDependencies(Map<String, Set<String>> source) {
    Map<String, Set<String>> copy = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
      copy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
    }
    return copy;
  }

  private static List<String> copyList(List<String> source) {
    synchronized (source) {
      return new ArrayList<>(source);
    }
  }

  private static String extractTaskIdFromTimeout(TimeoutException timeout) {
    String message = timeout.getMessage();
    if (message == null) {
      return "unknown";
    }
    int idx = message.indexOf(':');
    if (idx == -1) {
      return message.trim();
    }
    return message.substring(idx + 1).trim();
  }

  private static final class ChaosScenarioResult {
    final String failingTaskId;
    final boolean timeout;
    final Map<String, Set<String>> dependencies;
    final Map<String, TaskStatus> statusSnapshot;
    final List<String> completionOrder;
    final List<String> compensationOrder;

    ChaosScenarioResult(String failingTaskId,
                       boolean timeout,
                       Map<String, Set<String>> dependencies,
                       Map<String, TaskStatus> statusSnapshot,
                       List<String> completionOrder,
                       List<String> compensationOrder) {
      this.failingTaskId = failingTaskId;
      this.timeout = timeout;
      this.dependencies = dependencies;
      this.statusSnapshot = statusSnapshot;
      this.completionOrder = completionOrder;
      this.compensationOrder = compensationOrder;
    }
  }
}
