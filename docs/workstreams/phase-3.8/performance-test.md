# Phase 3.8 性能测试指南

## 测试目标

验证 Phase 3.8 新增功能在大量数据场景下的性能表现：
1. JOIN LATERAL 查询在大量 workflow 场景下的性能
2. Replay 验证在并发场景下的吞吐量
3. 系统资源消耗（CPU、内存、数据库连接）

## 测试环境准备

### 1. 数据准备脚本

创建大量测试数据以模拟生产环境：

```sql
-- 创建性能测试数据准备脚本
-- performance_test_data_setup.sql

-- 清理测试数据
DELETE FROM anomaly_reports WHERE policy_id LIKE 'perf-test-%';
DELETE FROM workflow_state WHERE policy_version_id IN (
    SELECT id FROM policy_versions WHERE policy_id LIKE 'perf-test-%'
);
DELETE FROM policy_versions WHERE policy_id LIKE 'perf-test-%';

-- 创建测试策略版本
INSERT INTO policy_versions (policy_id, version, module_name, function_name, content, active, created_at)
SELECT
    'perf-test-policy-' || i,
    extract(epoch from now())::bigint,
    'test.module',
    'testFunction',
    'test content',
    true,
    NOW()
FROM generate_series(1, 100) i;  -- 100 个策略版本

-- 为每个策略版本创建 workflow 数据
-- 场景1: 每个策略 1000 个 workflow（总计 100,000 个）
DO $$
DECLARE
    v_id BIGINT;
    policy_id_val TEXT;
BEGIN
    FOR v_id, policy_id_val IN
        SELECT id, policy_id FROM policy_versions WHERE policy_id LIKE 'perf-test-%'
    LOOP
        -- 为每个策略创建 1000 个 workflow（70% 成功，30% 失败）
        INSERT INTO workflow_state (
            workflow_id, policy_version_id, started_at, status, duration_ms, clock_times
        )
        SELECT
            gen_random_uuid(),
            v_id,
            NOW() - (random() * INTERVAL '7 days'),  -- 最近 7 天内的随机时间
            CASE WHEN random() < 0.7 THEN 'COMPLETED' ELSE 'FAILED' END,
            (random() * 5000)::bigint,
            CASE
                WHEN random() < 0.9  -- 90% 有 clock_times
                THEN '["2025-01-01T00:00:00Z","2025-01-01T00:00:01Z"]'
                ELSE NULL
            END
        FROM generate_series(1, 1000);
    END LOOP;
END $$;

-- 验证数据创建成功
SELECT
    COUNT(*) as total_workflows,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_count,
    COUNT(*) FILTER (WHERE clock_times IS NOT NULL) as with_clock_times
FROM workflow_state
WHERE policy_version_id IN (
    SELECT id FROM policy_versions WHERE policy_id LIKE 'perf-test-%'
);
-- 预期: total_workflows = 100,000, failed_count ≈ 30,000, with_clock_times ≈ 90,000
```

### 2. 性能测试配置

```properties
# application-performance-test.properties

# 增加数据库连接池大小
quarkus.datasource.jdbc.max-size=50
quarkus.datasource.jdbc.min-size=10

# 增加查询超时
quarkus.datasource.jdbc.acquisition-timeout=30

# 启用性能监控
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.bind-parameters=true
```

---

## 性能测试用例

### 测试 1: JOIN LATERAL 查询性能

**目标**: 验证异常检测查询在大量 workflow 场景下的性能

**测试脚本**:
```java
@QuarkusTest
@QuarkusTestProfile(PerformanceTestProfile.class)
class Phase38PerformanceTest {

    @Inject
    PolicyAnalyticsService analyticsService;

    /**
     * 测试1: JOIN LATERAL 查询性能
     *
     * 场景: 100 个策略，每个策略 1000 个 workflow
     * 预期: 检测时间 < 1000ms
     */
    @Test
    void testDetectAnomalies_LargeDataset_Performance() {
        // 预热（JIT 编译优化）
        for (int i = 0; i < 3; i++) {
            analyticsService.detectAnomalies(0.3, 7);
        }

        // 性能测试（执行 10 次取平均）
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            List<AnomalyReportDTO> anomalies = analyticsService.detectAnomalies(0.3, 7);
            long duration = (System.nanoTime() - start) / 1_000_000;  // ms

            durations.add(duration);
            System.out.printf("Run %d: %d ms, found %d anomalies%n",
                i + 1, duration, anomalies.size());
        }

        // 统计结果
        long avgDuration = durations.stream().mapToLong(Long::longValue).sum() / durations.size();
        long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
        long minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.printf("Average: %d ms, Min: %d ms, Max: %d ms%n",
            avgDuration, minDuration, maxDuration);

        // 断言性能要求
        assertTrue(avgDuration < 1000, "Average detection time should be < 1000ms");
        assertTrue(maxDuration < 1500, "Max detection time should be < 1500ms");
    }
}
```

