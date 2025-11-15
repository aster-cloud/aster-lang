package com.wontlost.aster.finance.dto.enterprise_lending;

/**
 * Aster DSL 自动生成 DTO：BusinessHistory（模块 aster.finance.enterprise_lending）。
 */
public record BusinessHistory(
  int previousLoans,
  int defaultCount,
  int latePayments,
  int creditUtilization,
  int largestLoanAmount,
  int relationshipYears
) {}
