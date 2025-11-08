# Aster Language - Phase 1 用户手册

## 概览

本手册介绍 Aster Language Phase 1 的核心功能，包括策略评估、版本控制、审计日志、实时预览和 CNL 导出导入。

### 架构概述

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Policy Editor  │────▶│ Quarkus-Policy   │────▶│ Aster Runtime   │
│  (Vaadin UI)    │     │  API (REST API)  │     │ (.aster files)  │
└─────────────────┘     └──────────────────┘     └─────────────────┘
        │                       │                         │
        │                       ▼                         │
        │               ┌──────────────┐                  │
        └──────────────▶│  PostgreSQL  │◀─────────────────┘
                        │ (Version &   │
                        │  Audit Log)  │
                        └──────────────┘
```

### 核心模块

1. **quarkus-policy-api** - REST API 服务，提供策略评估、版本控制和审计日志功能
2. **policy-editor** - Web UI，用于可视化编辑和测试策略
3. **aster-finance** - 金融领域策略库（贷款审批、信用卡申请等）
4. **aster-policy-common** - 共用工具库（JSON/CNL 序列化、PII 脱敏）

## 快速开始

### 1. 环境准备

**系统要求：**
- Java 21 或更高版本
- Gradle 9.0
- PostgreSQL 15+ (可选，使用 H2 内存数据库用于开发)
- Docker (可选，用于 PostgreSQL)

**启动 PostgreSQL（可选）：**

```bash
docker run -d \
  --name aster-postgres \
  -e POSTGRES_DB=aster_policy \
  -e POSTGRES_USER=aster \
  -e POSTGRES_PASSWORD=aster123 \
  -p 5432:5432 \
  postgres:15-alpine
```

### 2. 启动 Quarkus Policy API

```bash
cd quarkus-policy-api

# 开发模式（使用 H2 内存数据库，支持热重载）
../../gradlew quarkusDev

# 生产模式（使用 PostgreSQL）
../../gradlew quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

API 将在 `http://localhost:8080` 启动。

### 3. 验证 API 服务

```bash
# 健康检查
curl http://localhost:8080/q/health

# Swagger UI
open http://localhost:8080/q/swagger-ui/

# Prometheus 指标
curl http://localhost:8080/q/metrics
```

### 4. 启动 Policy Editor (可选)

```bash
cd policy-editor
../../gradlew quarkusDev
```

UI 将在 `http://localhost:8081` 启动。

## 端到端示例

### 场景 1：贷款审批策略评估

#### 1.1 查看可用策略

```bash
curl http://localhost:8080/api/policies/validate \
  -H "Content-Type: application/json" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility"
  }'
```

**响应：**
```json
{
  "valid": true,
  "message": null,
  "parameterCount": 2,
  "returnType": "aster.finance.loan.LoanDecision"
}
```

#### 1.2 评估贷款申请（拒绝场景）

```bash
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-a" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility",
    "context": [
      {
        "applicantId": "LOAN-1001",
        "amount": 250000,
        "termMonths": 360,
        "purpose": "购房"
      },
      {
        "age": 35,
        "creditScore": 600,
        "annualIncome": 500000,
        "monthlyDebt": 8000,
        "yearsEmployed": 5
      }
    ]
  }'
```

**响应：**
```json
{
  "result": {
    "approved": false,
    "reason": "Credit below 650",
    "approvedAmount": 0,
    "interestRateBps": 0,
    "termMonths": 0
  },
  "executionTimeMs": 15,
  "error": null
}
```

**解释：**
- 信用分 600 低于最低要求 650，贷款被拒绝
- 执行时间 15ms，性能良好

#### 1.3 评估贷款申请（批准场景）

```bash
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-a" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility",
    "context": [
      {
        "applicantId": "LOAN-1002",
        "amount": 250000,
        "termMonths": 360,
        "purpose": "购房"
      },
      {
        "age": 35,
        "creditScore": 750,
        "annualIncome": 800000,
        "monthlyDebt": 5000,
        "yearsEmployed": 10
      }
    ]
  }'
```

**响应：**
```json
{
  "result": {
    "approved": true,
    "reason": "Approved",
    "approvedAmount": 250000,
    "interestRateBps": 425,
    "termMonths": 360
  },
  "executionTimeMs": 8,
  "error": null
}
```

