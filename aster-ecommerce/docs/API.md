# Aster E-commerce REST API

> 更新日期：2025-11-10 13:49 NZST · 执行者：Codex

本文件描述 Phase 2.4 后的 OrderResource / WorkflowAuditResource REST API，实现订单履约调度、状态查询与事件审计。所有示例均可直接在 `quarkus-policy-api` Quarkus 应用（默认端口 `8080`）上执行。

## 多租户与审计头

- `X-Tenant-Id`（必填）：租户隔离键，缺省为 `default`。同租户+订单号通过 `UUID.nameUUIDFromBytes(tenantId:orderId)` 计算幂等 workflowId。
- `X-User-Id`（可选）：操作者标识，写入审计事件 `performedBy` 字段，默认 `anonymous`。
- 所有 API 都会将 `tenantId` 注入 `AuditEvent` 与 Micrometer 指标（`order_api_requests_total{operation,status}`）。

## 端点一览

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `POST` | `/api/orders` | 提交订单，调度 `fulfillOrder` workflow（OrderResource） |
| `GET` | `/api/orders/{orderId}/status` | 查询订单状态+事件快照（OrderResource） |
| `GET` | `/api/workflows/{workflowId}/events` | 查看 workflow 事件历史（WorkflowAuditResource） |

## 请求示例（curl）

```bash
# 提交订单
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-001" \
  -H "X-User-Id: user-123" \
  -d '{
    "orderId": "ORD-001",
    "customerId": "CUST-123",
    "items": [
      {"productId": "PROD-A", "quantity": 2, "price": 99.99}
    ],
    "metadata": {"channel": "web"}
  }'

# 查询订单状态
curl -X GET http://localhost:8080/api/orders/ORD-001/status \
  -H "X-Tenant-Id: tenant-001"

# 查询 workflow 事件（支持 fromSeq 增量游标）
curl -X GET "http://localhost:8080/api/workflows/7aa5d.../events?fromSeq=10"
```

## `POST /api/orders`

- **Headers**：`Content-Type: application/json`，`X-Tenant-Id`，`X-User-Id`。
- **请求体**（`OrderRequest`）：

```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-123",
  "items": [
    {"productId": "PROD-A", "quantity": 2, "price": 99.99}
  ],
  "metadata": {
    "channel": "web",
    "priority": "rush"
  }
}
```

### 成功响应（`OrderResponse.success`）

```json
{
  "orderId": "ORD-001",
  "workflowId": "7aa5d20d-4e45-3a1e-8f1c-d8dc4c18366c",
  "status": "SCHEDULED",
  "timestamp": 1731220145123,
  "message": "订单已成功提交并进入履约流程"
}
```

### 业务失败（`OrderResponse.error`）

返回 HTTP 200，`status=ERROR`，`workflowId=null`，`message` 为失败原因（例如 Workflow Runtime 不可用）：

```json
{
  "orderId": "ORD-FAIL",
  "workflowId": null,
  "status": "ERROR",
  "timestamp": 1731220160456,
  "message": "workflow runtime unavailable"
}
```

### 400 Bad Request

Bean Validation（如缺少 `orderId`、`items` 为空）会返回 `application/json`，示例：

```json
{
  "exception": "jakarta.validation.ConstraintViolationException",
  "propertyViolations": [
    {
      "path": "submitOrder.request.items",
      "message": "size must be between 1 and 2147483647"
    }
  ]
}
```

## `GET /api/orders/{orderId}/status`

- **Headers**：`X-Tenant-Id`（缺省 default）。
- **成功响应**（`OrderStatusResponse`）：

```json
{
  "orderId": "ORD-001",
  "workflowId": "7aa5d20d-4e45-3a1e-8f1c-d8dc4c18366c",
  "status": "RUNNING",
  "events": [
    {
      "sequence": 1,
      "workflowId": "7aa5d20d-4e45-3a1e-8f1c-d8dc4c18366c",
      "eventType": "WORKFLOW_STARTED",
      "payload": {"step": "validate_order"},
      "occurredAt": "2025-11-10T01:44:11.512Z"
    }
  ],
  "lastUpdated": 1731220478123
}
```

### 错误响应

- `404 Not Found`：`OrderResource` 抛出 `jakarta.ws.rs.NotFoundException`，body 形如：

```json
{
  "error": "Not Found",
  "message": "未找到订单: ORD-4040"
}
```

- `500 Internal Server Error`：事件存储失败等异常，由 RESTEasy Reactive 统一序列化：

```json
{
  "error": "Internal Server Error",
  "message": "store down"
}
```

## `GET /api/workflows/{workflowId}/events`

- **参数**：
  - `workflowId`：`UUID` 字符串，与 `OrderResponse.workflowId` 对应。
  - `fromSeq`（可选，默认 `0`）：事件序列起点，用于分页/增量消费。
- **响应**：`WorkflowEventDTO[]`，字段：
  - `sequence`：`long`
  - `workflowId`：`string`
  - `eventType`：`string`（例如 `STEP_COMPLETED`、`COMPENSATION_STARTED`）
  - `payload`：步骤上下文
  - `occurredAt`：ISO-8601 时间戳

```json
[
  {
    "sequence": 11,
    "workflowId": "7aa5d20d-4e45-3a1e-8f1c-d8dc4c18366c",
    "eventType": "STEP_COMPLETED",
    "payload": {"step": "reserve_inventory"},
    "occurredAt": "2025-11-10T01:44:13.002Z"
  }
]
```

## 幂等性与重试

- `tenantId + orderId` 是唯一幂等键，`resolveWorkflowId` 使用 `UUID.nameUUIDFromBytes` 计算 Deterministic workflowId。
- 客户端多次提交相同订单会收到同一 `workflowId`，可直接轮询状态。
- 若 HTTP 超时可安全重试；如 workflow 已在运行，Runtime 会忽略重复调度。

## 多租户隔离

- `OrderResource` 在 event store 中使用 `workflowId` 作为主键，`tenantId` 仅通过 `workflowId` 算法混入，避免跨租户泄漏。
- 审计事件将 `tenantId` 写入 metadata，并过滤 `metadata` 字段中的受限键（`tenantId/status/workflowId/orderId/performedBy` 等）。
- 指标标签仅包含低基数维度（tenant、operation、status），避免 Prometheus 爆炸。

## 错误码列表

| HTTP 状态 | 场景 | 说明 |
| --- | --- | --- |
| `200` + `status=SCHEDULED` | 订单提交成功 | 返回 `OrderResponse.success` |
| `200` + `status=ERROR` | Runtime/业务异常 | 仍返回 200，message 为具体原因 |
| `400 Bad Request` | 请求体验证失败 | Hibernate Validator 提供字段级错误信息 |
| `404 Not Found` | `getOrderStatus` 未找到 | 抛出 `NotFoundException` |
| `500 Internal Server Error` | 事件存储、Runtime 等内部异常 | RESTEasy Reactive 统一序列化异常 |

## 参考

- 详细 workflow 步骤与补偿逻辑见 `docs/WORKFLOWS.md`
- 快速体验命令见 `docs/QUICKSTART.md`
