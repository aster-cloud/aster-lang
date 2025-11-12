package com.wontlost.aster.insurance.dto.life;

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
