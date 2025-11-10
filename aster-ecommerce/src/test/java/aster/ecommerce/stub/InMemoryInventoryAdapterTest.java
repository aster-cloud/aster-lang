package aster.ecommerce.stub;

import aster.ecommerce.InventoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * InMemoryInventoryAdapter 单元测试
 */
class InMemoryInventoryAdapterTest {

    private InMemoryInventoryAdapter adapter;

    @BeforeEach
    void setUp() {
        Map<String, Integer> initialInventory = new HashMap<>();
        initialInventory.put("SKU-001", 100);
        initialInventory.put("SKU-002", 50);
        initialInventory.put("SKU-003", 5); // 低库存
        adapter = new InMemoryInventoryAdapter(initialInventory, 10);
    }

    @Test
    void shouldReserveSuccessfully() throws Exception {
        String orderId = "ORDER-001";
        List<InventoryAdapter.OrderItem> items = Arrays.asList(
            new InventoryAdapter.OrderItem("SKU-001", 10),
            new InventoryAdapter.OrderItem("SKU-002", 5)
        );

        String reservationId = adapter.reserve(orderId, items);

        assertThat(reservationId).isNotNull().startsWith("RES-");
        assertThat(adapter.getStock("SKU-001")).isEqualTo(90);
        assertThat(adapter.getStock("SKU-002")).isEqualTo(45);
        assertThat(adapter.getReservationCount()).isEqualTo(1);
    }

    @Test
    void shouldFailWhenInsufficientStock() {
        String orderId = "ORDER-001";
        List<InventoryAdapter.OrderItem> items = Arrays.asList(
            new InventoryAdapter.OrderItem("SKU-001", 200) // 超过可用库存
        );

        assertThatThrownBy(() -> adapter.reserve(orderId, items))
            .isInstanceOf(InventoryAdapter.InventoryException.class)
            .hasMessageContaining("库存不足");
    }

    @Test
    void shouldReleaseSuccessfully() throws Exception {
        String orderId = "ORDER-001";
        List<InventoryAdapter.OrderItem> items = Arrays.asList(
            new InventoryAdapter.OrderItem("SKU-001", 10)
        );
        String reservationId = adapter.reserve(orderId, items);

        adapter.release(reservationId);

        assertThat(adapter.getStock("SKU-001")).isEqualTo(100); // 恢复库存
        assertThat(adapter.getReservationCount()).isEqualTo(0);
    }

    @Test
    void shouldSupportIdempotentRelease() throws Exception {
        String orderId = "ORDER-001";
        List<InventoryAdapter.OrderItem> items = Arrays.asList(
            new InventoryAdapter.OrderItem("SKU-001", 10)
        );
        String reservationId = adapter.reserve(orderId, items);

        adapter.release(reservationId);
        adapter.release(reservationId); // 重复释放不应报错

        assertThat(adapter.getStock("SKU-001")).isEqualTo(100);
    }

    @Test
    void shouldCheckLowStock() {
        List<String> lowStockItems = adapter.checkLowStock();

        assertThat(lowStockItems).containsExactly("SKU-003");
    }

    @Test
    void shouldUpdateStock() {
        List<InventoryAdapter.StockUpdate> updates = Arrays.asList(
            new InventoryAdapter.StockUpdate("SKU-001", 50),  // 增加库存
            new InventoryAdapter.StockUpdate("SKU-002", -10)  // 减少库存
        );

        adapter.updateStock(updates);

        assertThat(adapter.getStock("SKU-001")).isEqualTo(150);
        assertThat(adapter.getStock("SKU-002")).isEqualTo(40);
    }

    @Test
    void shouldHandleNewProductInUpdate() {
        List<InventoryAdapter.StockUpdate> updates = Arrays.asList(
            new InventoryAdapter.StockUpdate("SKU-NEW", 100) // 新商品
        );

        adapter.updateStock(updates);

        assertThat(adapter.getStock("SKU-NEW")).isEqualTo(100);
    }
}
