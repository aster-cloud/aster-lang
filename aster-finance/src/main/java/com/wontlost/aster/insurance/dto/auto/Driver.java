package com.wontlost.aster.insurance.dto.auto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Driver（模块 aster.insurance.auto）。
 */
public record Driver(
  @NotNull
  String driverId,
  int age,
  int yearsLicensed,
  int accidentCount,
  int violationCount,
  int creditScore
) {}
