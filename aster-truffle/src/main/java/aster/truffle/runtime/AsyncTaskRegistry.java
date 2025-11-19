package aster.truffle.runtime;

import aster.core.exceptions.MaxRetriesExceededException;
import io.aster.workflow.DeterminismContext;
import aster.runtime.workflow.WorkflowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.logging.Level;

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
  private static final Logger logger = Logger.getLogger(AsyncTaskRegistry.class.getName());
  private static final long DEFAULT_TTL_MILLIS = 60_000L;

  // 任务状态存储：task_id -> TaskState
  private final ConcurrentHashMap<String, TaskState> tasks = new ConcurrentHashMap<>();
  // 调度信息存储：task_id -> TaskInfo
  private final ConcurrentHashMap<String, TaskInfo> taskInfos = new ConcurrentHashMap<>();
  // 依赖图：内部自管理，避免依赖外部 WorkflowScheduler
  private final DependencyGraph dependencyGraph = new DependencyGraph();
  // 重试策略存储
  private final Map<String, RetryPolicy> retryPolicies = new ConcurrentHashMap<>();
  private final Map<String, Integer> attemptCounters = new ConcurrentHashMap<>();
  private final Set<String> failedTasks = ConcurrentHashMap.newKeySet();
  private volatile String workflowId;
  private PostgresEventStore eventStore;
  // 剩余待完成任务计数
  private final AtomicInteger remainingTasks = new AtomicInteger();
  // 线程池（默认 CPU 核数，可配置），size=1 时即单线程回退模式
  private final ExecutorService executor;
  private final boolean singleThreadMode;
  // graph 同步锁，DependencyGraph 非线程安全
  private final Object graphLock = new Object();
  // 补偿 LIFO 栈
  private final Deque<String> compensationStack = new ConcurrentLinkedDeque<>();
  // 默认超时时间（毫秒），0 表示无限制
  private final long defaultTimeoutMs;
  private final PriorityQueue<DelayedTask> delayQueue = new PriorityQueue<>();
  private final ReentrantLock delayQueueLock = new ReentrantLock();
  private Thread pollThread;
  private volatile boolean running = false;
  private final DeterminismContext determinismContext;
  private volatile boolean replayMode = false;

  /**
   * 并发调度需要的任务元数据
   */
  private static final class TaskInfo {
    final String taskId;
    final Callable<?> callable;
    final Set<String> dependencies;
    final CompletableFuture<Object> future = new CompletableFuture<>();
    final AtomicBoolean submitted = new AtomicBoolean(false);
    final long timeoutMs;
    final Runnable compensationCallback;
    final int priority;

    TaskInfo(String taskId, Callable<?> callable, Set<String> dependencies, long timeoutMs,
             Runnable compensationCallback, int priority) {
      this.taskId = taskId;
      this.callable = callable;
      this.dependencies = Collections.unmodifiableSet(new LinkedHashSet<>(dependencies));
      this.timeoutMs = timeoutMs;
      this.compensationCallback = compensationCallback;
      this.priority = priority;
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

  /**
   * 重试策略配置
   */
  public static class RetryPolicy {
    public final int maxAttempts;
    public final String backoff; // "exponential" or "linear"
    public final long baseDelayMs;

    public RetryPolicy(int maxAttempts, String backoff, long baseDelayMs) {
      this.maxAttempts = maxAttempts;
      this.backoff = backoff;
      this.baseDelayMs = baseDelayMs;
    }
  }

  public AsyncTaskRegistry() {
    this(loadThreadPoolSize(), loadDefaultTimeout(), new DeterminismContext());
  }

  public AsyncTaskRegistry(int threadPoolSize) {
    this(threadPoolSize, loadDefaultTimeout(), new DeterminismContext());
  }

  public AsyncTaskRegistry(int threadPoolSize, DeterminismContext determinismContext) {
    this(threadPoolSize, loadDefaultTimeout(), determinismContext);
  }

  private AsyncTaskRegistry(int threadPoolSize, long defaultTimeoutMs) {
    this(threadPoolSize, defaultTimeoutMs, new DeterminismContext());
  }

  private AsyncTaskRegistry(int threadPoolSize, long defaultTimeoutMs, DeterminismContext determinismContext) {
    int normalizedSize = Math.max(1, threadPoolSize);
    this.executor = Executors.newFixedThreadPool(normalizedSize);
    this.singleThreadMode = normalizedSize == 1;
    this.defaultTimeoutMs = Math.max(0L, defaultTimeoutMs);
    this.determinismContext = Objects.requireNonNull(determinismContext, "determinismContext cannot be null");
  }

  /**
   * 设置 workflowId（用于重试日志记录）
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * 注入事件存储
   */
  public void setEventStore(PostgresEventStore eventStore) {
    this.eventStore = eventStore;
  }

  /**
   * 获取当前的 DeterminismContext。
   *
   * @return 确定性上下文
   */
  public DeterminismContext getDeterminismContext() {
    return determinismContext;
  }

  /**
   * 切换重放模式：开启后重试退避从事件日志读取，而非重新计算。
   */
  public void setReplayMode(boolean replayMode) {
    this.replayMode = replayMode;
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
    registerInternal(taskId, callable, Collections.emptySet(), defaultTimeoutMs, null, 0);
    return taskId;
  }

  /**
   * 新的依赖感知注册接口：Workflow emitter 使用
   */
  public String registerTaskWithDependencies(
      String taskId, Callable<?> callable, Set<String> dependencies) {
    return registerTaskWithDependencies(taskId, callable, dependencies, 0L, null, 0);
  }

  /**
   * 依赖感知注册接口（可指定超时）
   */
  public String registerTaskWithDependencies(
      String taskId, Callable<?> callable, Set<String> dependencies, long timeoutMs) {
    return registerTaskWithDependencies(taskId, callable, dependencies, timeoutMs, null, 0);
  }

  /**
   * 依赖感知注册接口（可指定超时与补偿回调）
   */
  public String registerTaskWithDependencies(
      String taskId, Callable<?> callable, Set<String> dependencies, long timeoutMs,
      Runnable compensationCallback) {
    return registerTaskWithDependencies(taskId, callable, dependencies, timeoutMs, compensationCallback, 0);
  }

  /**
   * 依赖感知注册接口（可指定超时、补偿回调与优先级）
   */
  public String registerTaskWithDependencies(
      String taskId, Callable<?> callable, Set<String> dependencies, long timeoutMs,
      Runnable compensationCallback, int priority) {
    Objects.requireNonNull(taskId, "taskId");
    Objects.requireNonNull(callable, "callable");
    Set<String> deps = (dependencies == null) ? Collections.emptySet() : new LinkedHashSet<>(dependencies);
    long effectiveTimeout = timeoutMs > 0 ? timeoutMs : defaultTimeoutMs;
    registerInternal(taskId, callable, deps, effectiveTimeout, compensationCallback, priority);
    return taskId;
  }

  /**
   * 注册带重试策略的任务
   */
  public String registerTaskWithRetry(String taskId, Supplier<?> task, Set<String> dependencies,
      RetryPolicy policy) {
    Objects.requireNonNull(taskId, "taskId");
    Objects.requireNonNull(task, "task");
    if (policy != null) {
      retryPolicies.put(taskId, policy);
      attemptCounters.putIfAbsent(taskId, 1);
    }
    Callable<Object> callable = task::get;
    return registerTaskWithDependencies(taskId, callable, dependencies);
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
    try {
      while (remainingTasks.get() > 0) {
        List<String> readyTasks = snapshotReadyTasks();
        if (readyTasks.isEmpty()) {
          // 1. 检查失败任务（保持现有逻辑）
          Optional<TaskState> failed =
              tasks.values().stream().filter(state -> state.getStatus() == TaskStatus.FAILED).findFirst();
          if (failed.isPresent()) {
            throw new RuntimeException("Task failed: " + failed.get().taskId, failed.get().exception);
          }

          // 2. 收集详细诊断信息（增强）
          List<String> runningTasks = new ArrayList<>();
          Map<String, Set<String>> pendingTaskDeps = new LinkedHashMap<>();

          for (Map.Entry<String, TaskInfo> entry : taskInfos.entrySet()) {
            String taskId = entry.getKey();
            TaskState state = tasks.get(taskId);
            if (state != null) {
              TaskStatus status = state.getStatus();
              if (status == TaskStatus.RUNNING) {
                runningTasks.add(taskId);
              } else if (status == TaskStatus.PENDING) {
                Set<String> uncompletedDeps = getUncompletedDependencies(entry.getValue().dependencies);
                pendingTaskDeps.put(taskId, uncompletedDeps);
              }
            }
          }

          // 3. 检测循环依赖（增强）
          List<List<String>> cycles = detectCycles();

          // 4. 构建详细错误消息（增强）
          StringBuilder errorMsg = new StringBuilder();
          errorMsg.append("死锁检测：无就绪任务但仍有 ").append(remainingTasks.get()).append(" 个任务待完成\n");

          errorMsg.append("运行中任务：").append(runningTasks).append("\n");

          errorMsg.append("待处理任务及其依赖：\n");
          for (Map.Entry<String, Set<String>> entry : pendingTaskDeps.entrySet()) {
            TaskInfo info = taskInfos.get(entry.getKey());
            int priority = (info != null) ? info.priority : 0;
            errorMsg.append("  - ").append(entry.getKey())
                .append(" (优先级 ").append(priority).append(") 等待: ")
                .append(entry.getValue()).append("\n");
          }

          if (!cycles.isEmpty()) {
            errorMsg.append("检测到循环依赖：\n");
            for (List<String> cycle : cycles) {
              errorMsg.append("  - ");
              for (int i = 0; i < cycle.size(); i++) {
                if (i > 0) {
                  errorMsg.append(" -> ");
                }
                errorMsg.append(cycle.get(i));
              }
              errorMsg.append(" -> ").append(cycle.get(0)).append("\n");
            }
          }

          // 防止 race condition：在抛出异常前再次检查 remainingTasks
          if (remainingTasks.get() == 0) {
            // 最后一个任务刚完成，退出循环
            break;
          }
          throw new IllegalStateException(errorMsg.toString());
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
    } catch (RuntimeException | Error ex) {
      executeCompensations();
      throw ex;
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

  private void registerInternal(String taskId, Callable<?> callable, Set<String> deps, long timeoutMs,
      Runnable compensationCallback, int priority) {
    if (tasks.putIfAbsent(taskId, new TaskState(taskId)) != null) {
      throw new IllegalArgumentException("Task already exists: " + taskId);
    }

    TaskInfo info = new TaskInfo(taskId, callable, deps, Math.max(0L, timeoutMs), compensationCallback, priority);
    taskInfos.put(taskId, info);
    remainingTasks.incrementAndGet();

    synchronized (graphLock) {
      dependencyGraph.addTask(taskId, deps, priority);
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

    CompletableFuture<Object> trackedFuture = info.future;
    if (info.timeoutMs > 0) {
      trackedFuture = info.future
          .orTimeout(info.timeoutMs, TimeUnit.MILLISECONDS)
          .handle((result, throwable) -> {
            if (throwable == null) {
              return result;
            }
            Throwable actual = throwable instanceof CompletionException
                ? throwable.getCause()
                : throwable;
            if (actual instanceof TimeoutException timeout) {
              TimeoutException enriched = new TimeoutException("任务超时: " + info.taskId);
              enriched.initCause(timeout);
              handleTaskTimeout(info.taskId, enriched);
              throw new CompletionException(enriched);
            }
            throw new CompletionException(actual);
          });
    }

    executor.submit(() -> runTask(info));
    return trackedFuture;
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

    boolean retryScheduled = false;
    Throwable failure = null;
    try {
      Object callResult = info.callable.call();
      if (callResult != null && state.result == null) {
        state.result = callResult;
      }
      if (state.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.COMPLETED)) {
        info.future.complete(state.result);
        if (info.compensationCallback != null) {
          compensationStack.push(info.taskId);
        }
        cleanupRetryState(info.taskId);
        synchronized (graphLock) {
          dependencyGraph.markCompleted(info.taskId);
        }
      }
    } catch (Throwable t) {
      failure = t;
      RetryPolicy policy = retryPolicies.get(info.taskId);
      if (policy != null) {
        failedTasks.add(info.taskId);
        Exception failureException = (t instanceof Exception) ? (Exception) t : new Exception(t);
        try {
          onTaskFailed(info.taskId, failureException, replayMode);
          retryScheduled = true;
        } catch (MaxRetriesExceededException maxEx) {
          failure = maxEx;
          retryPolicies.remove(info.taskId);
          attemptCounters.remove(info.taskId);
          failedTasks.remove(info.taskId);
        }
      }

      if (retryScheduled) {
        state.exception = t;
        state.status.set(TaskStatus.PENDING);
      } else {
        handleFinalTaskFailure(info, state, failure);
      }
    } finally {
      if (!retryScheduled) {
        // 仅在任务真正结束时递减计数器
        remainingTasks.decrementAndGet();
      }
    }
  }

  private void handleFinalTaskFailure(TaskInfo info, TaskState state, Throwable error) {
    if (state.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.FAILED)) {
      state.exception = error;
      info.future.completeExceptionally(error);
      synchronized (graphLock) {
        dependencyGraph.markCompleted(info.taskId);
      }
      cancelDownstreamTasks(info.taskId);
    }
    cleanupRetryState(info.taskId);
  }

  private void cleanupRetryState(String taskId) {
    retryPolicies.remove(taskId);
    attemptCounters.remove(taskId);
    failedTasks.remove(taskId);
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

  /**
   * 处理任务超时：标记失败、触发补偿链并抛出带 taskId 的异常
   * 注意：不递减 remainingTasks，因为 runTask 的 finally 块会处理
   */
  private void handleTaskTimeout(String taskId, TimeoutException timeoutException) {
    TaskState state = tasks.get(taskId);
    if (state == null) {
      return;
    }

    boolean updated =
        state.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.FAILED)
            || state.status.compareAndSet(TaskStatus.PENDING, TaskStatus.FAILED);
    if (!updated) {
      return;
    }

    state.exception = timeoutException;
    TaskInfo info = taskInfos.get(taskId);
    if (info != null) {
      info.future.obtrudeException(timeoutException);
    }

    // 标记超时任务为已完成（让依赖图更新）
    synchronized (graphLock) {
      dependencyGraph.markCompleted(taskId);
    }

    cancelDownstreamTasks(taskId);

    // 触发补偿栈（Traditional Saga 模式：超时失败时回滚已完成任务）
    executeCompensations();

    // 注意：不递减 remainingTasks，runTask 的 finally 块会处理
  }

  /**
   * 执行补偿栈中的回调（LIFO 顺序）
   */
  private void executeCompensations() {
    while (true) {
      String taskId = compensationStack.poll();
      if (taskId == null) {
        break;
      }
      TaskInfo info = taskInfos.get(taskId);
      if (info == null || info.compensationCallback == null) {
        continue;
      }
      try {
        info.compensationCallback.run();
      } catch (Throwable t) {
        String message = t.getMessage();
        System.err.println("补偿失败 [" + taskId + "]: " + (message != null ? message : t.getClass().getSimpleName()));
      }
    }
  }

  /**
   * 查询给定依赖集合中尚未完成的任务
   *
   * @param dependencies 依赖任务 ID 集合
   * @return 状态不为 COMPLETED 的任务 ID 集合（包括 PENDING/RUNNING/FAILED/CANCELLED 或不存在的任务）
   */
  private Set<String> getUncompletedDependencies(Set<String> dependencies) {
    Set<String> uncompleted = new HashSet<>();
    for (String depId : dependencies) {
      TaskState state = tasks.get(depId);
      // null 状态或非 COMPLETED 状态均视为未完成
      if (state == null || state.status.get() != TaskStatus.COMPLETED) {
        uncompleted.add(depId);
      }
    }
    return uncompleted;
  }

  /**
   * 检测任务依赖图中的循环依赖
   * <p>
   * 使用深度优先搜索（DFS）算法遍历依赖图，识别反向边以检测循环。
   * 仅检查未完成的依赖（状态不为 COMPLETED），因为已完成的任务不会导致死锁。
   * 时间复杂度：O(V+E)，V=任务数，E=依赖边数
   *
   * @return 检测到的所有循环列表，每个循环是一个任务 ID 列表
   */
  private List<List<String>> detectCycles() {
    Set<String> visited = new HashSet<>();
    Set<String> recStack = new HashSet<>();
    List<List<String>> cycles = new ArrayList<>();

    for (String taskId : taskInfos.keySet()) {
      if (!visited.contains(taskId)) {
        List<String> path = new ArrayList<>();
        dfsCycleDetect(taskId, visited, recStack, path, cycles);
      }
    }
    return cycles;
  }

  /**
   * DFS 递归方法，检测从指定任务开始的循环依赖
   *
   * @param taskId 当前访问的任务 ID
   * @param visited 已访问节点集合（全局）
   * @param recStack 当前递归路径上的节点集合（用于检测反向边）
   * @param path 当前路径上的节点列表（用于提取循环）
   * @param cycles 存储检测到的循环列表
   * @return 如果检测到循环返回 true，否则返回 false
   */
  private boolean dfsCycleDetect(String taskId, Set<String> visited, Set<String> recStack,
                                 List<String> path, List<List<String>> cycles) {
    visited.add(taskId);
    recStack.add(taskId);
    path.add(taskId);

    TaskInfo info = taskInfos.get(taskId);
    if (info != null && info.dependencies != null) {
      for (String depId : info.dependencies) {
        TaskState depState = tasks.get(depId);
        // 仅检查未完成的依赖，已完成的任务不会导致死锁
        if (depState == null || depState.status.get() != TaskStatus.COMPLETED) {
          if (!visited.contains(depId)) {
            // 递归访问未访问的依赖
            if (dfsCycleDetect(depId, visited, recStack, path, cycles)) {
              return true;
            }
          } else if (recStack.contains(depId)) {
            // 发现反向边，提取循环路径
            int cycleStartIndex = path.indexOf(depId);
            List<String> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
            cycles.add(cycle);
            return true;
          }
        }
      }
    }

    // 回溯：从路径和递归栈中移除当前节点
    path.remove(path.size() - 1);
    recStack.remove(taskId);
    return false;
  }

  /**
   * 计算 backoff 延迟（确定性版本）
   *
   * 使用 DeterminismContext 确保重放时 jitter 一致
   */
  private long calculateBackoff(int attempt, String strategy, long baseDelayMs, DeterminismContext ctx) {
    long normalizedAttempt = Math.max(1, attempt);
    long backoffBase;
    if ("exponential".equalsIgnoreCase(strategy)) {
      backoffBase = (long) (baseDelayMs * Math.pow(2, normalizedAttempt - 1));
    } else {
      backoffBase = baseDelayMs * normalizedAttempt;
    }
    long jitterBound = Math.max(0L, baseDelayMs / 2);
    // 优先使用传入的 ctx，否则使用实例字段（保证非 null）
    DeterminismContext targetCtx = ctx != null ? ctx : this.determinismContext;
    long jitter = 0L;
    if (jitterBound > 0) {
      long raw = targetCtx.random().nextLong("async-task-backoff");
      jitter = Math.floorMod(raw, jitterBound);
    }
    return backoffBase + jitter;
  }

  private void onTaskFailed(String taskId, Exception exception, boolean isReplay) {
    RetryPolicy policy = retryPolicies.get(taskId);
    if (policy == null) {
      throw new RuntimeException("Task failed: " + taskId, exception);
    }

    int attempt = attemptCounters.getOrDefault(taskId, 1);
    if (attempt >= policy.maxAttempts) {
      throw new MaxRetriesExceededException(policy.maxAttempts, exception.getMessage(), exception);
    }

    long delayMs = isReplay
        ? getBackoffFromLog(taskId, attempt)
        : calculateBackoff(attempt, policy.backoff, policy.baseDelayMs, determinismContext);
    String failureReason = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();

    if (eventStore != null && workflowId != null) {
      try {
        eventStore.appendEvent(
            workflowId,
            "RETRY_SCHEDULED",
            Map.of("taskId", taskId, "reason", failureReason),
            attempt + 1,
            delayMs,
            failureReason
        );
      } catch (Exception logEx) {
        logger.log(Level.WARNING, String.format("Failed to log retry event for task %s", taskId), logEx);
      }
    }

    if (workflowId == null) {
      throw new IllegalStateException("workflowId must be set before scheduling retries");
    }

    attemptCounters.put(taskId, attempt + 1);
    scheduleRetry(workflowId, delayMs, attempt + 1, failureReason);

    logger.info(String.format("Task %s failed (attempt %d/%d), retrying in %dms",
        taskId, attempt, policy.maxAttempts, delayMs));
  }

  /**
   * 从事件日志恢复重试状态
   *
   * @param events 事件列表（按序列号升序）
   */
  public void restoreRetryState(List<WorkflowEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    for (WorkflowEvent event : events) {
      if ("RETRY_SCHEDULED".equals(event.getEventType())) {
        Object payload = event.getPayload();
        String taskId = null;
        if (payload instanceof Map<?, ?> map) {
          Object raw = map.get("taskId");
          if (raw != null) {
            taskId = raw.toString();
          }
        }
        if (taskId == null) {
          continue;
        }
        Integer attemptNumber = event.getAttemptNumber();
        if (attemptNumber != null) {
          int normalizedAttempt = Math.max(1, attemptNumber - 1);
          int merged = attemptCounters.merge(taskId, normalizedAttempt, Math::min);
          logger.fine(String.format("Restored retry state for task %s: attempt=%d, backoff=%dms",
              taskId, merged, event.getBackoffDelayMs()));
        }
      }
    }
  }

  private long getBackoffFromLog(String taskId, int attempt) {
    if (eventStore == null || workflowId == null) {
      throw new IllegalStateException("eventStore 和 workflowId 未设置，无法从日志恢复 backoff");
    }
    List<WorkflowEvent> events = eventStore.getEvents(workflowId, 0L);
    int expectedAttemptNumber = attempt + 1;
    for (WorkflowEvent event : events) {
      if (!"RETRY_SCHEDULED".equals(event.getEventType())) {
        continue;
      }
      Integer attemptNumber = event.getAttemptNumber();
      if (!Objects.equals(attemptNumber, expectedAttemptNumber)) {
        continue;
      }
      Object payload = event.getPayload();
      if (payload instanceof Map<?, ?> map) {
        Object rawTaskId = map.get("taskId");
        if (rawTaskId != null && taskId.equals(rawTaskId.toString())) {
          Long delay = event.getBackoffDelayMs();
          if (delay != null) {
            return delay;
          }
        }
      }
    }
    throw new IllegalStateException("未找到任务 " + taskId + " 第 " + expectedAttemptNumber + " 次重试的 backoff 记录");
  }

  /**
   * 调度延迟重试任务
   *
   * @param workflowId workflow 唯一标识符
   * @param delayMs 延迟时间（毫秒）
   * @param attemptNumber 重试次数（从1开始）
   * @param failureReason 失败原因
   */
  public void scheduleRetry(String workflowId, long delayMs, int attemptNumber, String failureReason) {
    long triggerAt = this.determinismContext.clock().now().toEpochMilli() + delayMs;

    delayQueueLock.lock();
    try {
      DelayedTask task = new DelayedTask(workflowId, triggerAt, attemptNumber, failureReason);
      delayQueue.offer(task);
      logger.fine(String.format("Scheduled retry for workflow %s in %dms (attempt %d)",
          workflowId, delayMs, attemptNumber));
    } finally {
      delayQueueLock.unlock();
    }
  }

  /**
   * 启动延迟任务轮询线程
   *
   * 注意：需在 WorkflowSchedulerService 启动时调用
   */
  public synchronized void startPolling() {
    if (running) {
      return;
    }

    running = true;
    pollThread = new Thread(this::pollDelayedTasks, "workflow-delay-poller");
    pollThread.setDaemon(true);
    pollThread.start();
    logger.info("Started workflow delay task poller");
  }

  /**
   * 停止延迟任务轮询线程
   *
   * 注意：需在 WorkflowSchedulerService 关闭时调用
   */
  public synchronized void stopPolling() {
    running = false;
    if (pollThread != null) {
      pollThread.interrupt();
      try {
        pollThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        pollThread = null;
      }
    }
    logger.info("Stopped workflow delay task poller");
  }

  /**
   * 后台轮询延迟任务队列，触发到期任务
   */
  private void pollDelayedTasks() {
    while (running) {
      try {
        delayQueueLock.lock();
        try {
          DelayedTask task = delayQueue.peek();
          long now = this.determinismContext.clock().now().toEpochMilli();

          if (task != null && task.triggerAtMs <= now) {
            delayQueue.poll();
            logger.fine(String.format("Triggering delayed retry for workflow %s (attempt %d)",
                task.workflowId, task.attemptNumber));

            // 触发 workflow 恢复（简化实现：重新调度任务）
            // TODO: 后续集成到 WorkflowScheduler.handleRetry
            resumeWorkflow(task.workflowId, task.attemptNumber);
          }
        } finally {
          delayQueueLock.unlock();
        }

        // 轮询间隔 100ms
        Thread.sleep(100);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error polling delayed tasks", e);
      }
    }
  }

  /**
   * 恢复 workflow 执行（由 timer 触发）
   */
  private void resumeWorkflow(String workflowId, int attemptNumber) {
    logger.info(String.format("Resuming workflow %s (attempt %d)", workflowId, attemptNumber));

    Set<String> tasksToRetry = new HashSet<>();
    Iterator<String> iterator = failedTasks.iterator();
    while (iterator.hasNext()) {
      String taskId = iterator.next();
      tasksToRetry.add(taskId);
      iterator.remove();
    }

    for (String taskId : tasksToRetry) {
      scheduleTask(taskId);
    }
  }

  private void scheduleTask(String taskId) {
    TaskInfo info = taskInfos.get(taskId);
    TaskState state = tasks.get(taskId);
    if (info == null || state == null) {
      return;
    }
    if (state.status.get() != TaskStatus.PENDING) {
      return;
    }
    if (!isDependencySatisfied(taskId)) {
      failedTasks.add(taskId);
      return;
    }
    info.submitted.set(false);
    submitTask(info);
  }

  /**
   * 读取线程池大小（环境变量 ASTER_THREAD_POOL_SIZE，默认 CPU 核心数）
   */
  private static int loadThreadPoolSize() {
    String env = System.getenv("ASTER_THREAD_POOL_SIZE");
    if (env == null || env.isEmpty()) {
      return Runtime.getRuntime().availableProcessors();
    }
    try {
      int size = Integer.parseInt(env);
      if (size > 0) {
        return size;
      }
      System.err.println("警告：ASTER_THREAD_POOL_SIZE 必须 > 0，使用默认值");
      return Runtime.getRuntime().availableProcessors();
    } catch (NumberFormatException e) {
      System.err.println("警告：ASTER_THREAD_POOL_SIZE 解析失败，使用默认值: " + env);
      return Runtime.getRuntime().availableProcessors();
    }
  }

  /**
   * 读取默认超时时间（环境变量 ASTER_DEFAULT_TIMEOUT_MS，0 表示无限制）
   */
  private static long loadDefaultTimeout() {
    String env = System.getenv("ASTER_DEFAULT_TIMEOUT_MS");
    if (env == null || env.isEmpty()) {
      return 0L;
    }
    try {
      long timeout = Long.parseLong(env);
      if (timeout >= 0) {
        return timeout;
      }
      System.err.println("警告：ASTER_DEFAULT_TIMEOUT_MS 必须 >= 0，使用默认值");
      return 0L;
    } catch (NumberFormatException e) {
      System.err.println("警告：ASTER_DEFAULT_TIMEOUT_MS 解析失败，使用默认值: " + env);
      return 0L;
    }
  }
}
