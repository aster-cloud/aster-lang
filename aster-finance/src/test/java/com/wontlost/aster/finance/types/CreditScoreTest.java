package com.wontlost.aster.finance.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CreditScoreTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateValidCreditScore() {
        CreditScore score = new CreditScore(700);

        assertThat(score.value()).isEqualTo(700);
    }

    @Test
    void shouldAcceptMinScore() {
        CreditScore score = new CreditScore(CreditScore.MIN_SCORE);

        assertThat(score.value()).isEqualTo(300);
    }

    @Test
    void shouldAcceptMaxScore() {
        CreditScore score = new CreditScore(CreditScore.MAX_SCORE);

        assertThat(score.value()).isEqualTo(850);
    }

    @Test
    void shouldRejectScoreBelowMin() {
        assertThatThrownBy(() -> new CreditScore(299))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be between 300 and 850");
    }

    @Test
    void shouldRejectScoreAboveMax() {
        assertThatThrownBy(() -> new CreditScore(851))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be between 300 and 850");
    }

    @Test
    void shouldIdentifyExcellentScore() {
        assertThat(new CreditScore(800).isExcellent()).isTrue();
        assertThat(new CreditScore(850).isExcellent()).isTrue();
        assertThat(new CreditScore(799).isExcellent()).isFalse();
    }

    @Test
    void shouldIdentifyVeryGoodScore() {
        assertThat(new CreditScore(740).isVeryGood()).isTrue();
        assertThat(new CreditScore(799).isVeryGood()).isTrue();
        assertThat(new CreditScore(739).isVeryGood()).isFalse();
    }

    @Test
    void shouldIdentifyGoodScore() {
        assertThat(new CreditScore(670).isGood()).isTrue();
        assertThat(new CreditScore(739).isGood()).isTrue();
        assertThat(new CreditScore(669).isGood()).isFalse();
    }

    @Test
    void shouldIdentifyFairScore() {
        assertThat(new CreditScore(580).isFair()).isTrue();
        assertThat(new CreditScore(669).isFair()).isTrue();
        assertThat(new CreditScore(579).isFair()).isFalse();
    }

    @Test
    void shouldIdentifyPoorScore() {
        assertThat(new CreditScore(579).isPoor()).isTrue();
        assertThat(new CreditScore(300).isPoor()).isTrue();
        assertThat(new CreditScore(580).isPoor()).isFalse();
    }

    @Test
    void shouldGetCorrectRating() {
        assertThat(new CreditScore(850).getRating()).isEqualTo("优秀");
        assertThat(new CreditScore(750).getRating()).isEqualTo("非常好");
        assertThat(new CreditScore(700).getRating()).isEqualTo("良好");
        assertThat(new CreditScore(600).getRating()).isEqualTo("一般");
        assertThat(new CreditScore(500).getRating()).isEqualTo("较差");
    }

    @Test
    void shouldGetCorrectEnglishRating() {
        assertThat(new CreditScore(850).getRatingEnglish()).isEqualTo("Excellent");
        assertThat(new CreditScore(750).getRatingEnglish()).isEqualTo("Very Good");
        assertThat(new CreditScore(700).getRatingEnglish()).isEqualTo("Good");
        assertThat(new CreditScore(600).getRatingEnglish()).isEqualTo("Fair");
        assertThat(new CreditScore(500).getRatingEnglish()).isEqualTo("Poor");
    }

    @Test
    void shouldCalculateGapToTarget() {
        CreditScore current = new CreditScore(650);
        CreditScore target = new CreditScore(750);

        assertThat(current.gapTo(target)).isEqualTo(100);
        assertThat(target.gapTo(current)).isEqualTo(-100);
    }

    @Test
    void shouldCompareScores() {
        CreditScore low = new CreditScore(600);
        CreditScore high = new CreditScore(700);
        CreditScore same = new CreditScore(600);

        assertThat(high.compareTo(low)).isPositive();
        assertThat(low.compareTo(high)).isNegative();
        assertThat(low.compareTo(same)).isZero();
    }

    @Test
    void shouldCheckIfHigherThan() {
        CreditScore low = new CreditScore(600);
        CreditScore high = new CreditScore(700);

        assertThat(high.isHigherThan(low)).isTrue();
        assertThat(low.isHigherThan(high)).isFalse();
        assertThat(low.isHigherThan(new CreditScore(600))).isFalse();
    }

    @Test
    void shouldCheckIfLowerThan() {
        CreditScore low = new CreditScore(600);
        CreditScore high = new CreditScore(700);

        assertThat(low.isLowerThan(high)).isTrue();
        assertThat(high.isLowerThan(low)).isFalse();
        assertThat(low.isLowerThan(new CreditScore(600))).isFalse();
    }

    @Test
    void shouldRecommendCorrectRiskLevel() {
        assertThat(new CreditScore(850).recommendRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(new CreditScore(750).recommendRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(new CreditScore(700).recommendRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(new CreditScore(600).recommendRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(new CreditScore(500).recommendRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldFormatToString() {
        CreditScore score = new CreditScore(700);

        assertThat(score.toString()).isEqualTo("700 (良好)");
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        CreditScore score = new CreditScore(700);

        String json = objectMapper.writeValueAsString(score);

        assertThat(json).isEqualTo("700");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = "700";

        CreditScore score = objectMapper.readValue(json, CreditScore.class);

        assertThat(score.value()).isEqualTo(700);
    }

    @Test
    void shouldRejectInvalidJsonValue() {
        assertThatThrownBy(() -> objectMapper.readValue("900", CreditScore.class))
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
