package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：PolicyQuote（模块 aster.insurance.auto）。
 */
public record PolicyQuote(
  boolean approved,
  @NotNull
  String reason,
  int monthlyPremium,
  int deductible,
  int coverageLimit
) {}
