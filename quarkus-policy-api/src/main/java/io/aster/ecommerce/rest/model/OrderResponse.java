package io.aster.ecommerce.rest.model;

/**
 * 订单提交响应 DTO。
 *
 * 返回 workflowId、状态与提示信息，便于客户端跟踪执行。
 */
public record OrderResponse(
    String orderId,
    String workflowId,
    String status,
    long timestamp,
    String message
) {

    /**
     * 构造成功响应。
     *
     * @param orderId   订单号
     * @param workflowId workflow 标识
     * @return 成功响应
     */
    public static OrderResponse success(String orderId, String workflowId) {
        return new OrderResponse(
            orderId,
            workflowId,
            "SCHEDULED",
            System.currentTimeMillis(),
            "订单已成功提交并进入履约流程"
        );
    }

    /**
     * 构造错误响应。
     *
     * @param orderId 订单号
     * @param message 错误信息
     * @return 错误响应
     */
    public static OrderResponse error(String orderId, String message) {
        return new OrderResponse(
            orderId,
            null,
            "ERROR",
            System.currentTimeMillis(),
            message == null ? "未知错误" : message
        );
    }
}
