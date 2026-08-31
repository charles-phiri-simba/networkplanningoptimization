package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import com.simba.snip.npo.twin.TwinMetrics;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalTwinApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private AssuranceCaseService assuranceCaseService;

    @Autowired
    private TwinMetrics twinMetrics;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID caseId;

    @BeforeEach
    void fixtures() {
        if (assuranceCaseService.listForCell("CELL-001").isEmpty()) {
            // Must stay inside snip.recent-kpi-hours (168h) relative to Instant.now().
            Instant t0 = Instant.now().minusSeconds(3_600);
            double[] bler = {0.04, 0.06, 0.09, 0.12};
            double[] prb = {0.60, 0.68, 0.77, 0.84};
            for (int i = 0; i < 4; i++) {
                Instant ts = t0.plusSeconds(i * 300L);
                String prefix = "p6-" + UUID.randomUUID();
                projectionService.project(event(prefix + "-bler", "CELL-001", "BLER_DL", bler[i], ts));
                projectionService.project(event(prefix + "-prb", "CELL-001", "PRB_UTILIZATION_DL", prb[i], ts));
            }
        }
        List<AssuranceCaseEntity> cases = assuranceCaseService.listForCell("CELL-001");
        assertFalse(cases.isEmpty());
        caseId = cases.get(0).getId();
    }

    @AfterAll
    void cleanup(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM simulation_limitation");
        jdbc.update("DELETE FROM simulation_result_metric");
        jdbc.update("DELETE FROM simulation_run");
        jdbc.update("DELETE FROM simulation_scenario_change");
        jdbc.update("DELETE FROM simulation_scenario");
        jdbc.update("DELETE FROM network_twin_version");
        jdbc.update("DELETE FROM network_twin");
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
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p6-%'");
    }

    @Test
    void unknownCellAndTwinAndVersionAreNotFound() {
        ResponseEntity<String> cell = http.postForEntity("/api/v1/twins/cells/CELL-999/synchronize", null, String.class);
        assertEquals(HttpStatus.NOT_FOUND, cell.getStatusCode());
        ResponseEntity<String> twin = http.getForEntity("/api/v1/twins/" + UUID.randomUUID(), String.class);
        assertEquals(HttpStatus.NOT_FOUND, twin.getStatusCode());
        TwinDetailDto created = synchronize("CELL-001");
        ResponseEntity<String> version = http.getForEntity(
                "/api/v1/twins/" + created.id() + "/versions/99", String.class);
        assertEquals(HttpStatus.NOT_FOUND, version.getStatusCode());
    }

    @Test
    void parameterValidationRejectsUnsupportedAndOutOfRangeAndMismatch() {
        TwinDetailDto twin = synchronize("CELL-001");
        assertEquals(HttpStatus.BAD_REQUEST, createScenarioRaw(twin.id(), "electricalTilt", 6.0, 4.0).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, createScenarioRaw(twin.id(), "pci", 12.0, 24.0).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, createScenarioRaw(twin.id(), "txPower", 5.0, 8.0).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, createScenarioRaw(twin.id(), "txPower", 46.0, 60.0).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, createScenarioRaw(twin.id(), "txPower", 40.0, 38.0).getStatusCode());
    }

    @Test
    void canonicalTxPowerScenarioRequiresApprovalThenProducesImmutableSyntheticResult() {
        TwinDetailDto twin = synchronize("CELL-001");
        assertEquals("CURRENT", twin.freshness());
        assertTrue(twin.synthetic());
        ScenarioDetailDto scenario = createScenario(twin.id(), "A", 46.0, 44.0);
        ActionDetailDto proposed = propose(scenario, 46.0, 44.0);
        assertEquals("MEDIUM", proposed.riskLevel());
        assertEquals("REQUIRE_APPROVAL", proposed.policyDecision());
        ResponseEntity<String> blocked = http.postForEntity(
                "/api/v1/actions/" + proposed.id() + "/execute", null, String.class);
        assertEquals(HttpStatus.CONFLICT, blocked.getStatusCode());
        ActionDetailDto success = approveAndExecute(proposed.id());
        assertEquals("SUCCEEDED", success.status());
        assertTrue(success.result().synthetic());
        assertTrue(success.result().output().contains("snip.synthetic.cell-parameter.v1"));
        assertTrue(success.result().output().contains("\"dryRun\":true"));
        assertTrue(success.result().output().contains("NO_RF_PROPAGATION_MODEL"));
        assertTrue(success.result().output().contains("\"networkWriteAttempted\":false"));
        SimulationDetailDto first = simulationFromAction(success);
        assertEquals("LOW", first.confidence());
        assertEquals(twin.latestVersion(), first.baselineTwinVersion());
        assertEquals("1.0", first.modelVersion());
        assertTrue(first.synthetic());
        assertTrue(first.limitations().contains("NO_RF_PROPAGATION_MODEL"));
        assertTrue(first.limitations().contains("SYNTHETIC_KPI_MODEL"));
        MetricComparisonDto tx = metric(first, "txPower");
        assertEquals(-2.0, tx.delta());
        MetricComparisonDto bler = metric(first, "BLER_DL");
        assertTrue(bler.candidateValue() > bler.baselineValue());

        ActionDetailDto rerun = approveAndExecute(propose(scenario, 46.0, 44.0).id());
        SimulationDetailDto second = simulationFromAction(rerun);
        assertNotEquals(first.id(), second.id());
        assertEquals(first.metrics(), second.metrics());
        SimulationDetailDto original = http.getForObject("/api/v1/simulations/" + first.id(), SimulationDetailDto.class);
        assertEquals(first.id(), original.id());
        assertEquals(first.metrics(), original.metrics());
    }

    @Test
    void comparisonExposesTradeoffsWithoutSelectingOptimum() {
        TwinDetailDto twin = synchronize("CELL-001");
        ScenarioDetailDto a = createScenario(twin.id(), "A", 46.0, 44.0);
        ScenarioDetailDto b = createScenario(twin.id(), "B", 46.0, 42.0);
        SimulationDetailDto left = simulationFromAction(approveAndExecute(propose(a, 46.0, 44.0).id()));
        SimulationDetailDto right = simulationFromAction(approveAndExecute(propose(b, 46.0, 42.0).id()));
        ResponseEntity<SimulationComparisonDto> compared = http.getForEntity(
                "/api/v1/simulation-comparisons?left=" + left.id() + "&right=" + right.id(),
                SimulationComparisonDto.class);
        assertEquals(HttpStatus.OK, compared.getStatusCode());
        SimulationComparisonDto body = compared.getBody();
        assertNotNull(body);
        assertFalse(body.automaticOptimumSelected());
        assertEquals(twin.id(), body.twinId());
        assertEquals(twin.latestVersion(), body.baselineTwinVersion());
        assertTrue(body.metricTradeoffs().stream().anyMatch(row -> "BLER_DL".equals(row.get("metric"))));
    }

    @Test
    void staleTwinBlocksSimulationUntilResynchronization() {
        TwinDetailDto twin = synchronize("CELL-001");
        int versionOne = twin.latestVersion();
        ScenarioDetailDto scenario = createScenario(twin.id(), "stale", 46.0, 44.0);
        // Telemetry newer than the just-synchronized twin must be after Instant.now().
        Instant later = Instant.now().plusSeconds(60);
        projectionService.project(event("p6-stale-" + UUID.randomUUID(), "CELL-001", "BLER_DL", 0.13, later));
        TwinDetailDto stale = http.getForObject("/api/v1/twins/" + twin.id(), TwinDetailDto.class);
        assertEquals("STALE", stale.freshness());
        ActionDetailDto failed = approveAndExecute(propose(scenario, 46.0, 44.0).id());
        assertEquals("FAILED", failed.status());
        assertTrue(failed.result().error().contains("STALE"));
        assertTrue(twinMetrics.twinStaleDetections() >= 1);

        TwinDetailDto resynced = synchronize("CELL-001");
        assertEquals("CURRENT", resynced.freshness());
        assertEquals(versionOne + 1, resynced.latestVersion());
        TwinVersionDetailDto kept = http.getForObject(
                "/api/v1/twins/" + twin.id() + "/versions/" + versionOne, TwinVersionDetailDto.class);
        assertEquals(versionOne, kept.version());
        ScenarioDetailDto freshScenario = createScenario(twin.id(), "fresh", 46.0, 44.0);
        ActionDetailDto success = approveAndExecute(propose(freshScenario, 46.0, 44.0).id());
        assertEquals("SUCCEEDED", success.status());
        assertTrue(success.result().output().contains("\"baselineTwinVersion\":" + resynced.latestVersion()));
    }

    @Test
    void expiredTwinBlocksSimulation() {
        TwinDetailDto twin = synchronize("CELL-001");
        ScenarioDetailDto scenario = createScenario(twin.id(), "exp", 46.0, 44.0);
        jdbc.update(
                "UPDATE network_twin_version SET synchronized_at = synchronized_at - INTERVAL '25 hours' WHERE twin_id = ?",
                twin.id());
        jdbc.update(
                "UPDATE network_twin SET synchronized_at = synchronized_at - INTERVAL '25 hours' WHERE id = ?",
                twin.id());
        TwinDetailDto expired = http.getForObject("/api/v1/twins/" + twin.id(), TwinDetailDto.class);
        assertEquals("EXPIRED", expired.freshness());
        ActionDetailDto failed = approveAndExecute(propose(scenario, 46.0, 44.0).id());
        assertEquals("FAILED", failed.status());
        assertTrue(failed.result().error().contains("EXPIRED"));
    }

    @Test
    void missingScenarioAndModelFailureFailClosed() {
        synchronize("CELL-001");
        ActionDetailDto missing = proposeRaw(Map.of(
                "parameter", "txPower",
                "currentValue", 46,
                "proposedValue", 44,
                "dryRun", true,
                "scenarioId", UUID.randomUUID().toString()
        ));
        ActionDetailDto missingExecuted = approveAndExecute(missing.id());
        assertEquals("FAILED", missingExecuted.status());

        TwinDetailDto cell003 = synchronize("CELL-003");
        ScenarioDetailDto scenario003 = createScenario(cell003.id(), "c3", 40.0, 38.0);
        ResponseEntity<ActionDetailDto> proposed = http.postForEntity(
                "/api/v1/assurance/cases/" + caseId + "/actions",
                new ProposeActionRequest(
                        "SIMULATE_CELL_PARAMETER_CHANGE",
                        "simulation.cell-parameter.v1",
                        "CELL",
                        "CELL-003",
                        Map.of(
                                "parameter", "txPower",
                                "currentValue", 40,
                                "proposedValue", 38,
                                "dryRun", true,
                                "scenarioId", scenario003.id().toString()
                        ),
                        "model failure",
                        "demo-user"
                ),
                ActionDetailDto.class);
        ActionDetailDto failed = approveAndExecute(proposed.getBody().id());
        assertEquals("FAILED", failed.status());
        assertNotNull(failed.result().error());
        assertTrue(failed.result().error().toLowerCase().contains("model failure")
                || failed.result().error().contains("PRB"));
    }

    private TwinDetailDto synchronize(String cellId) {
        ResponseEntity<TwinDetailDto> response = http.postForEntity(
                "/api/v1/twins/cells/" + cellId + "/synchronize", null, TwinDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ScenarioDetailDto createScenario(UUID twinId, String name, double current, double proposed) {
        ResponseEntity<ScenarioDetailDto> response = http.postForEntity(
                "/api/v1/twins/" + twinId + "/scenarios",
                new CreateScenarioRequest(
                        name, "phase-6 scenario", "demo-user", null,
                        new ScenarioChangeRequest("txPower", current, proposed)
                ),
                ScenarioDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<String> createScenarioRaw(UUID twinId, String parameter, double current, double proposed) {
        return http.postForEntity(
                "/api/v1/twins/" + twinId + "/scenarios",
                new CreateScenarioRequest(
                        "bad", "invalid", "demo-user", null,
                        new ScenarioChangeRequest(parameter, current, proposed)
                ),
                String.class);
    }

    private ActionDetailDto propose(ScenarioDetailDto scenario, double current, double proposed) {
        return proposeRaw(Map.of(
                "parameter", "txPower",
                "currentValue", current,
                "proposedValue", proposed,
                "dryRun", true,
                "scenarioId", scenario.id().toString()
        ));
    }

    private ActionDetailDto proposeRaw(Map<String, Object> parameters) {
        ResponseEntity<ActionDetailDto> response = http.postForEntity(
                "/api/v1/assurance/cases/" + caseId + "/actions",
                new ProposeActionRequest(
                        "SIMULATE_CELL_PARAMETER_CHANGE",
                        "simulation.cell-parameter.v1",
                        "CELL",
                        "CELL-001",
                        parameters,
                        "phase-6 test",
                        "demo-user"
                ),
                ActionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ActionDetailDto approveAndExecute(UUID actionId) {
        ResponseEntity<ActionDetailDto> approved = http.postForEntity(
                "/api/v1/actions/" + actionId + "/approve",
                new ApprovalRequest("demo-approver", "synthetic dry-run only"),
                ActionDetailDto.class);
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        ResponseEntity<ActionDetailDto> executed = http.postForEntity(
                "/api/v1/actions/" + actionId + "/execute", null, ActionDetailDto.class);
        assertEquals(HttpStatus.OK, executed.getStatusCode());
        return executed.getBody();
    }

    private SimulationDetailDto simulationFromAction(ActionDetailDto action) {
        String marker = "\"simulationId\":\"";
        int start = action.result().output().indexOf(marker) + marker.length();
        String id = action.result().output().substring(start, start + 36);
        return http.getForObject("/api/v1/simulations/" + id, SimulationDetailDto.class);
    }

    private static MetricComparisonDto metric(SimulationDetailDto simulation, String name) {
        return simulation.metrics().stream().filter(m -> name.equals(m.metric())).findFirst().orElseThrow();
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
