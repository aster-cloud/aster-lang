# Phase 3.8 监控运维手册

## 监控概述

Phase 3.8 引入了代表性样本和 Replay 验证自动化，需要监控以下关键指标以确保功能正常运行。

## 核心监控指标

### 1. sampleWorkflowId 捕获率

**定义**: 检测到的异常中成功捕获 sampleWorkflowId 的比例

**重要性**: 🔴 关键指标
- 捕获率低说明数据链路断裂
- 影响自动验证功能

**监控查询**:
```sql
SELECT
    COUNT(*) as total_anomalies,
    COUNT(sample_workflow_id) as with_sample,
    COUNT(*) - COUNT(sample_workflow_id) as without_sample,
    ROUND(COUNT(sample_workflow_id)::numeric / NULLIF(COUNT(*), 0) * 100, 2) as capture_rate_percent
FROM anomaly_reports
WHERE detected_at > NOW() - INTERVAL '24 hours'
AND anomaly_type = 'HIGH_FAILURE_RATE';  -- 仅统计应该有样本的类型
```

**告警阈值**:
- ⚠️ Warning: capture_rate < 80%
- 🚨 Critical: capture_rate < 50%

**可能原因**:
- workflow 数据被提前清理
- 检测查询性能问题导致超时
- JOIN LATERAL 子查询失败

**修复建议**:
1. 检查 workflow 数据保留策略
2. 优化检测查询性能
3. 查看应用日志中的 SQL 错误

---

### 2. Replay 验证成功率

**定义**: 提交的 replay 验证中成功执行的比例

**重要性**: 🔴 关键指标
- 成功率低说明 replay 机制不稳定
- 影响异常确认的可靠性

**监控查询**:
```sql
SELECT
    COUNT(*) as total_verifications,
    SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'true' THEN 1 ELSE 0 END) as successful_replays,
    SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'false' THEN 1 ELSE 0 END) as failed_replays,
    SUM(CASE WHEN verification_result IS NULL THEN 1 ELSE 0 END) as pending,
    ROUND(
        SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'true' THEN 1 ELSE 0 END)::numeric
        / NULLIF(COUNT(*) - SUM(CASE WHEN verification_result IS NULL THEN 1 ELSE 0 END), 0) * 100,
        2
    ) as success_rate_percent
FROM anomaly_reports
WHERE status IN ('VERIFIED', 'VERIFYING')
AND verified_at > NOW() - INTERVAL '24 hours';
```

**告警阈值**:
- ⚠️ Warning: success_rate < 70%
- 🚨 Critical: success_rate < 50%

**可能原因**:
- clock_times 数据缺失或损坏
- Policy 编译失败
- 超时配置不合理

**修复建议**:
1. 检查 clock_times 数据质量
2. 增加超时时间（当前 5 分钟）
3. 检查 policy 编译日志

---

### 3. Replay 超时频率

**定义**: 因超时而失败的 replay 验证比例

**重要性**: 🟡 重要指标
- 超时频率高影响用户体验
- 可能需要优化性能或调整配置

**监控查询**:
```sql
SELECT
    COUNT(*) as total_actions,
    SUM(CASE
        WHEN verification_result::jsonb->>'replaySucceeded' = 'false'
        AND verification_result::jsonb->>'errorMessage' LIKE '%timeout%'
        THEN 1 ELSE 0
    END) as timeout_count,
    ROUND(
        SUM(CASE
            WHEN verification_result::jsonb->>'replaySucceeded' = 'false'
            AND verification_result::jsonb->>'errorMessage' LIKE '%timeout%'
            THEN 1 ELSE 0
        END)::numeric / NULLIF(COUNT(*), 0) * 100,
        2
    ) as timeout_rate_percent
FROM anomaly_reports
WHERE status = 'VERIFIED'
AND verified_at > NOW() - INTERVAL '24 hours';
```

**告警阈值**:
- ⚠️ Warning: timeout_rate > 10%
- 🚨 Critical: timeout_rate > 20%

**可能原因**:
- Policy 执行复杂度高
- 数据库查询慢
- 系统负载过高

**修复建议**:
1. 分析超时的 policy 特征
2. 考虑增加超时配置（修改 `Duration.ofMinutes(5)`）
3. 优化 policy 执行性能

---

### 4. 异常检测性能

**定义**: detectAnomalies() 查询的平均执行时间

**重要性**: 🟡 重要指标
- 性能下降影响检测频率
- JOIN LATERAL 可能成为瓶颈

