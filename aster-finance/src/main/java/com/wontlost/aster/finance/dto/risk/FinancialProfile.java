package com.wontlost.aster.finance.dto.risk;

import io.aster.validation.constraints.Range;

/**
 * Aster DSL 自动生成 DTO：FinancialProfile（模块 aster.finance.risk）。
 */
public record FinancialProfile(
  int totalAssets,
  int totalLiabilities,
  int monthlyIncome,
  int monthlyExpenses,
  @Range(min = 300, max = 850)
  int creditScore
) {}
