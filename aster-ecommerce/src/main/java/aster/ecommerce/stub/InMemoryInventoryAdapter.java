package aster.ecommerce.stub;

import aster.ecommerce.InventoryAdapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 库存适配器内存实现
 *
 * 用于测试和演示，所有库存数据存储在内存中。
 * 支持线程安全的库存预留和释放操作。
 */
public class InMemoryInventoryAdapter implements InventoryAdapter {

    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final int lowStockThreshold;

    /**
     * 创建库存适配器
     *
     * @param initialInventory 初始库存（productId → quantity）
     * @param lowStockThreshold 低库存阈值，默认 10
     */
    public InMemoryInventoryAdapter(Map<String, Integer> initialInventory, int lowStockThreshold) {
        if (initialInventory != null) {
            this.inventory.putAll(initialInventory);
        }
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * 创建库存适配器（默认低库存阈值为 10）
     */
    public InMemoryInventoryAdapter() {
        this(null, 10);
    }

    /**
     * 为订单预留库存
     *
     * @param orderId 订单唯一标识符
     * @param items 订单项列表
     * @return 预留ID
     * @throws InventoryException 当库存不足时抛出
     */
    @Override
    public String reserve(String orderId, List<OrderItem> items) throws InventoryException {
        // 检查所有商品库存是否充足
        for (OrderItem item : items) {
            Integer available = inventory.getOrDefault(item.getProductId(), 0);
            if (available < item.getQuantity()) {
                throw new InventoryException(
                    String.format("库存不足: productId=%s, 需要=%d, 可用=%d",
                        item.getProductId(), item.getQuantity(), available)
                );
            }
        }

        // 原子性地扣减库存
        for (OrderItem item : items) {
            inventory.computeIfPresent(item.getProductId(), (k, v) -> v - item.getQuantity());
        }

        // 创建预留记录
        String reservationId = "RES-" + UUID.randomUUID();
        reservations.put(reservationId, new Reservation(orderId, items));

        return reservationId;
    }

    /**
     * 释放指定的库存预留
     *
     * @param reservationId 预留ID
     */
    @Override
    public void release(String reservationId) {
        Reservation reservation = reservations.remove(reservationId);
        if (reservation == null) {
            return; // 幂等性：重复释放不报错
        }

        // 恢复库存
        for (OrderItem item : reservation.items) {
            inventory.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
    }

    /**
     * 查询低库存商品列表
     *
     * @return 库存低于阈值的商品ID列表
     */
    @Override
    public List<String> checkLowStock() {
        return inventory.entrySet().stream()
            .filter(entry -> entry.getValue() < lowStockThreshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 批量更新库存数量
     *
     * @param updates 库存更新列表
     */
    @Override
    public void updateStock(List<StockUpdate> updates) {
        for (StockUpdate update : updates) {
            inventory.merge(update.getProductId(), update.getDelta(), Integer::sum);
        }
    }

    /**
     * 清空所有库存记录（用于测试）
     */
    public void clear() {
        inventory.clear();
        reservations.clear();
    }

    /**
     * 获取商品当前库存（用于测试）
     */
    public int getStock(String productId) {
        return inventory.getOrDefault(productId, 0);
    }

    /**
     * 获取预留数量（用于测试）
     */
    public int getReservationCount() {
        return reservations.size();
    }

    // ==================== 内部类 ====================

    /**
     * 库存预留记录
     */
    private static class Reservation {
        final String orderId;
        final List<OrderItem> items;

        Reservation(String orderId, List<OrderItem> items) {
            this.orderId = orderId;
            this.items = new ArrayList<>(items);
        }
    }
}
