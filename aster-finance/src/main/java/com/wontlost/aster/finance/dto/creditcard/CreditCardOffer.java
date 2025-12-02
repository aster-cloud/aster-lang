package com.wontlost.aster.finance.dto.creditcard;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：CreditCardOffer（模块 aster.finance.creditcard）。
 */
public record CreditCardOffer(
  @NotNull
  String productType,
  int requestedLimit,
  boolean hasRewards,
  int annualFee
) {}
