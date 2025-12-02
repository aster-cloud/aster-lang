package com.wontlost.aster.finance.dto.personal_lending;

import io.aster.validation.constraints.Range;

/**
 * Aster DSL 自动生成 DTO：CreditProfile（模块 aster.finance.personal_lending）。
 */
public record CreditProfile(
  @Range(min = 300, max = 850)
  int creditScore,
  @Range(min = 0, max = 600)
  int creditHistory,
  int activeLoans,
  int creditCardCount,
  @Range(min = 0, max = 100)
  int creditUtilization,
  int latePayments,
  int defaults,
  int bankruptcies,
  int inquiries
) {}
