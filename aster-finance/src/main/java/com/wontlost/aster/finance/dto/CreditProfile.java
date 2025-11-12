package com.wontlost.aster.finance.dto;

/**
 * Aster DSL 自动生成 DTO：CreditProfile（模块 aster.finance.personal_lending）。
 */
public record CreditProfile(
  int creditScore,
  int creditHistory,
  int activeLoans,
  int creditCardCount,
  int creditUtilization,
  int latePayments,
  int defaults,
  int bankruptcies,
  int inquiries
) {}
