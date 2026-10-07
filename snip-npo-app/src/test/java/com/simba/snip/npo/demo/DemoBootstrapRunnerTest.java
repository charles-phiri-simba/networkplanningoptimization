package com.simba.snip.npo.demo;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.CaseStatus;
import com.simba.snip.npo.assurance.CaseType;
import com.simba.snip.npo.assurance.Severity;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.twin.SimulationQueryService;
import com.simba.snip.npo.twin.TwinScopeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
@ActiveProfiles("demo")
class DemoBootstrapRunnerTest extends AbstractPostgresIT {

    @Autowired private DemoBootstrapRunner runner;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private CellRepository cellRepository;
    @Autowired private NetworkTwinRepository twinRepository;
    @Autowired private SimulationQueryService simulationQueryService;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private KnowledgeGate knowledgeGate;
    @Autowired private ProductionChangeProperties productionChange;
    @Autowired private ChangeExecutionProperties changeExecution;
    @Autowired private SnipProperties snip;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void bootstrapCreatesFeaturedStateAndIsIdempotent() {
        assertFalse(productionChange.isEnabled());
        assertFalse(productionChange.isGlobalExecutionEnabled());
        assertFalse(snip.isKafkaEnabled());
        assertTrue(changeExecution.isEnabled());

        List<AssuranceCaseEntity> first = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, first.size());
        UUID caseId = first.get(0).getId();
        assertEquals(CaseType.DEGRADING_RADIO_QUALITY.name(), first.get(0).getCaseType());
        assertEquals(Severity.CRITICAL.name(), first.get(0).getSeverity());
        assertEquals(CaseStatus.OPEN.name(), first.get(0).getStatus());

        runner.run(new DefaultApplicationArguments());

        List<AssuranceCaseEntity> second = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, second.size());
        assertEquals(caseId, second.get(0).getId());

        for (String cellId : List.of("CELL-001", "CELL-002", "CELL-003", "CELL-007")) {
            var twin = twinRepository.findByScopeTypeAndScopeId(TwinScopeType.CELL.name(), cellId).orElseThrow();
            assertEquals("CURRENT", simulationQueryService.twin(twin.getId()).freshness());
        }

        var knowledge = knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope("ERICSSON_ENM_SIMULATOR", "DEFAULT")
                .orElseThrow();
        assertTrue(knowledgeGate.evaluate(knowledge.getConfidence()).allowsRecommendation());

        for (String cellId : DemoInventory.CELL_IDS) {
            assertTrue(cellRepository.findByCellId(cellId).isPresent(), cellId);
        }
        Integer planning = jdbc.queryForObject("SELECT COUNT(*) FROM planning_scenario", Integer.class);
        assertEquals(0, planning);
    }
}
