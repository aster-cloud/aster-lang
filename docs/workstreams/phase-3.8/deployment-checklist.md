# Phase 3.8 部署检查清单

## 📋 部署前验证

### 数据库迁移验证

- [ ] **在 Staging 环境验证迁移脚本**
  ```bash
  # 1. 备份当前数据库
  pg_dump -h <host> -U <user> -d <database> > backup_before_v3.8.0.sql

  # 2. 检查 Flyway 迁移状态
  ./gradlew :quarkus-policy-api:flywayInfo

  # 3. 执行迁移
  ./gradlew :quarkus-policy-api:flywayMigrate

  # 4. 验证新列存在
  psql -h <host> -U <user> -d <database> -c "\d anomaly_reports"
  ```

- [ ] **验证索引创建成功**
  ```sql
  SELECT indexname, indexdef
  FROM pg_indexes
  WHERE tablename = 'anomaly_reports'
  AND indexname = 'idx_anomaly_reports_sample_workflow';
  ```

- [ ] **验证列约束**
  ```sql
  SELECT column_name, data_type, is_nullable
  FROM information_schema.columns
  WHERE table_name = 'anomaly_reports'
  AND column_name = 'sample_workflow_id';
  -- 预期: column_name = sample_workflow_id, data_type = uuid, is_nullable = YES
  ```

### 应用启动验证

- [ ] **启动应用无错误**
  ```bash
  ./gradlew :quarkus-policy-api:quarkusDev
  # 检查日志中没有 Flyway 错误
  ```

- [ ] **健康检查通过**
  ```bash
  curl http://localhost:8080/q/health/ready
  # 预期: {"status":"UP",...}
  ```

### 功能验证

- [ ] **异常检测捕获 sampleWorkflowId**
  - 创建高失败率场景（手动或通过测试）
  - 触发异常检测
  - 验证 `anomaly_reports.sample_workflow_id` 不为 NULL

- [ ] **Payload 构建正确**
  - 提交验证动作
  - 检查 `anomaly_actions.payload` 包含 `workflowId`

- [ ] **Replay 验证执行成功**
  - 执行一个完整的 replay 验证
  - 确认 `verification_result` 正确写入

## 🔄 回滚方案

### 如果需要回滚到 Phase 3.7

1. **停止应用**
   ```bash
   # 在生产环境停止服务
   ```

2. **执行回滚脚本**
   ```bash
   psql -h <host> -U <user> -d <database> -f src/main/resources/db/migration/ROLLBACK_V3.8.0.sql
   ```

3. **恢复旧版本应用**
   ```bash
   # 部署 Phase 3.7 版本
   ```

4. **验证回滚成功**
   ```sql
   SELECT column_name FROM information_schema.columns
   WHERE table_name = 'anomaly_reports' AND column_name = 'sample_workflow_id';
   -- 应该返回 0 行
   ```

### 数据恢复

如果需要恢复数据：
```bash
# 从备份恢复
pg_restore -h <host> -U <user> -d <database> backup_before_v3.8.0.sql
```

## 📊 监控指标

### 部署后监控（前 48 小时）

- [ ] **sampleWorkflowId 捕获率**
  ```sql
  SELECT
    COUNT(*) as total_anomalies,
    COUNT(sample_workflow_id) as with_sample,
    ROUND(COUNT(sample_workflow_id)::numeric / NULLIF(COUNT(*), 0) * 100, 2) as capture_rate_percent
  FROM anomaly_reports
  WHERE detected_at > NOW() - INTERVAL '24 hours';
  -- 预期: capture_rate_percent > 80%
  ```

- [ ] **Replay 验证成功率**
  ```sql
  SELECT
    COUNT(*) as total_verifications,
    SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'true' THEN 1 ELSE 0 END) as successful_replays,
    ROUND(SUM(CASE WHEN verification_result::jsonb->>'replaySucceeded' = 'true' THEN 1 ELSE 0 END)::numeric / NULLIF(COUNT(*), 0) * 100, 2) as success_rate_percent
  FROM anomaly_reports
  WHERE status = 'VERIFIED'
  AND verified_at > NOW() - INTERVAL '24 hours';
  -- 预期: success_rate_percent > 70%
  ```

- [ ] **Replay 超时频率**
  ```sql
  SELECT
    COUNT(*) as total_actions,
    SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout_count,
    ROUND(SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END)::numeric / NULLIF(COUNT(*), 0) * 100, 2) as timeout_rate_percent
  FROM anomaly_actions
  WHERE action_type = 'VERIFY_REPLAY'
  AND created_at > NOW() - INTERVAL '24 hours';
  -- 预期: timeout_rate_percent < 5%
  ```

- [ ] **异常检测性能**
  ```sql
  -- 监控 detectAnomalies() 查询性能
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  WHERE query LIKE '%JOIN LATERAL%sample_workflow%'
  ORDER BY mean_exec_time DESC;
  -- 预期: mean_exec_time < 500ms
  ```

## 📝 部署记录

- **部署时间**: _______________
- **部署人员**: _______________
- **Staging 验证时间**: _______________
- **生产部署时间**: _______________
- **回滚决策点**: 部署后 24 小时内，如果 sampleWorkflowId 捕获率 < 50% 或 Replay 超时率 > 10%

## ✅ 签收确认

- [ ] DBA 已审核迁移脚本
- [ ] 运维已准备回滚方案
- [ ] 监控已配置告警
- [ ] 团队已知晓新功能和监控指标
