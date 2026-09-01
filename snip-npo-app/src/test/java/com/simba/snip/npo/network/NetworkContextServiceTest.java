package com.simba.snip.npo.network;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.telemetry.Trend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class NetworkContextServiceTest extends AbstractPostgresIT {

    @Autowired
    private NetworkContextService contextService;

    @Test
    void cell001ContextHasStableStructureAndSyntheticProvenance() {
        CellContext ctx = contextService.resolve("CELL-001");
        assertEquals("CELL-001", ctx.cell().cellId());
        assertEquals("GNB-001", ctx.gnb().gnbId());
        assertEquals("SITE-001", ctx.site().siteId());
        assertEquals("n78", ctx.cell().band());
        assertFalse(ctx.kpis().isEmpty());
        assertTrue(ctx.kpis().stream().anyMatch(k -> "BLER_DL".equals(k.metric()) && k.value() == 0.12));
        assertTrue(ctx.kpis().stream().allMatch(CellContext.KpiObservationView::synthetic));
        assertFalse(ctx.radioConfiguration().isEmpty());
        assertTrue(ctx.neighbours().stream().anyMatch(n -> "CELL-002".equals(n.targetCellId())));
        assertEquals("DEMO_SEED", ctx.provenance().source());
        assertTrue(ctx.provenance().synthetic());
        CellContext.KpiSeriesView bler = ctx.telemetry().stream()
                .filter(s -> "BLER_DL".equals(s.metric()))
                .findFirst()
                .orElseThrow();
        assertEquals(0.12, bler.current().value());
        assertEquals(Trend.INSUFFICIENT_DATA, bler.trend());
        assertFalse(bler.history().isEmpty());
        assertEquals("DEMO_SEED", bler.current().source());
        assertTrue(bler.current().eventId() != null && !bler.current().eventId().isBlank());
        assertTrue(bler.current().observedAt() != null);
        assertTrue(bler.current().ingestedAt() != null);
    }

    @Test
    void unknownCellIsNotFound() {
        assertThrows(DomainNotFoundException.class, () -> contextService.resolve("CELL-MISSING"));
    }
}