**预期结果**:
- 平均时间: < 500ms
- 最大时间: < 1000ms
- sampleWorkflowId 捕获率: > 95%

**如果失败**:
1. 检查索引是否存在：`workflow_state.policy_version_id`, `workflow_state.started_at`
2. 更新统计信息：`ANALYZE workflow_state;`
3. 检查 PostgreSQL 配置：`shared_buffers`, `work_mem`

---

### 测试 2: Replay 验证吞吐量

**目标**: 验证 Replay 验证在并发场景下的吞吐量

**测试脚本**:
```java
/**
 * 测试2: Replay 验证并发性能
 *
 * 场景: 同时提交 50 个 replay 验证
 * 预期: 全部在 10 秒内完成
 */
@Test
void testReplayVerification_Concurrent_Throughput() throws Exception {
    // 创建 50 个异常报告（带 sampleWorkflowId）
    List<Long> anomalyIds = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
        AnomalyReportEntity anomaly = createTestAnomaly();
        anomaly.persist();
        anomalyIds.add(anomaly.id);
    }

    // 并发提交验证
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(anomalyIds.size());
    List<Long> durations = Collections.synchronizedList(new ArrayList<>());

    long globalStart = System.currentTimeMillis();

    for (Long anomalyId : anomalyIds) {
        executor.submit(() -> {
            try {
                long start = System.currentTimeMillis();

                // 提交验证动作
                workflowService.submitVerificationAction(anomalyId)
                    .await().indefinitely();

                long duration = System.currentTimeMillis() - start;
                durations.add(duration);
            } finally {
                latch.countDown();
            }
        });
    }

    // 等待全部完成
    boolean completed = latch.await(30, TimeUnit.SECONDS);
    long totalDuration = System.currentTimeMillis() - globalStart;

    executor.shutdown();

    // 统计结果
    long avgDuration = durations.stream().mapToLong(Long::longValue).sum() / durations.size();
    long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);

    System.out.printf("Total time: %d ms, Average per verification: %d ms, Max: %d ms%n",
        totalDuration, avgDuration, maxDuration);

    // 断言性能要求
    assertTrue(completed, "All verifications should complete within 30 seconds");
    assertTrue(totalDuration < 10000, "Total time should be < 10 seconds");
    assertTrue(avgDuration < 500, "Average verification time should be < 500ms");
}

private AnomalyReportEntity createTestAnomaly() {
    // 创建带 sampleWorkflowId 的测试异常
    WorkflowStateEntity workflow = new WorkflowStateEntity();
    workflow.workflowId = UUID.randomUUID();
    workflow.policyVersionId = getTestVersionId();
    workflow.status = "FAILED";
    workflow.startedAt = Instant.now();
    workflow.durationMs = 1000L;
    workflow.clockTimes = "[\"2025-01-01T00:00:00Z\"]";
    workflow.persist();

    AnomalyReportEntity anomaly = new AnomalyReportEntity();
    anomaly.anomalyType = "HIGH_FAILURE_RATE";
    anomaly.versionId = getTestVersionId();
    anomaly.policyId = "perf-test-policy";
    anomaly.severity = "CRITICAL";
    anomaly.status = "PENDING";
    anomaly.sampleWorkflowId = workflow.workflowId;
    anomaly.metricValue = 0.5;
    anomaly.threshold = 0.3;
    anomaly.description = "Test";
    anomaly.recommendation = "Test";
    anomaly.detectedAt = Instant.now();

    return anomaly;
}
```

**预期结果**:
- 总时间: < 10 秒（并发执行）
- 单个验证平均时间: < 500ms
- 全部验证成功提交

**如果失败**:
1. 检查数据库连接池大小（建议 >= 20）
2. 检查数据库 CPU 使用率
3. 考虑增加 Quarkus worker 线程数

---

### 测试 3: 内存消耗测试

**目标**: 验证大量异常检测和验证不会导致内存泄漏

