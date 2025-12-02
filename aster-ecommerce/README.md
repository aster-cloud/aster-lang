# Aster E-commerce Domain Library

## 概述

`aster-ecommerce` 是 Aster 语言的电商领域示例库，演示如何使用 Workflow DSL 实现复杂的业务流程，包括订单履约、支付补偿和库存管理。

本模块是 **Phase 2.3: Domain Library** 的交付成果，基于 Phase 2.1 的 Workflow 语言扩展和 Phase 2.2 的 Durable Execution Runtime 实现。

## 项目结构

```
aster-ecommerce/
├── src/main/java/aster/ecommerce/
│   ├── PaymentGateway.java          # 支付网关接口
│   ├── InventoryAdapter.java        # 库存管理接口
│   ├── FulfillmentService.java      # 物流履约接口
│   └── stub/
│       ├── InMemoryPaymentGateway.java     # 支付网关 Stub 实现
│       ├── InMemoryInventoryAdapter.java   # 库存管理 Stub 实现
│       └── InMemoryFulfillmentService.java # 物流服务 Stub 实现
└── src/test/java/aster/ecommerce/stub/
    ├── InMemoryPaymentGatewayTest.java     # 94% 测试覆盖率
    ├── InMemoryInventoryAdapterTest.java   # 94% 测试覆盖率
    └── InMemoryFulfillmentServiceTest.java # 94% 测试覆盖率
```

## 核心组件

### 1. 接口层（Interface Layer）

#### PaymentGateway - 支付网关
```java
public interface PaymentGateway {
    String charge(String orderId, BigDecimal amount) throws PaymentException;
    String refund(String paymentId) throws PaymentException;
    boolean attemptRefund(String orderId);
}
```

**设计要点**：
- 支持幂等性：同一 `orderId` 多次 `charge` 返回相同 `paymentId`
- 异常安全：`attemptRefund` 不抛出异常，适合补偿逻辑

#### InventoryAdapter - 库存管理
```java
public interface InventoryAdapter {
    String reserve(String orderId, List<OrderItem> items) throws InventoryException;
    void release(String reservationId);
    List<String> checkLowStock();
    void updateStock(List<StockUpdate> updates);
}
```

**设计要点**：
- 原子性保证：`reserve` 检查所有商品后一次性扣减库存
- 幂等释放：`release` 可重复调用
- 低库存检查：支持补货触发

#### FulfillmentService - 物流履约
```java
public interface FulfillmentService {
    String createShipment(String orderId) throws FulfillmentException;
    void cancelShipment(String shipmentId) throws FulfillmentException;
}
```

**设计要点**：
- 幂等取消：已取消的发货单可重复取消
- 状态验证：已发货的订单无法取消

### 2. Stub 实现层（Stub Layer）

所有 Stub 实现遵循以下设计原则：

1. **线程安全**：使用 `ConcurrentHashMap` 支持并发操作
2. **幂等性**：关键操作支持重复调用
3. **原子性**：使用 `computeIfAbsent`/`computeIfPresent` 保证操作原子性
4. **测试覆盖率**：单元测试达到 94% 覆盖率

#### InMemoryPaymentGateway 实现亮点

```java
// 幂等性实现：同一 orderId 返回相同 paymentId
return orderToPayment.computeIfAbsent(orderId, key -> {
    String paymentId = "PAY-" + UUID.randomUUID();
    payments.put(paymentId, new PaymentRecord(orderId, amount, PaymentStatus.CHARGED));
    return paymentId;
});
```

#### InMemoryInventoryAdapter 实现亮点

```java
// 原子扣减库存
for (OrderItem item : items) {
    inventory.computeIfPresent(item.getProductId(), (k, v) -> v - item.getQuantity());
}
```

## Workflow DSL 文件

**当前状态**：Phase 2.4 引入的 hybrid compilation strategy 已完成验证，`npm run emit:class quarkus-policy-api/src/main/resources/policies/ecommerce/*.aster` 会先利用 TypeScript emitter 生成 Java 源码，再由 `javac` 与 `:quarkus-policy-api:generateAsterJar` 产出 `build/jvm-classes`。`order-fulfillment.aster`、`payment-compensation.aster` 与 `inventory-replenishment.aster` 均可成功编译并作为 workflow module 导入 Quarkus。

