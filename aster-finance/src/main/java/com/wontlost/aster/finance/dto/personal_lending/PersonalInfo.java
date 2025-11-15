package com.wontlost.aster.finance.dto.personal_lending;

import io.aster.validation.constraints.NotEmpty;
import io.aster.validation.constraints.Range;
import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：PersonalInfo（模块 aster.finance.personal_lending）。
 */
public record PersonalInfo(
  @NotNull
  @NotEmpty
  String applicantId,
  @Range(min = 18, max = 120)
  int age,
  @NotNull
  @NotEmpty
  String educationLevel,
  @NotNull
  @NotEmpty
  String employmentStatus,
  @NotNull
  @NotEmpty
  String occupation,
  @Range(min = 0, max = 50)
  int yearsAtJob,
  @Range(min = 0, max = 600)
  int monthsAtAddress,
  @NotNull
  @NotEmpty
  String maritalStatus,
  int dependents
) {}
