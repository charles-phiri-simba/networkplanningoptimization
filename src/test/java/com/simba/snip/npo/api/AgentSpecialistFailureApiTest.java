package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.agent.AgentMetrics;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = NpoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "snip.agent-force-fail-agent-id=knowledge-agent"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentSpecialistFailureApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private AssuranceCaseService assuranceCaseService;

    @Autowired
    private AgentMetrics agentMetrics;

    private UUID caseId;

    @BeforeEach
    void assuranceCase() {
        if (!assuranceCaseService.listForCell("CELL-001").isEmpty()) {
            caseId = assuranceCaseService.listForCell("CELL-001").get(0).getId();
            return;
        }
        String prefix = "p5f-" + UUID.randomUUID();
        Instant t0 = Instant.parse("2026-08-24T16:00:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            projectionService.project(event(prefix + "-bler-" + i, "CELL-001", "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb-" + i, "CELL-001", "PRB_UTILIZATION_DL", prb[i], ts));
        }
        List<AssuranceCaseEntity> cases = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, cases.size());
        caseId = cases.get(0).getId();
    }

    @AfterAll
    void cleanup(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM action_audit_event");
        jdbc.update("DELETE FROM action_result");
        jdbc.update("DELETE FROM action_approval");
        jdbc.update("DELETE FROM policy_decision");
        jdbc.update("DELETE FROM proposed_action");
        jdbc.update("DELETE FROM agent_run_audit_event");
        jdbc.update("DELETE FROM agent_case_memory");
        jdbc.update("DELETE FROM agent_plan_step");
        jdbc.update("DELETE FROM agent_plan");
        jdbc.update("DELETE FROM agent_run");
        jdbc.update("DELETE FROM assurance_evidence");
        jdbc.update("DELETE FROM assurance_case");
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p5f-%'");
    }

    @Test
    void knowledgeFailureRetriesThenContinuesWithoutFabricatedEvidence() {
        long retriesBefore = agentMetrics.retries();
        ResponseEntity<AgentRunDetailDto> response = http.postForEntity(
                "/api/v1/agent-runs",
                new CreateAgentRunRequest(
                        "Investigate the DEGRADING_RADIO_QUALITY case for CELL-001 and recommend the next safe action.",
                        caseId,
                        "demo-user",
                        null,
                        null,
                        null,
                        null
                ),
                AgentRunDetailDto.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AgentRunDetailDto run = response.getBody();
        assertEquals("COMPLETED", run.status());
        assertEquals("FAILED", run.plan().steps().get(2).status());
        assertTrue(run.plan().steps().get(2).outputSummary().contains("forced specialist failure"));
        assertFalse(run.plan().steps().get(2).outputSummary().toLowerCase().contains("3gpp"));
        assertEquals("COMPLETED", run.plan().steps().get(3).status());
        assertTrue(run.auditEvents().stream().anyMatch(e -> "STEP_FAILED".equals(e.eventType())));
        assertTrue(agentMetrics.retries() > retriesBefore);
        assertTrue(agentMetrics.stepsFailed() >= 1);
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
