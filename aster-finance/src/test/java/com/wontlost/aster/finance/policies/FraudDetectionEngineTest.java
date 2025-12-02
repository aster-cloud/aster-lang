package com.wontlost.aster.finance.policies;

import com.wontlost.aster.finance.dto.fraud.AccountHistory;
import com.wontlost.aster.finance.dto.fraud.FraudResult;
import com.wontlost.aster.finance.dto.fraud.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FraudDetectionEngine 單元測試（對齊 fraud.aster）
 */
class FraudDetectionEngineTest {

    private FraudDetectionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FraudDetectionEngine();
    }

    @Test
    void 超大金額應標記極高風險() {
        FraudResult result = engine.detectFraud(
            transaction("tx-1", 1_500_000),
            history("acct-1", 0, 50)
        );

        assertThat(result.isSuspicious()).isTrue();
        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.reason()).isEqualTo("Extremely large transaction");
    }

    @Test
    void 可疑計數過多時返回85分() {
        FraudResult result = engine.detectFraud(
            transaction("tx-2", 500_000),
            history("acct-2", 6, 60)
        );

        assertThat(result.isSuspicious()).isTrue();
        assertThat(result.riskScore()).isEqualTo(85);
        assertThat(result.reason()).isEqualTo("High suspicious activity history");
    }

    @Test
    void 新帳戶會被標記為70分() {
        FraudResult result = engine.detectFraud(
            transaction("tx-3", 20_000),
            new AccountHistory("acct-3", 2_000, 1, 10, 100)
        );

        assertThat(result.isSuspicious()).isTrue();
        assertThat(result.riskScore()).isEqualTo(70);
        assertThat(result.reason()).isEqualTo("New account risk");
    }

    @Test
    void 正常交易返回低風險() {
        FraudResult result = engine.detectFraud(
            transaction("tx-4", 5_000),
            history("acct-4", 0, 200)
        );

        assertThat(result.isSuspicious()).isFalse();
        assertThat(result.riskScore()).isEqualTo(10);
        assertThat(result.reason()).isEqualTo("Normal transaction");
    }

    private Transaction transaction(String id, int amount) {
        return new Transaction(id, "acct-test", amount, 1_000);
    }

    private AccountHistory history(String accountId, int suspiciousCount, int accountAge) {
        return new AccountHistory(accountId, 5_000, suspiciousCount, accountAge, 100);
    }
}
