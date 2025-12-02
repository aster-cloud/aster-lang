package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LeverageAnalysis（模块 aster.finance.enterprise_lending）。
 */
public record LeverageAnalysis(
  int debtToEquity,
  int debtToAssets,
  @NotNull
  String riskLevel,
  int maxLoanCapacity
) {}