**解释：**
- 信用分 750 满足要求，贷款批准
- 利率为 4.25% (425 基点)，根据信用分 740-799 区间计算
- 执行时间 8ms，由于缓存命中，性能更好

### 场景 2：批量评估

```bash
curl -X POST http://localhost:8080/api/policies/evaluate/batch \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-a" \
  -d '{
    "requests": [
      {
        "policyModule": "aster.finance.loan",
        "policyFunction": "evaluateLoanEligibility",
        "context": [
          {"applicantId": "LOAN-2001", "amount": 100000, "termMonths": 240, "purpose": "购车"},
          {"age": 28, "creditScore": 680, "annualIncome": 400000, "monthlyDebt": 3000, "yearsEmployed": 3}
        ]
      },
      {
        "policyModule": "aster.finance.loan",
        "policyFunction": "evaluateLoanEligibility",
        "context": [
          {"applicantId": "LOAN-2002", "amount": 50000, "termMonths": 120, "purpose": "装修"},
          {"age": 45, "creditScore": 720, "annualIncome": 600000, "monthlyDebt": 4000, "yearsEmployed": 15}
        ]
      }
    ]
  }'
```

**响应：**
```json
{
  "results": [
    {
      "result": {"approved": true, "reason": "Approved", "approvedAmount": 100000, "interestRateBps": 550, "termMonths": 240},
      "executionTimeMs": 12,
      "error": null
    },
    {
      "result": {"approved": true, "reason": "Approved", "approvedAmount": 50000, "interestRateBps": 550, "termMonths": 120},
      "executionTimeMs": 10,
      "error": null
    }
  ],
  "totalExecutionTimeMs": 25,
  "successCount": 2,
  "failureCount": 0
}
```

### 场景 3：策略版本控制与回滚

#### 3.1 查看策略版本历史

```bash
curl http://localhost:8080/api/policies/aster.finance.loan/versions \
  -H "X-Tenant-Id: bank-a"
```

**响应：**
```json
[
  {
    "version": 1731034800000,
    "active": true,
    "moduleName": "aster.finance.loan",
    "functionName": "evaluateLoanEligibility",
    "createdAt": "2025-11-08T10:00:00Z",
    "createdBy": "admin@bank-a.com",
    "notes": "修复信用分计算逻辑"
  },
  {
    "version": 1731028200000,
    "active": false,
    "moduleName": "aster.finance.loan",
    "functionName": "evaluateLoanEligibility",
    "createdAt": "2025-11-08T08:10:00Z",
    "createdBy": "developer@bank-a.com",
    "notes": "初始部署"
  }
]
```

#### 3.2 回滚到历史版本

```bash
curl -X POST http://localhost:8080/api/policies/aster.finance.loan/rollback \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-a" \
  -H "X-User-Id: admin@bank-a.com" \
  -d '{
    "targetVersion": 1731028200000,
    "reason": "新版本导致误批准，回滚到稳定版本"
  }'
```

**响应：**
```json
{
  "success": true,
  "message": "Policy rolled back successfully to version 1731028200000",
  "version": 1731028200000
}
```

#### 3.3 验证回滚操作的审计日志

```bash
curl http://localhost:8080/api/audit/type/ROLLBACK \
  -H "X-Tenant-Id: bank-a"
```

**响应：**
```json
[
  {
    "id": 42,
    "eventType": "ROLLBACK",
    "timestamp": "2025-11-08T11:30:00Z",
    "tenantId": "bank-a",
    "policyModule": "aster.finance.loan",
    "policyFunction": null,
    "performedBy": "***@***.***",
    "success": true,
    "executionTimeMs": 0,
    "errorMessage": null,
    "metadata": "{\"fromVersion\":1731034800000,\"toVersion\":1731028200000,\"reason\":\"新版本导致误批准，回滚到稳定版本\"}"
  }
]
```

### 场景 4：审计日志合规性查询

#### 4.1 查询所有策略评估日志

```bash
curl http://localhost:8080/api/audit/type/POLICY_EVALUATION \
  -H "X-Tenant-Id: bank-a"
```

#### 4.2 查询特定策略的审计日志

