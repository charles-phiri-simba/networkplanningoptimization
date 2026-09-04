package com.simba.snip.npo.assurance;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceCaseRepository;
import com.simba.snip.npo.telemetry.ProjectionOutcome;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
@Transactional
class AssuranceDetectionTest extends AbstractPostgresIT {

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private AssuranceCaseService assuranceCaseService;

    @Autowired
    private AssuranceCaseRepository assuranceCaseRepository;

    @Autowired
    private AssuranceDetectionService detectionService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void isolateSyntheticAssuranceState() {
        SyntheticAssuranceFixtureCleanup.deleteAndAssertSyntheticDegradingCases(jdbc, "CELL-001");
        SyntheticAssuranceFixtureCleanup.deleteAndAssertSyntheticDegradingCases(jdbc, "CELL-002");
    }

    @Test
    void highBlerLoadCreatesOneCaseAndRepeatedEventsUpdateIt() {
        projectHighBler("p3-a");
        List<AssuranceCaseEntity> first = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, first.size());
        UUID id = first.get(0).getId();
        assertEquals(CaseType.DEGRADING_RADIO_QUALITY.name(), first.get(0).getCaseType());
        assertEquals(Severity.CRITICAL.name(), first.get(0).getSeverity());
        assertEquals(Confidence.HIGH.name(), first.get(0).getConfidence());
        assertEquals(CaseStatus.OPEN.name(), first.get(0).getStatus());
        Instant firstDetected = first.get(0).getDetectedAt();
        int evidenceCount = first.get(0).getEvidence().size();
        assertTrue(evidenceCount >= 2);

        projectHighBler("p3-b");
        List<AssuranceCaseEntity> second = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, second.size());
        assertEquals(id, second.get(0).getId());
        assertEquals(firstDetected, second.get(0).getDetectedAt());
        assertEquals(1, assuranceCaseRepository.count());
    }

    @Test
    void seedCell001DoesNotCreateCaseWithoutIncreasingTrend() {
        assertTrue(detectionService.evaluateCell("CELL-001").isEmpty());
        assertTrue(assuranceCaseService.listForCell("CELL-001").isEmpty());
    }

    @Test
    void healthyStableDoesNotCreateDegradingCase() {
        Instant t0 = Instant.now().minusSeconds(3_600);
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            assertEquals(ProjectionOutcome.PROJECTED, projectionService.project(event(
                    "p3-healthy-bler-" + i, "CELL-002", "BLER_DL", 0.008, ts)));
            assertEquals(ProjectionOutcome.PROJECTED, projectionService.project(event(
                    "p3-healthy-prb-" + i, "CELL-002", "PRB_UTILIZATION_DL", 0.41, ts)));
        }
        assertTrue(assuranceCaseService.listForCell("CELL-002").isEmpty());
        assertTrue(assuranceCaseRepository.findFirstByAffectedEntityIdAndCaseTypeAndStatusIn(
                "CELL-002",
                CaseType.DEGRADING_RADIO_QUALITY.name(),
                Set.of(CaseStatus.OPEN.name(), CaseStatus.ACKNOWLEDGED.name())
        ).isEmpty());
    }

    private void projectHighBler(String prefix) {
        // Must stay inside snip.recent-kpi-hours (168h) relative to Instant.now().
        Instant t0 = Instant.now().minusSeconds(3_600);
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            projectionService.project(event(prefix + "-bler-" + i, "CELL-001", "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb-" + i, "CELL-001", "PRB_UTILIZATION_DL", prb[i], ts));
        }
    }

    private static TelemetryEvent event(String eventId, String cellId, String metric, double value, Instant eventTime) {
        return new TelemetryEvent(
                eventId,
                TelemetryEvent.TYPE_CELL_KPI_OBSERVED,
                TelemetryEvent.SCHEMA_V1,
                TelemetryEvent.SOURCE_SIMULATOR,
                cellId,
                metric,
                value,
                "ratio",
                eventTime,
                null,
                true
        );
    }
}