**监控查询** (需要启用 `pg_stat_statements`):
```sql
SELECT
    substring(query from 1 for 100) as query_snippet,
    calls,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time
FROM pg_stat_statements
WHERE query LIKE '%JOIN LATERAL%sample_workflow%'
OR query LIKE '%detectAnomalies%'
ORDER BY mean_exec_time DESC
LIMIT 5;
```

**告警阈值**:
- ⚠️ Warning: mean_exec_time > 500ms
- 🚨 Critical: mean_exec_time > 1000ms

**可能原因**:
- workflow_state 表数据量过大
- 索引缺失或失效
- 统计信息过时

**修复建议**:
1. 清理历史 workflow 数据
2. 重建索引：`REINDEX TABLE workflow_state;`
3. 更新统计信息：`ANALYZE workflow_state;`

---

### 5. Payload 构建成功率

**定义**: 提交验证动作时成功构建 payload 的比例

**重要性**: 🟢 辅助指标
- 辅助诊断数据链路问题

**监控查询**:
```sql
SELECT
    COUNT(*) as total_actions,
    COUNT(payload) as with_payload,
    COUNT(*) - COUNT(payload) as without_payload,
    ROUND(COUNT(payload)::numeric / NULLIF(COUNT(*), 0) * 100, 2) as payload_rate_percent
FROM anomaly_actions
WHERE action_type = 'VERIFY_REPLAY'
AND created_at > NOW() - INTERVAL '24 hours';
```

**告警阈值**:
- ⚠️ Warning: payload_rate < 80%

**可能原因**:
- sampleWorkflowId 捕获失败
- 数据传递链路断裂

**修复建议**:
1. 检查 sampleWorkflowId 捕获率
2. 验证异常报告持久化逻辑

---

## 监控仪表板配置

### Grafana 配置示例

**Panel 1: sampleWorkflowId 捕获率**
```json
{
  "title": "Phase 3.8 - Sample Workflow 捕获率",
  "targets": [{
    "format": "time_series",
    "rawSql": "SELECT\n  $__timeGroup(detected_at, '1h') as time,\n  ROUND(COUNT(sample_workflow_id)::numeric / COUNT(*) * 100, 2) as capture_rate\nFROM anomaly_reports\nWHERE $__timeFilter(detected_at) AND anomaly_type = 'HIGH_FAILURE_RATE'\nGROUP BY 1\nORDER BY 1"
  }],
  "alert": {
    "conditions": [{
      "evaluator": {
        "params": [80],
        "type": "lt"
      }
    }]
  }
}
```

**Panel 2: Replay 验证成功率**
```json
{
  "title": "Phase 3.8 - Replay 验证成功率",
  "targets": [{
    "format": "time_series",
    "rawSql": "SELECT\n  $__timeGroup(verified_at, '1h') as time,\n  ROUND(\n    SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'true' THEN 1 ELSE 0 END)::numeric\n    / NULLIF(COUNT(*), 0) * 100,\n    2\n  ) as success_rate\nFROM anomaly_reports\nWHERE $__timeFilter(verified_at) AND status = 'VERIFIED'\nGROUP BY 1\nORDER BY 1"
  }],
  "alert": {
    "conditions": [{
      "evaluator": {
        "params": [70],
        "type": "lt"
      }
    }]
  }
}
```

---

## 日志监控

### 关键日志关键词

**检测成功日志**:
```
INFO  [io.aster.audit.service.PolicyAnalyticsService] Detected 5 anomalies, 4 with sampleWorkflowId
```

**Payload 构建日志**:
```
INFO  [io.aster.audit.service.AnomalyWorkflowService] Built payload for anomaly 123, workflowId: 550e8400-e29b-41d4-a716-446655440000
```

**Replay 执行日志**:
```
INFO  [io.aster.workflow.WorkflowSchedulerService] Starting replay for workflow 550e8400-e29b-41d4-a716-446655440000
INFO  [io.aster.workflow.WorkflowSchedulerService] Replay completed in 1234ms, status: COMPLETED
```

**错误日志**:
```
ERROR [io.aster.audit.service.PolicyAnalyticsService] Failed to detect anomalies: SQL timeout
ERROR [io.aster.workflow.WorkflowSchedulerService] Workflow 550e8400-... missing clock_times, cannot replay
ERROR [io.aster.audit.service.AnomalyActionExecutor] Replay verification failed: timeout after 5 minutes
```

### 日志监控规则 (ELK/Splunk)

