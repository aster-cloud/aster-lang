package io.aster.ecommerce.rest.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单提交请求 DTO。
 *
 * 包含下单所需的全部关键信息，用于驱动 fulfillment workflow。
 */
public record OrderRequest(
    @NotBlank(message = "orderId 不能为空")
    String orderId,
    @NotBlank(message = "customerId 不能为空")
    String customerId,
    @NotEmpty(message = "items 不能为空")
    List<@Valid OrderItem> items,
    Map<String, Object> metadata
) { }
