package com.wontlost.aster.finance.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wontlost.aster.finance.dto.fraud.AccountHistory;
import com.wontlost.aster.finance.dto.fraud.FraudResult;
import com.wontlost.aster.finance.dto.fraud.Transaction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FraudDetectionEngine Golden 測試
 */
class FraudDetectionGoldenTest {

    private final FraudDetectionEngine engine = new FraudDetectionEngine();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 低風險案例應吻合() throws IOException {
        verifyGolden("fraud-lowrisk.json");
    }

    @Test
    void 中風險案例應吻合() throws IOException {
        verifyGolden("fraud-mediumrisk.json");
    }

    @Test
    void 高風險案例應吻合() throws IOException {
        verifyGolden("fraud-highrisk.json");
    }

    private void verifyGolden(String filename) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("golden/" + filename);
        assertThat(stream).as("Golden file " + filename).isNotNull();

        JsonNode node = mapper.readTree(stream);
        Transaction transaction = toTransaction(node.get("transaction"));
        AccountHistory history = toHistory(node.get("history"));
        JsonNode expected = node.get("expectedResult");

        FraudResult result = engine.detectFraud(transaction, history);

        assertThat(result.isSuspicious()).isEqualTo(expected.get("isSuspicious").asBoolean());
        assertThat(result.riskScore()).isEqualTo(expected.get("riskScore").asInt());
        assertThat(result.reason()).isEqualTo(expected.get("reason").asText());
    }

    private Transaction toTransaction(JsonNode node) {
        return new Transaction(
            node.get("transactionId").asText(),
            node.get("accountId").asText(),
            node.get("amount").asInt(),
            node.get("timestamp").asInt()
        );
    }

    private AccountHistory toHistory(JsonNode node) {
        return new AccountHistory(
            node.get("accountId").asText(),
            node.get("averageAmount").asInt(),
            node.get("suspiciousCount").asInt(),
            node.get("accountAge").asInt(),
            node.get("lastTimestamp").asInt()
        );
    }
}
