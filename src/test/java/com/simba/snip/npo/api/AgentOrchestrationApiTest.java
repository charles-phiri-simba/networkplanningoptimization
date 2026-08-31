package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.action.ActionMetrics;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentOrchestrationApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private AssuranceCaseService assuranceCaseService;

    @Autowired
    private ActionMetrics actionMetrics;

    @Autowired
    private AgentMetrics agentMetrics;

    private UUID caseId;

    @BeforeEach
    void assuranceCase() {
        if (!assuranceCaseService.listForCell("CELL-001").isEmpty()) {
            caseId = assuranceCaseService.listForCell("CELL-001").get(0).getId();
            return;
        }
        String prefix = "p5-" + UUID.randomUUID();
        // Must stay inside snip.recent-kpi-hours (168h) relative to Instant.now().
        Instant t0 = Instant.now().minusSeconds(3_600);
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
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p5-%'");
    }

    @Test
    void scenarioAGenerateRemediationPlanGoesThroughPhase4AllowWithoutAgentMcp() {
        long mcpBefore = actionMetrics.mcpInvocations();
        AgentRunDetailDto run = start("Investigate the DEGRADING_RADIO_QUALITY case for CELL-001 and recommend the next safe action.", null);
        assertEquals("COMPLETED", run.status());
        assertEquals(4, run.plan().steps().size());
        assertEquals("CONTEXT", run.plan().steps().get(0).agentRole());
        assertEquals("ASSURANCE", run.plan().steps().get(1).agentRole());
        assertEquals("KNOWLEDGE", run.plan().steps().get(2).agentRole());
        assertEquals("DECISION", run.plan().steps().get(3).agentRole());
        assertTrue(run.auditEvents().stream().anyMatch(e -> "PLAN_CREATED".equals(e.eventType())));
        assertTrue(run.auditEvents().stream().anyMatch(e -> "ACTION_PROPOSED".equals(e.eventType())));
        assertFalse(run.auditEvents().stream().anyMatch(e -> e.summary() != null && e.summary().toLowerCase().contains("chain-of-thought")));
        assertEquals(1, run.proposedActionIds().size());
        ActionDetailDto action = http.getForObject("/api/v1/actions/" + run.proposedActionIds().get(0), ActionDetailDto.class);
        assertNotNull(action);
        assertEquals("GENERATE_REMEDIATION_PLAN", action.actionType());
        assertEquals("LOW", action.riskLevel());
        assertEquals("ALLOW", action.policyDecision());
        assertEquals("AGENT", action.proposedBy());
        assertEquals(run.id(), action.agentRunId());
        assertEquals("decision-agent", action.agentId());
        assertEquals(mcpBefore, actionMetrics.mcpInvocations());
        assertTrue(agentMetrics.runsCompleted() >= 1);
        assertTrue(agentMetrics.actionsProposed() >= 1);
    }

    @Test
    void scenarioBSimulationStillRequiresApprovalAndAgentCannotApprove() {
        long mcpBefore = actionMetrics.mcpInvocations();
        AgentRunDetailDto run = start("Propose SIMULATE_CELL_PARAMETER_CHANGE for CELL-001 after investigating the case.", null);
        assertEquals("COMPLETED", run.status());
        ActionDetailDto action = http.getForObject("/api/v1/actions/" + run.proposedActionIds().get(0), ActionDetailDto.class);
        assertEquals("SIMULATE_CELL_PARAMETER_CHANGE", action.actionType());
        assertEquals("MEDIUM", action.riskLevel());
        assertEquals("REQUIRE_APPROVAL", action.policyDecision());
        assertEquals("APPROVAL_REQUIRED", action.status());
        ResponseEntity<String> blocked = http.postForEntity("/api/v1/actions/" + action.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, blocked.getStatusCode());
        assertEquals(mcpBefore, actionMetrics.mcpInvocations());
    }

    @Test
    void scenarioCApplyRemainsDeniedWithNoMcp() {
        long mcpBefore = actionMetrics.mcpInvocations();
        AgentRunDetailDto run = start("Propose APPLY_CELL_PARAMETER_CHANGE for CELL-001.", null);
        assertEquals("COMPLETED", run.status());
        ActionDetailDto action = http.getForObject("/api/v1/actions/" + run.proposedActionIds().get(0), ActionDetailDto.class);
        assertEquals("APPLY_CELL_PARAMETER_CHANGE", action.actionType());
        assertEquals("HIGH", action.riskLevel());
        assertEquals("DENY", action.policyDecision());
        assertEquals("DENIED", action.status());
        ResponseEntity<String> executed = http.postForEntity("/api/v1/actions/" + action.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, executed.getStatusCode());
        assertEquals(mcpBefore, actionMetrics.mcpInvocations());
        assertFalse(action.auditEvents().stream().anyMatch(e -> e.eventType().startsWith("MCP_")));
    }

    @Test
    void boundedRunStopsOnMaxStepsWithoutMcp() {
        long mcpBefore = actionMetrics.mcpInvocations();
        AgentRunDetailDto run = start("Investigate CELL-001 with a tight step budget.", 1);
        assertEquals("FAILED", run.status());
        assertTrue(run.auditEvents().stream().anyMatch(e -> "LIMIT_REACHED".equals(e.eventType())));
        assertTrue(run.proposedActionIds() == null || run.proposedActionIds().isEmpty());
        assertEquals(mcpBefore, actionMetrics.mcpInvocations());
        assertEquals("COMPLETED", run.plan().steps().get(0).status());
        assertTrue(run.plan().steps().stream().skip(1).allMatch(step -> "SKIPPED".equals(step.status())));
    }

    @Test
    void unknownRunIs404() {
        ResponseEntity<String> missing = http.getForEntity("/api/v1/agent-runs/" + UUID.randomUUID(), String.class);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
    }

    private AgentRunDetailDto start(String objective, Integer maxSteps) {
        ResponseEntity<AgentRunDetailDto> response = http.postForEntity(
                "/api/v1/agent-runs",
                new CreateAgentRunRequest(objective, caseId, "demo-user", maxSteps, null, null, null),
                AgentRunDetailDto.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
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
