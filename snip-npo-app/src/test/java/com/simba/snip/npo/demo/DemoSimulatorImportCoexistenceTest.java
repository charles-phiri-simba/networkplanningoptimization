package com.simba.snip.npo.demo;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.persist.SiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class DemoSimulatorImportCoexistenceTest extends AbstractPostgresIT {

    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private SiteRepository siteRepository;
    @Autowired private GnbRepository gnbRepository;
    @Autowired private CellRepository cellRepository;
    @Autowired private RadioConfigurationRepository radioConfigurationRepository;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private KnowledgeGate knowledgeGate;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void simulatorImportDoesNotReplaceV21DemoInventory() {
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update("DELETE FROM synchronization_checkpoint");
        jdbc.update(
                """
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        vendorImportAuthorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));

        for (String siteId : DemoInventory.SITE_IDS) {
            assertTrue(siteRepository.findBySiteId(siteId).isPresent(), siteId);
        }
        for (String gnbId : DemoInventory.GNB_IDS) {
            assertTrue(gnbRepository.findByGnbId(gnbId).isPresent(), gnbId);
        }
        for (String cellId : DemoInventory.CELL_IDS) {
            assertTrue(cellRepository.findByCellId(cellId).isPresent(), cellId);
        }

        var featured = cellRepository.findByCellId("CELL-001").orElseThrow();
        assertEquals("CELL-001", featured.getCellId());
        assertEquals("46", radioConfigurationRepository
                .findByCell_IdAndParameterName(featured.getId(), "txPower").orElseThrow().getParameterValue());

        Integer demoKpis = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM kpi_observation k
                JOIN cell c ON c.id = k.cell_id
                WHERE c.cell_id = 'CELL-001' AND k.source = 'DEMO_SEED' AND k.synthetic = TRUE
                """,
                Integer.class);
        assertTrue(demoKpis != null && demoKpis >= 10);

        Integer v21Cells = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cell WHERE cell_id BETWEEN 'CELL-001' AND 'CELL-018'",
                Integer.class);
        assertEquals(18, v21Cells);

        assertTrue(siteRepository.findBySiteId("SITE-SIM-001").isPresent());
        assertTrue(gnbRepository.findByGnbId("GNB-SIM-001").isPresent());
        assertTrue(cellRepository.findByCellId("CELL-SIM-001").isPresent());

        var knowledge = knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope("ERICSSON_ENM_SIMULATOR", "DEFAULT")
                .orElseThrow();
        assertTrue(knowledgeGate.evaluate(knowledge.getConfidence()).allowsRecommendation());
    }
}
