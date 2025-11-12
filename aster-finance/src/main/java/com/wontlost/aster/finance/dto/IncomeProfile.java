package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：IncomeProfile（模块 aster.finance.personal_lending）。
 */
public record IncomeProfile(
  int monthlyIncome,
  int additionalIncome,
  int spouseIncome,
  int rentIncome,
  @NotNull
  String incomeStability,
  int incomeGrowthRate
) {}
