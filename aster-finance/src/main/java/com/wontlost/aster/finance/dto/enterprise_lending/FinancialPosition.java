package com.wontlost.aster.finance.dto.enterprise_lending;

/**
 * Aster DSL 自动生成 DTO：FinancialPosition（模块 aster.finance.enterprise_lending）。
 */
public record FinancialPosition(
  int totalAssets,
  int currentAssets,
  int totalLiabilities,
  int currentLiabilities,
  int equity,
  int cashFlow,
  int outstandingDebt
) {}
