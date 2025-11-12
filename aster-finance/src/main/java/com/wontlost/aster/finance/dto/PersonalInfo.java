package com.wontlost.aster.finance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：PersonalInfo（模块 aster.finance.personal_lending）。
 */
public record PersonalInfo(
  @NotNull
  String applicantId,
  int age,
  @NotNull
  String educationLevel,
  @NotNull
  String employmentStatus,
  @NotNull
  String occupation,
  int yearsAtJob,
  int monthsAtAddress,
  @NotNull
  String maritalStatus,
  int dependents
) {}
