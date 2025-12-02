package com.wontlost.aster.finance.dto.loan;

import io.aster.validation.constraints.Range;

/**
 * Aster DSL 自动生成 DTO：ApplicantProfile（模块 aster.finance.loan）。
 */
public record ApplicantProfile(
  @Range(min = 18, max = 120)
  int age,
  @Range(min = 300, max = 850)
  int creditScore,
  int annualIncome,
  int monthlyDebt,
  @Range(min = 0, max = 50)
  int yearsEmployed
) {}
