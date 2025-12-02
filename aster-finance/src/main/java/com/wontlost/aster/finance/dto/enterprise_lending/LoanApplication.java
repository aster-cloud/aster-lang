package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanApplication（模块 aster.finance.enterprise_lending）。
 */
public record LoanApplication(
  int requestedAmount,
  @NotNull
  String loanPurpose,
  int termMonths,
  int collateralValue,
  int guarantorCount
) {}
