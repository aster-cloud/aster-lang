# Phase 3.8 API 变更文档

## 概述

Phase 3.8 为异常响应自动化增加了代表性样本支持，在异常报告中添加了 `sampleWorkflowId` 字段，用于存储可用于重放验证的失败 workflow 实例。

## API 变更

### 1. AnomalyReportDTO 新增字段

**影响范围**: 所有返回异常报告的 API 端点

#### 新增字段

```java
public class AnomalyReportDTO {
    // ... 现有字段 ...

    /**
     * 代表性失败 workflow 实例 ID（Phase 3.8 新增）
     *
     * 用途：
     * - 提供可用于 replay 验证的具体 workflow 实例
     * - 从检测到的失败 workflow 中选择最近的一个
     * - 用于构建验证动作的 payload
     *
     * 可能为 null 的情况：
     * - ZOMBIE_VERSION 类型异常（无关联 workflow）
     * - 检测时所有 workflow 都已被清理
     * - 老版本检测到的异常（升级前）
     */
    public UUID sampleWorkflowId;
}
```

#### JSON 示例

**有 sampleWorkflowId 的异常报告**:
```json
{
  "anomalyType": "HIGH_FAILURE_RATE",
  "versionId": 12345,
  "policyId": "aster.finance.loan.evaluateLoanEligibility",
  "severity": "CRITICAL",
  "status": "PENDING",
  "description": "策略失败率 50.0% 超过阈值 30.0%",
  "recommendation": "检查策略逻辑或输入数据",
  "metricValue": 0.5,
  "threshold": 0.3,
  "detectedAt": "2025-11-11T06:00:00Z",
  "sampleWorkflowId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**无 sampleWorkflowId 的异常报告**（降级场景）:
```json
{
  "anomalyType": "ZOMBIE_VERSION",
  "versionId": 12345,
  "policyId": "aster.finance.loan.oldPolicy",
  "severity": "WARNING",
  "status": "PENDING",
  "description": "策略版本已停用但仍有活动 workflow",
  "recommendation": "清理活动 workflow 或重新启用策略",
  "detectedAt": "2025-11-11T06:00:00Z",
  "sampleWorkflowId": null
}
```

### 2. 受影响的 API 端点

#### GET /api/analytics/anomalies

**变更**: 响应中每个异常报告现在包含 `sampleWorkflowId` 字段

**请求参数**（无变更）:
```
GET /api/analytics/anomalies?threshold=0.3&lookbackDays=7
```

**响应示例**:
```json
[
  {
    "anomalyType": "HIGH_FAILURE_RATE",
    "policyId": "aster.finance.loan.evaluateLoanEligibility",
    "sampleWorkflowId": "550e8400-e29b-41d4-a716-446655440000",
    "severity": "CRITICAL",
    ...
  },
  {
    "anomalyType": "ZOMBIE_VERSION",
    "policyId": "aster.finance.loan.oldPolicy",
    "sampleWorkflowId": null,
    ...
  }
]
```

#### POST /api/anomalies/{anomalyId}/verify

**变更**: 内部会使用 `sampleWorkflowId` 构建验证 payload（对外部 API 调用者透明）

**行为变化**:
- **Phase 3.7**: 验证动作的 payload 为空，需要手动指定 workflowId
- **Phase 3.8**: 如果异常报告包含 sampleWorkflowId，payload 会自动构建为 `{"workflowId":"<uuid>"}`

**降级处理**: 如果 sampleWorkflowId 为 null，payload 仍为 null，但验证动作仍会创建

## 向后兼容性

### ✅ 完全向后兼容

- **新增字段**: `sampleWorkflowId` 为可选字段，老客户端可以忽略
- **API 调用方式**: 无需修改现有 API 调用代码
- **数据库**: 新列为 nullable，不影响老数据

### 客户端升级建议

#### 建议升级的客户端

如果你的应用：
- 需要自动触发 replay 验证
- 需要展示具体的失败实例
- 需要追踪异常来源

**升级步骤**:
1. 更新 DTO 定义，添加 `sampleWorkflowId` 字段
2. 在 UI 中展示 sampleWorkflowId（可选）
3. 利用 sampleWorkflowId 自动触发验证（可选）

#### 无需升级的客户端

如果你的应用：
- 仅展示异常统计
- 不涉及 replay 验证
- 不需要追溯具体实例

**行为**: 继续使用现有代码，新字段会被忽略

## 使用示例

### 示例 1: 获取异常并检查 sampleWorkflowId

```typescript
// TypeScript 客户端示例
interface AnomalyReport {
  anomalyType: string;
  policyId: string;
  severity: string;
  sampleWorkflowId?: string;  // Phase 3.8 新增
  // ... 其他字段
}

