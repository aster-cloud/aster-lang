package aster.truffle.runtime;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步任务注册表 - 管理所有异步任务的生命周期、状态和结果
 *
 * Phase 1 实现：单线程协作式调度
 * - 任务按注册顺序存入队列
 * - executeNext() 方法显式调度下一个 PENDING 任务
 * - 无真实并发，简化调试和测试
 */
public final class AsyncTaskRegistry {
  // 任务存储：task_id -> TaskState
  private final ConcurrentHashMap<String, TaskState> tasks = new ConcurrentHashMap<>();

  // Phase 1: 任务队列（FIFO 调度）
  private final LinkedList<String> pendingQueue = new LinkedList<>();

  // 任务状态枚举
  public enum TaskStatus {
    PENDING,    // 任务已注册，尚未开始执行
    RUNNING,    // 任务正在执行中
    COMPLETED,  // 任务成功完成，结果已存储
    FAILED      // 任务执行失败，异常已存储
  }

  /**
   * 任务状态封装
   */
  public static final class TaskState {
    final String taskId;
    final Runnable taskBody;              // 任务执行体
    final AtomicReference<TaskStatus> status;
    volatile Object result;                // 任务结果（COMPLETED 时）
    volatile Throwable exception;          // 任务异常（FAILED 时）
    final long createdAt;                 // 创建时间戳（用于 TTL）

    TaskState(String taskId, Runnable taskBody) {
      this.taskId = taskId;
      this.taskBody = taskBody;
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
   * 注册新任务到注册表
   *
   * @param taskId 任务唯一标识符（由 AsterContext.generateTaskId() 生成）
   * @param taskBody 任务执行体
   * @return 任务 ID
   */
  public String registerTask(String taskId, Runnable taskBody) {
    TaskState state = new TaskState(taskId, taskBody);
    tasks.put(taskId, state);
    pendingQueue.add(taskId);
    return taskId;
  }

  /**
   * 获取任务状态对象（用于 AwaitNode/WaitNode）
   *
   * @param taskId 任务 ID
   * @return TaskState 对象，如果任务不存在返回 null
   */
  public TaskState getTaskState(String taskId) {
    return tasks.get(taskId);
  }

  /**
   * 获取任务状态
   *
   * @param taskId 任务 ID
   * @return 任务状态，如果任务不存在返回 null
   */
  public TaskStatus getStatus(String taskId) {
    TaskState state = tasks.get(taskId);
    return state != null ? state.status.get() : null;
  }

  /**
   * 检查任务是否已完成
   *
   * @param taskId 任务 ID
   * @return true 如果任务状态为 COMPLETED
   */
  public boolean isCompleted(String taskId) {
    TaskStatus status = getStatus(taskId);
    return status == TaskStatus.COMPLETED;
  }

  /**
   * 检查任务是否失败
   *
   * @param taskId 任务 ID
   * @return true 如果任务状态为 FAILED
   */
  public boolean isFailed(String taskId) {
    TaskStatus status = getStatus(taskId);
    return status == TaskStatus.FAILED;
  }

  /**
   * 获取任务结果（仅在 COMPLETED 状态）
   *
   * @param taskId 任务 ID
   * @return 任务结果
   * @throws RuntimeException 如果任务未完成或已失败
   */
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

  /**
   * 获取任务异常（仅在 FAILED 状态）
   *
   * @param taskId 任务 ID
   * @return 任务异常，如果任务未失败返回 null
   */
  public Throwable getException(String taskId) {
    TaskState state = tasks.get(taskId);
    return state != null ? state.exception : null;
  }

  /**
   * Phase 1: 执行队列中下一个 PENDING 任务
   *
   * 协作式调度 - 调用方需要在适当位置主动调用此方法：
   * - AwaitNode.executeGeneric() 调用前
   * - WaitNode.executeGeneric() 轮询循环中
   * - 循环末尾、条件分支前等
   */
  public void executeNext() {
    if (pendingQueue.isEmpty()) {
      return;
    }

    String taskId = pendingQueue.poll();
    if (taskId == null) {
      return;
    }

    TaskState state = tasks.get(taskId);
    if (state == null) {
      return;
    }

    // CAS 状态转换：PENDING -> RUNNING
    if (!state.status.compareAndSet(TaskStatus.PENDING, TaskStatus.RUNNING)) {
      return;
    }

    try {
      state.taskBody.run();
      // 注意：result 需要在 taskBody 中通过 setResult() 设置
      state.status.set(TaskStatus.COMPLETED);
    } catch (Throwable t) {
      state.exception = t;
      state.status.set(TaskStatus.FAILED);
    }
  }

  /**
   * 设置任务结果（由任务执行体内部调用）
   *
   * @param taskId 任务 ID
   * @param result 任务结果
   */
  public void setResult(String taskId, Object result) {
    TaskState state = tasks.get(taskId);
    if (state != null) {
      state.result = result;
    }
  }

  /**
   * 移除指定任务
   *
   * @param taskId 任务 ID
   */
  public void removeTask(String taskId) {
    tasks.remove(taskId);
  }

  /**
   * 垃圾回收 - 清理已完成/失败的过期任务
   *
   * TTL-based GC：移除超过 60 秒的终态任务
   */
  public void gc() {
    long now = System.currentTimeMillis();
    long ttl = 60_000;  // 60 seconds

    tasks.entrySet().removeIf(entry -> {
      TaskState state = entry.getValue();
      TaskStatus status = state.status.get();

      return (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED)
          && (now - state.createdAt > ttl);
    });
  }

  /**
   * 获取当前任务数量（用于调试和监控）
   *
   * @return 任务总数
   */
  public int getTaskCount() {
    return tasks.size();
  }

  /**
   * 获取待执行任务数量（用于调试和监控）
   *
   * @return 队列中待执行任务数
   */
  public int getPendingCount() {
    return pendingQueue.size();
  }
}
