package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：AffordabilityAnalysis（模块 aster.finance.personal_lending）。
 */
public record AffordabilityAnalysis(
  int debtToIncomeRatio,
  int residualIncome,
  @NotNull
  String affordabilityGrade,
  int maxMonthlyPayment
) {}
