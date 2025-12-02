package com.wontlost.aster.finance.dto.personal_lending;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LoanDecision（模块 aster.finance.personal_lending）。
 */
public record LoanDecision(
  boolean approved,
  int approvedAmount,
  @Range(min = 0, max = 5000)
  int interestRateBps,
  @Range(min = 0, max = 600)
  int termMonths,
  int monthlyPayment,
  int downPaymentRequired,
  @NotNull
  @NotEmpty
  String conditions,
  @NotNull
  @NotEmpty
  String riskLevel,
  int decisionScore,
  @NotNull
  @NotEmpty
  String reasonCode,
  @NotNull
  @NotEmpty
  String recommendations
) {}
