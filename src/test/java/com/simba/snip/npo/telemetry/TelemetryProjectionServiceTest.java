package com.simba.snip.npo.telemetry;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.KpiObservationEntity;
import com.simba.snip.npo.persist.KpiObservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
@Transactional
class TelemetryProjectionServiceTest extends AbstractPostgresIT {

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private KpiObservationRepository kpiObservationRepository;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private NetworkContextService contextService;

    @Test
    void validEventPersistsEventTimeAndIngestedAt() {
        Instant eventTime = Instant.parse("2026-08-24T10:15:00Z");
        TelemetryEvent event = event("p2-test-bler-1", "CELL-001", "BLER_DL", 0.04, eventTime);

        assertEquals(ProjectionOutcome.PROJECTED, projectionService.project(event));

        KpiObservationEntity stored = kpiObservationRepository.findByEventId("p2-test-bler-1").orElseThrow();
        assertEquals(eventTime, stored.getEventTime());
        assertEquals("SNIP_SIMULATOR", stored.getSource());
        assertTrue(stored.isSynthetic());
        assertTrue(stored.getIngestedAt().isAfter(eventTime) || stored.getIngestedAt().equals(eventTime)
                || stored.getIngestedAt().isAfter(Instant.parse("2026-01-01T00:00:00Z")));
        assertNotEquals(eventTime, stored.getIngestedAt());
    }

    @Test
    void duplicateEventIdIsIgnored() {
        Instant eventTime = Instant.parse("2026-08-24T10:20:00Z");
        TelemetryEvent event = event("p2-test-dup-1", "CELL-001", "BLER_DL", 0.06, eventTime);
        assertEquals(ProjectionOutcome.PROJECTED, projectionService.project(event));
        assertEquals(ProjectionOutcome.DUPLICATE, projectionService.project(event));
        assertEquals(1, kpiObservationRepository.countByEventId("p2-test-dup-1"));
    }

    @Test
    void unknownCellDoesNotCreateTopology() {
        long cellsBefore = cellRepository.count();
        TelemetryEvent event = event("p2-test-missing-1", "CELL-MISSING", "BLER_DL", 0.12,
                Instant.parse("2026-08-24T10:25:00Z"));
        assertThrows(UnrecoverableTelemetryException.class, () -> projectionService.project(event));
        assertEquals(cellsBefore, cellRepository.count());
        assertTrue(cellRepository.findByCellId("CELL-MISSING").isEmpty());
        assertTrue(kpiObservationRepository.findByEventId("p2-test-missing-1").isEmpty());
    }

    @Test
    void historyIsOrderedByEventTimeAndTrendIncreases() {
        Instant t0 = Instant.parse("2026-08-24T11:00:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            projectionService.project(event("p2-test-hist-bler-" + (i + 1), "CELL-001", "BLER_DL", bler[i], ts));
            projectionService.project(event("p2-test-hist-prb-" + (i + 1), "CELL-001", "PRB_UTILIZATION_DL", prb[i], ts));
        }

        CellContext ctx = contextService.resolve("CELL-001");
        CellContext.KpiSeriesView blerSeries = series(ctx, "BLER_DL");
        assertEquals(Trend.INCREASING, blerSeries.trend());
        assertEquals(0.12, blerSeries.current().value());
        assertEquals(List.of(0.04, 0.06, 0.09, 0.12), blerSeries.history().stream().map(CellContext.KpiObservationView::value).toList());
        assertEquals(Trend.INCREASING, series(ctx, "PRB_UTILIZATION_DL").trend());
        assertEquals("CELL-001", ctx.cell().cellId());
        assertEquals("GNB-001", ctx.gnb().gnbId());
        assertTrue(ctx.neighbours().stream().anyMatch(n -> "CELL-002".equals(n.targetCellId())));
    }

    private static CellContext.KpiSeriesView series(CellContext ctx, String metric) {
        return ctx.telemetry().stream().filter(s -> metric.equals(s.metric())).findFirst().orElseThrow();
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
