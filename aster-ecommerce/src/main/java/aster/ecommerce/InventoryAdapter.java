package aster.ecommerce;

import java.util.List;
import java.util.Objects;

/**
 * 库存适配器接口
 *
 * 提供库存预留、释放、查询和更新能力，支持事务性库存操作。
 *
 * 主要职责：
 * - 为订单预留库存
 * - 释放未使用的库存预留
 * - 查询低库存商品
 * - 更新库存数量
 */
public interface InventoryAdapter {

    /**
     * 为订单预留库存
     *
     * @param orderId 订单唯一标识符
     * @param items 订单项列表，包含商品ID和数量
     * @return 预留ID，用于后续释放操作
     * @throws InventoryException 当库存不足或预留失败时抛出
     */
    String reserve(String orderId, List<OrderItem> items) throws InventoryException;

    /**
     * 释放指定的库存预留
     *
     * @param reservationId 预留ID（由 reserve 方法返回）
     */
    void release(String reservationId);

    /**
     * 查询低库存商品列表
     *
     * @return 库存不足的商品ID列表
     */
    List<String> checkLowStock();

    /**
     * 批量更新库存数量
     *
     * @param updates 库存更新列表
     */
    void updateStock(List<StockUpdate> updates);

    /**
     * 订单项
     *
     * 包含商品ID和订购数量。
     */
    class OrderItem {
        private final String productId;
        private final int quantity;

        public OrderItem(String productId, int quantity) {
            this.productId = Objects.requireNonNull(productId, "productId 不能为 null");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity 必须大于 0");
            }
            this.quantity = quantity;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderItem orderItem = (OrderItem) o;
            return quantity == orderItem.quantity && productId.equals(orderItem.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, quantity);
        }

        @Override
        public String toString() {
            return "OrderItem{productId='" + productId + "', quantity=" + quantity + '}';
        }
    }

    /**
     * 库存更新
     *
     * 包含商品ID和库存增量（正数表示入库，负数表示出库）。
     */
    class StockUpdate {
        private final String productId;
        private final int delta;

        public StockUpdate(String productId, int delta) {
            this.productId = Objects.requireNonNull(productId, "productId 不能为 null");
            this.delta = delta;
        }

        public String getProductId() {
            return productId;
        }

        public int getDelta() {
            return delta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StockUpdate that = (StockUpdate) o;
            return delta == that.delta && productId.equals(that.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, delta);
        }

        @Override
        public String toString() {
            return "StockUpdate{productId='" + productId + "', delta=" + delta + '}';
        }
    }

    /**
     * 库存异常
     *
     * 封装库存操作过程中的各类错误，如库存不足、商品不存在等。
     */
    class InventoryException extends Exception {
        private static final long serialVersionUID = 1L;

        public InventoryException(String message) {
            super(message);
        }

        public InventoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
