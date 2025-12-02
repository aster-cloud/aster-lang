package aster.ecommerce.stub;

import aster.ecommerce.FulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * InMemoryFulfillmentService 单元测试
 */
class InMemoryFulfillmentServiceTest {

    private InMemoryFulfillmentService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryFulfillmentService();
    }

    @Test
    void shouldCreateShipmentSuccessfully() throws Exception {
        String orderId = "ORDER-001";

        String shipmentId = service.createShipment(orderId);

        assertThat(shipmentId).isNotNull().startsWith("SHIP-");
        assertThat(service.getShipmentCount()).isEqualTo(1);
        assertThat(service.getShipmentStatus(shipmentId)).isEqualTo("CREATED");
    }

    @Test
    void shouldFailWhenCreatingDuplicateShipment() throws Exception {
        String orderId = "ORDER-001";
        service.createShipment(orderId);

        assertThatThrownBy(() -> service.createShipment(orderId))
            .isInstanceOf(FulfillmentService.FulfillmentException.class)
            .hasMessageContaining("订单已存在发货单");
    }

    @Test
    void shouldCancelShipmentSuccessfully() throws Exception {
        String orderId = "ORDER-001";
        String shipmentId = service.createShipment(orderId);

        service.cancelShipment(shipmentId);

        assertThat(service.getShipmentStatus(shipmentId)).isEqualTo("CANCELLED");
    }

    @Test
    void shouldFailWhenCancellingNonexistentShipment() {
        assertThatThrownBy(() -> service.cancelShipment("INVALID-SHIPMENT"))
            .isInstanceOf(FulfillmentService.FulfillmentException.class)
            .hasMessageContaining("发货单不存在");
    }

    @Test
    void shouldFailWhenCancellingShippedOrder() throws Exception {
        String orderId = "ORDER-001";
        String shipmentId = service.createShipment(orderId);
        service.ship(shipmentId); // 模拟发货

        assertThatThrownBy(() -> service.cancelShipment(shipmentId))
            .isInstanceOf(FulfillmentService.FulfillmentException.class)
            .hasMessageContaining("发货单已发货");
    }

    @Test
    void shouldSupportIdempotentCancel() throws Exception {
        String orderId = "ORDER-001";
        String shipmentId = service.createShipment(orderId);

        service.cancelShipment(shipmentId);
        service.cancelShipment(shipmentId); // 重复取消不应报错

        assertThat(service.getShipmentStatus(shipmentId)).isEqualTo("CANCELLED");
    }

    @Test
    void shouldAllowCreatingShipmentAfterCancellation() throws Exception {
        String orderId = "ORDER-001";
        String shipmentId1 = service.createShipment(orderId);
        service.cancelShipment(shipmentId1);

        String shipmentId2 = service.createShipment(orderId);

        assertThat(shipmentId2).isNotNull().isNotEqualTo(shipmentId1);
        assertThat(service.getShipmentStatus(shipmentId2)).isEqualTo("CREATED");
    }
}