```bash
curl http://localhost:8080/api/audit/policy/aster.finance.loan/evaluateLoanEligibility \
  -H "X-Tenant-Id: bank-a"
```

#### 4.3 验证 PII 脱敏

审计日志中的 PII 数据自动脱敏：

- **Email**: `admin@bank-a.com` → `***@***.***`
- **手机号**: `+86-138-1234-5678` → `***-***-****`
- **信用卡号**: `4532-1234-5678-9012` → 自动检测并脱敏

### 场景 5：多租户隔离

#### 5.1 租户 A 的评估

```bash
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-a" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility",
    "context": [...]
  }'
```

#### 5.2 租户 B 的评估

```bash
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: bank-b" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility",
    "context": [...]
  }'
```

#### 5.3 验证租户隔离

```bash
# 租户 A 只能看到自己的审计日志
curl http://localhost:8080/api/audit \
  -H "X-Tenant-Id: bank-a"

# 租户 B 只能看到自己的审计日志
curl http://localhost:8080/api/audit \
  -H "X-Tenant-Id: bank-b"
```

### 场景 6：Policy Editor 实时预览

#### 6.1 启动 Policy Editor

```bash
cd policy-editor
../../gradlew quarkusDev
```

访问 `http://localhost:8081`

#### 6.2 编辑策略文件

1. 在编辑器中打开 `aster.finance.loan.aster`
2. 启用 "Live Preview" 开关
3. 修改策略代码：

```aster
This module is aster.finance.loan.

Define LoanApplication with applicantId: Text, amount: Int, termMonths: Int, purpose: Text.
Define ApplicantProfile with age: Int, creditScore: Int, annualIncome: Int, monthlyDebt: Int, yearsEmployed: Int.
Define LoanDecision with approved: Bool, reason: Text, approvedAmount: Int, interestRateBps: Int, termMonths: Int.

To evaluateLoanEligibility with application: LoanApplication, applicant: ApplicantProfile, produce LoanDecision:
  If <(applicant.creditScore, 700),:
    Return LoanDecision with approved = false, reason = "Credit below 700", approvedAmount = 0, interestRateBps = 0, termMonths = 0.
  Return LoanDecision with approved = true, reason = "Approved", approvedAmount = application.amount, interestRateBps = 500, termMonths = application.termMonths.
```

4. 查看右侧面板的实时评估结果
5. 调整信用分阈值（650 → 700），立即看到评估结果变化

#### 6.3 导出 CNL 文件

1. 点击 "Export CNL" 按钮
2. 文件自动保存为 `aster.finance.loan.aster`
3. 可以分享给其他开发者或团队

#### 6.4 导入 CNL 文件

1. 点击 "Import CNL" 按钮
2. 选择 `.aster` 文件
3. 策略自动加载到编辑器
4. 使用 Live Preview 立即验证

### 场景 7：性能监控

#### 7.1 查看 Prometheus 指标

```bash
curl http://localhost:8080/q/metrics | grep policy
```

**关键指标：**

```
# 策略评估延迟（直方图）
policy_evaluation_duration_seconds_bucket{module="aster.finance.loan",function="evaluateLoanEligibility",le="0.01"} 150
policy_evaluation_duration_seconds_bucket{module="aster.finance.loan",function="evaluateLoanEligibility",le="0.05"} 200
policy_evaluation_duration_seconds_sum{module="aster.finance.loan",function="evaluateLoanEligibility"} 2.5
policy_evaluation_duration_seconds_count{module="aster.finance.loan",function="evaluateLoanEligibility"} 200

# 策略评估总数
policy_evaluation_total{module="aster.finance.loan",function="evaluateLoanEligibility",status="success"} 195
policy_evaluation_total{module="aster.finance.loan",function="evaluateLoanEligibility",status="failure"} 5

# 缓存命中率
cache_hit_ratio{cache="policy-results"} 0.875
```

#### 7.2 运行性能测试

```bash
cd quarkus-policy-api
../../gradlew test --tests "PolicyEvaluationPerformanceTest"
```

**预期结果：**
- 冷启动延迟：< 10ms
- 热启动延迟（缓存命中）：< 1ms
- 批量评估吞吐量：> 1000 req/s

