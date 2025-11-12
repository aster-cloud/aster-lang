package com.wontlost.aster.finance.dto;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanDecision（模块 aster.finance.loan）。
 */
public record LoanDecision(
  boolean approved,
  @NotNull
  @NotEmpty
  String reason,
  int approvedAmount,
  @Range(min = 0, max = 5000)
  int interestRateBps,
  @Range(min = 0, max = 600)
  int termMonths
) {}
