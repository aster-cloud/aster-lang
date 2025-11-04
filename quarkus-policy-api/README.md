# Aster Policy API - REST API Documentation

Production-ready policy evaluation REST API powered by Quarkus 3.28.3.

## Quick Start

### Running the API

```bash
# Development mode with hot reload
./gradlew :quarkus-policy-api:quarkusDev

# Production mode
./gradlew :quarkus-policy-api:quarkusBuild
java -jar quarkus-policy-api/build/quarkus-app/quarkus-run.jar
```

The API will be available at `http://localhost:8080`.

### OpenAPI Documentation

- Swagger UI: `http://localhost:8080/q/swagger-ui/`
- OpenAPI Spec: `http://localhost:8080/q/openapi`

### Health Checks

- Liveness: `http://localhost:8080/q/health/live`
- Readiness: `http://localhost:8080/q/health/ready`

### Metrics

- Prometheus: `http://localhost:8080/q/metrics`

## 性能最佳实践（更新于 2025-11-05 07:10 NZST，由 Codex）

- **预热策略元数据**：服务启动时自动预加载九个核心策略函数，避免首次请求触发类加载与验证，冷启动平均耗时降低至 9.179ms。
- **充分利用缓存**：`policy-results` Caffeine 缓存已扩容至初始 512 / 上限 4096，并启用 30 分钟写入过期 + 10 分钟访问续期，适合高频评估场景；建议通过 Prometheus 指标持续观察命中率。
- **输入上下文最小化**：GraphQL 查询服务不再附带租户标记进入上下文，减少数组拷贝和哈希计算，批量场景中缓存命中平均耗时降至 0.044ms。
- **性能验证流程**：使用 `./gradlew :quarkus-policy-api:test --tests "io.aster.policy.performance.PolicyEvaluationPerformanceTest"` 持续跟踪冷/热延迟，当前基线（200 次迭代）为冷 9.179ms / 热 0.044ms。
- **批量与响应式注意事项**：批量接口保持并行执行，请在极端高负载下调整 Mutiny worker-pool 大小，确保不会压占 Quarkus 事件循环线程。

## API Endpoints

### 1. Evaluate Single Policy

**POST** `/api/policies/evaluate`

Evaluates a single policy function with provided context data.

#### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-Id` | No | Tenant identifier for multi-tenant isolation (default: "default") |
| `Content-Type` | Yes | Must be `application/json` |

#### Request Body

```json
{
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
      "creditScore": 720,
      "annualIncome": 500000,
      "monthlyDebt": 8000,
      "yearsEmployed": 5
    }
  ]
}
```

#### Response (Success)

```json
{
  "result": {
    "approved": true,
    "reason": "Approved",
    "approvedAmount": 250000,
    "interestRateBps": 550,
    "termMonths": 360
  },
  "executionTimeMs": 15,
  "error": null
}
```

#### Response (Error)

```json
{
  "result": null,
  "executionTimeMs": 0,
  "error": "Schema 验证失败：缺失字段 [amount]"
}
```

#### Example using curl

```bash
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: production" \
  -d '{
    "policyModule": "aster.finance.loan",
    "policyFunction": "evaluateLoanEligibility",
    "context": [
      {"applicantId": "LOAN-1001", "amount": 250000, "termMonths": 360, "purpose": "购房"},
      {"age": 35, "creditScore": 720, "annualIncome": 500000, "monthlyDebt": 8000, "yearsEmployed": 5}
    ]
  }'
```

### 2. Batch Evaluate Multiple Policies

**POST** `/api/policies/evaluate/batch`

Evaluates multiple policies in parallel for improved performance.

#### Request Body

```json
{
  "requests": [
    {
      "policyModule": "aster.finance.loan",
      "policyFunction": "evaluateLoanEligibility",
      "context": [
        {"applicantId": "LOAN-1001", "amount": 250000, "termMonths": 360, "purpose": "购房"},
        {"age": 35, "creditScore": 720, "annualIncome": 500000, "monthlyDebt": 8000, "yearsEmployed": 5}
      ]
    },
    {
      "policyModule": "aster.finance.creditcard",
      "policyFunction": "evaluateCreditCardApplication",
      "context": [
        {"applicantId": "CC-2001", "age": 28, "annualIncome": 300000, "creditScore": 680, "existingCreditCards": 1, "monthlyRent": 6000, "employmentStatus": "Full-time", "yearsAtCurrentJob": 3},
        {"bankruptcyCount": 0, "latePayments": 0, "utilization": 30, "accountAge": 5, "hardInquiries": 1},
        {"productType": "Standard", "requestedLimit": 50000, "hasRewards": true, "annualFee": 800}
      ]
    }
  ]
}
```

#### Response

```json
{
  "results": [
    {
      "result": {"approved": true, "reason": "Approved", "approvedAmount": 250000, "interestRateBps": 550, "termMonths": 360},
      "executionTimeMs": 12,
      "error": null
    },
    {
      "result": {"approved": true, "reason": "Approved", "approvedLimit": 50000, "interestRateAPR": 1599, "monthlyFee": 0, "creditLine": 50000, "requiresDeposit": false, "depositAmount": 0},
      "executionTimeMs": 18,
      "error": null
    }
  ],
  "totalExecutionTimeMs": 25,
  "successCount": 2,
  "failureCount": 0
}
```

