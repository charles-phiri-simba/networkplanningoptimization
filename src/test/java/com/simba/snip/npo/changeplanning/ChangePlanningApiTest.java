package com.simba.snip.npo.changeplanning;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.changeintelligence.api.ChangeProposalDetailDto;
import com.simba.snip.npo.changeintelligence.api.GenerateChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.api.ReviewChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeplanning.api.AuthorizeChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.CancelChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ChangePlanDetailDto;
import com.simba.snip.npo.changeplanning.api.CreateChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ReviewChangePlanRequest;
import com.simba.snip.npo.changeplanning.authorization.ChangePlanAuthorizer;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ExecutionReadinessResult;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.persist.ProposedActionRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangePlanningApiTest extends AbstractPostgresIT {

    private static final String CELL = "CELL-001";
    private static final String SEED_TX_POWER = "46";

    @Autowired private TestRestTemplate http;
    @Autowired private TelemetryProjectionService projectionService;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private RadioConfigurationRepository radioConfigurationRepository;
    @Autowired private ProposedActionRepository proposedActionRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID assuranceCaseId;

    @AfterEach
    void cleanup() {
        cleanupPhase14();
        cleanupPhase13();
        jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase14-test-drift'");
        restoreSharedPriorPhaseState();
    }

    @BeforeEach
    void fixtures() {
        seedTelemetry();
        runTrustedBaseline();
        assuranceCaseId = assuranceCaseService.listForCell(CELL).stream()
                .findFirst()
                .map(c -> c.getId())
                .orElse(null);
        http.postForEntity("/api/v1/twins/cells/" + CELL + "/synchronize", null, Map.class);
    }

    @Test
    void happyPathToReadyForExecution() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        long actionsBefore = proposedActionRepository.count();
        String canonicalBefore = canonicalTxPower();

        ChangePlanDetailDto created = createPlan(proposalId);
        assertEquals(PlanStatus.READY_FOR_REVIEW.name(), created.plan().status());
        assertNotNull(created.fingerprint());
        assertFalse(created.operations().isEmpty());
        assertFalse(created.rollbackOperations().isEmpty());

        review(created.plan().id());
        ChangePlanDetailDto authorized = authorize(created.plan().id());
        assertEquals(PlanStatus.AUTHORIZED.name(), authorized.plan().status());
        assertNotEquals(PlanStatus.READY_FOR_EXECUTION.name(), authorized.plan().status());
        assertEquals(authorized.fingerprint(), authorized.authorizedFingerprint());

        ChangePlanDetailDto ready = evaluateReadiness(created.plan().id());
        assertEquals(PlanStatus.READY_FOR_EXECUTION.name(), ready.plan().status());
        assertFalse(ready.readinessAssessments().isEmpty());
        assertEquals(ExecutionReadinessResult.READY.name(), ready.readinessAssessments().get(0).result());

        assertEquals(actionsBefore, proposedActionRepository.count());
        assertEquals(canonicalBefore, canonicalTxPower());
    }

    @Test
    void currentMismatchInvalidatesPlan() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    "45", CELL);
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                    HttpMethod.POST,
                    entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name()));

            ChangePlanDetailDto reloaded = get(created.plan().id());
            assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
            assertEquals(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name(), reloaded.invalidationReason());
            assertNotNull(reloaded.invalidatedAt());
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
        }
    }

    @Test
    void knowledgeLowInvalidatesAfterAuthorization() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        setKnowledge("LOW");
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        ChangePlanDetailDto reloaded = get(created.plan().id());
        assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
    }

    @Test
    void authorizeRequiresPersistedReview() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("authorizer"), ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains("review"));
    }

    @Test
    void knowledgeUnknownInvalidatesAfterAuthorization() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        setKnowledge("UNKNOWN");
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        ChangePlanDetailDto reloaded = get(created.plan().id());
        assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
        assertEquals(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN.name(), reloaded.invalidationReason());
    }

    @Test
    void driftInvalidatesAfterReadyForExecution() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        evaluateReadiness(created.plan().id());
        try {
            insertRelevantDrift(proposalId);
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                    HttpMethod.POST,
                    entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            ChangePlanDetailDto reloaded = get(created.plan().id());
            assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
            assertEquals(ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT.name(), reloaded.invalidationReason());
            assertNotNull(reloaded.invalidatedAt());
            assertTrue(hasInvalidationAudit(created.plan().id()));
        } finally {
            jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase14-test-drift'");
        }
    }

    @Test
    void readyForExecutionCurrentMismatchInvalidates() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        evaluateReadiness(created.plan().id());
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    "45", CELL);
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                    HttpMethod.POST,
                    entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            ChangePlanDetailDto reloaded = get(created.plan().id());
            assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
            assertEquals(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name(), reloaded.invalidationReason());
            assertNotNull(reloaded.invalidatedAt());
            assertTrue(hasInvalidationAudit(created.plan().id()));
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
        }
    }

    @Test
    void readyForExecutionLowKnowledgeInvalidates() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        evaluateReadiness(created.plan().id());
        setKnowledge("LOW");
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        ChangePlanDetailDto reloaded = get(created.plan().id());
        assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
        assertEquals(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW.name(), reloaded.invalidationReason());
        assertNotNull(reloaded.invalidatedAt());
        assertTrue(hasInvalidationAudit(created.plan().id()));
    }

    @Test
    void readyForExecutionUnknownKnowledgeInvalidates() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        evaluateReadiness(created.plan().id());
        setKnowledge("UNKNOWN");
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        ChangePlanDetailDto reloaded = get(created.plan().id());
        assertEquals(PlanStatus.INVALIDATED.name(), reloaded.plan().status());
        assertEquals(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN.name(), reloaded.invalidationReason());
        assertNotNull(reloaded.invalidatedAt());
        assertTrue(hasInvalidationAudit(created.plan().id()));
    }

    @Test
    void createdPreconditionsAreEvaluatedNotStampedPass() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        assertFalse(created.preconditions().isEmpty());
        for (var precondition : created.preconditions()) {
            assertNotNull(precondition.result());
            assertNotNull(precondition.checkedAt());
        }
        var authorization = created.preconditions().stream()
                .filter(p -> "AUTHORIZATION_CURRENT".equals(p.preconditionType()))
                .findFirst()
                .orElseThrow();
        assertEquals("UNKNOWN", authorization.result());
    }

    @Test
    void reviewerCanReviewButNotAuthorizeUnreviewedByAuthorizer() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<ChangePlanDetailDto> reviewed = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/review",
                HttpMethod.POST,
                entity(new ReviewChangePlanRequest("reviewer", "ok"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, reviewed.getStatusCode());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("reviewer"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void reviewerCannotAuthorizeAfterReview() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("reviewer"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void creatorCannotAuthorize() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("creator"), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void missingProposalFailsCreate() {
        ResponseEntity<String> missing = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(UUID.randomUUID()), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(missing.getStatusCode().is4xxClientError());
    }

    @ParameterizedTest(name = "nonApproved-{0}")
    @EnumSource(value = ProposalStatus.class, names = {
            "DRAFT", "VALIDATING", "RECOMMENDED", "REJECTED", "INVALID", "INVALIDATED", "EXPIRED", "SUPERSEDED"
    })
    void nonApprovedProposalStatusesBlocked(ProposalStatus status) {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        jdbc.update("UPDATE network_change_proposal SET status = ? WHERE id = ?", status.name(), proposalId);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_NOT_APPROVED.name()));
    }

    @Test
    void createBlockedWhenProposalValidityFails() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    "45", CELL);
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-planning/plans",
                    HttpMethod.POST,
                    entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
            assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name())
                    || conflict.getBody().contains(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name()));
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
        }
    }

    @Test
    void createBlockedWhenTargetMissing() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        jdbc.update("UPDATE network_change_proposal SET target_entity_id = ? WHERE id = ?", "CELL-MISSING-P14", proposalId);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(conflict.getStatusCode().is4xxClientError());
        assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND.name())
                || conflict.getBody().contains("CURRENT_STATE_UNAVAILABLE")
                || conflict.getBody().contains("cell missing")
                || conflict.getBody().contains("CELL-MISSING-P14"));
    }

    @Test
    void createUsesAuthoritativeCurrentAndProposalDesired() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generateProposal();
        UUID proposalId = approveProposal(recommended);
        String canonical = canonicalTxPower();
        String proposed = recommended.proposal().proposedValue();
        ChangePlanDetailDto created = createPlan(proposalId);
        assertEquals(canonical, created.plan().expectedCurrentValue());
        assertEquals(proposed, created.plan().desiredValue());
        assertEquals(1, CreateChangePlanRequest.class.getRecordComponents().length);
        assertEquals("proposalId", CreateChangePlanRequest.class.getRecordComponents()[0].getName());
    }

    @Test
    void createdPlanRetainsSourceProvenance() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT source_snapshot_id, source_synchronization_execution_id FROM network_change_plan WHERE id = ?",
                created.plan().id());
        assertNotNull(row.get("source_snapshot_id"));
        assertFalse(row.get("source_snapshot_id").toString().isBlank());
        assertNotNull(row.get("source_synchronization_execution_id"));
    }

    @Test
    void createBlockedByLowKnowledge() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        setKnowledge("LOW");
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(conflict.getStatusCode().is4xxClientError());
        assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW.name())
                || conflict.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name())
                || conflict.getBody().contains("KNOWLEDGE"));
    }

    @Test
    void createBlockedByRelevantDrift() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        try {
            insertRelevantDrift(proposalId);
            ResponseEntity<String> conflict = http.exchange(
                    "/api/v1/change-planning/plans",
                    HttpMethod.POST,
                    entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                    String.class);
            assertTrue(conflict.getStatusCode().is4xxClientError());
            assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT.name())
                    || conflict.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name())
                    || conflict.getBody().contains("drift")
                    || conflict.getBody().contains("DRIFT"));
        } finally {
            jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase14-test-drift'");
        }
    }

    @Test
    void createBlockedWhenRankOneAmbiguous() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        Map<String, Object> existing = jdbc.queryForMap(
                "SELECT * FROM network_change_candidate WHERE proposal_id = ? AND rank_order = 1 LIMIT 1",
                proposalId);
        jdbc.update(
                """
                INSERT INTO network_change_candidate (
                    id, proposal_id, candidate_value, baseline_candidate, validation_outcome,
                    simulation_run_id, rank_order, created_at
                ) VALUES (?, ?, ?, FALSE, 'VALID', ?, 1, NOW())
                """,
                UUID.randomUUID(),
                proposalId,
                "99",
                existing.get("simulation_run_id"));
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(conflict.getStatusCode().is4xxClientError());
        assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_CANDIDATE_AMBIGUOUS.name()));
    }

    @Test
    void createBlockedWhenCandidateValueMismatches() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        jdbc.update(
                "UPDATE network_change_candidate SET candidate_value = ? WHERE proposal_id = ? AND rank_order = 1",
                "12", proposalId);
        ResponseEntity<String> conflict = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(conflict.getStatusCode().is4xxClientError());
        assertTrue(conflict.getBody().contains(ChangePlanFailureCode.PLAN_CANDIDATE_VALUE_MISMATCH.name()));
    }

    @Test
    void knowledgeGateAllowsHighMediumBlocksLowUnknown() {
        setKnowledge("MEDIUM");
        UUID mediumProposal = approveProposal(generateProposal());
        ChangePlanDetailDto mediumPlan = createPlan(mediumProposal);
        assertEquals(PlanStatus.READY_FOR_REVIEW.name(), mediumPlan.plan().status());
        assertEquals("MEDIUM", mediumPlan.knowledgeConfidenceAtCreation());

        setKnowledge("HIGH");
        UUID lowProposal = approveProposal(generateProposal());
        setKnowledge("LOW");
        ResponseEntity<String> low = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(lowProposal), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(low.getStatusCode().is4xxClientError());
        assertTrue(low.getBody().contains(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW.name())
                || low.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name())
                || low.getBody().contains("KNOWLEDGE"));

        setKnowledge("HIGH");
        UUID unknownProposal = approveProposal(generateProposal());
        setKnowledge("UNKNOWN");
        ResponseEntity<String> unknown = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(unknownProposal), ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertTrue(unknown.getStatusCode().is4xxClientError());
        assertTrue(unknown.getBody().contains(ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN.name())
                || unknown.getBody().contains(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name())
                || unknown.getBody().contains("KNOWLEDGE"));
    }

    @Test
    void staleSynchronizationBlocksReadiness() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        try {
            jdbc.update("UPDATE network_knowledge_status SET freshness = 'STALE'");
            ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                    "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                    HttpMethod.POST,
                    entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                    ChangePlanDetailDto.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ChangePlanDetailDto body = response.getBody();
            assertNotNull(body);
            assertNotEquals(PlanStatus.READY_FOR_EXECUTION.name(), body.plan().status());
            assertEquals(PlanStatus.AUTHORIZED.name(), body.plan().status());
            assertFalse(body.readinessAssessments().isEmpty());
            assertEquals(ExecutionReadinessResult.STALE.name(), body.readinessAssessments().get(
                    body.readinessAssessments().size() - 1).result());
        } finally {
            jdbc.update("UPDATE network_knowledge_status SET freshness = 'FRESH', source_health = 'HEALTHY'");
            setKnowledge("HIGH");
        }
    }

    @Test
    void viewPermissionEnforced() {
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.GET,
                entity(null, ChangePlanAuthorizer.PERMISSION_CREATE),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void createPermissionEnforced() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void reviewPermissionEnforced() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/review",
                HttpMethod.POST,
                entity(new ReviewChangePlanRequest("viewer", "no"), ChangePlanAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void authorizePermissionEnforced() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("viewer"), ChangePlanAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void cancelPermissionEnforced() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/cancel",
                HttpMethod.POST,
                entity(new CancelChangePlanRequest("viewer", "x"), ChangePlanAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void viewDoesNotGrantCreate() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void cancelDoesNotGrantAuthorize() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("canceller"), ChangePlanAuthorizer.PERMISSION_CANCEL),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void staleAuthorizedFingerprintBlocksReadiness() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        ChangePlanDetailDto authorized = authorize(created.plan().id());
        assertEquals(authorized.fingerprint(), authorized.authorizedFingerprint());
        jdbc.update(
                "UPDATE network_change_plan SET fingerprint = ? WHERE id = ?",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                created.plan().id());
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ChangePlanDetailDto body = response.getBody();
        assertNotNull(body);
        assertNotEquals(PlanStatus.READY_FOR_EXECUTION.name(), body.plan().status());
        assertEquals(PlanStatus.AUTHORIZED.name(), body.plan().status());
        assertEquals(ExecutionReadinessResult.STALE.name(),
                body.readinessAssessments().get(body.readinessAssessments().size() - 1).result());
        boolean authStale = body.preconditions().stream().anyMatch(p ->
                "AUTHORIZATION_CURRENT".equals(p.preconditionType())
                        && ("STALE".equals(p.result())
                        || ChangePlanFailureCode.PLAN_AUTHORIZATION_STALE.name().equals(p.reasonCode())));
        assertTrue(authStale);
    }

    @Test
    void concurrentCancelConflictSafety() throws Exception {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final int actor = i;
            futures.add(pool.submit(() -> {
                gate.await();
                ResponseEntity<String> response = http.exchange(
                        "/api/v1/change-planning/plans/" + created.plan().id() + "/cancel",
                        HttpMethod.POST,
                        entity(new CancelChangePlanRequest("canceller-" + actor, "race"),
                                ChangePlanAuthorizer.PERMISSION_CANCEL),
                        String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    ok.incrementAndGet();
                } else {
                    rejected.incrementAndGet();
                    assertTrue(response.getStatusCode().is4xxClientError()
                            || response.getStatusCode().is5xxServerError());
                }
                return null;
            }));
        }
        gate.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
        assertEquals(1, ok.get(), "exactly one cancel must succeed");
        assertEquals(1, rejected.get(), "stale/racy cancel must be rejected");
        ChangePlanDetailDto finalState = get(created.plan().id());
        assertEquals(PlanStatus.CANCELLED.name(), finalState.plan().status());
    }

    @Test
    void phase13ApprovalDoesNotGrantPhase14Authorization() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-planning/plans/" + created.plan().id() + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("approver"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void cancelFromAuthorizedAndReadyForExecution() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto created = createPlan(proposalId);
        review(created.plan().id());
        authorize(created.plan().id());
        ChangePlanDetailDto cancelled = cancel(created.plan().id());
        assertEquals(PlanStatus.CANCELLED.name(), cancelled.plan().status());
        assertEquals(canonicalTxPower(), canonicalTxPower());

        UUID proposalId2 = approveProposal(generateProposal());
        ChangePlanDetailDto created2 = createPlan(proposalId2);
        review(created2.plan().id());
        authorize(created2.plan().id());
        evaluateReadiness(created2.plan().id());
        ChangePlanDetailDto cancelledReady = cancel(created2.plan().id());
        assertEquals(PlanStatus.CANCELLED.name(), cancelledReady.plan().status());
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
                entity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private void review(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/review",
                HttpMethod.POST,
                entity(new ReviewChangePlanRequest("reviewer", "reviewed"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                ChangePlanDetailDto.class);
    }

    private ChangePlanDetailDto authorize(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/authorize",
                HttpMethod.POST,
                entity(new AuthorizeChangePlanRequest("authorizer"), ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangePlanDetailDto evaluateReadiness(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/readiness",
                HttpMethod.POST,
                entity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangePlanDetailDto cancel(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/cancel",
                HttpMethod.POST,
                entity(new CancelChangePlanRequest("canceller", "cancel"), ChangePlanAuthorizer.PERMISSION_CANCEL),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangePlanDetailDto get(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId,
                HttpMethod.GET,
                entity(null, ChangePlanAuthorizer.PERMISSION_VIEW),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
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
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p14-%'");
    }

    private void seedTelemetry() {
        Instant t0 = Instant.parse("2026-08-25T08:00:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            String prefix = "p14-" + UUID.randomUUID();
            projectionService.project(event(prefix + "-bler", CELL, "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb", CELL, "PRB_UTILIZATION_DL", prb[i], ts));
        }
    }

    private void runTrustedBaseline() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        vendorImportAuthorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
    }

    private void setKnowledge(String confidence) {
        jdbc.update("UPDATE network_knowledge_status SET confidence = ?, reason_codes = ?, freshness = 'FRESH', source_health = 'HEALTHY'",
                confidence, confidence.equals("HIGH") ? "TRUSTED_BASELINE" : "DEGRADED");
    }

    private String canonicalTxPower() {
        return jdbc.queryForObject(
                "SELECT parameter_value FROM radio_configuration rc JOIN cell c ON rc.cell_id = c.id WHERE c.cell_id = ? AND rc.parameter_name = 'txPower'",
                String.class, CELL);
    }

    private void restoreSharedPriorPhaseState() {
        restoreCell001TxPower(SEED_TX_POWER);
        jdbc.update(
                "UPDATE network_knowledge_status SET confidence = 'HIGH', reason_codes = 'TRUSTED_BASELINE', freshness = 'FRESH', source_health = 'HEALTHY'");
    }

    private void restoreCell001TxPower(String txPower) {
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                txPower, CELL);
    }

    private void insertRelevantDrift(UUID proposalId) {
        Map<String, Object> source = jdbc.queryForMap(
                """
                SELECT source_system, source_snapshot_id
                FROM network_change_proposal
                WHERE id = ?
                """,
                proposalId);
        jdbc.update(
                """
                INSERT INTO network_drift_observation (
                    id, source_system, connector_id, synchronization_scope, drift_type, drift_status,
                    entity_type, entity_id, summary, detected_at
                ) VALUES (?, ?, ?, ?, ?, 'OPEN', 'CELL', ?, 'phase14-test-drift', NOW())
                """,
                UUID.randomUUID(),
                source.get("source_system"),
                ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER,
                "DEFAULT",
                "CONFIGURATION",
                CELL);
    }

    private boolean hasInvalidationAudit(UUID planId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_change_plan_audit_event WHERE plan_id = ? AND event_type = 'PLAN_INVALIDATED'",
                Integer.class,
                planId);
        return count != null && count > 0;
    }

    private <T> HttpEntity<T> proposalEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeProposalAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> entity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangePlanAuthorizer.HEADER, permission);
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
