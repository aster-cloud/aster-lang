package com.wontlost.aster.insurance.dto.life;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：Applicant（模块 aster.insurance.life）。
 */
public record Applicant(
  @NotNull
  String applicantId,
  int age,
  @NotNull
  String gender,
  boolean smoker,
  int bmi,
  @NotNull
  String occupation,
  int healthScore
) {}
