package com.wontlost.aster.finance.dto.fraud;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：AccountHistory（模块 aster.finance.fraud）。
 */
public record AccountHistory(
  @NotNull
  String accountId,
  int averageAmount,
  int suspiciousCount,
  int accountAge,
  int lastTimestamp
) {}
