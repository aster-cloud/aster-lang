package com.wontlost.aster.finance.properties;

import com.wontlost.aster.finance.dto.fraud.AccountHistory;
import com.wontlost.aster.finance.dto.fraud.FraudResult;
import com.wontlost.aster.finance.dto.fraud.Transaction;
import com.wontlost.aster.finance.policies.FraudDetectionEngine;
import net.jqwik.api.*;
import net.jqwik.api.Combinators;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FraudDetectionEngine 屬性測試
 */
class FraudDetectionPropertiesTest {

    private static final Set<Integer> EXPECTED_SCORES = Set.of(10, 70, 85, 100);

    private final FraudDetectionEngine engine = new FraudDetectionEngine();

    @Provide
    Arbitrary<Transaction> transactions() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(12);
        Arbitrary<String> accountIds = Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(10);
        Arbitrary<Integer> amounts = Arbitraries.integers().between(100, 2_000_000);
        Arbitrary<Integer> timestamps = Arbitraries.integers().between(0, Integer.MAX_VALUE);

        return Combinators.combine(ids, accountIds, amounts, timestamps)
            .as(Transaction::new);
    }

    @Provide
    Arbitrary<AccountHistory> histories() {
        Arbitrary<String> accountIds = Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(10);
        Arbitrary<Integer> average = Arbitraries.integers().between(100, 100_000);
        Arbitrary<Integer> suspicious = Arbitraries.integers().between(0, 10);
        Arbitrary<Integer> accountAge = Arbitraries.integers().between(0, 365);
        Arbitrary<Integer> lastTimestamp = Arbitraries.integers().between(0, Integer.MAX_VALUE);

        return Combinators.combine(accountIds, average, suspicious, accountAge, lastTimestamp)
            .as(AccountHistory::new);
    }

    @Property
    void DSL僅產生限定分數(@ForAll("transactions") Transaction transaction, @ForAll("histories") AccountHistory history) {
        FraudResult result = engine.detectFraud(transaction, history);

        assertThat(EXPECTED_SCORES).contains(result.riskScore());
        if (!result.isSuspicious()) {
            assertThat(result.riskScore()).isEqualTo(10);
            assertThat(result.reason()).isEqualTo("Normal transaction");
        }
    }

    @Property
    void 大額交易優先觸發最高分(@ForAll("transactions") Transaction base, @ForAll("histories") AccountHistory history) {
        Transaction extreme = new Transaction(base.transactionId(), base.accountId(), 2_000_001, base.timestamp());
        FraudResult result = engine.detectFraud(extreme, history);

        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.reason()).isEqualTo("Extremely large transaction");
    }
}
