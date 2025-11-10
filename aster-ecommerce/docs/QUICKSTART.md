# Aster E-commerce 快速开始

> 更新日期：2025-11-10 13:49 NZST · 执行者：Codex

本指南帮助你在本地完成 workflow 编译、测试与 REST API 体验，覆盖 Phase 2.4 hybrid compilation strategy 所需的全部步骤。

## 1. 环境准备

| 组件 | 要求 | 说明 |
| --- | --- | --- |
| Java | Temurin 25（含 `--enable-preview`） | `java -version` 确认 |
| Gradle | 9.0+（已随仓库提供 wrapper） | 使用 `./gradlew` |
| Node.js / npm | Node 20+ | 运行 DSL emitter |
| 数据库 | PostgreSQL 16+（生产）/ H2（测试默认） | `OrderResourceTest` 通过 QuarkusMock 免 DB，运行时建议配置 PostgreSQL |

> 提示：首次拉起 Postgres 可直接使用项目根目录的 `docker-compose.yml`。

## 2. 编译 Workflow DSL

Phase 2.4 采用“TypeScript emitter + javac”混合策略，确保 DSL 可以直接被 Runtime 调用。

```bash
# 生成 Java class（输出到 build/jvm-classes）
npm install
npm run emit:class quarkus-policy-api/src/main/resources/policies/ecommerce/*.aster

# 触发 Gradle generateAsterJar，验证类已被引入 Quarkus 模块
./gradlew :quarkus-policy-api:compileJava
```

常见验证：
- `ls build/jvm-classes/io/aster/ecommerce`：应包含 `order_fulfillment/FulfillOrder.class` 等。
- `quarkus-policy-api/build/classes/...` 中可看到相同模块。

## 3. 运行测试

```bash
# aster-ecommerce 接口 & stub
./gradlew :aster-ecommerce:test

# OrderResource 集成测试（RestAssured + QuarkusMock）
./gradlew :quarkus-policy-api:test --tests io.aster.ecommerce.rest.OrderResourceTest
```

Jacoco 报告位于 `aster-ecommerce/build/reports/jacoco/test/html/index.html`（94% 覆盖），RestAssured 会覆盖 8 个关键 REST 场景。

## 4. 启动 Quarkus 应用

```bash
./gradlew :quarkus-policy-api:quarkusDev
```

默认监听 `localhost:8080`。在 dev 模式中，若未配置 PostgreSQL，可在 `application.properties` 指定 H2 或运行 `docker-compose up postgres`.

## 5. 测试 API

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

# 轮询状态
curl -X GET http://localhost:8080/api/orders/ORD-001/status -H "X-Tenant-Id: tenant-001"
```

预期：
- `POST` 返回 `status: "SCHEDULED"` 与 `workflowId`。
- `GET` 返回 `RUNNING/COMPLETED` 状态以及事件列表。

更多细节（错误码、Payload）请参考 `docs/API.md`。

## 6. 查看监控与审计

- **Metrics**：Micrometer 暴露在 `http://localhost:8080/q/metrics`，可检索 `order_api_requests_total`、`workflow_result_futures_cleaned_total` 等指标。
- **Workflow 事件**：`curl http://localhost:8080/api/workflows/<workflowId>/events` 可查看完整事件时间线；`fromSeq` 查询增量。
- **审计日志**：默认输出到 Quarkus 控制台，可通过 `./gradlew quarkusDev --console=plain | grep AuditEvent` 过滤，或自定义 `AuditEventListener` sink。

## 7. 常见问题

| 问题 | 处理方式 |
| --- | --- |
| `workflow runtime unavailable` | 确认 `PostgresWorkflowRuntime` 已连接数据库；重启应用即可恢复，客户端可重试提交 |
| `404 Not Found` 查询状态 | 核对 `X-Tenant-Id` 与 `orderId`，不同租户将映射到不同 workflowId |
| DSL 未重新编译 | 执行 `npm run emit:class ...` 后再运行 `./gradlew :quarkus-policy-api:compileJava --rerun-tasks` |

## 8. 参考

- Workflow 细节：`docs/WORKFLOWS.md`
- REST API 说明：`docs/API.md`
