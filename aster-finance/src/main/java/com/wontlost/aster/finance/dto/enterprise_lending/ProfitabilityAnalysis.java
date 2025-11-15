package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：ProfitabilityAnalysis（模块 aster.finance.enterprise_lending）。
 */
public record ProfitabilityAnalysis(
  int returnOnAssets,
  int returnOnEquity,
  @NotNull
  String profitabilityGrade,
  int repaymentCapacity
) {}
