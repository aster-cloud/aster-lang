package com.wontlost.aster.healthcare.dto.eligibility;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：EligibilityCheck（模块 aster.healthcare.eligibility）。
 */
public record EligibilityCheck(
  boolean eligible,
  @NotNull
  String reason,
  int coveragePercent,
  int estimatedCost,
  boolean requiresPreAuth
) {}
