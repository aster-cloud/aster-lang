package com.wontlost.aster.finance.dto.enterprise_lending;

import jakarta.validation.constraints.NotNull;

/**
 * Aster DSL 自动生成 DTO：EnterpriseInfo（模块 aster.finance.enterprise_lending）。
 */
public record EnterpriseInfo(
  @NotNull
  String companyId,
  @NotNull
  String companyName,
  @NotNull
  String industry,
  int yearsInBusiness,
  int employeeCount,
  int annualRevenue,
  int revenueGrowthRate,
  int profitMargin
) {}
