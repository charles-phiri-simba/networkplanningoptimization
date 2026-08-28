package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.sync.SynchronizationFreshnessEvaluator;
import com.simba.snip.npo.integration.sync.SynchronizationPolicy;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizationEvaluatorUnitTest {

    private final SynchronizationFreshnessEvaluator freshnessEvaluator = new SynchronizationFreshnessEvaluator();
    private final NetworkKnowledgeConfidenceEvaluator confidenceEvaluator = new NetworkKnowledgeConfidenceEvaluator();

    private static SynchronizationPolicy policy() {
        return new SynchronizationPolicy(
                "ERICSSON_ENM_SIMULATOR",
                "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER",
                "DEFAULT",
                true,
                SynchronizationMode.INCREMENTAL,
                Duration.ofMinutes(15),
                Duration.ofSeconds(2),
                Duration.ofSeconds(30),
                3,
                Duration.ofMinutes(1),
                Duration.ofHours(2),
                SynchronizationOverlapPolicy.SKIP,
                2,
                Duration.ofSeconds(5),
                false
        );
    }

    private static SynchronizationCheckpointEntity validCheckpoint(Instant completedAt) {
        SynchronizationCheckpointEntity entity = SynchronizationCheckpointEntity.create(
                java.util.UUID.randomUUID(),
                "ERICSSON_ENM_SIMULATOR",
                "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER",
                "DEFAULT",
                SynchronizationCheckpointService.SYNTHETIC_CHECKPOINT_TYPE,
                "sim-seq:1",
                "sim-v1",
                SynchronizationMode.FULL.name(),
                "COMPLETE",
                1L,
                SynchronizationCheckpointStatus.VALID.name(),
                completedAt
        );
        entity.advance(
                java.util.UUID.randomUUID(),
                "snap-1",
                completedAt,
                completedAt,
                "sim-seq:1",
                "sim-v1",
                SynchronizationMode.FULL.name(),
                "COMPLETE",
                1L,
                SynchronizationCheckpointStatus.VALID.name(),
                completedAt,
                completedAt
        );
        return entity;
    }

    @Test
    void matrix42_freshnessUnknownBeforeTrustedBaseline() {
        assertEquals(
                SynchronizationFreshness.UNKNOWN,
                freshnessEvaluator.evaluate(policy(), Optional.empty(), Instant.now(), false, false)
        );
    }

    @Test
    void matrix43_successfulTrustedFullIsFresh() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        assertEquals(
                SynchronizationFreshness.FRESH,
                freshnessEvaluator.evaluate(policy(), Optional.of(validCheckpoint(now.minusSeconds(10))), now, false, false)
        );
    }

    @Test
    void matrix44_timeProgressionProducesAging() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        Instant completed = now.minus(Duration.ofMinutes(90));
        assertEquals(
                SynchronizationFreshness.AGING,
                freshnessEvaluator.evaluate(policy(), Optional.of(validCheckpoint(completed)), now, false, false)
        );
    }

    @Test
    void matrix45_timeProgressionProducesStale() {
        Instant now = Instant.parse("2026-08-28T12:00:00Z");
        Instant completed = now.minus(Duration.ofHours(3));
        assertEquals(
                SynchronizationFreshness.STALE,
                freshnessEvaluator.evaluate(policy(), Optional.of(validCheckpoint(completed)), now, false, false)
        );
    }

    @Test
    void matrix46_operationalFailureProducesDegraded() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        assertEquals(
                SynchronizationFreshness.DEGRADED,
                freshnessEvaluator.evaluate(
                        policy(), Optional.of(validCheckpoint(now.minusSeconds(30))), now, false, true)
        );
    }

    @Test
    void matrix56_highConfidenceAfterTrustedFreshCompleteState() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                SynchronizationFreshness.FRESH,
                SynchronizationSourceHealth.HEALTHY,
                false,
                Optional.of(validCheckpoint(now.minusSeconds(30)))
        );
        assertEquals(NetworkKnowledgeConfidence.HIGH, evaluation.confidence());
        assertTrue(evaluation.reasonCodes().contains(KnowledgeConfidenceReason.TRUSTED_FRESH_COMPLETE.name()));
    }

    @Test
    void matrix57_agingProducesMediumConfidence() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                SynchronizationFreshness.AGING,
                SynchronizationSourceHealth.HEALTHY,
                false,
                Optional.of(validCheckpoint(Instant.now()))
        );
        assertEquals(NetworkKnowledgeConfidence.MEDIUM, evaluation.confidence());
        assertTrue(evaluation.reasonCodes().contains(KnowledgeConfidenceReason.AGING_TRUSTED_STATE.name()));
    }

    @Test
    void matrix58_staleProducesLowConfidence() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                SynchronizationFreshness.STALE,
                SynchronizationSourceHealth.HEALTHY,
                false,
                Optional.of(validCheckpoint(Instant.now()))
        );
        assertEquals(NetworkKnowledgeConfidence.LOW, evaluation.confidence());
        assertTrue(evaluation.reasonCodes().contains(KnowledgeConfidenceReason.STALE_TRUSTED_STATE.name()));
    }

    @Test
    void matrix59_recoveryRequiredProducesLowConfidence() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                SynchronizationFreshness.FRESH,
                SynchronizationSourceHealth.RECOVERING,
                true,
                Optional.of(validCheckpoint(Instant.now()))
        );
        assertEquals(NetworkKnowledgeConfidence.LOW, evaluation.confidence());
        assertTrue(evaluation.reasonCodes().contains(KnowledgeConfidenceReason.RECOVERY_REQUIRED.name()));
    }

    @Test
    void matrix60_noTrustedBaselineProducesUnknownConfidence() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                SynchronizationFreshness.UNKNOWN,
                SynchronizationSourceHealth.HEALTHY,
                false,
                Optional.empty()
        );
        assertEquals(NetworkKnowledgeConfidence.UNKNOWN, evaluation.confidence());
        assertTrue(evaluation.reasonCodes().contains(KnowledgeConfidenceReason.NO_TRUSTED_BASELINE.name()));
    }

    @Test
    void matrix61_agentPackagesDoNotReferenceConfidenceEvaluator() throws Exception {
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java/com/simba/snip/npo/agent"))) {
            boolean offender = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> {
                        try {
                            return java.nio.file.Files.readString(path).contains("NetworkKnowledgeConfidenceEvaluator");
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
            assertFalse(offender);
        }
    }

    @Test
    void matrix62_confidenceReasonCodesAreDeterministic() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation first = confidenceEvaluator.evaluate(
                SynchronizationFreshness.AGING,
                SynchronizationSourceHealth.DEGRADED,
                false,
                Optional.of(validCheckpoint(Instant.now()))
        );
        NetworkKnowledgeConfidenceEvaluator.Evaluation second = confidenceEvaluator.evaluate(
                SynchronizationFreshness.AGING,
                SynchronizationSourceHealth.DEGRADED,
                false,
                Optional.of(validCheckpoint(Instant.now()))
        );
        assertEquals(first.confidence(), second.confidence());
        assertEquals(first.reasonCodes(), second.reasonCodes());
    }

    @Test
    void matrix63_sourceScopedConfidenceUsesCheckpointPresence() {
        NetworkKnowledgeConfidenceEvaluator.Evaluation scoped = confidenceEvaluator.evaluate(
                SynchronizationFreshness.UNKNOWN,
                SynchronizationSourceHealth.HEALTHY,
                false,
                Optional.empty()
        );
        assertEquals(List.of(KnowledgeConfidenceReason.NO_TRUSTED_BASELINE), scoped.reasons());
    }
}
