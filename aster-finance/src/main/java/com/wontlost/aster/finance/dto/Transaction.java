package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Transaction（模块 aster.finance.fraud）。
 */
public record Transaction(
  @NotNull
  String transactionId,
  @NotNull
  String accountId,
  int amount,
  int timestamp
) {}
