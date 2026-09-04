package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.SyntheticAssuranceFixtureCleanup;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionAdapter;
import com.simba.snip.npo.changeexecution.api.AuthorizeExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CreateExecutionRequest;
import com.simba.snip.npo.changeexecution.api.ExecutionDetailDto;
import com.simba.snip.npo.changeexecution.api.ReviewExecutionRequest;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.SimulatorFailureMode;
import com.simba.snip.npo.changeexecution.security.ChangeExecutionAuthorizer;
import com.simba.snip.npo.changeintelligence.api.ChangeProposalDetailDto;
import com.simba.snip.npo.changeintelligence.api.GenerateChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.api.ReviewChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeplanning.api.AuthorizeChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ChangePlanDetailDto;
import com.simba.snip.npo.changeplanning.api.CreateChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ReviewChangePlanRequest;
import com.simba.snip.npo.changeplanning.authorization.ChangePlanAuthorizer;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangeExecutionSimulatorTest extends AbstractPostgresIT {

    private static final String CELL = "CELL-001";
    private static final String SEED_TX_POWER = "46";
    private static final String TARGET = SimulatorExecutionAdapter.TARGET_ID;

    @Autowired private TestRestTemplate http;
    @Autowired private SimulatorExecutionAdapter simulatorAdapter;
    @Autowired private TelemetryProjectionService projectionService;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private SimulatorEnmSyncState syncState;
    @Autowired private JdbcTemplate jdbc;

    private UUID assuranceCaseId;

    @DynamicPropertySource
    static void enableChangeExecution(DynamicPropertyRegistry registry) {
        registry.add("snip.change-execution.enabled", () -> "true");
    }

    @BeforeEach
    void fixtures() {
        simulatorAdapter.clearFailureMode();
        seedTelemetry();
        runTrustedBaseline();
        assuranceCaseId = assuranceCaseService.listForCell(CELL).stream()
                .findFirst()
                .map(c -> c.getId())
                .orElse(null);
        http.postForEntity("/api/v1/twins/cells/" + CELL + "/synchronize", null, Map.class);
    }

    @AfterEach
    void cleanup() {
        simulatorAdapter.clearFailureMode();
        cleanupPhase15();
        cleanupPhase14();
        cleanupPhase13();
        SyntheticAssuranceFixtureCleanup.deleteAndAssertSyntheticDegradingCases(jdbc, CELL);
        restoreSharedPriorPhaseState();
    }

    @Test
    void failureInjectionModesSupported() {
        assertTrue(Arrays.stream(SimulatorFailureMode.values()).anyMatch(m -> m == SimulatorFailureMode.SUCCESS));
        assertTrue(Arrays.stream(SimulatorFailureMode.values()).anyMatch(m -> m == SimulatorFailureMode.TIMEOUT_AFTER_APPLY));
        assertTrue(Arrays.stream(SimulatorFailureMode.values()).anyMatch(m -> m == SimulatorFailureMode.APPLY_WRONG_VALUE));
        simulatorAdapter.setFailureMode(SimulatorFailureMode.REJECT_BEFORE_APPLY);
        simulatorAdapter.clearFailureMode();
    }

    @Test
    void timeoutAfterApplyOutcomeUnknownThenVerified() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.TIMEOUT_AFTER_APPLY);
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        ExecutionDetailDto executed = execute(created.executionId());
        assertEquals(ExecutionStatus.EXECUTION_OUTCOME_UNKNOWN.name(), executed.status());
        ExecutionDetailDto verified = verify(created.executionId());
        assertEquals(ExecutionStatus.VERIFIED.name(), verified.status());
        Integer attempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ? AND direction = 'FORWARD'",
                Integer.class,
                created.executionId());
        assertEquals(1, attempts);
    }

    @Test
    void rejectBeforeApplyPersistsFailureWithZeroTargetMutation() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.REJECT_BEFORE_APPLY);
        ExecutionDetailDto created = governedExecution();
        ExecutionDetailDto failed = execute(created.executionId());
        assertEquals(ExecutionStatus.EXECUTION_FAILED.name(), failed.status());
        assertEquals("EXECUTION_OPERATION_REJECTED", failed.failureCode());
        assertEquals(1, forwardAttempts(created.executionId()));
        assertEquals(SEED_TX_POWER, simulatorValue());
    }

    @Test
    void timeoutBeforeApplyPersistsFailureWithZeroTargetMutation() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.TIMEOUT_BEFORE_APPLY);
        ExecutionDetailDto created = governedExecution();
        ExecutionDetailDto failed = execute(created.executionId());
        assertEquals(ExecutionStatus.EXECUTION_FAILED.name(), failed.status());
        assertEquals("EXECUTION_OPERATION_TIMEOUT", failed.failureCode());
        assertEquals(1, forwardAttempts(created.executionId()));
        assertEquals(SEED_TX_POWER, simulatorValue());
    }

    @Test
    void readbackTimeoutCannotVerifyAndDurableEvidenceSurvivesResponse() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.READBACK_TIMEOUT);
        ExecutionDetailDto created = governedExecution();
        ExecutionDetailDto failed = execute(created.executionId());
        assertEquals(ExecutionStatus.VERIFICATION_FAILED.name(), failed.status());
        assertEquals("EXECUTION_VERIFICATION_TIMEOUT", failed.failureCode());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_verification WHERE execution_id = ? AND outcome = 'TIMEOUT'",
                Integer.class, created.executionId()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_audit_event WHERE execution_id = ? AND event_type = 'VERIFICATION_FAILED'",
                Integer.class, created.executionId()));
    }

    @Test
    void ambiguousOutcomeObservedAtPreChangeValueStopsWithoutRetry() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.TIMEOUT_AFTER_APPLY);
        ExecutionDetailDto created = governedExecution();
        assertEquals(ExecutionStatus.EXECUTION_OUTCOME_UNKNOWN.name(), execute(created.executionId()).status());
        jdbc.update(
                """
                UPDATE simulator_execution_cell_state
                SET parameter_value = ?, revision = revision + 1, updated_at = NOW()
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                SEED_TX_POWER, TARGET, CELL);
        ExecutionDetailDto stopped = verify(created.executionId());
        assertEquals(ExecutionStatus.MANUAL_INTERVENTION_REQUIRED.name(), stopped.status());
        assertTrue(stopped.failureDetailSafe().contains("pre-change value"));
        assertEquals(1, forwardAttempts(created.executionId()));
    }

    @Test
    void wrongValueVerificationFailedRecoveryRequired() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.APPLY_WRONG_VALUE);
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        ExecutionDetailDto executed = execute(created.executionId());
        // Spec: VERIFICATION_FAILED → RECOVERY_REQUIRED (recovery evaluation is automatic; rollback is not).
        assertEquals(ExecutionStatus.RECOVERY_REQUIRED.name(), executed.status());
        assertEquals("REQUIRED", executed.recoveryStatus());
    }

    @Test
    void rollbackCurrentMismatchManualIntervention() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        jdbc.update(
                "UPDATE network_change_execution SET status = ?, recovery_status = ? WHERE id = ?",
                ExecutionStatus.VERIFICATION_FAILED.name(), "REQUIRED", created.executionId());
        requestRollback(created.executionId());
        reviewRollback(created.executionId());
        authorizeRollback(created.executionId());
        jdbc.update(
                """
                UPDATE simulator_execution_cell_state
                SET parameter_value = '99'
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                TARGET, CELL);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/rollback/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.ROLLBACK_CURRENT_VALUE_MISMATCH.name()));
    }

    @Test
    void staleReadbackCannotVerify() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.READBACK_STALE);
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        ExecutionDetailDto executed = execute(created.executionId());
        assertNotEquals(ExecutionStatus.VERIFIED.name(), executed.status());
    }

    @Test
    void rollbackOutcomeUnknownRequiresReadback() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.ROLLBACK_TIMEOUT_AFTER_APPLY);
        UUID planId = readyPlanId();
        String original = SEED_TX_POWER;
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        jdbc.update(
                "UPDATE network_change_execution SET status = ?, recovery_status = ? WHERE id = ?",
                ExecutionStatus.VERIFICATION_FAILED.name(), "REQUIRED", created.executionId());
        requestRollback(created.executionId());
        reviewRollback(created.executionId());
        authorizeRollback(created.executionId());
        simulatorAdapter.setFailureMode(SimulatorFailureMode.ROLLBACK_TIMEOUT_AFTER_APPLY);
        ExecutionDetailDto outcomeUnknown = rollbackExecute(created.executionId());
        assertEquals(ExecutionStatus.ROLLBACK_OUTCOME_UNKNOWN.name(), outcomeUnknown.status());
        ResponseEntity<String> blindRetry = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/rollback/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_AUTHORIZE),
                String.class);
        assertTrue(blindRetry.getStatusCode().is4xxClientError());
        String simulatorValue = jdbc.queryForObject(
                """
                SELECT parameter_value FROM simulator_execution_cell_state
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                String.class,
                TARGET, CELL);
        assertEquals(original, simulatorValue);
        Integer rollbackAttempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ? AND direction = 'ROLLBACK'",
                Integer.class,
                created.executionId());
        assertEquals(1, rollbackAttempts);
        ExecutionDetailDto verifiedRollback = verify(created.executionId());
        assertEquals(ExecutionStatus.ROLLED_BACK.name(), verifiedRollback.status());
        assertEquals(1, rollbackAttempts);
    }

    @Test
    void thirdValueAfterAmbiguousForwardManualIntervention() {
        simulatorAdapter.setFailureMode(SimulatorFailureMode.TIMEOUT_AFTER_APPLY);
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        jdbc.update(
                """
                UPDATE simulator_execution_cell_state
                SET parameter_value = '99'
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                TARGET, CELL);
        ExecutionDetailDto verified = verify(created.executionId());
        assertEquals(ExecutionStatus.MANUAL_INTERVENTION_REQUIRED.name(), verified.status());
        assertEquals("MANUAL_INTERVENTION", verified.recoveryStatus());
    }

    private ExecutionDetailDto governedExecution() {
        ExecutionDetailDto created = createExecution(readyPlanId());
        review(created.executionId());
        authorize(created.executionId());
        return created;
    }

    private int forwardAttempts(UUID executionId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ? AND direction = 'FORWARD'",
                Integer.class, executionId);
    }

    private String simulatorValue() {
        return jdbc.queryForObject(
                "SELECT parameter_value FROM simulator_execution_cell_state WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'",
                String.class, TARGET, CELL);
    }

    private UUID readyPlanId() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto plan = createPlan(proposalId);
        reviewPlan(plan.plan().id());
        authorizePlan(plan.plan().id());
        evaluateReadiness(plan.plan().id());
        ChangePlanDetailDto ready = getPlan(plan.plan().id());
        assertEquals(PlanStatus.READY_FOR_EXECUTION.name(), ready.plan().status());
        return ready.plan().id();
    }

    private ExecutionDetailDto createExecution(UUID planId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planId, TARGET), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    private void review(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/review",
                HttpMethod.POST,
                execEntity(new ReviewExecutionRequest("reviewer", "ok"), ChangeExecutionAuthorizer.PERMISSION_REVIEW),
                ExecutionDetailDto.class);
    }

    private void authorize(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("authorizer"), ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                ExecutionDetailDto.class);
    }

    private ExecutionDetailDto execute(UUID executionId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ExecutionDetailDto verify(UUID executionId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/verify",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_VIEW),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private void requestRollback(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/rollback/request",
                HttpMethod.POST,
                execEntity(new ReviewExecutionRequest("operator", "rollback"), ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_REQUEST),
                ExecutionDetailDto.class);
    }

    private void reviewRollback(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/rollback/review",
                HttpMethod.POST,
                execEntity(new ReviewExecutionRequest("reviewer", "ok"), ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_REVIEW),
                ExecutionDetailDto.class);
    }

    private void authorizeRollback(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/rollback/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("authorizer"), ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_AUTHORIZE),
                ExecutionDetailDto.class);
    }

    private ExecutionDetailDto rollbackExecute(UUID executionId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/rollback/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_AUTHORIZE),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangeProposalDetailDto generateProposal() {
        ResponseEntity<ChangeProposalDetailDto> response = http.exchange(
                "/api/v1/change-intelligence/proposals",
                HttpMethod.POST,
                proposalEntity(new GenerateChangeProposalRequest("CELL", CELL, "txPower", assuranceCaseId, null,
                        GenerationInitiator.MANUAL, "generator"), ChangeProposalAuthorizer.PERMISSION_GENERATE),
                ChangeProposalDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private UUID approveProposal(ChangeProposalDetailDto recommended) {
        assertEquals(ProposalStatus.RECOMMENDED.name(), recommended.proposal().status());
        http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                proposalEntity(new ReviewChangeProposalRequest("approver", null, "approved"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                ChangeProposalDetailDto.class);
        return recommended.proposal().id();
    }

    private ChangePlanDetailDto createPlan(UUID proposalId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                planEntity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private void reviewPlan(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/review",
                HttpMethod.POST,
                planEntity(new ReviewChangePlanRequest("reviewer", "reviewed"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                ChangePlanDetailDto.class);
    }

    private void authorizePlan(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/authorize",
                HttpMethod.POST,
                planEntity(new AuthorizeChangePlanRequest("authorizer"), ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
    }

    private void evaluateReadiness(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/readiness",
                HttpMethod.POST,
                planEntity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
    }

    private ChangePlanDetailDto getPlan(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId,
                HttpMethod.GET,
                planEntity(null, ChangePlanAuthorizer.PERMISSION_VIEW),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private void cleanupPhase15() {
        jdbc.update("DELETE FROM network_change_execution_lease");
        jdbc.update("DELETE FROM network_change_execution_audit_event");
        jdbc.update("DELETE FROM network_change_execution_verification");
        jdbc.update("DELETE FROM network_change_execution_recovery");
        jdbc.update("DELETE FROM network_change_execution_attempt");
        jdbc.update("DELETE FROM network_change_execution_authorization");
        jdbc.update("DELETE FROM network_change_execution_rollback");
        jdbc.update("DELETE FROM network_change_execution_operation");
        jdbc.update("DELETE FROM simulator_execution_cell_state");
        jdbc.update("DELETE FROM network_change_execution");
    }

    private void cleanupPhase14() {
        jdbc.update("DELETE FROM network_change_plan_audit_event");
        jdbc.update("DELETE FROM network_change_plan_readiness_assessment");
        jdbc.update("DELETE FROM network_change_plan_review");
        jdbc.update("DELETE FROM network_change_plan_precondition");
        jdbc.update("DELETE FROM network_change_plan_operation_dependency");
        jdbc.update("DELETE FROM network_change_plan_rollback_operation");
        jdbc.update("DELETE FROM network_change_plan_operation");
        jdbc.update("DELETE FROM network_change_plan");
    }

    private void cleanupPhase13() {
        jdbc.update("DELETE FROM change_proposal_audit_event");
        jdbc.update("DELETE FROM change_proposal_review");
        jdbc.update("DELETE FROM network_change_candidate");
        jdbc.update("DELETE FROM network_change_proposal");
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p15-sim-%'");
    }

    private void seedTelemetry() {
        // Must stay inside snip.recent-kpi-hours (168h) relative to Instant.now().
        Instant t0 = Instant.now().minusSeconds(3_600);
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            String prefix = "p15-sim-" + UUID.randomUUID();
            projectionService.project(event(prefix + "-bler", CELL, "BLER_DL", 0.05 + i * 0.02, ts));
            projectionService.project(event(prefix + "-prb", CELL, "PRB_UTILIZATION_DL", 0.60 + i * 0.05, ts));
        }
    }

    private void runTrustedBaseline() {
        syncState.resetAll();
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update(
                "UPDATE synchronization_checkpoint SET status = 'VALID' WHERE status = 'RECOVERY_REQUIRED'");
        jdbc.update(
                """
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
        vendorImportAuthorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
    }

    private void setKnowledge(String confidence) {
        jdbc.update(
                "UPDATE network_knowledge_status SET confidence = ?, reason_codes = ?, freshness = 'FRESH', source_health = 'HEALTHY'",
                confidence, confidence.equals("HIGH") ? "TRUSTED_BASELINE" : "DEGRADED");
    }

    private void restoreSharedPriorPhaseState() {
        syncState.resetAll();
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update(
                "UPDATE synchronization_checkpoint SET status = 'VALID' WHERE status = 'RECOVERY_REQUIRED'");
        jdbc.update(
                """
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                SEED_TX_POWER, CELL);
        jdbc.update(
                "UPDATE network_knowledge_status SET confidence = 'HIGH', reason_codes = 'TRUSTED_BASELINE', freshness = 'FRESH', source_health = 'HEALTHY'");
    }

    private <T> HttpEntity<T> execEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeExecutionAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> planEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangePlanAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> proposalEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeProposalAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
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
