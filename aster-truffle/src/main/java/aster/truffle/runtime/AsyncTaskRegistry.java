package aster.truffle.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步任务注册表 - 负责任务注册、状态跟踪与调度
 *
 * Phase 2.0 之前：
 * - 依赖 LinkedList 顺序执行，无法并行
 *
 * Phase 2.2：
 * - 基于 CompletableFuture + ExecutorService 的依赖感知并发调度
 * - 维持 TaskState API，保证 Await/Wait 节点向后兼容
 * - 暴露 registerTaskWithDependencies 支撑 Workflow emitter
 */
public final class AsyncTaskRegistry {
  private static final long DEFAULT_TTL_MILLIS = 60_000L;

  // 任务状态存储：task_id -> TaskState
  private final ConcurrentHashMap<String, TaskState> tasks = new ConcurrentHashMap<>();
  // 调度信息存储：task_id -> TaskInfo
  private final ConcurrentHashMap<String, TaskInfo> taskInfos = new ConcurrentHashMap<>();
  // 依赖图：内部自管理，避免依赖外部 WorkflowScheduler
  private final DependencyGraph dependencyGraph = new DependencyGraph();
  // 剩余待完成任务计数
  private final AtomicInteger remainingTasks = new AtomicInteger();
  // 线程池（默认 CPU 核数，可配置），size=1 时即单线程回退模式
  private final ExecutorService executor;
  private final boolean singleThreadMode;
  // graph 同步锁，DependencyGraph 非线程安全
  private final Object graphLock = new Object();

  /**
   * 并发调度需要的任务元数据
   */
  private static final class TaskInfo {
    final String taskId;
    final Callable<?> callable;
    final Set<String> dependencies;
    final CompletableFuture<Object> future = new CompletableFuture<>();
    final AtomicBoolean submitted = new AtomicBoolean(false);

    TaskInfo(String taskId, Callable<?> callable, Set<String> dependencies) {
      this.taskId = taskId;
      this.callable = callable;
      this.dependencies = Collections.unmodifiableSet(new LinkedHashSet<>(dependencies));
    }
  }

  /**
   * 任务状态封装（保持 Phase 1 API）
   */
  public static final class TaskState {
    final String taskId;
    final AtomicReference<TaskStatus> status;
    volatile Object result;
    volatile Throwable exception;
    final long createdAt;

    TaskState(String taskId) {
      this.taskId = taskId;
      this.status = new AtomicReference<>(TaskStatus.PENDING);
      this.createdAt = System.currentTimeMillis();
    }

    public String getTaskId() {
      return taskId;
    }

    public TaskStatus getStatus() {
      return status.get();
    }

    public Object getResult() {
      return result;
    }

    public Throwable getException() {
      return exception;
    }
  }

