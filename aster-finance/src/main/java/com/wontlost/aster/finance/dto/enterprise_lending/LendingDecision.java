package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LendingDecision（模块 aster.finance.enterprise_lending）。
 */
public record LendingDecision(
  boolean approved,
  int approvedAmount,
  int interestRateBps,
  int termMonths,
  int collateralRequired,
  @NotNull
  String specialConditions,
  @NotNull
  String riskCategory,
  int confidenceScore,
  @NotNull
  String reasonCode,
  @NotNull
  String detailedAnalysis
) {}
