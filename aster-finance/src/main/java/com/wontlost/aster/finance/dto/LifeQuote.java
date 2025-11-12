package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LifeQuote（模块 aster.insurance.life）。
 */
public record LifeQuote(
  boolean approved,
  @NotNull
  String reason,
  int monthlyPremium,
  int coverageAmount,
  int termYears
) {}