**实现要点**：
- Workflow module 会被发射到 `io.aster.ecommerce.*` 命名空间，并由 Gradle 任务自动打包入 policy JAR。
- hybrid 策略将 workflow&policy 共存的 class 目录合并，避免手动 copy 或重复 `javac`。
- `PostgresWorkflowRuntime` 现在直接引用这些已编译类，因此 OrderResource REST API 可以调度运行。

### 1. 订单履约 Workflow（order-fulfillment.aster）

**位置**：`quarkus-policy-api/src/main/resources/policies/ecommerce/order-fulfillment.aster`

**功能**：完整的订单履约流程，包含验证、库存预留、支付、物流和通知。

**特点**：
- 串行执行：步骤按声明顺序执行
- 自动补偿：失败时按 LIFO 顺序执行补偿逻辑
- 超时控制：设置 5 分钟超时

**步骤**：
1. `validate_order` - 验证订单有效性
2. `reserve_inventory` - 预留库存（compensate: 释放库存）
3. `charge_payment` - 扣款（compensate: 退款）
4. `create_shipment` - 创建发货单（compensate: 取消发货）
5. `notify_customer` - 发送通知

### 2. 支付失败补偿 Workflow（payment-compensation.aster）

**位置**：`quarkus-policy-api/src/main/resources/policies/ecommerce/payment-compensation.aster`

**功能**：演示 Saga 补偿模式的失败恢复流程。

**特点**：
- `attempt_payment` 步骤返回 `err of "insufficient funds"` 触发补偿
- 补偿按 LIFO 顺序执行：先 `attempt_payment.compensate`，再 `prepare_order.compensate`
- `send_failure_notification` 步骤不会执行

### 3. 库存补货 Workflow（inventory-replenishment.aster）

**位置**：`quarkus-policy-api/src/main/resources/policies/ecommerce/inventory-replenishment.aster`

**功能**：检查低库存并向供应商下单补货。

**当前限制**：
- 串行采购：供应商 A → 供应商 B（等待前者完成）
- **未来升级**：待 Phase 2.4 实现 `depends on` 语法后可改为并发采购

**步骤**：
1. `check_stock_levels` - 检查低库存
2. `order_from_supplier_a` - 向供应商 A 下单
3. `order_from_supplier_b` - 向供应商 B 下单（串行）
4. `update_inventory` - 更新库存

## 技术规格

### 依赖项

```kotlin
dependencies {
    implementation(project(":aster-runtime"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.0")
}
```

### 构建配置

- **Java 版本**：25（with `--enable-preview`）
- **编码**：UTF-8
- **测试框架**：JUnit 5 + AssertJ
- **覆盖率工具**：Jacoco（目标 80%）

## 语法限制（仍在演进）

- ❌ **不支持并发 workflow**：当前语法不支持 `depends on` 关键字
- ✅ **串行执行**：所有步骤按声明顺序执行
- ✅ **LIFO 补偿**：失败时按相反顺序执行补偿

**未来升级（Phase 2.5 目标）**：
- `depends on` 语法支持并发采购/支付
- `inventory-replenishment.aster` 升级为混合并发策略

## 运行时优化（已完成）

### PostgresWorkflowRuntime 内存管理

**问题**：`resultFutures` ConcurrentHashMap 无限增长导致内存泄漏。

**解决方案**：
1. 添加 `@Scheduled` 定时任务 `cleanupExpiredFutures()`，每小时执行一次
2. 清理超过 TTL 的 workflow 结果 future（默认 24 小时）
3. 配置项：`workflow.result-futures.ttl-hours=24`
4. 监控指标：`workflow.result_futures.cleaned.total`

