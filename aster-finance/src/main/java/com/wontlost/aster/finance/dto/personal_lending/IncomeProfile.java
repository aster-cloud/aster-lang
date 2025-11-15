package com.wontlost.aster.finance.dto.personal_lending;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
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
  @NotEmpty
  String incomeStability,
  @Range(min = 0, max = 100)
  int incomeGrowthRate
) {}