async function fetchAnomaliesWithSample() {
  const response = await fetch('/api/analytics/anomalies?threshold=0.3');
  const anomalies: AnomalyReport[] = await response.json();

  anomalies.forEach(anomaly => {
    if (anomaly.sampleWorkflowId) {
      console.log(`异常 ${anomaly.policyId} 有代表性样本: ${anomaly.sampleWorkflowId}`);
      // 可以用于自动触发 replay 验证
    } else {
      console.log(`异常 ${anomaly.policyId} 无代表性样本（降级场景）`);
    }
  });
}
```

### 示例 2: 自动触发验证（利用 sampleWorkflowId）

```java
// Java 客户端示例
public void autoVerifyCriticalAnomalies() {
    List<AnomalyReportDTO> anomalies = analyticsClient.getAnomalies(0.3, 7);

    anomalies.stream()
        .filter(a -> "CRITICAL".equals(a.severity))
        .filter(a -> a.sampleWorkflowId != null)  // Phase 3.8: 有样本才自动验证
        .forEach(anomaly -> {
            // 提交验证动作（Phase 3.8 会自动使用 sampleWorkflowId）
            anomalyClient.submitVerification(anomaly.id);
            log.info("已自动提交验证: anomaly={}, sample={}",
                     anomaly.id, anomaly.sampleWorkflowId);
        });
}
```

### 示例 3: UI 展示增强

```jsx
// React 组件示例
function AnomalyCard({ anomaly }) {
  return (
    <div className="anomaly-card">
      <h3>{anomaly.policyId}</h3>
      <p>严重程度: {anomaly.severity}</p>
      <p>类型: {anomaly.anomalyType}</p>

      {/* Phase 3.8: 展示代表性样本 */}
      {anomaly.sampleWorkflowId && (
        <div className="sample-workflow">
          <label>代表性失败实例:</label>
          <code>{anomaly.sampleWorkflowId}</code>
          <button onClick={() => navigateToWorkflow(anomaly.sampleWorkflowId)}>
            查看详情
          </button>
        </div>
      )}

      <button onClick={() => verifyAnomaly(anomaly.id)}>
        提交验证
      </button>
    </div>
  );
}
```

## 数据库变更

### anomaly_reports 表新增列

```sql
-- 新增列
ALTER TABLE anomaly_reports
ADD COLUMN sample_workflow_id UUID;

-- 新增索引（稀疏索引，仅索引非 NULL 值）
CREATE INDEX idx_anomaly_reports_sample_workflow
ON anomaly_reports(sample_workflow_id)
WHERE sample_workflow_id IS NOT NULL;
```

### 查询示例

**查询有代表性样本的异常**:
```sql
SELECT id, policy_id, severity, sample_workflow_id
FROM anomaly_reports
WHERE sample_workflow_id IS NOT NULL
AND status = 'PENDING'
ORDER BY detected_at DESC;
```

**统计 sampleWorkflowId 捕获率**:
```sql
SELECT
  COUNT(*) as total_anomalies,
  COUNT(sample_workflow_id) as with_sample,
  ROUND(COUNT(sample_workflow_id)::numeric / COUNT(*) * 100, 2) as capture_rate_percent
FROM anomaly_reports
WHERE detected_at > NOW() - INTERVAL '7 days';
```

## 迁移指南

### 对于已有数据

- **Phase 3.8 升级前的异常**: `sampleWorkflowId` 为 NULL
- **Phase 3.8 升级后的异常**: 会自动捕获 `sampleWorkflowId`（如果有失败 workflow）

### 性能影响

- **检测性能**: 使用 JOIN LATERAL 优化，单次检测增加约 50-100ms
- **存储开销**: 每个异常增加 16 字节（UUID 大小）
- **索引开销**: 稀疏索引仅索引非 NULL 值，空间开销极小

## 常见问题 (FAQ)

### Q1: 为什么有些异常的 sampleWorkflowId 为 null？

**A**: 以下情况 sampleWorkflowId 会为 null：
1. ZOMBIE_VERSION 类型异常（无失败 workflow）
2. 检测时所有相关 workflow 已被清理
3. Phase 3.8 升级前创建的异常报告

### Q2: sampleWorkflowId 是如何选择的？

**A**: 从该策略版本的失败 workflow 中选择 **最近的一个**（按 `started_at DESC` 排序）

### Q3: 如果 sampleWorkflowId 为 null，还能触发验证吗？

**A**: 可以提交验证动作，但 payload 会为 null，需要手动提供 workflowId

### Q4: 老客户端需要升级吗？

**A**: 不需要，新字段完全向后兼容。但升级可以利用新功能（自动验证、实例追踪）

### Q5: 性能监控指标是什么？

**A**: 建议监控：
- sampleWorkflowId 捕获率（应 > 80%）
- 异常检测查询时间（应 < 500ms）
- Replay 验证成功率（应 > 70%）

## 变更历史

- **Phase 3.8 (2025-11-11)**: 新增 `sampleWorkflowId` 字段，支持代表性样本
- **Phase 3.7 (之前)**: 基础异常检测功能

## 联系支持

如有问题，请联系：
- 技术支持: [support@example.com]
- 文档反馈: [docs@example.com]
