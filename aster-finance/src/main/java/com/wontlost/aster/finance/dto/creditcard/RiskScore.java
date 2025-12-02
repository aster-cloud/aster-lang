package com.wontlost.aster.finance.dto.creditcard;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：RiskScore（模块 aster.finance.creditcard）。
 */
public record RiskScore(
  int score,
  @NotNull
  String category,
  @NotNull
  String factors
) {}
