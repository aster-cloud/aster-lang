package aster.ecommerce.stub;

import aster.ecommerce.FulfillmentService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 履约服务内存实现
 *
 * 用于测试和演示，所有发货单数据存储在内存中。
 */
public class InMemoryFulfillmentService implements FulfillmentService {

    private final Map<String, ShipmentRecord> shipments = new ConcurrentHashMap<>();
    private final Map<String, String> orderToShipment = new ConcurrentHashMap<>();

    /**
     * 为订单创建发货单
     *
     * @param orderId 订单唯一标识符
     * @return 发货单ID
     * @throws FulfillmentException 当订单已存在发货单时抛出
     */
    @Override
    public String createShipment(String orderId) throws FulfillmentException {
        // 检查是否已为该订单创建发货单
        if (orderToShipment.containsKey(orderId)) {
            String existingShipmentId = orderToShipment.get(orderId);
            ShipmentRecord record = shipments.get(existingShipmentId);
            if (record != null && record.status != ShipmentStatus.CANCELLED) {
                throw new FulfillmentException("订单已存在发货单: orderId=" + orderId + ", shipmentId=" + existingShipmentId);
            }
        }

        // 创建发货单
        String shipmentId = "SHIP-" + UUID.randomUUID();
        shipments.put(shipmentId, new ShipmentRecord(orderId, ShipmentStatus.CREATED));
        orderToShipment.put(orderId, shipmentId);

        return shipmentId;
    }

    /**
     * 取消指定的发货单
     *
     * @param shipmentId 发货单ID
     * @throws FulfillmentException 当发货单不存在或已发货时抛出
     */
    @Override
    public void cancelShipment(String shipmentId) throws FulfillmentException {
        ShipmentRecord record = shipments.get(shipmentId);
        if (record == null) {
            throw new FulfillmentException("发货单不存在: " + shipmentId);
        }
        if (record.status == ShipmentStatus.SHIPPED) {
            throw new FulfillmentException("发货单已发货，无法取消: " + shipmentId);
        }
        if (record.status == ShipmentStatus.CANCELLED) {
            return; // 幂等性：已取消的发货单可重复取消
        }

        // 更新状态为已取消
        record.status = ShipmentStatus.CANCELLED;
    }

    /**
     * 模拟发货操作（用于测试）
     *
     * @param shipmentId 发货单ID
     */
    public void ship(String shipmentId) {
        ShipmentRecord record = shipments.get(shipmentId);
        if (record != null && record.status == ShipmentStatus.CREATED) {
            record.status = ShipmentStatus.SHIPPED;
        }
    }

    /**
     * 清空所有发货单记录（用于测试）
     */
    public void clear() {
        shipments.clear();
        orderToShipment.clear();
    }

    /**
     * 获取发货单数量（用于测试）
     */
    public int getShipmentCount() {
        return shipments.size();
    }

    /**
     * 获取发货单状态（用于测试）
     */
    public String getShipmentStatus(String shipmentId) {
        ShipmentRecord record = shipments.get(shipmentId);
        return record != null ? record.status.name() : null;
    }

    // ==================== 内部类 ====================

    /**
     * 发货单记录
     */
    private static class ShipmentRecord {
        final String orderId;
        ShipmentStatus status;

        ShipmentRecord(String orderId, ShipmentStatus status) {
            this.orderId = orderId;
            this.status = status;
        }
    }

    /**
     * 发货单状态
     */
    private enum ShipmentStatus {
        CREATED,    // 已创建
        SHIPPED,    // 已发货
        CANCELLED   // 已取消
    }
}