**测试脚本**:
```java
/**
 * 测试3: 内存消耗测试
 *
 * 场景: 连续执行 100 次异常检测
 * 预期: 内存增长 < 100MB
 */
@Test
void testMemoryConsumption_RepeatedDetection() {
    // 获取初始内存使用
    System.gc();
    long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    // 连续执行 100 次异常检测
    for (int i = 0; i < 100; i++) {
        List<AnomalyReportDTO> anomalies = analyticsService.detectAnomalies(0.3, 7);

        if (i % 10 == 0) {
            System.gc();
            long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryGrowth = (currentMemory - initialMemory) / (1024 * 1024);  // MB
            System.out.printf("Iteration %d: Memory growth = %d MB%n", i, memoryGrowth);
        }
    }

    // 最终内存检查
    System.gc();
    long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long totalGrowth = (finalMemory - initialMemory) / (1024 * 1024);  // MB

    System.out.printf("Total memory growth: %d MB%n", totalGrowth);

    // 断言内存增长在合理范围内
    assertTrue(totalGrowth < 100, "Memory growth should be < 100MB");
}
```

**预期结果**:
- 内存增长: < 50MB
- 无内存泄漏迹象

**如果失败**:
1. 检查是否有未关闭的数据库连接
2. 检查 DTO 对象是否被正确回收
3. 使用 JProfiler/VisualVM 分析内存堆

---

## 数据库性能优化建议

### PostgreSQL 配置优化

```sql
-- 1. 确保索引存在
CREATE INDEX IF NOT EXISTS idx_workflow_state_policy_version_started
ON workflow_state(policy_version_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_workflow_state_status_started
ON workflow_state(status, started_at DESC)
WHERE status = 'FAILED';

-- 2. 更新表统计信息
ANALYZE workflow_state;
ANALYZE anomaly_reports;

-- 3. 检查查询计划
EXPLAIN ANALYZE
SELECT ...  -- 复制 detectAnomalies() 的 SQL 查询
```

### 查询性能分析

```sql
-- 使用 pg_stat_statements 分析慢查询
SELECT
    substring(query from 1 for 100) as query_snippet,
    calls,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
WHERE query LIKE '%JOIN LATERAL%sample_workflow%'
ORDER BY mean_exec_time DESC
LIMIT 5;
```

---

## 性能基准报告模板

### 报告格式

```markdown
# Phase 3.8 性能测试报告

**测试日期**: YYYY-MM-DD
**测试环境**: Staging / Production-like
**数据规模**: 100 策略 × 1000 workflows = 100,000 workflows

## 测试结果

### 1. JOIN LATERAL 查询性能

| 指标 | 结果 | 目标 | 状态 |
|------|------|------|------|
| 平均时间 | XXX ms | < 500ms | ✅/❌ |
| 最大时间 | XXX ms | < 1000ms | ✅/❌ |
| 最小时间 | XXX ms | - | - |
| sampleWorkflowId 捕获率 | XX% | > 95% | ✅/❌ |

### 2. Replay 验证吞吐量

| 指标 | 结果 | 目标 | 状态 |
|------|------|------|------|
| 50 个并发总时间 | XXX ms | < 10s | ✅/❌ |
| 单个平均时间 | XXX ms | < 500ms | ✅/❌ |
| 成功率 | XX% | 100% | ✅/❌ |

### 3. 内存消耗

| 指标 | 结果 | 目标 | 状态 |
|------|------|------|------|
| 100 次检测后内存增长 | XX MB | < 100MB | ✅/❌ |
| 是否有内存泄漏 | 是/否 | 否 | ✅/❌ |

## 优化建议

1. [如有性能问题，列出优化建议]
2. ...

## 结论

Phase 3.8 性能表现: ✅ 满足要求 / ⚠️ 需要优化 / ❌ 不满足要求
```

---

## 持续监控建议

1. **在生产环境启用 `pg_stat_statements`**
   ```sql
   CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
   ```

2. **定期（每周）运行性能测试**
   - 监控查询时间趋势
   - 及时发现性能退化

3. **设置性能告警**
   - 异常检测时间 > 1000ms
   - Replay 超时率 > 10%

---

## 附录: 性能测试工具

### JMeter 测试脚本

```xml
<!-- JMeter HTTP 请求示例 -->
<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy">
  <elementProp name="HTTPsampler.Arguments">
    <collectionProp name="Arguments.arguments"/>
  </elementProp>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/analytics/anomalies?threshold=0.3&amp;lookbackDays=7</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
</HTTPSamplerProxy>
```

### k6 负载测试脚本

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 10 },  // 2 分钟内增加到 10 VU
    { duration: '5m', target: 10 },  // 保持 10 VU 持续 5 分钟
    { duration: '2m', target: 0 },   // 2 分钟内降到 0
  ],
};

export default function () {
  let response = http.get('http://localhost:8080/api/analytics/anomalies?threshold=0.3&lookbackDays=7');

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(1);
}
```

---

**性能测试完成后，请更新此文档并归档测试结果。**
