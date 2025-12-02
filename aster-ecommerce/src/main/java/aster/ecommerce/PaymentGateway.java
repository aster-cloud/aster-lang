package aster.ecommerce;

import java.math.BigDecimal;

/**
 * 支付网关接口
 *
 * 提供支付、退款等核心支付能力，支持幂等性操作。
 *
 * 主要职责：
 * - 处理订单支付请求
 * - 执行支付退款操作
 * - 提供支付状态查询
 */
public interface PaymentGateway {

    /**
     * 对订单进行扣款
     *
     * @param orderId 订单唯一标识符
     * @param amount 扣款金额，必须大于 0
     * @return 支付交易ID，用于后续退款操作
     * @throws PaymentException 当支付失败时抛出，包含失败原因
     */
    String charge(String orderId, BigDecimal amount) throws PaymentException;

    /**
     * 退款指定的支付交易
     *
     * @param paymentId 支付交易ID（由 charge 方法返回）
     * @return 退款交易ID
     * @throws PaymentException 当退款失败时抛出，包含失败原因
     */
    String refund(String paymentId) throws PaymentException;

    /**
     * 尝试退款（best-effort），不抛出异常
     *
     * 用于补偿逻辑中，即使退款失败也不中断流程。
     *
     * @param orderId 订单唯一标识符
     * @return true 表示退款成功或订单未支付，false 表示退款失败
     */
    boolean attemptRefund(String orderId);

    /**
     * 支付异常
     *
     * 封装支付和退款过程中的各类错误。
     */
    class PaymentException extends Exception {
        private static final long serialVersionUID = 1L;

        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
