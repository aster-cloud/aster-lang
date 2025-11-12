package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：PolicyRequest（模块 aster.insurance.life）。
 */
public record PolicyRequest(
  int coverageAmount,
  int termYears,
  @NotNull
  String policyType
) {}
