package io.aster.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DeterminismSnapshotTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void testSerializeDeserialize() throws Exception {
        ReplayDeterministicClock clock = new ReplayDeterministicClock();
        Instant t1 = Instant.parse("2025-01-10T08:00:00Z");
        Instant t2 = Instant.parse("2025-01-10T08:00:01Z");
        clock.recordTimeDecision(t1);
        clock.recordTimeDecision(t2);

        ReplayDeterministicUuid uuid = new ReplayDeterministicUuid();
        UUID u1 = uuid.randomUUID();
        UUID u2 = uuid.randomUUID();

        ReplayDeterministicRandom random = new ReplayDeterministicRandom();
        long r1 = random.nextLong("alpha");
        long r2 = random.nextLong("alpha");

        DeterminismSnapshot snapshot = DeterminismSnapshot.from(clock, uuid, random);
        String json = mapper.writeValueAsString(snapshot);

        DeterminismSnapshot restored = mapper.readValue(json, DeterminismSnapshot.class);
        assertThat(restored.getClockTimes()).containsExactly(t1.toString(), t2.toString());
        assertThat(restored.getUuids()).containsExactly(u1.toString(), u2.toString());
        assertThat(restored.getRandoms()).containsEntry("alpha", List.of(r1, r2));
    }

    @Test
    void testBackwardCompatibilityClockOnly() throws Exception {
        String legacyJson = """
                {
                  "recordedTimes": [
                    "2025-02-01T00:00:00Z",
                    "2025-02-01T00:00:01Z"
                  ],
                  "replayIndex": 0,
                  "replayMode": false,
                  "version": 1
                }
                """;
        DeterminismSnapshot snapshot = mapper.readValue(legacyJson, DeterminismSnapshot.class);
        assertThat(snapshot.getClockTimes()).hasSize(2);

        ReplayDeterministicClock clock = new ReplayDeterministicClock();
        snapshot.applyTo(clock, null, null);

        assertThat(clock.now()).isEqualTo(Instant.parse("2025-02-01T00:00:00Z"));
        assertThat(clock.now()).isEqualTo(Instant.parse("2025-02-01T00:00:01Z"));
    }

    @Test
    void testBackwardCompatibilityNullFields() {
        DeterminismSnapshot snapshot = new DeterminismSnapshot();
        ReplayDeterministicClock clock = new ReplayDeterministicClock();
        ReplayDeterministicUuid uuid = new ReplayDeterministicUuid();
        ReplayDeterministicRandom random = new ReplayDeterministicRandom();

        assertDoesNotThrow(() -> snapshot.applyTo(clock, uuid, random));
        assertThat(clock.getRecordedTimes()).isEmpty();
        assertThat(uuid.getRecordedUuids()).isEmpty();
        assertThat(random.getRecordedRandoms()).isEmpty();
    }

    @Test
    void testSizeLimitClocks() {
        ReplayDeterministicClock clock = new ReplayDeterministicClock();
        for (int i = 0; i < 600; i++) {
            clock.recordTimeDecision(Instant.ofEpochSecond(1_700_000_000L + i));
        }

        DeterminismSnapshot snapshot = DeterminismSnapshot.from(clock, null, null);
        assertThat(snapshot.getClockTimes()).hasSize(500);
        assertThat(snapshot.getClockTimes().get(0))
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L).toString());
        assertThat(snapshot.getClockTimes().get(499))
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L + 499).toString());
    }

    @Test
    void testSizeLimitUuids() {
        ReplayDeterministicUuid uuid = new ReplayDeterministicUuid();
        List<String> recorded = new ArrayList<>();
        for (int i = 0; i < 700; i++) {
            recorded.add(uuid.randomUUID().toString());
        }

        DeterminismSnapshot snapshot = DeterminismSnapshot.from(null, uuid, null);
        assertThat(snapshot.getUuids()).hasSize(500);
        assertThat(snapshot.getUuids()).containsExactlyElementsOf(recorded.subList(0, 500));
    }

    @Test
    void testSizeLimitRandoms() {
        ReplayDeterministicRandom random = new ReplayDeterministicRandom();
        Map<String, List<Long>> expected = new LinkedHashMap<>();
        expected.put("alpha", new ArrayList<>());
        expected.put("beta", new ArrayList<>());

        for (int i = 0; i < 150; i++) {
            long alpha = random.nextLong("alpha");
            expected.get("alpha").add(alpha);
            long beta = random.nextLong("beta");
            expected.get("beta").add(beta);
        }

        DeterminismSnapshot snapshot = DeterminismSnapshot.from(null, null, random);
        assertThat(snapshot.getRandoms()).containsKeys("alpha", "beta");
        assertThat(snapshot.getRandoms().get("alpha"))
                .containsExactlyElementsOf(expected.get("alpha").subList(0, 100));
        assertThat(snapshot.getRandoms().get("beta"))
                .containsExactlyElementsOf(expected.get("beta").subList(0, 100));
    }

    @Test
    void testApplyToFacades() {
        DeterminismSnapshot snapshot = new DeterminismSnapshot();
        snapshot.setClockTimes(List.of(
                "2025-03-01T00:00:00Z",
                "2025-03-01T00:00:01Z"
        ));
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        snapshot.setUuids(List.of(u1.toString(), u2.toString()));
        Map<String, List<Long>> randoms = new LinkedHashMap<>();
        randoms.put("alpha", List.of(10L, 20L));
        snapshot.setRandoms(randoms);

        ReplayDeterministicClock clock = new ReplayDeterministicClock();
        ReplayDeterministicUuid uuid = new ReplayDeterministicUuid();
        ReplayDeterministicRandom random = new ReplayDeterministicRandom();
        snapshot.applyTo(clock, uuid, random);

        assertThat(clock.now()).isEqualTo(Instant.parse("2025-03-01T00:00:00Z"));
        assertThat(clock.now()).isEqualTo(Instant.parse("2025-03-01T00:00:01Z"));

        assertThat(uuid.randomUUID()).isEqualTo(u1);
        assertThat(uuid.randomUUID()).isEqualTo(u2);

        assertThat(random.nextLong("alpha")).isEqualTo(10L);
        assertThat(random.nextLong("alpha")).isEqualTo(20L);
    }

    @Test
    void testDeprecatedAlias() throws Exception {
        ClockTimesSnapshot snapshot = new ClockTimesSnapshot();
        snapshot.setClockTimes(List.of("2025-04-01T00:00:00Z"));

        String json = mapper.writeValueAsString(snapshot);
        DeterminismSnapshot restored = mapper.readValue(json, DeterminismSnapshot.class);
        assertThat(restored.getClockTimes()).containsExactly("2025-04-01T00:00:00Z");
    }
}
