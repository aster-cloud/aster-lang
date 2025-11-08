package com.wontlost.aster.finance.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelTest {

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(RiskLevel.LOW.getDisplayName()).isEqualTo("低风险");
        assertThat(RiskLevel.MEDIUM.getDisplayName()).isEqualTo("中等风险");
        assertThat(RiskLevel.HIGH.getDisplayName()).isEqualTo("高风险");
        assertThat(RiskLevel.CRITICAL.getDisplayName()).isEqualTo("极高风险");
    }

    @Test
    void shouldHaveCorrectSeverityLevel() {
        assertThat(RiskLevel.LOW.getSeverityLevel()).isEqualTo(0);
        assertThat(RiskLevel.MEDIUM.getSeverityLevel()).isEqualTo(1);
        assertThat(RiskLevel.HIGH.getSeverityLevel()).isEqualTo(2);
        assertThat(RiskLevel.CRITICAL.getSeverityLevel()).isEqualTo(3);
    }

    @Test
    void shouldHaveCorrectRiskMultiplier() {
        assertThat(RiskLevel.LOW.getRiskMultiplier()).isEqualTo(1.0);
        assertThat(RiskLevel.MEDIUM.getRiskMultiplier()).isEqualTo(1.5);
        assertThat(RiskLevel.HIGH.getRiskMultiplier()).isEqualTo(2.0);
        assertThat(RiskLevel.CRITICAL.getRiskMultiplier()).isEqualTo(3.0);
    }

    @Test
    void shouldIdentifyHighRisk() {
        assertThat(RiskLevel.LOW.isHighRisk()).isFalse();
        assertThat(RiskLevel.MEDIUM.isHighRisk()).isFalse();
        assertThat(RiskLevel.HIGH.isHighRisk()).isTrue();
        assertThat(RiskLevel.CRITICAL.isHighRisk()).isTrue();
    }

    @Test
    void shouldIdentifyLowRisk() {
        assertThat(RiskLevel.LOW.isLowRisk()).isTrue();
        assertThat(RiskLevel.MEDIUM.isLowRisk()).isFalse();
        assertThat(RiskLevel.HIGH.isLowRisk()).isFalse();
        assertThat(RiskLevel.CRITICAL.isLowRisk()).isFalse();
    }

    @Test
    void shouldIdentifyAcceptableRisk() {
        assertThat(RiskLevel.LOW.isAcceptable()).isTrue();
        assertThat(RiskLevel.MEDIUM.isAcceptable()).isTrue();
        assertThat(RiskLevel.HIGH.isAcceptable()).isFalse();
        assertThat(RiskLevel.CRITICAL.isAcceptable()).isFalse();
    }

    @Test
    void shouldCompareMoreSevere() {
        assertThat(RiskLevel.HIGH.isMoreSevereThan(RiskLevel.MEDIUM)).isTrue();
        assertThat(RiskLevel.CRITICAL.isMoreSevereThan(RiskLevel.HIGH)).isTrue();
        assertThat(RiskLevel.LOW.isMoreSevereThan(RiskLevel.MEDIUM)).isFalse();
        assertThat(RiskLevel.MEDIUM.isMoreSevereThan(RiskLevel.MEDIUM)).isFalse();
    }

    @Test
    void shouldCompareLessSevere() {
        assertThat(RiskLevel.LOW.isLessSevereThan(RiskLevel.MEDIUM)).isTrue();
        assertThat(RiskLevel.MEDIUM.isLessSevereThan(RiskLevel.HIGH)).isTrue();
        assertThat(RiskLevel.HIGH.isLessSevereThan(RiskLevel.LOW)).isFalse();
        assertThat(RiskLevel.MEDIUM.isLessSevereThan(RiskLevel.MEDIUM)).isFalse();
    }
}
