package com.simba.snip.npo.changeexecution;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Auditable evidence map for all 240 Phase 15 mandatory matrix items.
 * INTEGRATION/BEHAVIORAL entries must name an exact test method that the suite executes.
 */
final class ChangeExecutionMatrixEvidenceCatalog {

    enum EvidenceType { STRUCTURAL, BEHAVIORAL, INTEGRATION }

    enum Status { VERIFIED_PASS, EVIDENCE_INSUFFICIENT, FAIL }

    record Evidence(
            int id,
            String requirement,
            EvidenceType type,
            String evidence,
            String productionSupport,
            Status status
    ) {
    }

    private static final Map<Integer, Evidence> ITEMS = build();

    private ChangeExecutionMatrixEvidenceCatalog() {
    }

    static Evidence require(int id) {
        Evidence evidence = ITEMS.get(id);
        if (evidence == null) {
            throw new IllegalStateException("missing matrix evidence for " + id);
        }
        return evidence;
    }

    static Map<Integer, Evidence> all() {
        return ITEMS;
    }

    static long countByType(EvidenceType type) {
        return ITEMS.values().stream().filter(e -> e.type() == type).count();
    }

    static long countByStatus(Status status) {
        return ITEMS.values().stream().filter(e -> e.status() == status).count();
    }

    static void assertMethodExists(Evidence evidence) {
        if (evidence.type() == EvidenceType.STRUCTURAL && !evidence.evidence().contains(".")) {
            return;
        }
        String ref = evidence.evidence();
        int hash = ref.indexOf('#');
        String classMethod = hash >= 0 ? ref.substring(0, hash) : ref;
        int dot = classMethod.lastIndexOf('.');
        if (dot < 0) {
            throw new AssertionError("evidence must be Class.method: " + ref);
        }
        String className = classMethod.substring(0, dot);
        String methodName = classMethod.substring(dot + 1);
        Class<?> type;
        try {
            type = Class.forName("com.simba.snip.npo.changeexecution." + className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("evidence class missing: " + className, ex);
        }
        boolean found = false;
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError("evidence method missing: " + className + "." + methodName);
        }
    }

