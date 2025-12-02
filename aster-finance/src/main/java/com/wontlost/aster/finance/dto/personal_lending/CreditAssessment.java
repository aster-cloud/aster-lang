package com.wontlost.aster.finance.dto.personal_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：CreditAssessment（模块 aster.finance.personal_lending）。
 */
public record CreditAssessment(
  @NotNull
  String scoreGrade,
  @NotNull
  String historyGrade,
  int utilizationRatio,
  int negativeMarks,
  @NotNull
  String riskIndicator,
  int maxLoanAmount
) {}