### 3. Validate Policy

**POST** `/api/policies/validate`

Validates that a policy function exists and is callable.

#### Request Body

```json
{
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility"
}
```

#### Response (Valid)

```json
{
  "valid": true,
  "message": null,
  "parameterCount": 2,
  "returnType": "aster.finance.loan.LoanDecision"
}
```

#### Response (Invalid)

```json
{
  "valid": false,
  "message": "Policy validation failed: Failed to load policy metadata",
  "parameterCount": 0,
  "returnType": null
}
```

### 4. Clear Policy Cache

**DELETE** `/api/policies/cache`

Clears the policy evaluation cache. Supports clearing all cache, module-level, or function-level.

#### Request Body

```json
{
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility"
}
```

**Note:** Both fields are optional. Omit both to clear all cache, provide only `policyModule` to clear all functions in that module.

#### Response

```json
{
  "success": true,
  "message": "Cache cleared for tenant production"
}
```

## Available Policy Functions

### Loan Evaluation

**Module:** `aster.finance.loan`  
**Function:** `evaluateLoanEligibility`

**Parameters:**
1. `LoanApplication`: `{applicantId: Text, amount: Int, termMonths: Int, purpose: Text}`
2. `ApplicantProfile`: `{age: Int, creditScore: Int, annualIncome: Int, monthlyDebt: Int, yearsEmployed: Int}`

**Returns:** `LoanDecision`: `{approved: Bool, reason: Text, approvedAmount: Int, interestRateBps: Int, termMonths: Int}`

### Credit Card Application

**Module:** `aster.finance.creditcard`  
**Function:** `evaluateCreditCardApplication`

**Parameters:**
1. `ApplicantInfo`: `{applicantId: Text, age: Int, annualIncome: Int, creditScore: Int, existingCreditCards: Int, monthlyRent: Int, employmentStatus: Text, yearsAtCurrentJob: Int}`
2. `FinancialHistory`: `{bankruptcyCount: Int, latePayments: Int, utilization: Int, accountAge: Int, hardInquiries: Int}`
3. `CreditCardOffer`: `{productType: Text, requestedLimit: Int, hasRewards: Bool, annualFee: Int}`

**Returns:** `ApprovalDecision`: `{approved: Bool, reason: Text, approvedLimit: Int, interestRateAPR: Int, monthlyFee: Int, creditLine: Int, requiresDeposit: Bool, depositAmount: Int}`

### Healthcare Eligibility

**Module:** `aster.healthcare.eligibility`  
**Function:** `evaluateHealthcareEligibility`

**Parameters:**
1. `Patient`: `{patientId: Text, age: Int, chronicConditions: Int, emergencyVisits: Int, hasInsurance: Bool}`
2. `Treatment`: `{treatmentType: Text, estimatedCost: Int, urgencyLevel: Text, requiresSpecialist: Bool}`

**Returns:** `HealthcareDecision`: `{eligible: Bool, reason: Text, coveragePercent: Int, maxCoverage: Int, requiresPreAuth: Bool}`

## Multi-Tenant Support

The API supports multi-tenant isolation using the `X-Tenant-Id` header. Each tenant has its own cache namespace.

```bash
# Tenant A
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "X-Tenant-Id: tenant-a" \
  -H "Content-Type: application/json" \
  -d '{ ... }'

# Tenant B (separate cache)
curl -X POST http://localhost:8080/api/policies/evaluate \
  -H "X-Tenant-Id: tenant-b" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
```

## Error Handling

All endpoints return proper HTTP status codes:

- `200 OK`: Successful evaluation (even if policy logic rejects the request)
- `400 Bad Request`: Invalid request format or missing required fields
- `404 Not Found`: Policy function not found
- `500 Internal Server Error`: Unexpected server error

Errors are returned in the response body with descriptive messages:

```json
{
  "result": null,
  "executionTimeMs": 0,
  "error": "Schema 验证失败：缺失字段 [amount, applicantId, purpose, termMonths]"
}
```

## Performance Considerations

- **Batch Evaluation**: Use `/api/policies/evaluate/batch` for evaluating multiple policies to reduce HTTP overhead
- **Caching**: Policy results are cached per tenant, reducing redundant computations
- **Reactive**: Built on Quarkus Reactive architecture for high throughput
- **Fast Startup**: Typical startup time < 3 seconds

## Development

### Running Tests

```bash
./gradlew :quarkus-policy-api:test
```

### Building

```bash
# JVM build
./gradlew :quarkus-policy-api:quarkusBuild

# Native build (requires GraalVM)
./gradlew :quarkus-policy-api:quarkusBuild -Dquarkus.package.type=native
```

### Adding New Policies

1. Create `.aster` policy file in `src/main/resources/policies/`
2. Run `./gradlew :quarkus-policy-api:generateAsterJar` to compile
3. Policies are automatically loaded at runtime

## Architecture

```
┌─────────────────┐
│   REST Layer    │  PolicyEvaluationResource
├─────────────────┤
│  Service Layer  │  PolicyEvaluationService (with @CacheResult)
├─────────────────┤
│ Validation Layer│  QuarkusPolicyMetadataLoader, PolicyTypeConverter
├─────────────────┤
│  Aster Runtime  │  Compiled .aster policies (aster.jar)
└─────────────────┘
```

## License

Part of the Aster Language project.
