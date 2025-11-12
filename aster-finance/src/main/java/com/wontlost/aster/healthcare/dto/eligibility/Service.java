package com.wontlost.aster.healthcare.dto.eligibility;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Service（模块 aster.healthcare.eligibility）。
 */
public record Service(
  @NotNull
  String serviceCode,
  @NotNull
  String serviceName,
  int basePrice,
  boolean requiresPreAuth
) {}
