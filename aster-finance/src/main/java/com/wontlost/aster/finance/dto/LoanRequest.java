package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanRequest（模块 aster.finance.personal_lending）。
 */
public record LoanRequest(
  int requestedAmount,
  @NotNull
  String purpose,
  int termMonths,
  int downPayment,
  int collateralValue
) {}
