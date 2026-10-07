package com.simba.snip.npo.demo;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.NeighbourRelationshipRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.persist.SiteRepository;
import com.simba.snip.npo.telemetry.Trend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class DemoNetworkSeedTest extends AbstractPostgresIT {

    private static final Set<String> KNOWN_METRICS = Set.of(
            "BLER_DL", "BLER_UL", "DROP_RATE", "THROUGHPUT_DL", "LATENCY", "PRB_UTILIZATION_DL");

    @Autowired private SiteRepository siteRepository;
    @Autowired private GnbRepository gnbRepository;
    @Autowired private CellRepository cellRepository;
    @Autowired private NeighbourRelationshipRepository neighbourRelationshipRepository;
    @Autowired private RadioConfigurationRepository radioConfigurationRepository;
    @Autowired private NetworkContextService networkContextService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void v21DemoIdentitiesAreStableAndSynthetic() {
        for (String siteId : DemoInventory.SITE_IDS) {
            var site = siteRepository.findBySiteId(siteId).orElseThrow();
            assertNotNull(site.getLatitude());
            assertNotNull(site.getLongitude());
        }
        for (String gnbId : DemoInventory.GNB_IDS) {
            assertTrue(gnbRepository.findByGnbId(gnbId).isPresent());
        }
        for (String cellId : DemoInventory.CELL_IDS) {
            assertTrue(cellRepository.findByCellId(cellId).isPresent());
        }
        assertEquals(6, DemoInventory.SITE_IDS.size());
        assertEquals(6, DemoInventory.GNB_IDS.size());
        assertEquals(18, DemoInventory.CELL_IDS.size());

        var featured = cellRepository.findByCellId(DemoInventory.FEATURED_CELL).orElseThrow();
        var tx = radioConfigurationRepository.findByCell_IdAndParameterName(featured.getId(), "txPower").orElseThrow();
        assertEquals(DemoInventory.FEATURED_TX_POWER, tx.getParameterValue());

        neighbourRelationshipRepository.findAll().forEach(rel -> {
            assertNotNull(rel.getSourceCell());
            assertNotNull(rel.getTargetCell());
            assertFalse(rel.getSourceCell().getId().equals(rel.getTargetCell().getId()));
        });

        Integer demoNeighbourCount = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM neighbour_relationship n
                JOIN cell s ON s.id = n.source_cell_id
                JOIN cell t ON t.id = n.target_cell_id
                WHERE s.cell_id LIKE 'CELL-%' AND s.cell_id NOT LIKE 'CELL-SIM-%'
                  AND t.cell_id LIKE 'CELL-%' AND t.cell_id NOT LIKE 'CELL-SIM-%'
                """,
                Integer.class);
        assertNotNull(demoNeighbourCount);
        assertTrue(demoNeighbourCount >= 36 && demoNeighbourCount <= 42, "neighbours=" + demoNeighbourCount);

        List<String> metrics = jdbc.queryForList(
                "SELECT DISTINCT metric FROM kpi_observation WHERE source = 'DEMO_SEED'",
                String.class);
        assertTrue(KNOWN_METRICS.containsAll(metrics));

        Integer nonSynthetic = jdbc.queryForObject(
                "SELECT COUNT(*) FROM kpi_observation WHERE source = 'DEMO_SEED' AND synthetic = FALSE",
                Integer.class);
        assertEquals(0, nonSynthetic);

        Integer recentFeaturedBler = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM kpi_observation k
                JOIN cell c ON c.id = k.cell_id
                WHERE c.cell_id = 'CELL-001' AND k.metric = 'BLER_DL' AND k.value = 0.12
                  AND k.source = 'DEMO_SEED' AND k.observed_at > NOW() - INTERVAL '168 hours'
                """,
                Integer.class);
        assertTrue(recentFeaturedBler != null && recentFeaturedBler >= 1);
        Integer recentFeaturedPrb = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM kpi_observation k
                JOIN cell c ON c.id = k.cell_id
                WHERE c.cell_id = 'CELL-001' AND k.metric = 'PRB_UTILIZATION_DL' AND k.value = 0.82
                  AND k.source = 'DEMO_SEED' AND k.observed_at > NOW() - INTERVAL '168 hours'
                """,
                Integer.class);
        assertTrue(recentFeaturedPrb != null && recentFeaturedPrb >= 1);

        var featuredContext = networkContextService.resolve("CELL-001");
        var bler = featuredContext.telemetry().stream().filter(s -> "BLER_DL".equals(s.metric())).findFirst().orElseThrow();
        assertEquals(Trend.INCREASING, bler.trend());
        var prb = featuredContext.telemetry().stream().filter(s -> "PRB_UTILIZATION_DL".equals(s.metric())).findFirst().orElseThrow();
        assertEquals(Trend.INCREASING, prb.trend());

        var healthy = networkContextService.resolve("CELL-002");
        var healthyBler = healthy.telemetry().stream().filter(s -> "BLER_DL".equals(s.metric())).findFirst().orElseThrow();
        assertTrue(healthyBler.trend() != Trend.INCREASING || healthyBler.current().value() < 0.08);

        Set<String> siteVendors = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT vendor FROM gnb WHERE gnb_id IN ('GNB-001','GNB-002','GNB-003','GNB-004','GNB-005','GNB-006')",
                String.class));
        assertEquals(Set.of("DemoVendor"), siteVendors);
    }
}
