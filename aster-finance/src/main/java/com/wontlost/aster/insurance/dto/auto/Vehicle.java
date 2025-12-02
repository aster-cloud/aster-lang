package com.wontlost.aster.insurance.dto.auto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Vehicle（模块 aster.insurance.auto）。
 */
public record Vehicle(
  @NotNull
  String vin,
  int year,
  @NotNull
  String make,
  @NotNull
  String model,
  int value,
  int safetyRating
) {}
