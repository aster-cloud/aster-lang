package aster.ecommerce.stub;

import aster.ecommerce.PaymentGateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付网关内存实现
 *
 * 用于测试和演示，所有支付数据存储在内存中。
 * 支持幂等性：相同 orderId 的重复支付请求返回相同的 paymentId。
 */
public class InMemoryPaymentGateway implements PaymentGateway {

    private final Map<String, PaymentRecord> payments = new ConcurrentHashMap<>();
    private final Map<String, String> orderToPayment = new ConcurrentHashMap<>();

    /**
     * 对订单进行扣款
     *
     * @param orderId 订单唯一标识符
     * @param amount 扣款金额，必须大于 0
     * @return 支付交易ID
     * @throws PaymentException 当金额无效时抛出
     */
    @Override
    public String charge(String orderId, BigDecimal amount) throws PaymentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("扣款金额必须大于 0，当前金额: " + amount);
        }

        // 幂等性检查：相同 orderId 返回相同 paymentId
        return orderToPayment.computeIfAbsent(orderId, key -> {
            String paymentId = "PAY-" + UUID.randomUUID();
            payments.put(paymentId, new PaymentRecord(orderId, amount, PaymentStatus.CHARGED));
            return paymentId;
        });
    }

    /**
     * 退款指定的支付交易
     *
     * @param paymentId 支付交易ID
     * @return 退款交易ID
     * @throws PaymentException 当支付不存在或已退款时抛出
     */
    @Override
    public String refund(String paymentId) throws PaymentException {
        PaymentRecord record = payments.get(paymentId);
        if (record == null) {
            throw new PaymentException("支付记录不存在: " + paymentId);
        }
        if (record.status == PaymentStatus.REFUNDED) {
            throw new PaymentException("支付已退款: " + paymentId);
        }

        // 更新状态为已退款
        record.status = PaymentStatus.REFUNDED;
        return "REFUND-" + UUID.randomUUID();
    }

    /**
     * 尝试退款（best-effort）
     *
     * @param orderId 订单唯一标识符
     * @return true 表示退款成功或订单未支付，false 表示退款失败
     */
    @Override
    public boolean attemptRefund(String orderId) {
        String paymentId = orderToPayment.get(orderId);
        if (paymentId == null) {
            return true; // 订单未支付，视为成功
        }

        try {
            refund(paymentId);
            return true;
        } catch (PaymentException e) {
            return false;
        }
    }

    /**
     * 清空所有支付记录（用于测试）
     */
    public void clear() {
        payments.clear();
        orderToPayment.clear();
    }

    /**
     * 获取支付记录数量（用于测试）
     */
    public int getPaymentCount() {
        return payments.size();
    }

    // ==================== 内部类 ====================

    /**
     * 支付记录
     */
    private static class PaymentRecord {
        final String orderId;
        final BigDecimal amount;
        PaymentStatus status;

        PaymentRecord(String orderId, BigDecimal amount, PaymentStatus status) {
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
    }

    /**
     * 支付状态
     */
    private enum PaymentStatus {
        CHARGED,   // 已扣款
        REFUNDED   // 已退款
    }
}