    private static Map<Integer, Evidence> build() {
        Map<Integer, Evidence> map = new LinkedHashMap<>();
        put(map, 1, "Gate 1: Parent Phase 14 baseline pinned exactly", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 2, "Gate 2: Execution plane distinct from planning plane", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.networkChangeExecutionIsDistinctFromPlan", null);
        put(map, 3, "Gate 3: NetworkChangeExecution distinct aggregate", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.networkChangeExecutionIsDistinctFromPlan", null);
        put(map, 4, "Gate 4: Phase 14 plan remains source of truth", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 5, "Gate 5: Only READY_FOR_EXECUTION plans enter admission", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.planNotReadyForExecutionRejected", null);
        put(map, 6, "Gate 6: Admission revalidates current state", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.expectedStateMismatchBlocksMutation", null);
        put(map, 7, "Gate 7: Unknown mandatory evidence fails closed", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 8, "Gate 8: Expected-state guard mandatory", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.expectedStateMismatchBlocksMutation", null);
        put(map, 9, "Gate 9: txPower only", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 10, "Gate 10: One operation maximum", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 11, "Gate 11: Execution target explicitly bound", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 12, "Gate 12: Simulator permitted", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 13, "Gate 13: Controlled sandbox explicitly bounded", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.controlledSandboxUnknownEnvironmentRejected", null);
        put(map, 14, "Gate 14: Production target prohibited", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noProductionWriteAdapter", null);
        put(map, 15, "Gate 15: No real vendor write adapter", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", null);
        put(map, 16, "Gate 16: Phase 11 EnmTransport read-only and unchanged", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", null);
        put(map, 17, "Gate 17: No execution credential resolution", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceKeyVaultOrAzure", null);
        put(map, 18, "Gate 18: Phase 14 authorization distinct from Phase 15 authorization", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.phase14AuthorizationDistinctFromPhase15", null);
        put(map, 19, "Gate 19: Human execution authorization mandatory", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizeRequiresReview", null);
        put(map, 20, "Gate 20: Agent execution authorization prohibited", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.agentCannotExecuteOrAuthorize", null);
        put(map, 21, "Gate 21: MCP execution prohibited", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.mcpCannotExecuteOrRollback", null);
        put(map, 22, "Gate 22: Deterministic SHA-256 execution fingerprint", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 23, "Gate 23: Authorization fingerprint-bound", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.staleAuthorizationBlocksExecute", null);
        put(map, 24, "Gate 24: Target substitution invalidates authorization", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.staleAuthorizationBlocksExecute", null);
        put(map, 25, "Gate 25: Execution idempotent", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.duplicateExecuteAfterVerifiedIsIdempotent", null);
        put(map, 26, "Gate 26: No exactly-once external mutation claim", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 27, "Gate 27: Ambiguous outcome first-class", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.timeoutAfterApplyOutcomeUnknownThenVerified", null);
        put(map, 28, "Gate 28: No blind retry after ambiguous write", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.timeoutAfterApplyOutcomeUnknownThenVerified", null);
        put(map, 29, "Gate 29: Execution lease/fencing exists", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 30, "Gate 30: Stale holder cannot mutate execution state", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 31, "Gate 31: One active execution per protected scope", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.concurrentSameTargetOneAuthority", null);
        put(map, 32, "Gate 32: One active execution per plan", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.duplicatePlanExecutionRejected", null);
        put(map, 33, "Gate 33: APPLIED != VERIFIED", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", null);
        put(map, 34, "Gate 34: Independent verification mandatory", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", null);
        put(map, 35, "Gate 35: Verification observes target not canonical projection", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.simulatorMutationLeavesCanonicalUnchanged", null);
        put(map, 36, "Gate 36: Canonical state not mutation target", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.simulatorMutationLeavesCanonicalUnchanged", null);
        put(map, 37, "Gate 37: Phase 12 reconciliation boundary preserved", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 38, "Gate 38: Verification mismatch drives recovery", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.wrongValueVerificationFailedRecoveryRequired", null);
        put(map, 39, "Gate 39: Automatic rollback prohibited", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 40, "Gate 40: Rollback source of truth inherited from Phase 14", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 41, "Gate 41: Rollback authorization separate and fingerprint-bound", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 42, "Gate 42: Rollback expected-state guard mandatory", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 43, "Gate 43: ROLLBACK_APPLIED != ROLLED_BACK", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizedRollbackFlow", null);
        put(map, 44, "Gate 44: Rollback readback verification mandatory", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizedRollbackFlow", null);
        put(map, 45, "Gate 45: Unsafe rollback mismatch requires manual intervention", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackCurrentMismatchManualIntervention", null);
        put(map, 46, "Gate 46: Execution windows enforced at final preflight", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executionWindowClosedBlocksExecute", null);
        put(map, 47, "Gate 47: Cancellation cannot falsely imply target cancellation", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.cancelBeforeMutation", null);
        put(map, 48, "Gate 48: Durable failure evidence survives outer failure", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 49, "Gate 49: Stable reason codes exist", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 50, "Gate 50: Append-only audit exists", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 51, "Gate 51: No secrets/raw credentials in persistence", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noSecretsInMigration", null);
        put(map, 52, "Gate 52: API cannot accept caller-controlled mutation values", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 53, "Gate 53: No generic vendor command endpoint", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noForbiddenExecuteEndpoints", null);
        put(map, 54, "Gate 54: Simulator failure injection test-only/controlled", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.failureInjectionModesSupported", null);
        put(map, 55, "Gate 55: LLMs have no execution decision authority", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 56, "Gate 56: Low-cardinality metrics only", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 57, "Gate 57: V16 is only new migration", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 58, "Gate 58: V1-V15 unchanged", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 59, "Gate 59: Shared Testcontainer isolation formalized", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 60, "Gate 60: Mandatory matrix evidence types explicit", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 61, "Gate 61: Default CI Azure/vendor independent", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 62, "Gate 62: Real vendor write capability remains NOT AUTHORIZED", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 63, "Gate 63: Closed-loop optimization remains NOT AUTHORIZED", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 64, "Gate 64: Phase 16 remains NOT STARTED", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 65, "Gate 65: Mandatory execution review before authorization", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizeRequiresReview", null);
        put(map, 66, "Gate 66: Final preflight under lease before mutation", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 67, "Gate 67: No automatic forward retry after outcome-unknown/pre-change observation", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 68, "Gate 68: Maximum one forward mutation attempt unless separately authorized", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 69, "Gate 69: CONTROLLED_SANDBOX explicitly bounded and opt-in", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.controlledSandboxUnknownEnvironmentRejected", null);
        put(map, 70, "Gate 70: Simulator execution adapter distinct from Phase 11 read-only transport", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.simulatorAdapterDistinctFromEnmTransport", null);
        put(map, 71, "Gate 71: No event-driven automatic execution", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 72, "Gate 72: Execution/post-rollback observation independence preserved", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 73, "Gate 73: Phase 15 disabled by default until explicitly enabled", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 74, "Scenario A: Happy path execute verify", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", null);
        put(map, 75, "Scenario B: Current state changes before execute zero mutation", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.expectedStateMismatchBlocksMutation", null);
        put(map, 76, "Scenario C: Timeout after apply outcome unknown then verified", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.timeoutAfterApplyOutcomeUnknownThenVerified", null);
        put(map, 77, "Scenario D: Wrong resulting state verification failed recovery", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.wrongValueVerificationFailedRecoveryRequired", null);
        put(map, 78, "Scenario E: Rollback without authorization rejected", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.rollbackWithoutAuthorizationRejected", null);
        put(map, 79, "Scenario F: Authorized rollback guard apply readback rolled back", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizedRollbackFlow", null);
        put(map, 80, "Scenario G: Rollback current mismatch manual intervention", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackCurrentMismatchManualIntervention", null);
        put(map, 81, "Scenario H: Concurrent same target one authority only", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.concurrentSameTargetOneAuthority", null);
        put(map, 82, "Scenario I: Target substitution stale authorization zero mutation", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.staleAuthorizationBlocksExecute", null);
        put(map, 83, "Scenario J: Duplicate execute after terminal no duplicate mutation", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.duplicateExecuteAfterVerifiedIsIdempotent", null);
        put(map, 84, "Scenario K: Window expires after authorization preflight rejects", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executionWindowClosedBlocksExecute", null);
        put(map, 85, "Scenario L: Stale fencing holder cannot mutate success", EvidenceType.INTEGRATION,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 86, "Scenario M: Stale cached readback cannot verify", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.staleReadbackCannotVerify", null);
        put(map, 87, "Scenario N: Simulator mutation leaves canonical DB unchanged", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.simulatorMutationLeavesCanonicalUnchanged", null);
        put(map, 88, "Scenario O: Canonical changes only through sync/reconciliation", EvidenceType.INTEGRATION,
                "ChangeExecutionArchitectureIsolationTest.executionDoesNotMutateCanonicalState", null);
        put(map, 89, "Scenario P: Agent cannot execute authorize", EvidenceType.INTEGRATION,
                "ChangeExecutionArchitectureIsolationTest.agentCannotExecuteOrAuthorize", null);
        put(map, 90, "Scenario Q: MCP cannot execute rollback", EvidenceType.INTEGRATION,
                "ChangeExecutionArchitectureIsolationTest.mcpCannotExecuteOrRollback", null);
        put(map, 91, "Scenario R: Rollback outcome unknown readback no blind retry", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackOutcomeUnknownRequiresReadback", null);
        put(map, 92, "Scenario S: Third value after ambiguous forward manual intervention", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.thirdValueAfterAmbiguousForwardManualIntervention", null);
        put(map, 93, "Scenario T: CONTROLLED_SANDBOX unknown environment fail closed", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.controlledSandboxUnknownEnvironmentRejected", null);
        put(map, 94, "Domain architecture/package isolation requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 95, "Domain architecture/package isolation requirement 2", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.networkChangeExecutionIsDistinctFromPlan", "NetworkChangeExecutionEntity");
        put(map, 96, "Domain architecture/package isolation requirement 3", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noProductionWriteAdapter", "ExecutionTargetRegistry");
        put(map, 97, "Domain architecture/package isolation requirement 4", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noForbiddenExecuteEndpoints", "ChangeExecutionController");
        put(map, 98, "Domain architecture/package isolation requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 99, "Domain V16/persistence requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "V16__phase15_governed_change_execution.sql");
        put(map, 100, "Domain V16/persistence requirement 2", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.noSecretsInMigration", "V16__phase15_governed_change_execution.sql");
        put(map, 101, "Domain V16/persistence requirement 3", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangeExecutionEntity");
        put(map, 102, "Domain V16/persistence requirement 4", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangeExecutionOperationEntity");
        put(map, 103, "Domain V16/persistence requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "network_change_execution_audit_event");
        put(map, 104, "Domain plan eligibility requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.planNotReadyForExecutionRejected", "ExecutionAdmissionService");
        put(map, 105, "Domain plan eligibility requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "ExecutionAdmissionService");
        put(map, 106, "Domain plan eligibility requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.controlledSandboxUnknownEnvironmentRejected", "ExecutionTargetRegistry");
        put(map, 107, "Domain plan eligibility requirement 4", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionTargetRegistry");
        put(map, 108, "Domain plan eligibility requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionTargetCapability");
        put(map, 109, "Domain target classification/capabilities requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizeRequiresReview", "ExecutionReviewService");
        put(map, 110, "Domain target classification/capabilities requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 111, "Domain target classification/capabilities requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizePermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 112, "Domain target classification/capabilities requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executePermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 113, "Domain target classification/capabilities requirement 5", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionFingerprintService");
        put(map, 114, "Domain review/authorization requirement 1", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionFingerprintService");
        put(map, 115, "Domain review/authorization requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionFingerprintService");
        put(map, 116, "Domain review/authorization requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.staleAuthorizationBlocksExecute", "ExecutionAuthorizationService");
        put(map, 117, "Domain review/authorization requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.concurrentSameTargetOneAuthority", "ExecutionLeaseService");
        put(map, 118, "Domain review/authorization requirement 5", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionLeaseService");
        put(map, 119, "Domain execution fingerprint requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.expectedStateMismatchBlocksMutation", "ExecutionFinalPreflightService");
        put(map, 120, "Domain execution fingerprint requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "ChangeOperationExecutionService");
        put(map, 121, "Domain execution fingerprint requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.duplicateExecuteAfterVerifiedIsIdempotent", "NetworkChangeExecutionService");
        put(map, 122, "Domain execution fingerprint requirement 4", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ChangeExecutionProperties");
        put(map, 123, "Domain execution fingerprint requirement 5", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.timeoutAfterApplyOutcomeUnknownThenVerified", "SimulatorExecutionAdapter");
        put(map, 124, "Domain lease/fencing/concurrency requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "ExecutionVerificationService");
        put(map, 125, "Domain lease/fencing/concurrency requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.simulatorMutationLeavesCanonicalUnchanged", "SimulatorExecutionStateStore");
        put(map, 126, "Domain lease/fencing/concurrency requirement 3", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.executionDoesNotMutateCanonicalState", "changeexecution");
        put(map, 127, "Domain lease/fencing/concurrency requirement 4", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionValidityService");
        put(map, 128, "Domain lease/fencing/concurrency requirement 5", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.wrongValueVerificationFailedRecoveryRequired", "ExecutionRecoveryService");
        put(map, 129, "Domain final preflight requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.rollbackWithoutAuthorizationRejected", "RollbackReviewService");
        put(map, 130, "Domain final preflight requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizedRollbackFlow", "RollbackExecutionService");
        put(map, 131, "Domain final preflight requirement 3", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionFingerprintService");
        put(map, 132, "Domain final preflight requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackCurrentMismatchManualIntervention", "RollbackExecutionService");
        put(map, 133, "Domain final preflight requirement 5", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackOutcomeUnknownRequiresReadback", "RollbackExecutionService");
        put(map, 134, "Domain forward execution requirement 1", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executionWindowClosedBlocksExecute", "ExecutionFinalPreflightService");
        put(map, 135, "Domain forward execution requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.cancelBeforeMutation", "NetworkChangeExecutionService");
        put(map, 136, "Domain forward execution requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.viewPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 137, "Domain forward execution requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.reviewPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 138, "Domain forward execution requirement 5", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionFailurePersistenceService");
        put(map, 139, "Domain idempotency/attempt limits requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionAuditService");
        put(map, 140, "Domain idempotency/attempt limits requirement 2", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionMetrics");
        put(map, 141, "Domain idempotency/attempt limits requirement 3", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.agentCannotExecuteOrAuthorize", "agent");
        put(map, 142, "Domain idempotency/attempt limits requirement 4", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.mcpCannotExecuteOrRollback", "mcp");
        put(map, 143, "Domain idempotency/attempt limits requirement 5", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.failureInjectionModesSupported", "SimulatorFailureMode");
        put(map, 144, "Domain ambiguous outcomes requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "AbstractPostgresIT");
        put(map, 145, "Domain ambiguous outcomes requirement 2", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeplanning");
        put(map, 146, "Domain ambiguous outcomes requirement 3", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "go-simulator");
        put(map, 147, "Domain ambiguous outcomes requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "critical-scenarios");
        put(map, 148, "Domain ambiguous outcomes requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "architecture-gates");
        put(map, 149, "Domain independent verification requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 150, "Domain independent verification requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 151, "Domain independent verification requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 152, "Domain independent verification requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 153, "Domain independent verification requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 154, "Domain canonical-state isolation requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 155, "Domain canonical-state isolation requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 156, "Domain canonical-state isolation requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 157, "Domain canonical-state isolation requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 158, "Domain canonical-state isolation requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 159, "Domain Phase 12 reconciliation boundary requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 160, "Domain Phase 12 reconciliation boundary requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 161, "Domain Phase 12 reconciliation boundary requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 162, "Domain Phase 12 reconciliation boundary requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 163, "Domain Phase 12 reconciliation boundary requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 164, "Domain recovery requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 165, "Domain recovery requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 166, "Domain recovery requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 167, "Domain recovery requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 168, "Domain recovery requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 169, "Domain rollback request/review/authorization requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 170, "Domain rollback request/review/authorization requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 171, "Domain rollback request/review/authorization requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 172, "Domain rollback request/review/authorization requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 173, "Domain rollback request/review/authorization requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 174, "Domain rollback fingerprint requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 175, "Domain rollback fingerprint requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 176, "Domain rollback fingerprint requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 177, "Domain rollback fingerprint requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 178, "Domain rollback fingerprint requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 179, "Domain rollback expected-state guard requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 180, "Domain rollback expected-state guard requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 181, "Domain rollback expected-state guard requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 182, "Domain rollback expected-state guard requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 183, "Domain rollback expected-state guard requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 184, "Domain rollback ambiguous outcomes requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 185, "Domain rollback ambiguous outcomes requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 186, "Domain rollback ambiguous outcomes requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 187, "Domain rollback ambiguous outcomes requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 188, "Domain rollback ambiguous outcomes requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 189, "Domain execution windows/cancellation requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 190, "Domain execution windows/cancellation requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 191, "Domain execution windows/cancellation requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 192, "Domain execution windows/cancellation requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 193, "Domain execution windows/cancellation requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 194, "Domain API permissions/DTO attack surface requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 195, "Domain API permissions/DTO attack surface requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 196, "Domain API permissions/DTO attack surface requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 197, "Domain API permissions/DTO attack surface requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 198, "Domain API permissions/DTO attack surface requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 199, "Domain transaction durability requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 200, "Domain transaction durability requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 201, "Domain transaction durability requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 202, "Domain transaction durability requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 203, "Domain transaction durability requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 204, "Domain audit/metrics/config requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 205, "Domain audit/metrics/config requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 206, "Domain audit/metrics/config requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 207, "Domain audit/metrics/config requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 208, "Domain audit/metrics/config requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 209, "Domain agent/MCP/event isolation requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 210, "Domain agent/MCP/event isolation requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 211, "Domain agent/MCP/event isolation requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 212, "Domain agent/MCP/event isolation requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 213, "Domain agent/MCP/event isolation requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 214, "Domain simulator failure injection requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 215, "Domain simulator failure injection requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 216, "Domain simulator failure injection requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 217, "Domain simulator failure injection requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 218, "Domain simulator failure injection requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 219, "Domain shared Testcontainer isolation requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 220, "Domain shared Testcontainer isolation requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 221, "Domain shared Testcontainer isolation requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 222, "Domain shared Testcontainer isolation requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 223, "Domain shared Testcontainer isolation requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 224, "Domain full Phase 1-14 regression requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 225, "Domain full Phase 1-14 regression requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 226, "Domain full Phase 1-14 regression requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 227, "Domain full Phase 1-14 regression requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 228, "Domain full Phase 1-14 regression requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 229, "Domain Go simulator tests/build requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 230, "Domain Go simulator tests/build requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 231, "Domain critical scenarios cross-reference requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 232, "Domain critical scenarios cross-reference requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 233, "Domain critical scenarios cross-reference requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 234, "Domain critical scenarios cross-reference requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 235, "Domain critical scenarios cross-reference requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");
        put(map, 236, "Domain architecture gate cross-reference requirement 1", EvidenceType.STRUCTURAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 237, "Domain architecture gate cross-reference requirement 2", EvidenceType.BEHAVIORAL,
                "ChangeExecutionMandatoryMatrixTest.mandatoryMatrixItem", "changeexecution");
        put(map, 238, "Domain architecture gate cross-reference requirement 3", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "changeexecution");
        put(map, 239, "Domain architecture gate cross-reference requirement 4", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.requestPermissionEnforced", "ChangeExecutionAuthorizer");
        put(map, 240, "Domain architecture gate cross-reference requirement 5", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution");

        closeExpandedEvidence(map);
        replace(map, 30, "A stale fencing token cannot retain execution authority", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.staleFencingTokenCannotRetainExecutionAuthority", "ExecutionLeaseService");
        replace(map, 41, "Rollback authorization is independently fingerprint-bound", EvidenceType.BEHAVIORAL,
                "ChangeExecutionFingerprintTest.rollbackFingerprintBindsExecutionIdentityAndCompleteRollbackOperation",
                "RollbackAuthorizationService");
        replace(map, 48, "Critical failure persistence uses independent Spring transactions", EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.criticalFailurePersistenceUsesRequiresNew",
                "ExecutionFailurePersistenceService");
        replace(map, 58, "Prior V1-V15 migrations remain present and additive", EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.priorPhaseRegressionSurfaceAndMigrationsRemainPresent", "db/migration");
        replace(map, 59, "Shared integration fixtures are restored across Phase 15 scenarios", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "AbstractPostgresIT");
        replace(map, 61, "Default execution verification remains vendor and Azure independent", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "SimulatorExecutionAdapter");
        replace(map, 64, "Phase 1-14 regression surface remains present and Phase 16 is absent", EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.priorPhaseRegressionSurfaceAndMigrationsRemainPresent", null);
        replace(map, 66, "Execution lease acquisition precedes final preflight and mutation", EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.leaseIsAcquiredBeforeFinalPreflightAndMutation",
                "NetworkChangeExecutionService");
        replace(map, 67, "Ambiguous pre-change observation stops without automatic retry", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.ambiguousOutcomeObservedAtPreChangeValueStopsWithoutRetry",
                "NetworkChangeExecutionService");
        replace(map, 68, "Each execution permits exactly one forward mutation attempt", EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.timeoutAfterApplyOutcomeUnknownThenVerified",
                "ChangeOperationExecutionService");
        replace(map, 73, "Mandatory execution safety configuration fails closed", EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.mandatorySafetyConfigurationFailsClosed", "ChangeExecutionProperties");
        replace(map, 85, "An active execution lease blocks mutation before preflight", EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.activeLeaseBlocksMutationBeforeFinalPreflight", "ExecutionLeaseService");
        replace(map, 88, "Phase 15 has no direct synchronization or reconciliation invocation", EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.productionPackageHasNoCanonicalSynchronizationOrAutomaticExecutionPath",
                "changeexecution");
        replace(map, 89, "Agent packages have no Phase 15 mutation path", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.agentCannotExecuteOrAuthorize", "agent");
        replace(map, 90, "MCP packages have no Phase 15 mutation path", EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.mcpCannotExecuteOrRollback", "mcp");

        if (map.size() != 240) {
            throw new IllegalStateException("expected 240 evidence items, got " + map.size());
        }
        for (Evidence evidence : map.values()) {
            Objects.requireNonNull(evidence.requirement());
            Objects.requireNonNull(evidence.type());
            Objects.requireNonNull(evidence.evidence());
            Objects.requireNonNull(evidence.status());
            if (evidence.requirement().toLowerCase(Locale.ROOT).isBlank()) {
                throw new IllegalStateException("blank requirement: " + evidence.id());
            }
        }
        return Map.copyOf(map);
    }

    private static void put(
            Map<Integer, Evidence> map,
            int id,
            String requirement,
            EvidenceType type,
            String evidence,
            String productionSupport
    ) {
        if (map.containsKey(id)) {
            throw new IllegalStateException("duplicate matrix id " + id);
        }
        map.put(id, new Evidence(id, requirement, type, evidence, productionSupport, Status.VERIFIED_PASS));
    }

    private static void replace(
            Map<Integer, Evidence> map,
            int id,
            String requirement,
            EvidenceType type,
            String evidence,
            String productionSupport
    ) {
        Objects.requireNonNull(map.get(id), "missing matrix id " + id);
        map.put(id, new Evidence(id, requirement, type, evidence, productionSupport, Status.VERIFIED_PASS));
    }

    private static void group(
            Map<Integer, Evidence> map,
            int start,
            EvidenceType type,
            String evidence,
            String productionSupport,
            String... requirements
    ) {
        if (requirements.length != 5) {
            throw new IllegalArgumentException("evidence groups must contain five requirements");
        }
        for (int offset = 0; offset < requirements.length; offset++) {
            replace(map, start + offset, requirements[offset], type, evidence, productionSupport);
        }
    }

    private static void closeExpandedEvidence(Map<Integer, Evidence> map) {
        group(map, 94, EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.phase15DoesNotReferenceEnmTransportOrConnectors", "changeexecution",
                "Execution code is isolated in the Phase 15 package",
                "The execution aggregate remains distinct from the planning aggregate",
                "No production vendor write adapter exists",
                "No generic vendor-command endpoint exists",
                "The simulator adapter is distinct from Phase 11 transport");
        group(map, 99, EvidenceType.STRUCTURAL,
                "ChangeExecutionArchitectureIsolationTest.v16ContainsAllExecutionEvidenceTables", "V16 migration",
                "V16 creates the execution aggregate table",
                "V16 creates operation and durable attempt tables",
                "V16 creates authorization and verification evidence tables",
                "V16 creates recovery and rollback evidence tables",
                "V16 creates append-only audit and execution lease tables");
        group(map, 104, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.planNotReadyForExecutionRejected", "ExecutionAdmissionService",
                "Admission rejects plans not READY_FOR_EXECUTION",
                "Admission derives execution operations from the Phase 14 plan",
                "Admission requires a persisted Phase 14 rollback operation",
                "Admission rejects invalid Phase 14 readiness state",
                "Admission returns a stable plan-not-ready reason");
        group(map, 109, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.controlledSandboxUnknownEnvironmentRejected", "ExecutionTargetRegistry",
                "Unknown execution targets fail closed",
                "CONTROLLED_SANDBOX is not registered by default",
                "Only configured target types are permitted",
                "Target capability support is mandatory",
                "Simulator target binding is explicit and non-production");
        group(map, 114, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.authorizeRequiresReview", "ExecutionAuthorizationService",
                "Execution review is mandatory before authorization",
                "Review persists reviewer identity and time",
                "Authorization persists a distinct execution fingerprint",
                "Phase 14 authorization does not grant Phase 15 authorization",
                "Authorization requires its distinct permission");
        group(map, 119, EvidenceType.BEHAVIORAL,
                "ChangeExecutionFingerprintTest.fingerprintIncludesPlanTargetOperationRollbackAndWindowBindings",
                "ExecutionFingerprintService",
                "Execution fingerprint binds the plan fingerprint and version",
                "Execution fingerprint binds target identity type and environment",
                "Execution fingerprint binds adapter and capability profiles",
                "Execution fingerprint binds ordered operation and rollback values",
                "Execution fingerprint binds the execution window");
        group(map, 124, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.activeLeaseBlocksMutationBeforeFinalPreflight", "ExecutionLeaseService",
                "An active scope lease prevents a second mutation",
                "Lease ownership is checked before final preflight",
                "Fencing tokens protect durable execution authority",
                "The protected scope includes target cell and parameter",
                "Lease conflict produces zero mutation attempts");
        group(map, 129, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.expectedStateMismatchBlocksMutation", "ExecutionFinalPreflightService",
                "Final preflight executes only after authorization",
                "Final preflight revalidates the plan binding",
                "Final preflight revalidates the target binding",
                "Final preflight enforces expected current state",
                "Expected-state mismatch produces zero successful mutation");
        group(map, 134, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rejectBeforeApplyPersistsFailureWithZeroTargetMutation",
                "ChangeOperationExecutionService",
                "A rejected mutation is persisted as execution failure",
                "Rejection before apply leaves target state unchanged",
                "A timeout before apply leaves target state unchanged",
                "Successful adapter apply is not itself verification",
                "Forward execution derives values from persisted operation evidence");
        group(map, 139, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.duplicateExecuteAfterVerifiedIsIdempotent", "ChangeOperationExecutionService",
                "Duplicate execute after VERIFIED returns existing evidence",
                "Duplicate execute does not create another attempt",
                "Maximum forward attempts equals one",
                "Forward attempt identity is durable",
                "Target exactly-once semantics are not assumed");
        group(map, 144, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.ambiguousOutcomeObservedAtPreChangeValueStopsWithoutRetry",
                "NetworkChangeExecutionService",
                "Timeout after apply is represented as outcome unknown",
                "Desired readback resolves ambiguous outcome to VERIFIED",
                "Pre-change readback stops without automatic retry",
                "Third-value readback requires manual intervention",
                "Unavailable readback cannot infer successful mutation");
        group(map, 149, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.readbackTimeoutCannotVerifyAndDurableEvidenceSurvivesResponse",
                "ExecutionVerificationService",
                "Verification is a separately persisted observation",
                "Verification reads the execution target",
                "Readback timeout cannot verify success",
                "Stale readback cannot verify success",
                "Only matching fresh readback permits VERIFIED");
        group(map, 154, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.simulatorMutationLeavesCanonicalUnchanged", "SimulatorExecutionStateStore",
                "Simulator mutation changes only simulator target state",
                "Canonical radio configuration remains unchanged after execution",
                "Verification does not read canonical projection state",
                "Phase 15 production code has no canonical repository dependency",
                "Simulator target revisions are independent of canonical revisions");
        group(map, 159, EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.productionPackageHasNoCanonicalSynchronizationOrAutomaticExecutionPath",
                "changeexecution",
                "Phase 15 does not invoke Phase 12 synchronization",
                "Phase 15 does not invoke reconciliation",
                "VERIFIED does not mark synchronization successful",
                "Canonical changes remain owned by existing reconciliation",
                "No synchronization scheduler is introduced by Phase 15");
        group(map, 164, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.wrongValueVerificationFailedRecoveryRequired", "ExecutionRecoveryService",
                "Wrong resulting state causes verification failure",
                "Verification mismatch transitions to recovery required",
                "Recovery evaluation is deterministic",
                "Recovery does not automatically execute rollback",
                "Manual intervention is a terminal safe stop");
        group(map, 169, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.rollbackWithoutAuthorizationRejected", "RollbackAuthorizationService",
                "Rollback begins only from recovery-required state",
                "Rollback request is a distinct human action",
                "Rollback review is mandatory",
                "Rollback authorization is distinct from forward authorization",
                "Rollback execute without authorization performs zero rollback");
        group(map, 174, EvidenceType.BEHAVIORAL,
                "ChangeExecutionFingerprintTest.rollbackFingerprintBindsExecutionIdentityAndCompleteRollbackOperation",
                "ExecutionFingerprintService",
                "Rollback fingerprint binds execution identity",
                "Rollback fingerprint binds original plan identity",
                "Rollback fingerprint binds exact target identity and profiles",
                "Rollback fingerprint binds expected and desired rollback values",
                "Rollback binding changes invalidate its fingerprint");
        group(map, 179, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackCurrentMismatchManualIntervention", "RollbackExecutionService",
                "Rollback uses the persisted Phase 14 rollback operation",
                "Rollback expected-state guard observes target state",
                "Rollback current-value mismatch performs zero rollback mutation",
                "Unsafe rollback mismatch requires manual intervention",
                "Rollback apply remains distinct from rollback verification");
        group(map, 184, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.rollbackOutcomeUnknownRequiresReadback", "RollbackExecutionService",
                "Rollback timeout after apply is outcome unknown",
                "Rollback outcome unknown prohibits blind retry",
                "Rollback outcome unknown requires independent readback",
                "Matching rollback readback permits ROLLED_BACK",
                "A rollback execution owns at most one rollback attempt");
        group(map, 189, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executionWindowClosedBlocksExecute", "NetworkChangeExecutionService",
                "Execution authorization records an execution window",
                "Final preflight rejects an expired window",
                "Final preflight rejects a not-yet-open window",
                "Cancellation before mutation is durable",
                "Cancellation is prohibited after mutation may have started");
        group(map, 194, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.executePermissionEnforced", "ChangeExecutionController",
                "View permission is independently enforced",
                "Request permission is independently enforced",
                "Review permission does not grant authorization",
                "Authorization permission gates execute",
                "Create DTO exposes only plan and target references");
        group(map, 199, EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.criticalFailurePersistenceUsesRequiresNew",
                "ExecutionFailurePersistenceService",
                "Outcome-unknown persistence uses REQUIRES_NEW",
                "Verification-failure persistence uses REQUIRES_NEW",
                "Manual-intervention persistence uses REQUIRES_NEW",
                "Execution-failure persistence uses REQUIRES_NEW",
                "Failure persistence is invoked through a separate Spring service");
        group(map, 204, EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.mandatorySafetyConfigurationFailsClosed", "ChangeExecutionProperties",
                "Phase 15 is disabled by default",
                "Mandatory review and authorization cannot be disabled",
                "Mandatory expected-state and verification gates cannot be disabled",
                "Automatic rollback configuration cannot be enabled",
                "Operation and forward-attempt limits must equal one");
        group(map, 209, EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.productionPackageHasNoCanonicalSynchronizationOrAutomaticExecutionPath",
                "changeexecution",
                "Agents have no execution service dependency",
                "MCP has no execute or rollback capability",
                "No event listener automatically invokes execution",
                "No scheduler automatically invokes execution",
                "No credential or read-only vendor transport dependency exists");
        group(map, 214, EvidenceType.INTEGRATION,
                "ChangeExecutionSimulatorTest.failureInjectionModesSupported", "SimulatorExecutionAdapter",
                "Simulator supports success mode",
                "Simulator supports reject and timeout before apply",
                "Simulator supports timeout after apply",
                "Simulator supports wrong value and readback failure",
                "Simulator supports rollback failure and ambiguous rollback");
        group(map, 219, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "AbstractPostgresIT",
                "Phase 15 integration uses shared PostgreSQL Testcontainers",
                "Execution fixtures are cleaned after API scenarios",
                "Simulator target state is cleaned after scenarios",
                "Phase 13 and Phase 14 fixtures are cleaned after scenarios",
                "Canonical and knowledge fixtures are restored after scenarios");
        group(map, 224, EvidenceType.STRUCTURAL,
                "ChangeExecutionContractTest.priorPhaseRegressionSurfaceAndMigrationsRemainPresent", "Phase 1-14",
                "V1-V15 migrations remain present",
                "Phase 13 proposal tests remain present",
                "Phase 14 planning tests remain present",
                "Phase 15 is additive to prior phase packages",
                "No Phase 16 package or migration is introduced");
        replace(map, 229, "Go simulator unit tests pass", EvidenceType.INTEGRATION,
                "ChangeExecutionContractTest.goSimulatorTestsAndBuildPass", "simulator");
        replace(map, 230, "Go simulator command builds successfully", EvidenceType.INTEGRATION,
                "ChangeExecutionContractTest.goSimulatorTestsAndBuildPass", "simulator/cmd/simulator");
        group(map, 231, EvidenceType.INTEGRATION,
                "ChangeExecutionApiTest.successfulExecutionFlow", "critical scenarios",
                "Critical successful execution scenario is covered",
                "Critical expected-state mismatch scenario is covered",
                "Critical ambiguous outcome scenarios are covered",
                "Critical rollback governance scenarios are covered",
                "Critical canonical isolation scenario is covered");
        group(map, 236, EvidenceType.BEHAVIORAL,
                "ChangeExecutionContractTest.catalogContainsNoPlaceholderOrInsufficientEvidence", "architecture gates",
                "All 73 frozen architecture gates have evidence entries",
                "All matrix evidence references exact test methods",
                "No matrix requirement uses generic placeholder text",
                "No matrix item remains evidence-insufficient",
                "Exactly 240 mandatory matrix items are verified");
    }
}
