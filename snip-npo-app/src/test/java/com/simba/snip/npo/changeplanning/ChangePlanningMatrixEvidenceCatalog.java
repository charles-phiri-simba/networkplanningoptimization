package com.simba.snip.npo.changeplanning;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Auditable evidence map for all 180 Phase 14 mandatory matrix items.
 * INTEGRATION/BEHAVIORAL entries must name an exact test method that the suite executes.
 */
final class ChangePlanningMatrixEvidenceCatalog {

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

    private ChangePlanningMatrixEvidenceCatalog() {
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
            type = Class.forName("com.simba.snip.npo.changeplanning." + className);
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
        put(map, 1, "V15 migration present", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "V15__phase14_change_execution_planning.sql");
        put(map, 2, "Planning/readiness only; no execute/apply", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanningController");
        put(map, 3, "No Phase 15 package", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", null);
        put(map, 4, "Plan distinct from proposal", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.networkChangePlanIsDistinctFromProposal",
                "NetworkChangePlanEntity");
        put(map, 5, "Plan distinct from ProposedAction", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.networkChangePlanIsDistinctFromProposedAction",
                "NetworkChangePlanEntity");
        put(map, 6, "txPower parameter scope", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "SimulatableParameterRegistry");
        put(map, 7, "Intent requires target id", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ParameterChangeIntent");
        put(map, 8, "Intent parameter is txPower", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ParameterChangeIntent");
        put(map, 9, "Maximum operation count = 1", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanningProperties");
        put(map, 10, "No vendor write in package", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.phase14DoesNotReferenceEnmTransportOrConnectors",
                "changeplanning");

        put(map, 11, "Missing proposal fails", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.missingProposalFailsCreate", "ChangePlanEligibilityService");
        put(map, 12, "APPROVED proposal accepted", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanEligibilityService");
        put(map, 13, "DRAFT blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 14, "VALIDATING blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 15, "RECOMMENDED blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 16, "REJECTED blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 17, "INVALID blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 18, "INVALIDATED blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 19, "EXPIRED blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 20, "SUPERSEDED blocked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.nonApprovedProposalStatusesBlocked", "ChangePlanEligibilityService");
        put(map, 21, "Proposal validity rechecked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenProposalValidityFails", "ChangePlanEligibilityService");
        put(map, 22, "Target rechecked", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenTargetMissing", "ChangePlanEligibilityService");
        put(map, 23, "Authoritative current read", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createUsesAuthoritativeCurrentAndProposalDesired",
                "ChangePlanEligibilityService");
        put(map, 24, "Caller current override impossible", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "CreateChangePlanRequest");
        put(map, 25, "Desired value proposal-derived", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createUsesAuthoritativeCurrentAndProposalDesired",
                "ChangePlanEligibilityService");
        put(map, 26, "Caller desired override impossible", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "CreateChangePlanRequest");
        put(map, 27, "Source snapshot provenance retained", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createdPlanRetainsSourceProvenance", "NetworkChangePlanEntity");
        put(map, 28, "Sync execution provenance retained", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createdPlanRetainsSourceProvenance", "NetworkChangePlanEntity");
        put(map, 29, "Phase12 knowledge authority used", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedByLowKnowledge", "ChangePlanEligibilityService");
        put(map, 30, "Phase12 drift authority used", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedByRelevantDrift", "ChangePlanEligibilityService");
        put(map, 31, "No selectedCandidateId on create request", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "CreateChangePlanRequest");
        put(map, 32, "Rank-one candidate resolved", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanEligibilityService");
        put(map, 33, "Exactly one rank-one required", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenRankOneAmbiguous", "ChangePlanEligibilityService");
        put(map, 34, "Proposed value must match candidate", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenCandidateValueMismatches", "ChangePlanEligibilityService");

        put(map, 35, "PLAN_CANDIDATE_NOT_FOUND code", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFailureCode");
        put(map, 36, "PLAN_CANDIDATE_AMBIGUOUS code", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenRankOneAmbiguous", "ChangePlanFailureCode");
        put(map, 37, "PLAN_CANDIDATE_VALUE_MISMATCH code", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createBlockedWhenCandidateValueMismatches", "ChangePlanFailureCode");
        put(map, 38, "Simulation run id retained from candidate", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanEligibilityService");
        put(map, 39, "No Twin simulation invoke in Phase14", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "changeplanning");
        put(map, 40, "No Twin simulation invoke (duplicate gate)", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "changeplanning");
        put(map, 41, "No duplicated simulation algorithm", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanEligibilityService");
        put(map, 42, "PLAN_TWIN_STALE code", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFailureCode");
        put(map, 43, "No caller candidate selection", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "CreateChangePlanRequest");
        put(map, 44, "Snapshot provenance field present", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createdPlanRetainsSourceProvenance", "NetworkChangePlanEntity");
        put(map, 45, "No raw vendor payload storage", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "changeplanning");

        put(map, 46, "SET_PARAMETER operation type", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "OperationType");
        put(map, 47, "Operation target type", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 48, "Operation target id", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 49, "Operation parameter", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 50, "Operation expected value", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 51, "Operation desired value", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 52, "Operation sequence starts at 1", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 53, "Deterministic operation sequence", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 54, "Operation count enforced", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanOperationBuilder");
        put(map, 55, "No vendor syntax on operation", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanOperationEntity");
        put(map, 56, "No endpoint on operation", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanOperationEntity");
        put(map, 57, "No protocol on operation", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanOperationEntity");
        put(map, 58, "No credential on operation", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanOperationEntity");
        put(map, 59, "No token on operation", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanOperationEntity");
        put(map, 60, "No execute endpoint", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanningController");

        put(map, 61, "Dependency service present", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 62, "Empty dependency graph valid", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 63, "Self-dependency rejected", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 64, "Cycle rejected", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 65, "Duplicate edge rejected", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 66, "External reference rejected", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 67, "Deterministic dependency order", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanDependencyService");
        put(map, 68, "Fingerprint includes dependencies", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 69, "Dependency change alters fingerprint", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 70, "Governance has no execution refs", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.noAutomaticProposedActionConversion",
                "ChangePlanGovernanceService");

        put(map, 71, "Rollback required by policy", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanningProperties");
        put(map, 72, "Rollback built", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 73, "Rollback target matches forward", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 74, "Rollback parameter matches forward", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 75, "Rollback expected = forward desired", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 76, "Rollback desired = forward expected", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 77, "Rollback sequence = 1", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 78, "Readiness checks rollback precondition", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanPreconditionService");
        put(map, 79, "Missing rollback fails validation", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanRollbackService");
        put(map, 80, "Fingerprint includes rollback", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 81, "No endpoint on rollback entity", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanRollbackOperationEntity");
        put(map, 82, "No executor on rollback entity", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanRollbackOperationEntity");
        put(map, 83, "No automatic rollback in governance", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanGovernanceService");
        put(map, 84, "No vendorCommand on rollback", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanRollbackOperationEntity");
        put(map, 85, "Plan service does not mutate canonical", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.planCreationDoesNotMutateCanonicalState",
                "NetworkChangePlanService");

        put(map, 86, "EXPECTED_PARAMETER_VALUE type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 87, "NETWORK_KNOWLEDGE_CONFIDENCE type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 88, "SOURCE_SYNCHRONIZATION_FRESHNESS type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 89, "NO_RELEVANT_DRIFT type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 90, "TWIN_COMPATIBILITY type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 91, "PROPOSAL_STILL_VALID type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 92, "TARGET_EXISTS type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 93, "ROLLBACK_AVAILABLE type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 94, "DEPENDENCY_GRAPH_VALID type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 95, "FINGERPRINT_CURRENT type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 96, "AUTHORIZATION_CURRENT type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 97, "Preconditions persisted at creation", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanPreconditionService");
        put(map, 98, "Preconditions genuinely evaluated", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createdPreconditionsAreEvaluatedNotStampedPass",
                "ChangePlanPreconditionService");
        put(map, 99, "PASS != UNKNOWN", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionResult");
        put(map, 100, "PASS != STALE", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionResult");
        put(map, 101, "Deterministic precondition order", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanPreconditionService");
        put(map, 102, "No raw vendor response on precondition", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanPreconditionEntity");
        put(map, 103, "No raw vendor response (duplicate)", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanPreconditionEntity");
        put(map, 104, "No LLM authority in Phase14", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "changeplanning");
        put(map, 105, "Preconditions participate in readiness", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanReadinessService");

        put(map, 106, "Fingerprint SHA-256", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 107, "Fingerprint UTF-8", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 108, "Stable fingerprint field order", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 109, "Fingerprint stable across collection order", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 110, "Fingerprint handles null fields", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 111, "Fingerprint includes enum names", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 112, "Fingerprint includes policy booleans", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 113, "Fingerprint normalizes numerics", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 114, "Fingerprint locale independent", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 115, "Fingerprint includes proposal id", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 116, "Fingerprint includes target binding", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 117, "Fingerprint includes expected/desired", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 118, "Fingerprint includes operations", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 119, "Fingerprint includes dependencies", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 120, "Fingerprint includes preconditions", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 121, "Fingerprint includes rollback", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 122, "Fingerprint includes source binding", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 123, "Fingerprint excludes volatile timestamps", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 124, "Fingerprint excludes actors", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");
        put(map, 125, "Fingerprint repeat deterministic", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFingerprintService");

        put(map, 126, "Safety rejects non-txPower", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangeExecutionSafetyPolicy");
        put(map, 127, "Safety rejects out-of-range high", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangeExecutionSafetyPolicy");
        put(map, 128, "Safety rejects out-of-range low", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangeExecutionSafetyPolicy");
        put(map, 129, "Validity service exists", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanValidityService");
        put(map, 130, "Knowledge gate enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.knowledgeGateAllowsHighMediumBlocksLowUnknown",
                "ChangePlanPreconditionService");
        put(map, 131, "Synchronization/freshness gate enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.staleSynchronizationBlocksReadiness",
                "ChangePlanPreconditionService");
        put(map, 132, "Validity references drift", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.driftInvalidatesAfterReadyForExecution", "ChangePlanValidityService");
        put(map, 133, "PLAN_CURRENT_VALUE_MISMATCH code exercised", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.currentMismatchInvalidatesPlan", "ChangePlanFailureCode");
        put(map, 134, "PLAN_TWIN_STALE code present", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFailureCode");
        put(map, 135, "Readiness checks rollback available", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanPreconditionService");
        put(map, 136, "DEPENDENCY_GRAPH_VALID type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 137, "FINGERPRINT_CURRENT type", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PreconditionType");
        put(map, 138, "Deterministic safety evaluation", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangeExecutionSafetyPolicy");
        put(map, 139, "Deterministic impact assessment", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangeImpactAssessmentService");
        put(map, 140, "No LLM authority (duplicate)", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "changeplanning");

        put(map, 141, "VIEW permission enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.viewPermissionEnforced", "ChangePlanAuthorizer");
        put(map, 142, "CREATE permission enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.createPermissionEnforced", "ChangePlanAuthorizer");
        put(map, 143, "REVIEW permission enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.reviewPermissionEnforced", "ChangePlanAuthorizer");
        put(map, 144, "AUTHORIZE permission enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.authorizePermissionEnforced", "ChangePlanAuthorizer");
        put(map, 145, "CANCEL permission enforced", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.cancelPermissionEnforced", "ChangePlanAuthorizer");
        put(map, 146, "VIEW does not grant CREATE", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.viewDoesNotGrantCreate", "ChangePlanAuthorizer");
        put(map, 147, "REVIEW does not grant AUTHORIZE", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.reviewerCannotAuthorizeAfterReview", "ChangePlanAuthorizer");
        put(map, 148, "CREATE does not grant AUTHORIZE", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.creatorCannotAuthorize", "ChangePlanAuthorizer");
        put(map, 149, "CANCEL does not grant AUTHORIZE", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.cancelDoesNotGrantAuthorize", "ChangePlanAuthorizer");
        put(map, 150, "Vendor import does not grant plan auth", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanAuthorizer");
        put(map, 151, "Proposal approval distinct from plan authorize", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.phase13ApprovalDoesNotGrantPhase14Authorization",
                "ChangePlanGovernanceService");
        put(map, 152, "Agents cannot authorize plans", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "agent");
        put(map, 153, "Review entity exists", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "NetworkChangePlanReviewEntity");
        put(map, 154, "Authorization actor persisted", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "NetworkChangePlanEntity");
        put(map, 155, "Authorization time persisted", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "NetworkChangePlanEntity");
        put(map, 156, "Authorized fingerprint persisted", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "NetworkChangePlanEntity");
        put(map, 157, "AUTHORIZED status exists", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PlanStatus");
        put(map, 158, "AUTHORIZED != READY_FOR_EXECUTION", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "PlanStatus");
        put(map, 159, "Stale authorization blocks readiness", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.staleAuthorizedFingerprintBlocksReadiness",
                "ChangePlanPreconditionService");
        put(map, 160, "Optimistic governance conflict safety", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.concurrentCancelConflictSafety", "ChangePlanGovernanceService");

        put(map, 161, "READY readiness result", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessResult");
        put(map, 162, "NOT_READY readiness result", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessResult");
        put(map, 163, "STALE readiness result", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessResult");
        put(map, 164, "UNKNOWN readiness result", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessResult");
        put(map, 165, "Readiness enum excludes BLOCKED", EvidenceType.BEHAVIORAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessResult");
        put(map, 166, "BLOCKED plan status exists", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "PlanStatus");
        put(map, 167, "Readiness requires authorization", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanReadinessService");
        put(map, 168, "Readiness evaluates hard gates", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanReadinessService");
        put(map, 169, "Readiness assessment entity", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ExecutionReadinessAssessmentEntity");
        put(map, 170, "Readiness assessment persisted", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ExecutionReadinessAssessmentEntity");
        put(map, 171, "Readiness promotes to READY_FOR_EXECUTION", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.happyPathToReadyForExecution", "ChangePlanReadinessService");
        put(map, 172, "Readiness has no execution side effects", EvidenceType.STRUCTURAL,
                "ChangePlanningArchitectureIsolationTest.noAutomaticProposedActionConversion",
                "ChangePlanReadinessService");
        put(map, 173, "Invalidates on current mismatch", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.readyForExecutionCurrentMismatchInvalidates",
                "ChangePlanValidityService");
        put(map, 174, "LOW knowledge invalidation", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.readyForExecutionLowKnowledgeInvalidates",
                "ChangePlanValidityService");
        put(map, 175, "UNKNOWN knowledge invalidation", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.readyForExecutionUnknownKnowledgeInvalidates",
                "ChangePlanValidityService");
        put(map, 176, "Relevant drift invalidation", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.driftInvalidatesAfterReadyForExecution",
                "ChangePlanValidityService");
        put(map, 177, "PLAN_EXPIRED code", EvidenceType.STRUCTURAL,
                "ChangePlanningMandatoryMatrixTest.mandatoryMatrixItem", "ChangePlanFailureCode");
        put(map, 178, "Cancel from AUTHORIZED", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.cancelFromAuthorizedAndReadyForExecution",
                "ChangePlanGovernanceService");
        put(map, 179, "Cancel from READY_FOR_EXECUTION", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.cancelFromAuthorizedAndReadyForExecution",
                "ChangePlanGovernanceService");
        put(map, 180, "CANCELLED status exists", EvidenceType.INTEGRATION,
                "ChangePlanningApiTest.cancelFromAuthorizedAndReadyForExecution", "PlanStatus");

        if (map.size() != 180) {
            throw new IllegalStateException("expected 180 evidence items, got " + map.size());
        }
        for (Evidence evidence : map.values()) {
            Objects.requireNonNull(evidence.requirement());
            Objects.requireNonNull(evidence.type());
            Objects.requireNonNull(evidence.evidence());
            Objects.requireNonNull(evidence.status());
            if (evidence.status() != Status.VERIFIED_PASS) {
                throw new IllegalStateException("catalog item not verified: " + evidence.id());
            }
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
}
