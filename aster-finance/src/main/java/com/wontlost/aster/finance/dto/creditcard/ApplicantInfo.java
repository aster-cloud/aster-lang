package com.wontlost.aster.finance.dto.creditcard;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：ApplicantInfo（模块 aster.finance.creditcard）。
 */
public record ApplicantInfo(
  @NotNull
  String applicantId,
  int age,
  int annualIncome,
  int creditScore,
  int existingCreditCards,
  int monthlyRent,
  @NotNull
  String employmentStatus,
  int yearsAtCurrentJob
) {}
