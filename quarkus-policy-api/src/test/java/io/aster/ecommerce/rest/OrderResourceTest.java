package io.aster.ecommerce.rest;

import aster.runtime.workflow.ExecutionHandle;
import aster.runtime.workflow.WorkflowEvent;
import aster.runtime.workflow.WorkflowState;
import io.aster.ecommerce.metrics.OrderMetrics;
import io.aster.policy.event.AuditEvent;
import io.aster.workflow.PostgresEventStore;
import io.aster.workflow.PostgresWorkflowRuntime;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * OrderResource 集成测试。
 *
 * 使用 RestAssured 验证订单提交与状态查询路径。
 */
@QuarkusTest
@TestProfile(OrderResourceTest.OrderApiTestProfile.class)
class OrderResourceTest {

    PostgresWorkflowRuntime workflowRuntime;
    PostgresEventStore eventStore;
    OrderMetrics orderMetrics;
    @Inject
    TestAuditEventRecorder auditEventRecorder;

    @BeforeEach
    void setUp() {
        workflowRuntime = Mockito.mock(PostgresWorkflowRuntime.class);
        eventStore = Mockito.mock(PostgresEventStore.class);
        orderMetrics = Mockito.mock(OrderMetrics.class);

        QuarkusMock.installMockForType(workflowRuntime, PostgresWorkflowRuntime.class);
        QuarkusMock.installMockForType(eventStore, PostgresEventStore.class);
        QuarkusMock.installMockForType(orderMetrics, OrderMetrics.class);

        Mockito.when(workflowRuntime.schedule(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> {
                String workflowId = invocation.getArgument(0);
                ExecutionHandle handle = Mockito.mock(ExecutionHandle.class);
                Mockito.when(handle.getWorkflowId()).thenReturn(workflowId);
                return handle;
            });
        auditEventRecorder.clear();
    }


    @Test
    void testSubmitOrder_Success() {
        String tenantId = "tenant-success";
        String orderId = "ORD-1001";

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
            .header("X-User-Id", "user-a")
            .body(sampleOrderPayload(orderId, 2))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("status", equalTo("SCHEDULED"))
            .body("workflowId", equalTo(expectedWorkflowId(tenantId, orderId)))
            .body("message", containsString("成功"));

        Mockito.verify(orderMetrics).recordOrderSubmission(Mockito.eq(tenantId), Mockito.anyLong(), Mockito.eq(true));
    }

    @Test
    void testSubmitOrder_Idempotent() {
        String tenantId = "tenant-repeat";
        String orderId = "ORD-2002";

        String firstWorkflowId =
            given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-Id", tenantId)
                .body(sampleOrderPayload(orderId, 1))
            .when()
                .post("/api/orders")
            .then()
                .statusCode(200)
                .extract()
                .path("workflowId");

        String secondWorkflowId =
            given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-Id", tenantId)
                .body(sampleOrderPayload(orderId, 3))
            .when()
                .post("/api/orders")
            .then()
                .statusCode(200)
                .extract()
                .path("workflowId");

        assertEquals(firstWorkflowId, secondWorkflowId, "同租户同订单号应映射到相同 workflowId");
        Mockito.verify(workflowRuntime, Mockito.times(2))
            .schedule(Mockito.anyString(), Mockito.eq(orderId), Mockito.any());
    }

    @Test
    void testSubmitOrder_RuntimeFailure() {
        String tenantId = "tenant-failure";
        String orderId = "ORD-FAIL";
        Mockito.when(workflowRuntime.schedule(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenThrow(new IllegalStateException("workflow runtime unavailable"));

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
            .header("X-User-Id", "user-b")
            .body(sampleOrderPayload(orderId, 1))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(200)
            .body("status", equalTo("ERROR"))
            .body("message", containsString("unavailable"));

        Mockito.verify(orderMetrics).recordOrderSubmission(Mockito.eq(tenantId), Mockito.anyLong(), Mockito.eq(false));

        AuditEvent event = auditEventRecorder.awaitLatest(1, Duration.ofSeconds(2));
        assertNotNull(event, "审计事件必须发布");
        assertEquals(tenantId, event.tenantId());
        assertFalse(event.success());
        assertEquals("failed", event.metadata().get("status"));
    }

    @Test
    void testSubmitOrder_InvalidRequest() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "customerId": "CUST-001",
                    "items": []
                }
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    @Test
    void testGetOrderStatus_Found() {
        String tenantId = "tenant-status";
        String orderId = "ORD-3003";
        String workflowId = expectedWorkflowId(tenantId, orderId);

        WorkflowState state = new WorkflowState(
            workflowId,
            WorkflowState.Status.RUNNING,
            5L,
            Map.of("status", "RUNNING"),
            null,
            null,
            Instant.now().minusSeconds(30),
            Instant.now()
        );
        List<WorkflowEvent> events = List.of(
            new WorkflowEvent(1L, workflowId, WorkflowEvent.Type.WORKFLOW_STARTED, Map.of("step", "created"), Instant.now())
        );

        Mockito.when(eventStore.getState(workflowId)).thenReturn(Optional.of(state));
        Mockito.when(eventStore.getEvents(workflowId, 0)).thenReturn(events);

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
        .when()
            .get("/api/orders/{orderId}/status", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("workflowId", equalTo(workflowId))
            .body("status", equalTo("RUNNING"))
            .body("events.size()", equalTo(1))
            .body("lastUpdated", notNullValue());

        Mockito.verify(orderMetrics).recordOrderStatusQuery(Mockito.eq(tenantId), Mockito.anyLong(), Mockito.eq(true));
    }

    @Test
    void testGetOrderStatus_NotFound() {
        String tenantId = "tenant-missing";
        String orderId = "ORD-4040";
        String workflowId = expectedWorkflowId(tenantId, orderId);

        Mockito.when(eventStore.getState(workflowId)).thenReturn(Optional.empty());

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
        .when()
            .get("/api/orders/{orderId}/status", orderId)
        .then()
            .statusCode(404);

        Mockito.verify(orderMetrics).recordOrderStatusQuery(Mockito.eq(tenantId), Mockito.anyLong(), Mockito.eq(false));
    }

    @Test
    void testGetOrderStatus_EventStoreFailure() {
        String tenantId = "tenant-store";
        String orderId = "ORD-ERR";
        String workflowId = expectedWorkflowId(tenantId, orderId);

        Mockito.when(eventStore.getState(workflowId)).thenThrow(new IllegalStateException("store down"));

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
        .when()
            .get("/api/orders/{orderId}/status", orderId)
        .then()
            .statusCode(500);

        Mockito.verify(orderMetrics).recordOrderStatusQuery(Mockito.eq(tenantId), Mockito.anyLong(), Mockito.eq(false));

        AuditEvent event = auditEventRecorder.awaitLatest(1, Duration.ofSeconds(2));
        assertNotNull(event, "状态查询失败也必须产生日志");
        assertEquals(tenantId, event.tenantId());
        assertFalse(event.success());
        assertEquals("UNKNOWN", event.metadata().get("status"));
    }

    @Test
    void testMultiTenant_Isolation() {
        String orderId = "ORD-5005";

        String tenantOneWorkflow =
            given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-Id", "tenant-one")
                .body(sampleOrderPayload(orderId, 1))
            .when()
                .post("/api/orders")
            .then()
                .statusCode(200)
                .extract()
                .path("workflowId");

        String tenantTwoWorkflow =
            given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-Id", "tenant-two")
                .body(sampleOrderPayload(orderId, 1))
            .when()
                .post("/api/orders")
            .then()
                .statusCode(200)
                .extract()
                .path("workflowId");

        assertNotEquals(tenantOneWorkflow, tenantTwoWorkflow, "不同租户必须生成不同 workflowId");
    }

    @Test
    void testAuditEvent_Verification() {
        String tenantId = "tenant-audit";
        String orderId = "ORD-7777";
        String payload = """
            {
                "orderId": "%s",
                "customerId": "CUST-777",
                "items": [
                    {
                        "productId": "SKU-99",
                        "quantity": 1,
                        "price": 199.00
                    }
                ],
                "metadata": {
                    "channel": "mobile",
                    "priority": "rush",
                    "tenantId": "malicious",
                    "status": "HACKED",
                    "workflowId": "override"
                }
            }
            """.formatted(orderId);

        given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
            .header("X-User-Id", "auditor")
            .body(payload)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(200)
            .body("status", equalTo("SCHEDULED"));

        AuditEvent event = auditEventRecorder.awaitLatest(1, Duration.ofSeconds(2));
        assertNotNull(event, "成功提交通知必须产生日志");
        Map<String, Object> metadata = event.metadata();
        assertEquals(tenantId, metadata.get("tenantId"));
        assertEquals("scheduled", metadata.get("status"));
        assertEquals(expectedWorkflowId(tenantId, orderId), metadata.get("workflowId"));
        assertEquals("mobile", metadata.get("channel"));
        assertEquals("rush", metadata.get("priority"));
        assertFalse(metadata.containsKey("performedBy"));
    }

    private String sampleOrderPayload(String orderId, int quantity) {
        return """
            {
                "orderId": "%s",
                "customerId": "CUST-001",
                "items": [
                    {
                        "productId": "SKU-1",
                        "quantity": %d,
                        "price": 19.99
                    }
                ],
                "metadata": {
                    "channel": "mobile"
                }
            }
            """.formatted(orderId, quantity);
    }

    private String expectedWorkflowId(String tenantId, String orderId) {
        String source = (tenantId == null ? "default" : tenantId) + ":" + orderId;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 关闭 Flyway 迁移，避免 H2 无法处理 PostgreSQL 分区语法。
     */
    public static class OrderApiTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.flyway.clean-at-start", "false",
                "quarkus.arc.exclude-types", "io.aster.workflow.WorkflowSchedulerService,io.aster.policy.event.AuditEventListener"
            );
        }
    }
}