  /**
   * 任务状态枚举
   */
  public enum TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
  }

  public AsyncTaskRegistry() {
    this(Runtime.getRuntime().availableProcessors());
  }

  public AsyncTaskRegistry(int threadPoolSize) {
    int normalizedSize = Math.max(1, threadPoolSize);
    this.executor = Executors.newFixedThreadPool(normalizedSize);
    this.singleThreadMode = normalizedSize == 1;
  }

  /**
   * 注册无依赖任务（兼容 StartNode 用途）
   */
  public String registerTask(String taskId, Runnable taskBody) {
    Objects.requireNonNull(taskId, "taskId");
    Objects.requireNonNull(taskBody, "taskBody");

    Callable<Object> callable = () -> {
      taskBody.run();
      return null;
    };
    registerInternal(taskId, callable, Collections.emptySet());
    return taskId;
  }

  /**
   * 新的依赖感知注册接口：Workflow emitter 使用
   */
  public String registerTaskWithDependencies(
      String taskId, Callable<?> callable, Set<String> dependencies) {
    Objects.requireNonNull(taskId, "taskId");
    Objects.requireNonNull(callable, "callable");
    Set<String> deps = (dependencies == null) ? Collections.emptySet() : new LinkedHashSet<>(dependencies);
    registerInternal(taskId, callable, deps);
    return taskId;
  }

  /**
   * 返回任务状态（供 Await/Wait Node 轮询）
   */
  public TaskState getTaskState(String taskId) {
    return tasks.get(taskId);
  }

  public TaskStatus getStatus(String taskId) {
    TaskState state = tasks.get(taskId);
    return state != null ? state.status.get() : null;
  }

  public boolean isCompleted(String taskId) {
    TaskStatus status = getStatus(taskId);
    return status == TaskStatus.COMPLETED;
  }

  public boolean isFailed(String taskId) {
    TaskStatus status = getStatus(taskId);
    return status == TaskStatus.FAILED;
  }

  public Object getResult(String taskId) {
    TaskState state = tasks.get(taskId);
    if (state == null) {
      throw new RuntimeException("Task not found: " + taskId);
    }

    TaskStatus status = state.status.get();
    if (status == TaskStatus.COMPLETED) {
      return state.result;
    } else if (status == TaskStatus.FAILED) {
      throw new RuntimeException("Task failed: " + taskId, state.exception);
    } else {
      throw new RuntimeException("Task not completed: " + taskId + " (status: " + status + ")");
    }
  }

  public Throwable getException(String taskId) {
    TaskState state = tasks.get(taskId);
    return state != null ? state.exception : null;
  }

  /**
   * Phase 1 兼容：显式执行一个就绪任务（串行 fallback）
   */
  public void executeNext() {
    String nextTaskId = nextReadyTaskId();
    if (nextTaskId == null) {
      return;
    }
    TaskInfo info = taskInfos.get(nextTaskId);
    if (info == null) {
      return;
    }
    // 单线程模式直接在调用线程执行；多线程模式下也允许显式拉起一个任务
    runTaskInline(info);
  }

  /**
   * 并发调度主入口：按依赖拓扑批量调度所有任务
   */
  public void executeUntilComplete() {
    while (remainingTasks.get() > 0) {
      List<String> readyTasks = snapshotReadyTasks();
      if (readyTasks.isEmpty()) {
        Optional<TaskState> failed =
            tasks.values().stream().filter(state -> state.getStatus() == TaskStatus.FAILED).findFirst();
        if (failed.isPresent()) {
          throw new RuntimeException("Task failed: " + failed.get().taskId, failed.get().exception);
        }
        throw new IllegalStateException("Deadlock detected: no ready tasks but graph still has nodes");
      }

      List<CompletableFuture<Object>> batchFutures = new ArrayList<>(readyTasks.size());
      for (String taskId : readyTasks) {
        TaskInfo info = taskInfos.get(taskId);
        if (info == null) {
          continue;
        }
        if (canSchedule(taskId)) {
          batchFutures.add(submitTask(info));
        }
      }

      if (batchFutures.isEmpty()) {
        // 所有 ready 节点都处于非 PENDING 状态，说明等待上轮运行结束
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        continue;
      }

      CompletableFuture<Void> barrier =
          CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]));
      try {
        barrier.join();
      } catch (CompletionException ex) {
        throw unwrapCompletion(ex);
      }
    }
  }

  /**
   * 设置任务执行结果（StartNode/Workflow step 共用）
   */
  public void setResult(String taskId, Object result) {
    TaskState state = tasks.get(taskId);
    if (state != null) {
      state.result = result;
    }
  }

  public void removeTask(String taskId) {
    tasks.remove(taskId);
    taskInfos.remove(taskId);
  }

  public void gc() {
    long now = System.currentTimeMillis();
    tasks.entrySet().removeIf(entry -> {
      TaskState state = entry.getValue();
      TaskStatus status = state.status.get();
      boolean expired = now - state.createdAt > DEFAULT_TTL_MILLIS;
      if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) && expired) {
        taskInfos.remove(state.taskId);
        return true;
      }
      return false;
    });
  }

  public int getTaskCount() {
    return tasks.size();
  }

  public int getPendingCount() {
    int pending = 0;
    for (TaskState state : tasks.values()) {
      if (state.getStatus() == TaskStatus.PENDING) {
        pending++;
      }
    }
    return pending;
  }

  public boolean isDependencySatisfied(String taskId) {
    TaskInfo info = taskInfos.get(taskId);
    if (info == null || info.dependencies.isEmpty()) {
      return true;
    }

    for (String dep : info.dependencies) {
      if (!isCompleted(dep)) {
        return false;
      }
    }
    return true;
  }

  public void cancelTask(String taskId) {
    TaskState state = tasks.get(taskId);
    TaskInfo info = taskInfos.get(taskId);
    if (state == null || info == null) {
      return;
    }
    if (state.status.compareAndSet(TaskStatus.PENDING, TaskStatus.CANCELLED)) {
      info.future.cancel(true);
      remainingTasks.decrementAndGet();
      synchronized (graphLock) {
        dependencyGraph.markCompleted(taskId);
      }
    }
  }

  public boolean isCancelled(String taskId) {
    TaskStatus status = getStatus(taskId);
    return status == TaskStatus.CANCELLED;
  }

  /**
   * 关闭线程池，释放资源
   */
  public void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // ========= 内部工具方法 =========

  private void registerInternal(String taskId, Callable<?> callable, Set<String> deps) {
    if (tasks.putIfAbsent(taskId, new TaskState(taskId)) != null) {
      throw new IllegalArgumentException("Task already exists: " + taskId);
    }

    TaskInfo info = new TaskInfo(taskId, callable, deps);
    taskInfos.put(taskId, info);
    remainingTasks.incrementAndGet();

    synchronized (graphLock) {
      dependencyGraph.addTask(taskId, deps);
    }
  }

  private List<String> snapshotReadyTasks() {
    synchronized (graphLock) {
      return dependencyGraph.getReadyTasks();
    }
  }

  private String nextReadyTaskId() {
    List<String> ready = snapshotReadyTasks();
    for (String taskId : ready) {
      if (canSchedule(taskId)) {
        return taskId;
      }
    }
    return null;
  }

  private boolean canSchedule(String taskId) {
    TaskState state = tasks.get(taskId);
    return state != null && state.status.get() == TaskStatus.PENDING;
  }

  private CompletableFuture<Object> submitTask(TaskInfo info) {
    if (!info.submitted.compareAndSet(false, true)) {
      return info.future;
    }
    executor.submit(() -> runTask(info));
    return info.future;
  }

  private void runTaskInline(TaskInfo info) {
    if (!info.submitted.compareAndSet(false, true)) {
      return;
    }
    if (singleThreadMode) {
      runTask(info);
    } else {
      // 在多线程模式下，同步执行仍运行于调用线程，适合旧节点的协作式调用
      runTask(info);
    }
  }

  /**
   * 核心执行逻辑：更新状态、写回依赖图、完成 future
   */
  private void runTask(TaskInfo info) {
    TaskState state = tasks.get(info.taskId);
    if (state == null) {
      info.future.completeExceptionally(new IllegalStateException("Unknown task: " + info.taskId));
      remainingTasks.decrementAndGet();
      return;
    }

    // Check if any dependency has failed before transitioning to RUNNING
    // This prevents race condition where task gets scheduled before cancellation propagates
    if (!info.dependencies.isEmpty()) {
      for (String dep : info.dependencies) {
        if (isFailed(dep)) {
          // Dependency failed, mark this task as cancelled
          if (state.status.compareAndSet(TaskStatus.PENDING, TaskStatus.CANCELLED)) {
            info.future.cancel(false);
            remainingTasks.decrementAndGet();
            synchronized (graphLock) {
              dependencyGraph.markCompleted(info.taskId);
            }
          }
          return;
        }
      }
    }

    if (!state.status.compareAndSet(TaskStatus.PENDING, TaskStatus.RUNNING)) {
      if (state.status.get() == TaskStatus.CANCELLED) {
        info.future.cancel(false);
        remainingTasks.decrementAndGet();
      }
      return;
    }

    try {
      Object callResult = info.callable.call();
      if (callResult != null && state.result == null) {
        state.result = callResult;
      }
      state.status.set(TaskStatus.COMPLETED);
      info.future.complete(state.result);
      synchronized (graphLock) {
        dependencyGraph.markCompleted(info.taskId);
      }
    } catch (Throwable t) {
      state.exception = t;
      state.status.set(TaskStatus.FAILED);
      info.future.completeExceptionally(t);
      // 取消所有依赖于失败任务的下游任务
      cancelDownstreamTasks(info.taskId);
    } finally {
      remainingTasks.decrementAndGet();
    }
  }

  /**
   * 取消所有直接或间接依赖于指定任务的下游任务
   */
  private void cancelDownstreamTasks(String failedTaskId) {
    for (Map.Entry<String, TaskInfo> entry : taskInfos.entrySet()) {
      String taskId = entry.getKey();
      TaskInfo info = entry.getValue();

      // 检查此任务是否依赖于失败的任务（直接或间接）
      if (dependsOnFailedTask(taskId, failedTaskId)) {
        cancelTask(taskId);
      }
    }
  }

  /**
   * 检查 taskId 是否直接或间接依赖于 failedTaskId
   */
  private boolean dependsOnFailedTask(String taskId, String failedTaskId) {
    TaskInfo info = taskInfos.get(taskId);
    if (info == null || info.dependencies.isEmpty()) {
      return false;
    }

    // 直接依赖
    if (info.dependencies.contains(failedTaskId)) {
      return true;
    }

    // 间接依赖：递归检查
    for (String dep : info.dependencies) {
      if (dependsOnFailedTask(dep, failedTaskId)) {
        return true;
      }
    }

    return false;
  }

  private RuntimeException unwrapCompletion(CompletionException ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof RuntimeException) {
      return (RuntimeException) cause;
    }
    return new RuntimeException("Async task execution failed", cause);
  }
}
