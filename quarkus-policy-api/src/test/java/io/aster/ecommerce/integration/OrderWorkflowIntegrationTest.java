package io.aster.ecommerce.integration;

import aster.ecommerce.stub.InMemoryFulfillmentService;
import aster.ecommerce.stub.InMemoryInventoryAdapter;
import aster.ecommerce.stub.InMemoryPaymentGateway;
import aster.runtime.Result;
import io.aster.ecommerce.order_fulfillment.fulfillOrder_fn;
import io.aster.ecommerce.payment_compensation.processFailedPayment_fn;
import io.aster.workflow.WorkflowEventEntity;
import io.aster.workflow.WorkflowStateEntity;
import io.aster.workflow.WorkflowTimerEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Order workflow 端到端整合測試。
 *
 * 採用 Pragmatic 策略：直接調用 workflow 函式驗證業務邏輯，並透過 REST API 檢查調度與事件庫。
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
class OrderWorkflowIntegrationTest {

    @Inject
    InMemoryPaymentGateway paymentGateway;

    @Inject
    InMemoryInventoryAdapter inventoryAdapter;

    @Inject
    InMemoryFulfillmentService fulfillmentService;

    @BeforeEach
    @Transactional
    void resetState() {
        WorkflowEventEntity.deleteAll();
        WorkflowStateEntity.deleteAll();
        WorkflowTimerEntity.deleteAll();
        paymentGateway.clear();
        inventoryAdapter.clear();
        fulfillmentService.clear();
    }

    @Test
    void testOrderFulfillmentDirectCall() {
        Result<String, String> result = fulfillOrder_fn.fulfillOrder();

        assertThat(result).isNull();
    }

    @Test
    void testOrderSubmissionAndEventStore() {
        String orderId = "ORD-IT-1001";
        String workflowId = submitOrder("tenant-it", orderId);

        awaitEventCount(workflowId, 1);
        List<Map<String, Object>> events = fetchEvents(workflowId);

        assertThat(events)
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event.get("workflowId")).isEqualTo(workflowId));
        assertThat(events.get(0).get("eventType")).isEqualTo("WorkflowStarted");
    }

    @Test
    void testPaymentCompensationDirectCall() {
        Result<String, String> result = processFailedPayment_fn.processFailedPayment();

        assertThat(result).isNull();
    }

    @Test
    void testWorkflowIdempotency() {
        String tenant = "tenant-idem";
        String orderId = "ORD-IT-2002";

        String firstWorkflowId = submitOrder(tenant, orderId);
        awaitEventCount(firstWorkflowId, 1);
        int initialEventCount = fetchEvents(firstWorkflowId).size();

        String secondWorkflowId = submitOrder(tenant, orderId);
        assertThat(secondWorkflowId).isEqualTo(firstWorkflowId);

        await().atMost(Duration.ofSeconds(3))
            .until(() -> fetchEvents(firstWorkflowId).size() == initialEventCount);
    }

    private String submitOrder(String tenantId, String orderId) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Tenant-Id", tenantId)
            .header("X-User-Id", "integration-tester")
            .body(orderRequest(orderId, 2))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(200)
            .extract()
            .path("workflowId");
    }

    private void awaitEventCount(String workflowId, int minimum) {
        await().atMost(Duration.ofSeconds(5))
            .until(() -> fetchEvents(workflowId).size() >= minimum);
    }

    private List<Map<String, Object>> fetchEvents(String workflowId) {
        return given()
            .accept(ContentType.JSON)
            .header("X-Tenant-Id", "tenant-it")
            .get("/api/workflows/{workflowId}/events", workflowId)
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("$");
    }

    private String orderRequest(String orderId, int quantity) {
        return """
            {
                "orderId": "%s",
                "customerId": "CUSTOMER-IT",
                "items": [
                    {
                        "productId": "SKU-IT-1",
                        "quantity": %d,
                        "price": 49.99
                    }
                ],
                "metadata": {
                    "channel": "integration",
                    "priority": "NORMAL"
                }
            }
            """.formatted(orderId, quantity);
    }
}
