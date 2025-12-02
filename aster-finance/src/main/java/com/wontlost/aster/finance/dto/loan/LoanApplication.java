package com.wontlost.aster.finance.dto.loan;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanApplication（模块 aster.finance.loan）。
 */
public record LoanApplication(
  @NotNull
  @NotEmpty
  String applicantId,
  int amount,
  @Range(min = 0, max = 600)
  int termMonths,
  @NotNull
  @NotEmpty
  String purpose
) {}
