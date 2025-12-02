package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.dto.fraud.AccountHistory;
import com.wontlost.aster.finance.dto.fraud.FraudResult;
import com.wontlost.aster.finance.dto.fraud.Transaction;

import java.util.Objects;

/**
 * 欺詐檢測引擎：依照 fraud.aster DSL 規則輸出 FraudResult
 */
public class FraudDetectionEngine {

    private static final int LARGE_TRANSACTION_THRESHOLD = 1_000_000;
    private static final int HISTORY_THRESHOLD = 5;
    private static final int NEW_ACCOUNT_AGE = 30;

    public FraudResult detectFraud(Transaction transaction, AccountHistory history) {
        Objects.requireNonNull(transaction, "Transaction 不能為 null");
        Objects.requireNonNull(history, "AccountHistory 不能為 null");

        if (transaction.amount() > LARGE_TRANSACTION_THRESHOLD) {
            return new FraudResult(true, 100, "Extremely large transaction");
        }
        if (history.suspiciousCount() > HISTORY_THRESHOLD) {
            return new FraudResult(true, 85, "High suspicious activity history");
        }
        if (history.accountAge() < NEW_ACCOUNT_AGE) {
            return new FraudResult(true, 70, "New account risk");
        }
        return new FraudResult(false, 10, "Normal transaction");
    }
}