## 常见问题 (FAQ)

### Q1: 如何添加新的策略文件？

1. 在 `quarkus-policy-api/src/main/resources/policies/` 创建 `.aster` 文件
2. 运行 `./gradlew :quarkus-policy-api:syncPolicyJar`
3. 重启应用，策略自动可用

### Q2: 如何清除策略缓存？

```bash
# 清除所有缓存
curl -X DELETE http://localhost:8080/api/policies/cache \
  -H "Content-Type: application/json" \
  -d '{}'

# 清除特定策略缓存
curl -X DELETE http://localhost:8080/api/policies/cache \
  -H "Content-Type: application/json" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility"
  }'
```

### Q3: 如何查看数据库迁移状态？

```bash
# 开发模式使用 H2 控制台
http://localhost:8080/q/dev/io.quarkus.quarkus-flyway/

# 生产模式查询 Flyway 元数据表
psql -h localhost -U aster -d aster_policy \
  -c "SELECT * FROM flyway_schema_history;"
```

### Q4: 如何监控审计日志数量？

```bash
# 查询审计日志总数
curl http://localhost:8080/api/audit \
  -H "X-Tenant-Id: production" \
  | jq 'length'

# 按事件类型统计
curl http://localhost:8080/api/audit \
  -H "X-Tenant-Id: production" \
  | jq 'group_by(.eventType) | map({eventType: .[0].eventType, count: length})'
```

### Q5: 如何优化缓存性能？

编辑 `application.properties`：

```properties
# 增加缓存容量
quarkus.cache.caffeine.policy-results.initial-capacity=1024
quarkus.cache.caffeine.policy-results.maximum-size=8192

# 调整过期时间
quarkus.cache.caffeine.policy-results.expire-after-write=60M
quarkus.cache.caffeine.policy-results.expire-after-access=30M
```

### Q6: 如何启用 GraphQL API？

GraphQL 已集成，访问：

- GraphQL UI: `http://localhost:8080/q/graphql-ui/`
- GraphQL Endpoint: `http://localhost:8080/graphql`

示例查询：

```graphql
query {
  evaluatePolicy(
    tenantId: "bank-a",
    module: "aster.finance.loan",
    function: "evaluateLoanEligibility",
    context: [
      {applicantId: "LOAN-3001", amount: 100000, termMonths: 240, purpose: "购车"},
      {age: 30, creditScore: 720, annualIncome: 500000, monthlyDebt: 3000, yearsEmployed: 5}
    ]
  ) {
    result
    executionTimeMs
    error
  }
}
```

### Q7: 如何部署到生产环境？

#### 方式 1：Docker 容器

```bash
# 构建 Docker 镜像
cd quarkus-policy-api
../../gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t aster-policy-api:1.0 .

# 运行容器
docker run -d \
  --name aster-api \
  -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres:5432/aster_policy \
  -e QUARKUS_DATASOURCE_USERNAME=aster \
  -e QUARKUS_DATASOURCE_PASSWORD=aster123 \
  --link aster-postgres:postgres \
  aster-policy-api:1.0
```

#### 方式 2：Kubernetes

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

#### 方式 3：Native Image (GraalVM)

```bash
cd quarkus-policy-api
../../gradlew build -Dquarkus.package.type=native

# 运行原生可执行文件
./build/quarkus-policy-api-1.0.0-runner
```

## 下一步

### Phase 2 计划功能

1. **策略模板库** - 预定义的行业标准策略模板
2. **A/B 测试** - 策略灰度发布和对比测试
3. **机器学习集成** - 基于历史数据优化策略参数
4. **可视化策略编排** - 拖拽式策略设计器
5. **多语言 SDK** - Python, JavaScript, Go 客户端库

### 参考文档

- [Aster Language 语法指南](../docs/aster-language-guide.md)
- [Quarkus Policy API 参考](../quarkus-policy-api/README.md)
- [Policy Editor 用户手册](../policy-editor/README.md)
- [性能优化指南](../docs/performance-tuning.md)

## 贡献

欢迎贡献代码、文档和测试用例！请参阅 [CONTRIBUTING.md](../CONTRIBUTING.md)。

## 许可证

与 Aster Language 项目相同。
