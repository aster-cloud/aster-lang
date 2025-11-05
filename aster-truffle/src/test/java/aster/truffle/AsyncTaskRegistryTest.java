package aster.truffle;

import aster.truffle.runtime.AsyncTaskRegistry;
import aster.truffle.runtime.AsyncTaskRegistry.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：AsyncTaskRegistry 基础功能
 *
 * 验证 Phase 1 单线程调度模型的核心功能：
 * 1. 任务注册
 * 2. 状态查询
 * 3. 结果存储
 * 4. executeNext() 调度
 * 5. 异常捕获
 */
public class AsyncTaskRegistryTest {

  private AsyncTaskRegistry registry;
  private AtomicLong taskIdGenerator;

  @BeforeEach
  public void setup() {
    // Create registry directly without AsterContext dependency
    registry = new AsyncTaskRegistry();
    taskIdGenerator = new AtomicLong(0);
  }

  /**
   * Generate task ID similar to AsterContext.generateTaskId()
   */
  private String generateTaskId() {
    return "task-" + taskIdGenerator.incrementAndGet();
  }

  /**
   * 测试1：任务注册与状态查询
   */
  @Test
  public void testRegisterTask() {
    String taskId = generateTaskId();
    registry.registerTask(taskId, () -> {
      // Empty task
    });

    // 验证任务已注册且状态为 PENDING
    assertEquals(TaskStatus.PENDING, registry.getStatus(taskId));
    assertEquals(1, registry.getTaskCount());
    assertEquals(1, registry.getPendingCount());
  }

  /**
   * 测试2：executeNext() 成功执行任务
   */
  @Test
  public void testExecuteNextSuccess() {
    AtomicInteger counter = new AtomicInteger(0);
    String taskId = generateTaskId();

    registry.registerTask(taskId, () -> {
      counter.incrementAndGet();
      registry.setResult(taskId, 42);
    });

    // 执行任务
    registry.executeNext();

    // 验证任务已完成
    assertEquals(TaskStatus.COMPLETED, registry.getStatus(taskId));
    assertEquals(1, counter.get());
    assertEquals(42, registry.getResult(taskId));
  }

  /**
   * 测试3：executeNext() 捕获任务异常
   */
  @Test
  public void testExecuteNextException() {
    String taskId = generateTaskId();
    RuntimeException expectedException = new RuntimeException("Test exception");

    registry.registerTask(taskId, () -> {
      throw expectedException;
    });

    // 执行任务
    registry.executeNext();

    // 验证任务状态为 FAILED
    assertEquals(TaskStatus.FAILED, registry.getStatus(taskId));
    assertEquals(expectedException, registry.getException(taskId));
  }

  /**
   * 测试4：getResult() 对失败任务抛出异常
   */
  @Test
  public void testGetResultThrowsOnFailedTask() {
    String taskId = generateTaskId();

    registry.registerTask(taskId, () -> {
      throw new RuntimeException("Test exception");
    });

    registry.executeNext();

    // 验证 getResult() 抛出异常
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      registry.getResult(taskId);
    });

    assertTrue(thrown.getMessage().contains("Task failed"));
  }

  /**
   * 测试5：多任务 FIFO 调度
   */
  @Test
  public void testMultipleTasksFIFO() {
    AtomicInteger counter = new AtomicInteger(0);

    String task1 = generateTaskId();
    String task2 = generateTaskId();
    String task3 = generateTaskId();

    registry.registerTask(task1, () -> {
      counter.set(1);
      registry.setResult(task1, 1);
    });

    registry.registerTask(task2, () -> {
      counter.set(2);
      registry.setResult(task2, 2);
    });

    registry.registerTask(task3, () -> {
      counter.set(3);
      registry.setResult(task3, 3);
    });

    // 执行第一个任务
    registry.executeNext();
    assertEquals(1, counter.get());
    assertEquals(TaskStatus.COMPLETED, registry.getStatus(task1));

    // 执行第二个任务
    registry.executeNext();
    assertEquals(2, counter.get());
    assertEquals(TaskStatus.COMPLETED, registry.getStatus(task2));

    // 执行第三个任务
    registry.executeNext();
    assertEquals(3, counter.get());
    assertEquals(TaskStatus.COMPLETED, registry.getStatus(task3));
  }

  /**
   * 测试6：isCompleted() 和 isFailed()
   */
  @Test
  public void testStatusHelpers() {
    String task1 = generateTaskId();
    String task2 = generateTaskId();

    registry.registerTask(task1, () -> {
      registry.setResult(task1, "success");
    });

    registry.registerTask(task2, () -> {
      throw new RuntimeException("fail");
    });

    // 初始状态
    assertFalse(registry.isCompleted(task1));
    assertFalse(registry.isFailed(task1));

    // 执行成功任务
    registry.executeNext();
    assertTrue(registry.isCompleted(task1));
    assertFalse(registry.isFailed(task1));

    // 执行失败任务
    registry.executeNext();
    assertFalse(registry.isCompleted(task2));
    assertTrue(registry.isFailed(task2));
  }

  /**
   * 测试7：任务 GC 清理
   */
  @Test
  public void testGarbageCollection() throws InterruptedException {
    String taskId = generateTaskId();

    registry.registerTask(taskId, () -> {
      registry.setResult(taskId, 42);
    });

    registry.executeNext();
    assertEquals(1, registry.getTaskCount());

    // GC 不应立即移除刚完成的任务
    registry.gc();
    assertEquals(1, registry.getTaskCount());

    // 等待 TTL 过期（设计文档中 TTL 为 60 秒，但测试中可以接受任务仍在）
    // 注意：这个测试仅验证 gc() 不会崩溃，实际 TTL 测试需要更长时间
  }

  /**
   * 测试8：不存在的任务
   */
  @Test
  public void testNonExistentTask() {
    String nonExistentId = "task-999999";

    assertNull(registry.getStatus(nonExistentId));
    assertFalse(registry.isCompleted(nonExistentId));
    assertFalse(registry.isFailed(nonExistentId));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      registry.getResult(nonExistentId);
    });

    assertTrue(thrown.getMessage().contains("Task not found"));
  }
}
