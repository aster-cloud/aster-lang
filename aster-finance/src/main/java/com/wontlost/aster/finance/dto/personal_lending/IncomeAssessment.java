package com.wontlost.aster.finance.dto.personal_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：IncomeAssessment（模块 aster.finance.personal_lending）。
 */
public record IncomeAssessment(
  int totalMonthlyIncome,
  @NotNull
  String stabilityGrade,
  @NotNull
  String adequacy,
  int loanCapacity
) {}
