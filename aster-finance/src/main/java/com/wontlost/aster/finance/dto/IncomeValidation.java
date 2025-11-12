package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：IncomeValidation（模块 aster.finance.creditcard）。
 */
public record IncomeValidation(
  boolean sufficient,
  int ratio,
  @NotNull
  String recommendation
) {}