**捕获失败告警**:
```
source="/var/log/quarkus-policy-api.log"
| search "Detected" AND "anomalies" AND "with sampleWorkflowId"
| rex field=_raw "Detected (?<total>\d+) anomalies, (?<with_sample>\d+) with sampleWorkflowId"
| eval capture_rate = (with_sample / total) * 100
| where capture_rate < 80
| alert
```

**Replay 超时告警**:
```
source="/var/log/quarkus-policy-api.log"
| search "Replay verification failed" AND "timeout"
| stats count by _time span=1h
| where count > 10
| alert
```

---

## 运维手册

### 每日检查清单

- [ ] 查看 sampleWorkflowId 捕获率（应 > 80%）
- [ ] 查看 Replay 验证成功率（应 > 70%）
- [ ] 检查是否有超时告警
- [ ] 检查异常检测性能（应 < 500ms）

### 每周检查清单

- [ ] 分析 Replay 失败原因分布
- [ ] 检查 workflow 数据保留策略
- [ ] 审查告警规则有效性
- [ ] 统计 Phase 3.8 功能使用情况

### 故障排查手册

#### 问题 1: sampleWorkflowId 捕获率突然下降

**诊断步骤**:
1. 检查 workflow_state 表是否有数据
   ```sql
   SELECT COUNT(*) FROM workflow_state WHERE started_at > NOW() - INTERVAL '1 day';
   ```
2. 检查异常检测查询是否报错（查看应用日志）
3. 检查数据库连接池是否耗尽

**修复步骤**:
1. 如果 workflow 数据为空，检查数据采集流程
2. 如果查询超时，临时增加查询超时配置
3. 重启应用（如果连接池耗尽）

#### 问题 2: Replay 验证全部失败

**诊断步骤**:
1. 检查 clock_times 是否存在
   ```sql
   SELECT COUNT(*) FROM workflow_state WHERE clock_times IS NOT NULL AND started_at > NOW() - INTERVAL '1 day';
   ```
2. 检查 WorkflowSchedulerService 日志
3. 检查 policy 编译是否正常

**修复步骤**:
1. 如果 clock_times 缺失，检查 workflow 运行时配置
2. 如果编译失败，检查 policy 代码和依赖
3. 临时禁用自动验证，改为手动验证

#### 问题 3: 异常检测性能下降

**诊断步骤**:
1. 查看 `pg_stat_statements` 慢查询
2. 检查 workflow_state 表大小
   ```sql
   SELECT pg_size_pretty(pg_total_relation_size('workflow_state'));
   ```
3. 检查索引是否存在
   ```sql
   SELECT indexname FROM pg_indexes WHERE tablename = 'workflow_state';
   ```

**修复步骤**:
1. 清理历史数据（保留最近 30 天）
2. 重建索引：`REINDEX TABLE workflow_state;`
3. 更新统计信息：`ANALYZE workflow_state;`

---

## 性能基准

### 正常情况下的性能指标

| 指标 | 目标值 | 可接受范围 | 告警阈值 |
|------|--------|------------|----------|
| sampleWorkflowId 捕获率 | > 95% | 80-95% | < 80% |
| Replay 验证成功率 | > 85% | 70-85% | < 70% |
| Replay 超时率 | < 5% | 5-10% | > 10% |
| 异常检测查询时间 | < 300ms | 300-500ms | > 500ms |
| Payload 构建成功率 | > 95% | 80-95% | < 80% |

### 负载测试基准

**测试场景**: 1000 个 workflow，50% 失败率，每 5 分钟检测一次

**预期结果**:
- 检测时间: < 500ms
- sampleWorkflowId 捕获: 100%
- 内存增长: < 50MB
- CPU 使用: < 20%

---

## 容量规划

### 存储增长估算

**每个异常的额外存储**:
- `sample_workflow_id`: 16 bytes (UUID)
- 索引开销: ~24 bytes (稀疏索引)

**月度增长估算**（假设每天 100 个异常）:
```
每天: 100 * 40 bytes = 4 KB
每月: 4 KB * 30 = 120 KB
每年: 120 KB * 12 = 1.44 MB
```

**结论**: 存储开销极小，无需特殊容量规划

### 性能影响

**JOIN LATERAL 查询开销**:
- 平均增加: 50-100ms
- 最坏情况: 200ms（大量 workflow）

**建议**:
- workflow_state 表保留最近 30 天数据
- 定期清理历史数据以保持查询性能

---

## 联系支持

**紧急问题**:
- On-call: [on-call@example.com]
- Slack: #platform-oncall

**非紧急咨询**:
- Email: [support@example.com]
- Jira: [项目看板链接]
