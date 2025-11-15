package com.wontlost.aster.finance.dto.personal_lending;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanRequest（模块 aster.finance.personal_lending）。
 */
public record LoanRequest(
  int requestedAmount,
  @NotNull
  @NotEmpty
  String purpose,
  @Range(min = 0, max = 600)
  int termMonths,
  int downPayment,
  int collateralValue
) {}