**实现细节**：
```java
@Scheduled(every = "1h")
void cleanupExpiredFutures() {
    Instant threshold = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
    for (Map.Entry<String, CompletableFuture<Object>> entry : resultFutures.entrySet()) {
        String workflowId = entry.getKey();
        Optional<WorkflowStateEntity> stateOpt = WorkflowStateEntity.findByWorkflowId(UUID.fromString(workflowId));
        if (stateOpt.isEmpty() || stateOpt.get().updatedAt.isBefore(threshold)) {
            resultFutures.remove(workflowId);
            removed++;
        }
    }
    metrics.recordResultFuturesCleaned(removed);
}
```

## OrderResource REST API

Phase 2.4 完成了基于 Workflow DSL 的 OrderResource，实现多租户、幂等与审计管道：

- **提交订单**：`POST /api/orders`，依据 `X-Tenant-Id`/`X-User-Id` 计算 workflowId 并调度 `fulfillOrder`。多次提交相同 `orderId` 会返回相同 workflowId，用于幂等。
- **查询订单状态**：`GET /api/orders/{orderId}/status`，聚合 `WorkflowState + WorkflowEvent` 返回状态、事件时间线与 `lastUpdated`。
- **Workflow 事件审计**：委托 `WorkflowAuditResource` 暴露的 `GET /api/workflows/{workflowId}/events`，支持从任意序列号增量查询。
- **审计/指标**：所有 API 调用都会通过 `AuditEvent` 发布可追踪日志，同时通过 `order_api_requests_total{operation,status}` 暴露指标。

详细的请求/响应示例、错误码定义与 curl 命令，见 `docs/API.md`。

## 测试

### 单元测试（已完成）

运行所有测试：
```bash
./gradlew :aster-ecommerce:test
```

查看覆盖率报告：
```bash
./gradlew :aster-ecommerce:jacocoTestReport
open aster-ecommerce/build/reports/jacoco/test/html/index.html
```

**Jacoco 覆盖率（2025-11-10）**：94%（`aster.ecommerce.stub` 包），OrderResource 相关 DTO/资源类纳入同一执行集。

### 集成测试（已完成）

```bash
# 订单 REST API 集成测试（RestAssured）
./gradlew :quarkus-policy-api:test --tests io.aster.ecommerce.rest.OrderResourceTest
```

**覆盖范围**：
1. 订单履约调度成功/失败
2. 幂等性与多租户隔离
3. 状态查询成功、未找到、事件存储失败
4. 审计事件与指标计数

**Workflow DSL 编译验证**：

```bash
npm run emit:class quarkus-policy-api/src/main/resources/policies/ecommerce/*.aster
./gradlew :quarkus-policy-api:compileJava
```

上述命令可确保 hybrid compilation strategy 与 Gradle pipeline 一致。

## 下一步

### Phase 2.3 交付（全部 ✅）

- ✅ 创建 `aster-ecommerce` Gradle 模块
- ✅ 定义电商接口层
- ✅ 实现 Stub 层
- ✅ 编写 Workflow DSL 文件
- ✅ 优化 PostgresWorkflowRuntime 内存管理
- ✅ 修复 DSL 编译器（通过 Phase 2.4 hybrid compilation）
- ✅ 实现 OrderResource REST API
- ✅ 完成端到端集成测试

### Phase 2.4 完成项

- ✅ 混合编译链：TypeScript emitter + javac + Gradle
- ✅ Workflow DSL 全量编译并可被 REST API 引用
- ✅ OrderResource 多租户、幂等与审计逻辑
- ✅ `OrderResourceTest`（RestAssured）覆盖 8 个关键场景
- ✅ `WorkflowAuditResource` 集成于 README/API 文档

### Phase 2.5 展望

1. 引入 `depends on` 并发语法并升级库存补货为并发版
2. 扩展更多场景（退货/换货）
3. 增加 SLA 监控（指标、审计仪表盘）

## 参考资料

- [Aster Workflow DSL 规范](../docs/workflow-dsl.md)
- [PostgresWorkflowRuntime 实现](../quarkus-policy-api/src/main/java/io/aster/workflow/PostgresWorkflowRuntime.java)
- [Phase 2.3 反思文档](../.claude/phase2.3-reflection.md)

## 许可证

参考项目根目录 LICENSE 文件。
