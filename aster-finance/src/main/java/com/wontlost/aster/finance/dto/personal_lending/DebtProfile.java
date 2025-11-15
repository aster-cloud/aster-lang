package com.wontlost.aster.finance.dto.personal_lending;

/**
 * Aster DSL 自动生成 DTO：DebtProfile（模块 aster.finance.personal_lending）。
 */
public record DebtProfile(
  int totalMonthlyDebt,
  int mortgagePayment,
  int carPayment,
  int studentLoanPayment,
  int creditCardMinPayment,
  int otherDebtPayment
) {}
