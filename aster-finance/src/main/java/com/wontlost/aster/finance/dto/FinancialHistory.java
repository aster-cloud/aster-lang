package com.wontlost.aster.finance.dto;

/**
 * Aster DSL 自动生成 DTO：FinancialHistory（模块 aster.finance.creditcard）。
 */
public record FinancialHistory(
  int bankruptcyCount,
  int latePayments,
  int utilization,
  int accountAge,
  int hardInquiries
) {}
