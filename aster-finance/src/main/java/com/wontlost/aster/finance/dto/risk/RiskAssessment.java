package com.wontlost.aster.finance.dto.risk;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：RiskAssessment（模块 aster.finance.risk）。
 */
public record RiskAssessment(
  @NotNull
  @NotEmpty
  String riskLevel,
  @Range(min = 0, max = 100)
  int riskScore,
  @NotNull
  @NotEmpty
  String recommendation
) {}
