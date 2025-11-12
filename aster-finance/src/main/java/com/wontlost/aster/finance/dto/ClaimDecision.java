package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：ClaimDecision（模块 aster.healthcare.claims）。
 */
public record ClaimDecision(
  boolean approved,
  @NotNull
  String reason,
  int approvedAmount,
  @NotNull
  String denialCode,
  boolean requiresReview
) {}
