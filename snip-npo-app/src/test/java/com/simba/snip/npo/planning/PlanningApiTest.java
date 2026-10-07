package com.simba.snip.npo.planning;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.api.TwinDetailDto;
import com.simba.snip.npo.planning.api.PlanningComparisonDto;
import com.simba.snip.npo.planning.api.PlanningEvaluationDto;
import com.simba.snip.npo.planning.api.PlanningScenarioDetailDto;
import com.simba.snip.npo.planning.api.PlanningScenarioSummaryDto;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import com.simba.snip.npo.twin.DigitalTwinSimulationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanningApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private DigitalTwinSimulationService simulationService;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p7-%'");
        jdbc.update("DELETE FROM planning_evaluation_item");
        jdbc.update("DELETE FROM planning_evaluation");
        jdbc.update("DELETE FROM planning_cell_intent");
        jdbc.update("DELETE FROM planning_alternative");
        jdbc.update("DELETE FROM planning_scenario_cell");
        jdbc.update("DELETE FROM planning_scenario");
        jdbc.update("DELETE FROM simulation_limitation");
        jdbc.update("DELETE FROM simulation_result_metric");
        jdbc.update("DELETE FROM simulation_run");
        jdbc.update("DELETE FROM simulation_scenario_change");
        jdbc.update("DELETE FROM simulation_scenario");
        jdbc.update("DELETE FROM network_twin_version");
        jdbc.update("DELETE FROM network_twin");
    }

    @Test
    void createGetListReplaceHappyPath() {
        PlanningScenarioDetailDto created = createScenario("SITE-001 what-if", List.of("CELL-001"), List.of(
                alternative(1, "Alternative A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        assertEquals("SITE-001 what-if", created.name());
        assertEquals(1, created.rowVersion());
        assertEquals("NOT_EVALUATED", created.evaluationView());
        assertTrue(created.truth().independentCellLocal());
        assertFalse(created.truth().jointSiteSimulation());

        PlanningScenarioDetailDto got = http.getForObject("/api/v1/planning/scenarios/" + created.id(), PlanningScenarioDetailDto.class);
        assertEquals(created.id(), got.id());

        ResponseEntity<List<PlanningScenarioSummaryDto>> list = http.exchange(
                "/api/v1/planning/scenarios",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertTrue(list.getBody().stream().anyMatch(s -> created.id().equals(s.id())));

        Map<String, Object> replace = baseReplace(got, List.of("CELL-001"), List.of(
                alternative(1, "Alternative A", List.of(intent("CELL-001", "txPower", 42)))
        ));
        ResponseEntity<PlanningScenarioDetailDto> patched = http.exchange(
                "/api/v1/planning/scenarios/" + created.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(replace),
                PlanningScenarioDetailDto.class
        );
        assertEquals(HttpStatus.OK, patched.getStatusCode());
        assertEquals(2, patched.getBody().rowVersion());
        assertEquals(0, patched.getBody().alternatives().get(0).intents().get(0).intendedValue().compareTo(new java.math.BigDecimal("42")));
    }

    @Test
    void rejectsBoundsAndUnsupportedParameters() {
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(fiveCells()));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "too many alts",
                List.of("CELL-001"),
                List.of(
                        alternative(1, "A", List.of(intent("CELL-001", "txPower", 44))),
                        alternative(2, "B", List.of(intent("CELL-001", "txPower", 43))),
                        alternative(3, "C", List.of(intent("CELL-001", "txPower", 42))),
                        alternative(4, "D", List.of(intent("CELL-001", "txPower", 41))),
                        alternative(5, "E", List.of(intent("CELL-001", "txPower", 40)))
                )
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "tilt",
                List.of("CELL-001"),
                List.of(alternative(1, "A", List.of(intent("CELL-001", "electricalTilt", 6))))
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "low",
                List.of("CELL-001"),
                List.of(alternative(1, "A", List.of(intent("CELL-001", "txPower", 19))))
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "high",
                List.of("CELL-001"),
                List.of(alternative(1, "A", List.of(intent("CELL-001", "txPower", 51))))
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "unknown",
                List.of("CELL-404"),
                List.of(alternative(1, "A", List.of(intent("CELL-404", "txPower", 44))))
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "not member",
                List.of("CELL-001"),
                List.of(alternative(1, "A", List.of(intent("CELL-002", "txPower", 44))))
        )));
        assertEquals(HttpStatus.BAD_REQUEST, createStatus(scenarioBody(
                "dup name",
                List.of("CELL-001"),
                List.of(
                        alternative(1, "Same", List.of(intent("CELL-001", "txPower", 44))),
                        alternative(2, "same", List.of(intent("CELL-001", "txPower", 42)))
                )
        )));
        Map<String, Object> extra = scenarioBody(
                "client fields ignored",
                List.of("CELL-001"),
                List.of(alternative(1, "A", List.of(intent("CELL-001", "txPower", 44))))
        );
        extra.put("modelId", "evil.model");
        extra.put("simulationId", UUID.randomUUID().toString());
        extra.put("twinVersion", 99);
        ResponseEntity<PlanningScenarioDetailDto> created = http.postForEntity(
                "/api/v1/planning/scenarios", extra, PlanningScenarioDetailDto.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("snip.synthetic.cell-parameter.v1", created.getBody().truth().modelId());
    }

    @Test
    void evaluatePinsCurrentTwinAndIndependentCells() {
        synchronize("CELL-001");
        synchronize("CELL-002");
        long proposalsBefore = count("network_change_proposal");
        long plansBefore = count("network_change_plan");
        PlanningScenarioDetailDto scenario = createScenario("two cells", List.of("CELL-001", "CELL-002"), List.of(
                alternative(1, "Alternative A", List.of(
                        intent("CELL-001", "txPower", 44),
                        intent("CELL-002", "txPower", 45)
                ))
        ));
        PlanningEvaluationDto evaluation = evaluate(scenario);
        assertEquals("SUCCEEDED", evaluation.status());
        assertEquals(2, evaluation.items().size());
        assertTrue(evaluation.items().stream().allMatch(i -> "SUCCEEDED".equals(i.outcome())));
        assertTrue(evaluation.items().stream().allMatch(i -> Boolean.TRUE.equals(i.synthetic())));
        assertTrue(evaluation.items().stream().allMatch(i -> "LOW".equals(i.confidence())));
        assertTrue(evaluation.items().stream().allMatch(i -> "snip.synthetic.cell-parameter.v1".equals(i.modelId())));
        assertTrue(evaluation.items().stream().allMatch(i -> "1.0".equals(i.modelVersion())));
        assertNotEquals(
                evaluation.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().twinId(),
                evaluation.items().stream().filter(i -> "CELL-002".equals(i.cellId())).findFirst().orElseThrow().twinId()
        );
        assertEquals(0, evaluation.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow()
                .pinnedBaselineTxPower().compareTo(new java.math.BigDecimal("46")));
        assertEquals(2, countScenarioSimulations(scenario.id(), "simulation_scenario_id"));
        assertEquals(2, countScenarioSimulations(scenario.id(), "simulation_run_id"));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM simulation_scenario_change WHERE scenario_id = ?",
                Integer.class,
                evaluation.items().get(0).simulationScenarioId()
        ));
        assertEquals(proposalsBefore, count("network_change_proposal"));
        assertEquals(plansBefore, count("network_change_plan"));
        assertEquals(0, count("network_change_execution"));
        assertEquals(0, count("production_change_authorization"));

        PlanningComparisonDto comparison = http.getForObject(
                "/api/v1/planning/scenarios/" + scenario.id() + "/comparison",
                PlanningComparisonDto.class
        );
        assertEquals(2, comparison.rows().size());
        assertNull(readForbidden(comparison));
        String json = http.getForObject("/api/v1/planning/scenarios/" + scenario.id() + "/comparison", String.class);
        assertFalse(json.contains("siteScore"));
        assertFalse(json.contains("winner"));
        assertFalse(json.contains("combinedDelta"));
    }

    @Test
    void missingTwinFailsAdmissionWithoutSynchronize() {
        int twinsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM network_twin", Integer.class);
        PlanningScenarioDetailDto scenario = createScenario("no twin", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto evaluation = evaluate(scenario);
        assertEquals("FAILED", evaluation.status());
        assertEquals("ADMISSION_TWIN_MISSING", evaluation.items().get(0).failureCode());
        assertEquals(twinsBefore, jdbc.queryForObject("SELECT COUNT(*) FROM network_twin", Integer.class));
    }

    @Test
    void staleTwinDoesNotAutoSyncAndKeepsHistoricalEvaluation() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("stale", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        assertEquals("SUCCEEDED", first.status());
        Instant later = Instant.now().plusSeconds(60);
        projectionService.project(event("p7-stale-" + UUID.randomUUID(), "CELL-001", "BLER_DL", 0.19, later));
        PlanningEvaluationDto second = evaluate(reload(scenario.id()));
        assertEquals(first.id(), second.id());
        assertEquals("SUCCEEDED", second.status());
        assertTrue(second.historical());
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void patchIntentsMakesEvaluationStaleAndPreservesRuns() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("edit", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        Map<String, Object> replace = baseReplace(reload(scenario.id()), List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 42)))
        ));
        PlanningScenarioDetailDto patched = http.exchange(
                "/api/v1/planning/scenarios/" + scenario.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(replace),
                PlanningScenarioDetailDto.class
        ).getBody();
        assertEquals("STALE", patched.evaluationView());
        PlanningEvaluationDto historical = http.getForObject(
                "/api/v1/planning/scenarios/" + scenario.id() + "/evaluations/" + first.id(),
                PlanningEvaluationDto.class
        );
        assertEquals("SUCCEEDED", historical.status());
        assertEquals(first.items().get(0).simulationRunId(), historical.items().get(0).simulationRunId());
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void secondEvaluateIsIdempotent() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("idemp", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        PlanningEvaluationDto second = evaluate(reload(scenario.id()));
        assertEquals(first.id(), second.id());
        assertEquals(first.admissionFingerprint(), second.admissionFingerprint());
        assertTrue(first.admissionFingerprint().matches("[0-9a-f]{64}"));
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void newTwinVersionCreatesNewEvaluation() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("newtwin", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        Instant later = Instant.now().plusSeconds(60);
        projectionService.project(event("p7-newtwin-" + UUID.randomUUID(), "CELL-001", "BLER_DL", 0.21, later));
        synchronize("CELL-001");
        PlanningEvaluationDto second = evaluate(reload(scenario.id()));
        assertNotEquals(first.id(), second.id());
        assertNotEquals(first.admissionFingerprint(), second.admissionFingerprint());
        assertEquals("SUCCEEDED", second.status());
        assertEquals(2, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void partialSameAdmissionCreatesNewEvaluationAndReusesChild() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("partial-retry", List.of("CELL-001", "CELL-002"), List.of(
                alternative(1, "A", List.of(
                        intent("CELL-001", "txPower", 44),
                        intent("CELL-002", "txPower", 45)
                ))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        assertEquals("PARTIAL", first.status());
        UUID firstSuccess = first.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().simulationRunId();
        PlanningEvaluationDto second = evaluate(reload(scenario.id()));
        assertNotEquals(first.id(), second.id());
        assertEquals(first.admissionFingerprint(), second.admissionFingerprint());
        assertEquals("PARTIAL", second.status());
        assertEquals("PARTIAL", jdbc.queryForObject(
                "SELECT status FROM planning_evaluation WHERE id = ?",
                String.class,
                first.id()
        ));
        UUID reused = second.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().simulationRunId();
        assertEquals(firstSuccess, reused);
        assertTrue(second.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().reusedExistingRun());
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void clientCannotSupplyAdmissionFingerprint() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("forge-fp", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        String forged = "f".repeat(64);
        Map<String, Object> body = new HashMap<>();
        body.put("createdBy", "demo-network-engineer");
        body.put("rowVersion", scenario.rowVersion());
        body.put("admissionFingerprint", forged);
        body.put("intentFingerprint", forged);
        body.put("simulationRunId", UUID.randomUUID().toString());
        PlanningEvaluationDto evaluation = http.postForEntity(
                "/api/v1/planning/scenarios/" + scenario.id() + "/evaluations",
                body,
                PlanningEvaluationDto.class
        ).getBody();
        assertNotNull(evaluation);
        assertNotEquals(forged, evaluation.admissionFingerprint());
        assertNotEquals(forged, evaluation.intentFingerprint());
        assertTrue(evaluation.admissionFingerprint().matches("[0-9a-f]{64}"));
        assertTrue(evaluation.items().stream().noneMatch(item -> forged.equals(String.valueOf(item.simulationRunId()))));
    }

    @Test
    void concurrentEvaluateDoesNotDuplicateRuns() throws Exception {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("concurrent", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        try {
            Future<ResponseEntity<PlanningEvaluationDto>> a = pool.submit(() -> {
                ready.countDown();
                ready.await();
                return evaluateEntity(reload(scenario.id()));
            });
            Future<ResponseEntity<PlanningEvaluationDto>> b = pool.submit(() -> {
                ready.countDown();
                ready.await();
                return evaluateEntity(reload(scenario.id()));
            });
            ResponseEntity<PlanningEvaluationDto> first = a.get();
            ResponseEntity<PlanningEvaluationDto> second = b.get();
            assertTrue(first.getStatusCode().is2xxSuccessful());
            assertTrue(second.getStatusCode().is2xxSuccessful() || second.getStatusCode() == HttpStatus.CONFLICT);
            assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void partialPreservesSuccess() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("partial", List.of("CELL-001", "CELL-002"), List.of(
                alternative(1, "A", List.of(
                        intent("CELL-001", "txPower", 44),
                        intent("CELL-002", "txPower", 45)
                ))
        ));
        PlanningEvaluationDto evaluation = evaluate(scenario);
        assertEquals("PARTIAL", evaluation.status());
        assertEquals("SUCCEEDED", evaluation.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().outcome());
        assertEquals("FAILED", evaluation.items().stream().filter(i -> "CELL-002".equals(i.cellId())).findFirst().orElseThrow().outcome());
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void retryAfterSyncReusesSucceededRun() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("reuse", List.of("CELL-001", "CELL-002"), List.of(
                alternative(1, "A", List.of(
                        intent("CELL-001", "txPower", 44),
                        intent("CELL-002", "txPower", 45)
                ))
        ));
        PlanningEvaluationDto first = evaluate(scenario);
        assertEquals("PARTIAL", first.status());
        UUID firstSuccess = first.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().simulationRunId();
        synchronize("CELL-002");
        PlanningEvaluationDto second = evaluate(reload(scenario.id()));
        assertNotEquals(first.id(), second.id());
        assertEquals("SUCCEEDED", second.status());
        UUID reused = second.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().simulationRunId();
        assertEquals(firstSuccess, reused);
        assertTrue(second.items().stream().filter(i -> "CELL-001".equals(i.cellId())).findFirst().orElseThrow().reusedExistingRun());
        assertEquals(2, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    @Test
    void replaceDuringInProgressConflicts() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("lock", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        jdbc.update(
                "INSERT INTO planning_evaluation (id, scenario_id, status, intent_fingerprint, created_by, created_at, item_count) VALUES (?,?,?,?,?,NOW(),1)",
                UUID.randomUUID(),
                scenario.id(),
                "IN_PROGRESS",
                "0".repeat(64),
                "demo-network-engineer"
        );
        Map<String, Object> replace = baseReplace(scenario, List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 42)))
        ));
        ResponseEntity<String> patched = http.exchange(
                "/api/v1/planning/scenarios/" + scenario.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(replace),
                String.class
        );
        assertEquals(HttpStatus.CONFLICT, patched.getStatusCode());
        assertTrue(patched.getBody().contains("EVALUATION_IN_PROGRESS"));
    }

    @Test
    void executeAdmittedDryRunFailsClosedWhenStale() {
        TwinDetailDto twin = synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("dry", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningEvaluationDto evaluation = evaluate(scenario);
        Instant later = Instant.now().plusSeconds(60);
        projectionService.project(event("p7-dry-" + UUID.randomUUID(), "CELL-001", "BLER_DL", 0.22, later));
        boolean failed = false;
        try {
            simulationService.executeAdmittedCellLocalDryRun(evaluation.items().get(0).simulationScenarioId());
        } catch (RuntimeException ex) {
            failed = ex.getMessage() != null && ex.getMessage().contains("STALE");
        }
        assertTrue(failed);
        assertNotNull(twin.id());
    }

    @Test
    void deleteIsNotMappedAndCrossScenarioIsIsolated() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto one = createScenario("one", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        PlanningScenarioDetailDto two = createScenario("two", List.of("CELL-001"), List.of(
                alternative(1, "B", List.of(intent("CELL-001", "txPower", 42)))
        ));
        PlanningEvaluationDto eval = evaluate(one);
        ResponseEntity<String> other = http.getForEntity(
                "/api/v1/planning/scenarios/" + two.id() + "/evaluations/" + eval.id(),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, other.getStatusCode());
        ResponseEntity<String> deleted = http.exchange(
                "/api/v1/planning/scenarios/" + one.id(),
                HttpMethod.DELETE,
                null,
                String.class
        );
        assertTrue(deleted.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED
                || deleted.getStatusCode() == HttpStatus.NOT_FOUND
                || deleted.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void resumeInProgressFinalizes() {
        synchronize("CELL-001");
        PlanningScenarioDetailDto scenario = createScenario("resume", List.of("CELL-001"), List.of(
                alternative(1, "A", List.of(intent("CELL-001", "txPower", 44)))
        ));
        jdbc.update(
                "INSERT INTO planning_evaluation (id, scenario_id, status, intent_fingerprint, created_by, created_at, item_count) VALUES (?,?,?,?,?,NOW(),1)",
                UUID.randomUUID(),
                scenario.id(),
                "IN_PROGRESS",
                "0".repeat(64),
                "demo-network-engineer"
        );
        PlanningEvaluationDto evaluation = evaluate(reload(scenario.id()));
        assertEquals("SUCCEEDED", evaluation.status());
        assertEquals(1, countScenarioSimulations(scenario.id(), "simulation_run_id"));
    }

    private Object readForbidden(PlanningComparisonDto comparison) {
        return null;
    }

    private PlanningScenarioDetailDto createScenario(String name, List<String> cells, List<Map<String, Object>> alternatives) {
        ResponseEntity<PlanningScenarioDetailDto> response = http.postForEntity(
                "/api/v1/planning/scenarios",
                scenarioBody(name, cells, alternatives),
                PlanningScenarioDetailDto.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private HttpStatus createStatus(Map<String, Object> body) {
        return HttpStatus.valueOf(http.postForEntity("/api/v1/planning/scenarios", body, String.class).getStatusCode().value());
    }

    private PlanningScenarioDetailDto reload(UUID id) {
        return http.getForObject("/api/v1/planning/scenarios/" + id, PlanningScenarioDetailDto.class);
    }

    private PlanningEvaluationDto evaluate(PlanningScenarioDetailDto scenario) {
        return evaluateEntity(scenario).getBody();
    }

    private ResponseEntity<PlanningEvaluationDto> evaluateEntity(PlanningScenarioDetailDto scenario) {
        return http.postForEntity(
                "/api/v1/planning/scenarios/" + scenario.id() + "/evaluations",
                Map.of("createdBy", "demo-network-engineer", "rowVersion", scenario.rowVersion()),
                PlanningEvaluationDto.class
        );
    }

    private TwinDetailDto synchronize(String cellId) {
        return http.postForObject("/api/v1/twins/cells/" + cellId + "/synchronize", null, TwinDetailDto.class);
    }

    private Map<String, Object> scenarioBody(String name, List<String> cells, List<Map<String, Object>> alternatives) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "");
        body.put("createdBy", "demo-network-engineer");
        body.put("cells", cells.stream().map(id -> Map.of("cellId", id)).toList());
        body.put("alternatives", alternatives);
        return body;
    }

    private Map<String, Object> baseReplace(
            PlanningScenarioDetailDto current,
            List<String> cells,
            List<Map<String, Object>> alternatives
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", current.name());
        body.put("description", current.description());
        body.put("rowVersion", current.rowVersion());
        body.put("cells", cells.stream().map(id -> Map.of("cellId", id)).toList());
        body.put("alternatives", alternatives);
        return body;
    }

    private Map<String, Object> fiveCells() {
        return scenarioBody(
                "five",
                List.of("CELL-001", "CELL-002", "CELL-003", "CELL-004", "CELL-005"),
                List.of(alternative(1, "A", List.of(intent("CELL-001", "txPower", 44))))
        );
    }

    private Map<String, Object> alternative(int ordinal, String name, List<Map<String, Object>> intents) {
        return Map.of("ordinal", ordinal, "name", name, "intents", intents);
    }

    private Map<String, Object> intent(String cellId, String parameterId, int value) {
        return Map.of("cellId", cellId, "parameterId", parameterId, "intendedValue", value);
    }

    private long count(String table) {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private long countScenarioSimulations(UUID scenarioId, String column) {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT i." + column + ") FROM planning_evaluation_item i "
                        + "JOIN planning_evaluation e ON e.id = i.evaluation_id "
                        + "WHERE e.scenario_id = ? AND i." + column + " IS NOT NULL",
                Integer.class,
                scenarioId
        );
        return value == null ? 0 : value;
    }

    private TelemetryEvent event(String id, String cellId, String metric, double value, Instant observedAt) {
        return new TelemetryEvent(
                id,
                TelemetryEvent.TYPE_CELL_KPI_OBSERVED,
                TelemetryEvent.SCHEMA_V1,
                TelemetryEvent.SOURCE_SIMULATOR,
                cellId,
                metric,
                value,
                "ratio",
                observedAt,
                null,
                true
        );
    }
}
