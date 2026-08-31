package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionAdapter;
import com.simba.snip.npo.changeexecution.api.AuthorizeExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CancelExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CreateExecutionRequest;
import com.simba.snip.npo.changeexecution.api.ExecutionDetailDto;
import com.simba.snip.npo.changeexecution.api.ReviewExecutionRequest;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.SimulatorFailureMode;
import com.simba.snip.npo.changeexecution.security.ChangeExecutionAuthorizer;
import com.simba.snip.npo.changeexecution.service.ExecutionLeaseService;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangeExecutionApiTest extends AbstractPostgresIT {

    private static final String CELL = "CELL-001";
    private static final String SEED_TX_POWER = "46";
    private static final String TARGET = SimulatorExecutionAdapter.TARGET_ID;

    @Autowired private TestRestTemplate http;
    @Autowired private TelemetryProjectionService projectionService;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private SimulatorEnmSyncState syncState;
    @Autowired private SimulatorExecutionAdapter simulatorAdapter;
    @Autowired private ExecutionLeaseService leaseService;
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
        jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase14-test-drift'");
        restoreSharedPriorPhaseState();
    }

    @Test
    void successfulExecutionFlow() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        assertEquals(ExecutionStatus.READY_FOR_REVIEW.name(), created.status());

        review(created.executionId());
        authorize(created.executionId());
        ExecutionDetailDto executed = execute(created.executionId());
        assertEquals(ExecutionStatus.VERIFIED.name(), executed.status());
        assertNotNull(executed.completedAt());
    }

    @Test
    void expectedStateMismatchBlocksMutation() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        jdbc.update(
                """
                INSERT INTO simulator_execution_cell_state (id, target_id, cell_id, parameter_name, parameter_value, revision, updated_at)
                VALUES (?, ?, ?, 'txPower', '45', 1, NOW())
                ON CONFLICT (target_id, cell_id, parameter_name)
                DO UPDATE SET parameter_value = '45', revision = simulator_execution_cell_state.revision + 1, updated_at = NOW()
                """,
                UUID.randomUUID(), TARGET, CELL);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_CURRENT_VALUE_MISMATCH.name()));
        ExecutionDetailDto reloaded = get(created.executionId());
        assertNotEquals(ExecutionStatus.VERIFIED.name(), reloaded.status());
    }

    @Test
    void planNotReadyForExecutionRejected() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto plan = createPlan(proposalId);
        reviewPlan(plan.plan().id());
        authorizePlan(plan.plan().id());
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(plan.plan().id(), TARGET), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_PLAN_NOT_READY.name()));
    }

    @Test
    void authorizeRequiresReview() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("authorizer"), ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    @Test
    void staleAuthorizationBlocksExecute() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        jdbc.update(
                "UPDATE network_change_execution SET authorized_execution_fingerprint = ? WHERE id = ?",
                "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
                created.executionId());
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE.name())
                || conflict.getBody().contains(ExecutionFailureCode.EXECUTION_AUTHORIZATION_MISSING.name()));
    }

    @Test
    void duplicateExecuteAfterVerifiedIsIdempotent() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        Integer attemptsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ?",
                Integer.class,
                created.executionId());
        ExecutionDetailDto second = execute(created.executionId());
        assertEquals(ExecutionStatus.VERIFIED.name(), second.status());
        Integer attemptsAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ?",
                Integer.class,
                created.executionId());
        assertEquals(attemptsBefore, attemptsAfter);
    }

    @Test
    void duplicatePlanExecutionRejected() {
        UUID planId = readyPlanId();
        ExecutionDetailDto first = createExecution(planId);
        review(first.executionId());
        authorize(first.executionId());
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planId, TARGET), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_CONFLICT.name()));
    }

    @Test
    void concurrentSameTargetOneAuthority() {
        UUID planA = readyPlanId();
        UUID planB = readyPlanId();
        ExecutionDetailDto execA = createExecution(planA);
        review(execA.executionId());
        authorize(execA.executionId());
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planB, TARGET), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_CONFLICT.name()));
    }

    @Test
    void activeLeaseBlocksMutationBeforeFinalPreflight() {
        ExecutionDetailDto created = createExecution(readyPlanId());
        review(created.executionId());
        authorize(created.executionId());
        ExecutionLeaseService.ExecutionLease held = leaseService.acquire(
                TARGET, CELL, "txPower", created.executionId()).orElseThrow();
        try {
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-execution/executions/" + created.executionId() + "/execute",
                    HttpMethod.POST,
                    execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_LEASE_UNAVAILABLE.name()));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM network_change_execution_attempt WHERE execution_id = ?",
                    Integer.class, created.executionId()));
        } finally {
            leaseService.release(held);
        }
    }

    @Test
    void staleFencingTokenCannotRetainExecutionAuthority() {
        ExecutionDetailDto created = createExecution(readyPlanId());
        ExecutionLeaseService.ExecutionLease lease = leaseService.acquire(
                TARGET, CELL, "txPower", created.executionId()).orElseThrow();
        jdbc.update("UPDATE network_change_execution_lease SET fencing_token = fencing_token + 1 WHERE lease_key = ?",
                lease.leaseKey());
        ChangeExecutionException stale = assertThrows(
                ChangeExecutionException.class, () -> leaseService.assertOwnership(lease));
        assertEquals(ExecutionFailureCode.EXECUTION_FENCING_TOKEN_STALE, stale.failureCode());
    }

    @Test
    void executionWindowClosedBlocksExecute() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        jdbc.update(
                "UPDATE network_change_execution SET execution_window_closes_at = NOW() - INTERVAL '1 minute' WHERE id = ?",
                created.executionId());
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ExecutionFailureCode.EXECUTION_WINDOW_CLOSED.name()));
    }

    @Test
    void cancelBeforeMutation() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        ResponseEntity<ExecutionDetailDto> cancelled = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/cancel",
                HttpMethod.POST,
                execEntity(new CancelExecutionRequest("operator", "cancelled"), ChangeExecutionAuthorizer.PERMISSION_CANCEL),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        assertEquals(ExecutionStatus.CANCELLED_BEFORE_MUTATION.name(), cancelled.getBody().status());
    }

    @Test
    void simulatorMutationLeavesCanonicalUnchanged() {
        String canonicalBefore = canonicalTxPower();
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        assertEquals(canonicalBefore, canonicalTxPower());
        String simulatorValue = jdbc.queryForObject(
                """
                SELECT parameter_value FROM simulator_execution_cell_state
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                String.class,
                TARGET, CELL);
        assertNotEquals(canonicalBefore, simulatorValue);
    }

    @Test
    void rollbackWithoutAuthorizationRejected() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        execute(created.executionId());
        jdbc.update(
                "UPDATE network_change_execution SET status = ?, recovery_status = ? WHERE id = ?",
                ExecutionStatus.VERIFICATION_FAILED.name(), "REQUIRED", created.executionId());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/rollback/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_ROLLBACK_AUTHORIZE),
                String.class);
        assertTrue(forbidden.getStatusCode().is4xxClientError());
    }

    @Test
    void authorizedRollbackFlow() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        ExecutionDetailDto verified = execute(created.executionId());
        assertEquals(ExecutionStatus.VERIFIED.name(), verified.status());
        String applied = jdbc.queryForObject(
                """
                SELECT parameter_value FROM simulator_execution_cell_state
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                String.class,
                TARGET, CELL);
        jdbc.update(
                "UPDATE network_change_execution SET status = ?, recovery_status = ? WHERE id = ?",
                ExecutionStatus.VERIFICATION_FAILED.name(), "REQUIRED", created.executionId());
        requestRollback(created.executionId());
        reviewRollback(created.executionId());
        authorizeRollback(created.executionId());
        ExecutionDetailDto rolledBack = rollbackExecute(created.executionId());
        assertEquals(ExecutionStatus.ROLLED_BACK.name(), rolledBack.status());
        assertEquals(SEED_TX_POWER, jdbc.queryForObject(
                """
                SELECT parameter_value FROM simulator_execution_cell_state
                WHERE target_id = ? AND cell_id = ? AND parameter_name = 'txPower'
                """,
                String.class,
                TARGET, CELL));
        assertNotEquals(SEED_TX_POWER, applied);
    }

    @Test
    void controlledSandboxUnknownEnvironmentRejected() {
        UUID planId = readyPlanId();
        ResponseEntity<String> notFound = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planId, "controlled-sandbox-unknown"), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                String.class);
        assertTrue(notFound.getStatusCode().is4xxClientError());
        assertTrue(notFound.getBody().contains(ExecutionFailureCode.EXECUTION_TARGET_NOT_FOUND.name())
                || notFound.getBody().contains(ExecutionFailureCode.EXECUTION_TARGET_NOT_ALLOWED.name())
                || notFound.getBody().contains(ExecutionFailureCode.EXECUTION_TARGET_ENVIRONMENT_PROHIBITED.name()));
    }

    @Test
    void phase14AuthorizationDistinctFromPhase15() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("plan-authorizer"), ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void viewPermissionEnforced() {
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.GET,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void requestPermissionEnforced() {
        UUID planId = readyPlanId();
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planId, TARGET), ChangeExecutionAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void reviewPermissionEnforced() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/review",
                HttpMethod.POST,
                execEntity(new ReviewExecutionRequest("viewer", "no"), ChangeExecutionAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void authorizePermissionEnforced() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("viewer"), ChangeExecutionAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void executePermissionEnforced() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createExecution(planId);
        review(created.executionId());
        authorize(created.executionId());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-execution/executions/" + created.executionId() + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_REVIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
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
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, CreateExecutionRequest.class.getRecordComponents().length);
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
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    private ExecutionDetailDto get(UUID executionId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions/" + executionId,
                HttpMethod.GET,
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
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p15-%'");
    }

    private void seedTelemetry() {
        Instant t0 = Instant.parse("2026-08-25T08:00:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            String prefix = "p15-" + UUID.randomUUID();
            projectionService.project(event(prefix + "-bler", CELL, "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb", CELL, "PRB_UTILIZATION_DL", prb[i], ts));
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

    private String canonicalTxPower() {
        return jdbc.queryForObject(
                "SELECT parameter_value FROM radio_configuration rc JOIN cell c ON rc.cell_id = c.id WHERE c.cell_id = ? AND rc.parameter_name = 'txPower'",
                String.class, CELL);
    }

    private void restoreSharedPriorPhaseState() {
        restoreCell001TxPower(SEED_TX_POWER);
        syncState.resetAll();
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update(
                "UPDATE synchronization_checkpoint SET status = 'VALID' WHERE status = 'RECOVERY_REQUIRED'");
        jdbc.update(
                """
                UPDATE network_knowledge_status
                SET confidence = 'HIGH', reason_codes = 'TRUSTED_BASELINE', freshness = 'FRESH', source_health = 'HEALTHY'
                """);
        jdbc.update(
                """
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
    }

    private void restoreCell001TxPower(String txPower) {
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                txPower, CELL);
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
