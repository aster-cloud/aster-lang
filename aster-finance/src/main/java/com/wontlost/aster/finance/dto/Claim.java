package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Claim（模块 aster.healthcare.claims）。
 */
public record Claim(
  @NotNull
  String claimId,
  @NotNull
  String patientId,
  @NotNull
  String providerId,
  @NotNull
  String serviceDate,
  int amount,
  @NotNull
  String diagnosisCode
) {}
