package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Patient（模块 aster.healthcare.eligibility）。
 */
public record Patient(
  @NotNull
  String patientId,
  int age,
  boolean hasInsurance,
  @NotNull
  String insuranceType,
  int chronicConditions
) {}
