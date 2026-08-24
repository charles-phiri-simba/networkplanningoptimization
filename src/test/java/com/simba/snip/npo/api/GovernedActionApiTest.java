package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.action.ActionMetrics;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernedActionApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private AssuranceCaseService assuranceCaseService;

    @Autowired
    private ActionMetrics metrics;

    private UUID caseId;

    @BeforeEach
    void assuranceCase() {
        if (!assuranceCaseService.listForCell("CELL-001").isEmpty()) {
            caseId = assuranceCaseService.listForCell("CELL-001").get(0).getId();
            return;
        }
        String prefix = "p4-" + UUID.randomUUID();
        Instant t0 = Instant.parse("2026-08-24T13:00:00Z");
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
    void cleanupCommittedTelemetry(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM action_audit_event");
        jdbc.update("DELETE FROM action_result");
        jdbc.update("DELETE FROM action_approval");
        jdbc.update("DELETE FROM policy_decision");
        jdbc.update("DELETE FROM proposed_action");
        jdbc.update("DELETE FROM assurance_evidence");
        jdbc.update("DELETE FROM assurance_case");
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p4-%'");
    }

    @Test
    void pathAGenerateRemediationPlanAllowThenMcpSuccessAndIdempotentReplay() {
        long before = metrics.mcpInvocations();
        ActionDetailDto proposed = propose("GENERATE_REMEDIATION_PLAN", "remediation.generate.v1", Map.of());
        assertEquals("LOW", proposed.riskLevel());
        assertEquals("ALLOW", proposed.policyDecision());
        assertEquals("POLICY_EVALUATED", proposed.status());
        assertNull(proposed.approval());

        ResponseEntity<ActionDetailDto> executed = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, ActionDetailDto.class);
        assertEquals(HttpStatus.OK, executed.getStatusCode());
        ActionDetailDto success = executed.getBody();
        assertNotNull(success);
        assertEquals("SUCCEEDED", success.status());
        assertEquals("SNIP_ACTION_SERVICE", success.executedBy());
        assertNotNull(success.result());
        assertEquals("SUCCEEDED", success.result().status());
        assertTrue(success.result().synthetic());
        assertTrue(success.result().output().contains("does not apply a network change")
                || success.result().output().contains("Investigation plan"));
        assertTrue(success.auditEvents().stream().anyMatch(e -> "MCP_INVOCATION_SUCCEEDED".equals(e.eventType())));
        assertEquals(before + 1, metrics.mcpInvocations());

        ResponseEntity<ActionDetailDto> replay = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, ActionDetailDto.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode());
        assertEquals("SUCCEEDED", replay.getBody().status());
        assertEquals(before + 1, metrics.mcpInvocations());
        assertTrue(metrics.idempotentHits() >= 1);
    }

    @Test
    void pathBSimulationRequiresApprovalThenSyntheticMcp() {
        long before = metrics.mcpInvocations();
        ActionDetailDto proposed = propose(
                "SIMULATE_CELL_PARAMETER_CHANGE",
                "simulation.cell-parameter.v1",
                Map.of("parameter", "pci", "currentValue", 12, "proposedValue", 24, "dryRun", true)
        );
        assertEquals("MEDIUM", proposed.riskLevel());
        assertEquals("REQUIRE_APPROVAL", proposed.policyDecision());
        assertEquals("APPROVAL_REQUIRED", proposed.status());

        ResponseEntity<String> blocked = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, blocked.getStatusCode());
        assertEquals(before, metrics.mcpInvocations());

        ResponseEntity<ActionDetailDto> approved = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/approve",
                new ApprovalRequest("demo-approver", "synthetic dry-run only"),
                ActionDetailDto.class);
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertEquals("APPROVED", approved.getBody().status());

        ResponseEntity<ActionDetailDto> executed = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, ActionDetailDto.class);
        assertEquals(HttpStatus.OK, executed.getStatusCode());
        ActionDetailDto success = executed.getBody();
        assertEquals("SUCCEEDED", success.status());
        assertTrue(success.result().output().contains("\"synthetic\":true")
                || success.result().output().contains("SYNTHETIC"));
        assertTrue(success.result().output().contains("dryRun"));
        assertEquals(before + 1, metrics.mcpInvocations());
    }

    @Test
    void pathCApplyIsDeniedWithNoMcpInvocation() {
        long before = metrics.mcpInvocations();
        ActionDetailDto proposed = propose("APPLY_CELL_PARAMETER_CHANGE", null, Map.of());
        assertEquals("HIGH", proposed.riskLevel());
        assertEquals("DENY", proposed.policyDecision());
        assertEquals("DENIED", proposed.status());
        assertTrue(proposed.auditEvents().stream().anyMatch(e -> "ACTION_DENIED".equals(e.eventType())));

        ResponseEntity<String> executed = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, executed.getStatusCode());
        assertEquals(before, metrics.mcpInvocations());
        ActionDetailDto stored = http.getForObject("/api/v1/actions/" + proposed.id(), ActionDetailDto.class);
        assertEquals("DENIED", stored.status());
        assertNull(stored.result());
        assertFalse(stored.auditEvents().stream().anyMatch(e -> e.eventType().startsWith("MCP_")));
    }

    @Test
    void rejectedSimulationCannotExecute() {
        long before = metrics.mcpInvocations();
        ActionDetailDto proposed = propose(
                "SIMULATE_CELL_PARAMETER_CHANGE",
                "simulation.cell-parameter.v1",
                Map.of("dryRun", true)
        );
        http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/reject",
                new ApprovalRequest("demo-approver", "no"),
                ActionDetailDto.class);
        ResponseEntity<String> executed = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, executed.getStatusCode());
        assertEquals(before, metrics.mcpInvocations());
    }

    @Test
    void unknownCapabilityAndDryRunFalseAreRejected() {
        ResponseEntity<String> unknown = http.postForEntity(
                "/api/v1/assurance/cases/" + caseId + "/actions",
                new ProposeActionRequest(
                        "GENERATE_REMEDIATION_PLAN",
                        "not.a.capability",
                        "CELL",
                        "CELL-001",
                        Map.of(),
                        "x",
                        "demo-user"
                ),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, unknown.getStatusCode());

        ResponseEntity<String> dryRun = http.postForEntity(
                "/api/v1/assurance/cases/" + caseId + "/actions",
                new ProposeActionRequest(
                        "SIMULATE_CELL_PARAMETER_CHANGE",
                        "simulation.cell-parameter.v1",
                        "CELL",
                        "CELL-001",
                        Map.of("dryRun", false),
                        "x",
                        "demo-user"
                ),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, dryRun.getStatusCode());
    }

    @Test
    void localMcpProtocolListsRegisteredTools() {
        ResponseEntity<Map> listed = http.postForEntity(
                "/mcp",
                Map.of("jsonrpc", "2.0", "id", "1", "method", "tools/list"),
                Map.class);
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertNotNull(listed.getBody());
        assertTrue(listed.getBody().toString().contains("remediation.generate.v1"));
        assertTrue(listed.getBody().toString().contains("simulation.cell-parameter.v1"));
        ResponseEntity<Map> health = http.getForEntity("/mcp/health", Map.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertEquals("UP", health.getBody().get("status"));
    }

    @Test
    void unknownActionIs404() {
        ResponseEntity<String> missing = http.getForEntity("/api/v1/actions/" + UUID.randomUUID(), String.class);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
    }

    private ActionDetailDto propose(String type, String capability, Map<String, Object> parameters) {
        ResponseEntity<ActionDetailDto> response = http.postForEntity(
                "/api/v1/assurance/cases/" + caseId + "/actions",
                new ProposeActionRequest(type, capability, "CELL", "CELL-001", parameters, "phase-4 test", "demo-user"),
                ActionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
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
