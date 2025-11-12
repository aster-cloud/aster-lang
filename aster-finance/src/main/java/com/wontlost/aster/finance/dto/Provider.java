package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Provider（模块 aster.healthcare.claims）。
 */
public record Provider(
  @NotNull
  String providerId,
  boolean inNetwork,
  @NotNull
  String specialtyType,
  int qualityScore
) {}
