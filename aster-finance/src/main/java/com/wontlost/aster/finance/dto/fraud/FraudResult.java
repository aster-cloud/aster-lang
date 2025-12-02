package com.wontlost.aster.finance.dto.fraud;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：FraudResult（模块 aster.finance.fraud）。
 */
public record FraudResult(
  boolean isSuspicious,
  int riskScore,
  @NotNull
  String reason
) {}
