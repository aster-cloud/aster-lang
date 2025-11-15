package com.wontlost.aster.finance.dto.creditcard;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：ApprovalDecision（模块 aster.finance.creditcard）。
 */
public record ApprovalDecision(
  boolean approved,
  @NotNull
  String reason,
  int approvedLimit,
  int interestRateAPR,
  int monthlyFee,
  int creditLine,
  boolean requiresDeposit,
  int depositAmount
) {}
