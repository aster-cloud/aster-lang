package io.aster.ecommerce.rest.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 订单行项目 DTO。
 *
 * 描述单个 SKU 的数量与价格信息。
 */
public record OrderItem(
    @NotBlank(message = "productId 不能为空")
    String productId,
    @Positive(message = "quantity 必须大于 0")
    int quantity,
    @DecimalMin(value = "0.0", inclusive = false, message = "price 必须大于 0")
    BigDecimal price
) { }
