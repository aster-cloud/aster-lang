package aster.ecommerce;

/**
 * 履约服务接口
 *
 * 提供订单履约能力，包括创建发货单、取消发货等操作。
 *
 * 主要职责：
 * - 为已支付订单创建发货单
 * - 取消未发货的发货单
 * - 跟踪发货状态
 */
public interface FulfillmentService {

    /**
     * 为订单创建发货单
     *
     * @param orderId 订单唯一标识符
     * @return 发货单ID，用于后续跟踪和取消操作
     * @throws FulfillmentException 当创建发货单失败时抛出，如订单不存在或未支付
     */
    String createShipment(String orderId) throws FulfillmentException;

    /**
     * 取消指定的发货单
     *
     * 仅当发货单尚未实际发货时可以取消。
     *
     * @param shipmentId 发货单ID（由 createShipment 方法返回）
     * @throws FulfillmentException 当取消失败时抛出，如发货单已发货
     */
    void cancelShipment(String shipmentId) throws FulfillmentException;

    /**
     * 履约异常
     *
     * 封装履约过程中的各类错误，如订单状态不正确、发货单已发货等。
     */
    class FulfillmentException extends Exception {
        private static final long serialVersionUID = 1L;

        public FulfillmentException(String message) {
            super(message);
        }

        public FulfillmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
