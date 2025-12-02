package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：LiquidityAnalysis（模块 aster.finance.enterprise_lending）。
 */
public record LiquidityAnalysis(
  int currentRatio,
  int quickRatio,
  boolean isHealthy,
  @NotNull
  String recommendation
) {}
