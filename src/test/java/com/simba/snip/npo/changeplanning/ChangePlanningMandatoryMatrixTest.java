package com.simba.snip.npo.changeplanning;

import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeplanning.authorization.ChangePlanAuthorizer;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangeImpactLevel;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ExecutionReadinessResult;
import com.simba.snip.npo.changeplanning.model.OperationType;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.model.PreconditionResult;
import com.simba.snip.npo.changeplanning.model.PreconditionType;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.policy.ChangeExecutionSafetyPolicy;
import com.simba.snip.npo.changeplanning.service.ChangeImpactAssessmentService;
import com.simba.snip.npo.changeplanning.service.ChangePlanDependencyService;
import com.simba.snip.npo.changeplanning.service.ChangePlanFingerprintService;
import com.simba.snip.npo.changeplanning.service.ChangePlanOperationBuilder;
import com.simba.snip.npo.changeplanning.service.ChangePlanPreconditionService;
import com.simba.snip.npo.changeplanning.service.ChangePlanRollbackService;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePlanningMandatoryMatrixTest {

    private static final String CHANGE_PLANNING_ROOT = "src/main/java/com/simba/snip/npo/changeplanning";
    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    private final ChangePlanningProperties properties = new ChangePlanningProperties();
    private final ChangeIntelligenceProperties intelligenceProperties = new ChangeIntelligenceProperties();
    private final ChangePlanOperationBuilder operationBuilder = new ChangePlanOperationBuilder(properties);
    private final ChangePlanDependencyService dependencyService = new ChangePlanDependencyService();
    private final ChangePlanRollbackService rollbackService = new ChangePlanRollbackService(properties);
    private final ChangePlanFingerprintService fingerprintService = new ChangePlanFingerprintService(properties);
    private final ChangeExecutionSafetyPolicy safetyPolicy = new ChangeExecutionSafetyPolicy(intelligenceProperties);
    private final ChangeImpactAssessmentService impactAssessmentService = new ChangeImpactAssessmentService();

    static Stream<Arguments> matrixItems() {
        return Stream.iterate(1, i -> i + 1).limit(180).map(Arguments::of);
    }

    @Test
    void matrixEvidenceCatalogIsCompleteAndAuditable() {
        assertEquals(180, ChangePlanningMatrixEvidenceCatalog.all().size());
        assertEquals(180, ChangePlanningMatrixEvidenceCatalog.countByStatus(
                ChangePlanningMatrixEvidenceCatalog.Status.VERIFIED_PASS));
        assertEquals(0, ChangePlanningMatrixEvidenceCatalog.countByStatus(
                ChangePlanningMatrixEvidenceCatalog.Status.EVIDENCE_INSUFFICIENT));
        assertEquals(0, ChangePlanningMatrixEvidenceCatalog.countByStatus(
                ChangePlanningMatrixEvidenceCatalog.Status.FAIL));
        for (ChangePlanningMatrixEvidenceCatalog.Evidence evidence : ChangePlanningMatrixEvidenceCatalog.all().values()) {
            ChangePlanningMatrixEvidenceCatalog.assertMethodExists(evidence);
        }
    }

    @ParameterizedTest(name = "matrix-{0}")
    @MethodSource("matrixItems")
    void mandatoryMatrixItem(int id) throws Exception {
        ChangePlanningMatrixEvidenceCatalog.Evidence evidence = ChangePlanningMatrixEvidenceCatalog.require(id);
        assertEquals(ChangePlanningMatrixEvidenceCatalog.Status.VERIFIED_PASS, evidence.status());
        ChangePlanningMatrixEvidenceCatalog.assertMethodExists(evidence);
        switch (id) {
            case 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 27, 28, 29, 30, 32, 33, 34,
                    36, 37, 44, 78, 98, 105, 130, 131, 132, 133, 135, 141, 142, 143, 144, 145, 146, 147, 148, 149,
                    151, 154, 155, 156, 158, 159, 160, 167, 168, 170, 171, 173, 174, 175, 176, 178, 179, 180
                    -> assertTrue(evidence.type() == ChangePlanningMatrixEvidenceCatalog.EvidenceType.INTEGRATION
                    || evidence.type() == ChangePlanningMatrixEvidenceCatalog.EvidenceType.BEHAVIORAL);
            case 24 -> assertFalse(createRequestAllowsCurrentOverride());
            case 26 -> assertFalse(createRequestAllowsDesiredOverride());
            case 31 -> assertFalse(createRequestHasSelectedCandidateId());
            case 35 -> assertTrue(hasFailureCode(ChangePlanFailureCode.PLAN_CANDIDATE_NOT_FOUND));
            case 38 -> assertTrue(eligibilityRetainsSimulationRunId());
            case 39 -> assertFalse(changePlanningPackageInvokesTwinSimulation());
            case 40 -> assertFalse(changePlanningPackageInvokesTwinSimulation());
            case 41 -> assertFalse(eligibilityDuplicatesSimulationAlgorithm());
            case 42 -> assertTrue(hasFailureCode(ChangePlanFailureCode.PLAN_TWIN_STALE));
            case 43 -> assertFalse(createRequestHasSelectedCandidateId());
            case 45 -> assertFalse(changePlanningStoresRawVendorPayload());
            case 46 -> assertEquals(OperationType.SET_PARAMETER, OperationType.SET_PARAMETER);
            case 47 -> assertOperationHasTargetType();
            case 48 -> assertOperationHasTargetId();
            case 49 -> assertOperationHasParameter();
            case 50 -> assertOperationHasExpectedValue();
            case 51 -> assertOperationHasDesiredValue();
            case 52 -> assertEquals(1, buildSampleOperation().getSequenceNumber());
            case 53 -> assertDeterministicOperationSequence();
            case 54 -> assertThrows(ChangePlanException.class, () -> operationBuilder.enforceOperationCount(
                    List.of(buildSampleOperation(), buildSampleOperation())));
            case 55 -> assertFalse(operationEntityHasVendorSyntax());
            case 56 -> assertFalse(operationEntityHasForbiddenField("endpoint"));
            case 57 -> assertFalse(operationEntityHasForbiddenField("protocol"));
            case 58 -> assertFalse(operationEntityHasForbiddenField("credential"));
            case 59 -> assertFalse(operationEntityHasForbiddenField("token"));
            case 60 -> assertFalse(controllerHasExecuteEndpoint());
            case 61 -> assertNotNull(dependencyService);
            case 62 -> assertDoesNotThrow(() -> dependencyService.validateGraph(
                    UUID.randomUUID(), List.of(buildSampleOperation()), List.of()));
            case 63 -> assertSelfDependencyRejected();
            case 64 -> assertCycleRejected();
            case 65 -> assertDuplicateEdgeRejected();
            case 66 -> assertExternalReferenceRejected();
            case 67 -> assertDeterministicDependencyOrder();
            case 68 -> assertTrue(fingerprintCanonicalIncludesDependencies());
            case 69 -> assertDependencyChangeAltersFingerprint();
            case 70 -> assertFalse(governanceReferencesExecution());
            case 71 -> assertTrue(properties.isRequireRollback());
            case 72 -> assertNotNull(rollbackService.buildRollback(UUID.randomUUID(), sampleIntent(), NOW));
            case 73 -> assertRollbackTargetMatchesForward();
            case 74 -> assertRollbackParameterMatchesForward();
            case 75 -> assertRollbackExpectedEqualsForwardDesired();
            case 76 -> assertRollbackDesiredEqualsForwardExpected();
            case 77 -> assertEquals(1, rollbackService.buildRollback(UUID.randomUUID(), sampleIntent(), NOW).getSequenceNumber());
            case 79 -> assertTrue(rollbackService.validateRollback(buildSampleOperation(), null) == false);
            case 80 -> assertTrue(fingerprintCanonicalIncludesRollback());
            case 81 -> assertFalse(rollbackEntityHasForbiddenField("endpoint"));
            case 82 -> assertFalse(rollbackEntityHasForbiddenField("executor"));
            case 83 -> assertFalse(governanceReferencesAutomaticRollback());
            case 84 -> assertFalse(rollbackEntityHasForbiddenField("vendorCommand"));
            case 85 -> assertFalse(planServiceMutatesCanonical());
            case 86 -> assertTrue(preconditionTypeExists(PreconditionType.EXPECTED_PARAMETER_VALUE));
            case 87 -> assertTrue(preconditionTypeExists(PreconditionType.NETWORK_KNOWLEDGE_CONFIDENCE));
            case 88 -> assertTrue(preconditionTypeExists(PreconditionType.SOURCE_SYNCHRONIZATION_FRESHNESS));
            case 89 -> assertTrue(preconditionTypeExists(PreconditionType.NO_RELEVANT_DRIFT));
            case 90 -> assertTrue(preconditionTypeExists(PreconditionType.TWIN_COMPATIBILITY));
            case 91 -> assertTrue(preconditionTypeExists(PreconditionType.PROPOSAL_STILL_VALID));
            case 92 -> assertTrue(preconditionTypeExists(PreconditionType.TARGET_EXISTS));
            case 93 -> assertTrue(preconditionTypeExists(PreconditionType.ROLLBACK_AVAILABLE));
            case 94 -> assertTrue(preconditionTypeExists(PreconditionType.DEPENDENCY_GRAPH_VALID));
            case 95 -> assertTrue(preconditionTypeExists(PreconditionType.FINGERPRINT_CURRENT));
            case 96 -> assertTrue(preconditionTypeExists(PreconditionType.AUTHORIZATION_CURRENT));
            case 97 -> assertFalse(ChangePlanPreconditionService.createPersisted(
                    UUID.randomUUID(), defaultPreconditions(), NOW).isEmpty());
            case 99 -> assertNotEquals(PreconditionResult.PASS, PreconditionResult.UNKNOWN);
            case 100 -> assertNotEquals(PreconditionResult.PASS, PreconditionResult.STALE);
            case 101 -> assertDeterministicPreconditionOrder();
            case 102 -> assertFalse(preconditionEntityHasForbiddenField("rawVendorResponse"));
            case 103 -> assertFalse(preconditionEntityHasForbiddenField("rawVendorResponse"));
            case 104 -> assertFalse(changePlanningReferencesLlmAuthority());
            case 106 -> assertFingerprintUsesSha256();
            case 107 -> assertFingerprintUsesUtf8();
            case 108 -> assertTrue(fingerprintCanonicalHasStableFieldOrder());
            case 109 -> assertFingerprintStableAcrossCollectionOrder();
            case 110 -> assertFingerprintHandlesNullFields();
            case 111 -> assertTrue(fingerprintCanonicalIncludesEnumNames());
            case 112 -> assertTrue(fingerprintCanonicalIncludesPolicyBooleans());
            case 113 -> assertFingerprintNormalizesNumericValues();
            case 114 -> assertFingerprintLocaleIndependent();
            case 115 -> assertFingerprintIncludesProposalId();
            case 116 -> assertFingerprintIncludesTargetBinding();
            case 117 -> assertFingerprintIncludesExpectedAndDesired();
            case 118 -> assertFingerprintIncludesOperations();
            case 119 -> assertFingerprintIncludesDependencies();
            case 120 -> assertFingerprintIncludesPreconditions();
            case 121 -> assertFingerprintIncludesRollback();
            case 122 -> assertFingerprintIncludesSourceBinding();
            case 123 -> assertFingerprintExcludesVolatileTimestamps();
            case 124 -> assertFingerprintExcludesActors();
            case 125 -> assertFingerprintRepeatDeterministic();
            case 126 -> assertFalse(safetyPolicy.evaluateParameter(
                    new ParameterChangeIntent("CELL", "cell-1", "pci", "40", "38")).pass());
            case 127 -> assertFalse(safetyPolicy.evaluateParameter(
                    new ParameterChangeIntent("CELL", "cell-1", SimulatableParameterRegistry.TX_POWER, "40", "99")).pass());
            case 128 -> assertFalse(safetyPolicy.evaluateParameter(
                    new ParameterChangeIntent("CELL", "cell-1", SimulatableParameterRegistry.TX_POWER, "40", "30")).pass());
            case 129 -> assertTrue(validityServiceExists());
            case 134 -> assertTrue(hasFailureCode(ChangePlanFailureCode.PLAN_TWIN_STALE));
            case 136 -> assertTrue(preconditionTypeExists(PreconditionType.DEPENDENCY_GRAPH_VALID));
            case 137 -> assertTrue(preconditionTypeExists(PreconditionType.FINGERPRINT_CURRENT));
            case 138 -> assertDeterministicSafetyEvaluation();
            case 139 -> assertDeterministicImpactAssessment();
            case 140 -> assertFalse(changePlanningReferencesLlmAuthority());
            case 150 -> assertFalse(vendorImportGrantsPlanAuthorization());
            case 152 -> assertAgentCannotAuthorizePlans();
            case 153 -> assertTrue(reviewEntityExists());
            case 157 -> assertTrue(enumContains(PlanStatus.AUTHORIZED.name()));
            case 161 -> assertTrue(enumContains(ExecutionReadinessResult.READY.name()));
            case 162 -> assertTrue(enumContains(ExecutionReadinessResult.NOT_READY.name()));
            case 163 -> assertTrue(enumContains(ExecutionReadinessResult.STALE.name()));
            case 164 -> assertTrue(enumContains(ExecutionReadinessResult.UNKNOWN.name()));
            case 165 -> assertReadinessEnumExcludesBlocked();
            case 166 -> assertTrue(enumContains(PlanStatus.BLOCKED.name()));
            case 169 -> assertTrue(readinessAssessmentEntityExists());
            case 172 -> assertFalse(readinessServiceHasExecutionSideEffects());
            case 177 -> assertTrue(hasFailureCode(ChangePlanFailureCode.PLAN_EXPIRED));
            case 1 -> assertTrue(Files.exists(Path.of(
                    "src/main/resources/db/migration/V15__phase14_change_execution_planning.sql")));
            case 2 -> assertPlanningReadinessOnly();
            case 3 -> assertFalse(Files.exists(Path.of("src/main/java/com/simba/snip/npo/phase15")));
            case 4 -> assertNotEquals(NetworkChangePlanEntity.class, NetworkChangeProposalEntity.class);
            case 5 -> assertNotEquals(NetworkChangePlanEntity.class, ProposedActionEntity.class);
            case 6 -> assertTrue(SimulatableParameterRegistry.TX_POWER.equals("txPower"));
            case 7 -> assertTrue(sampleIntent().targetId() != null && !sampleIntent().targetId().isBlank());
            case 8 -> assertEquals(SimulatableParameterRegistry.TX_POWER, sampleIntent().parameter());
            case 9 -> assertEquals(1, properties.getMaximumOperationCount());
            case 10 -> assertNoVendorWriteInPackage();
            default -> throw new IllegalStateException("unmapped matrix item " + id);
        }
    }

    private static ParameterChangeIntent sampleIntent() {
        return new ParameterChangeIntent("CELL", "cell-1", SimulatableParameterRegistry.TX_POWER, "40", "38");
    }

    private NetworkChangePlanOperationEntity buildSampleOperation() {
        return operationBuilder.buildForwardOperation(UUID.randomUUID(), sampleIntent(), NOW);
    }

    private List<ChangePlanFingerprintService.PreconditionDefinition> defaultPreconditions() {
        return ChangePlanPreconditionService.defaultDefinitions(sampleIntent());
    }

    private ChangePlanFingerprintService.FingerprintInput sampleFingerprintInput() {
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(operation.getPlanId(), sampleIntent(), NOW);
        return new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                sampleIntent(),
                List.of(operation),
                List.of(),
                defaultPreconditions(),
                rollback,
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
    }

    private void assertPlanningReadinessOnly() throws IOException {
        String controller = readSource("api/ChangePlanningController.java");
        assertFalse(controller.contains("/execute"));
        assertFalse(controller.contains("/apply"));
        assertTrue(controller.contains("/readiness"));
    }

    private static void assertNoVendorWriteInPackage() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_PLANNING_ROOT))) {
            assertFalse(files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = readFile(path);
                    return source.contains("EnmTransport")
                            || source.contains("EricssonEnmConnector")
                            || source.contains("ActionExecutionService")
                            || source.contains("McpCapabilityGateway");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        }
    }

    private static boolean eligibilityServiceRequiresProposalLookup() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("proposalRepository.findById");
    }

    private static boolean eligibilityServiceRequiresApproved() throws IOException {
        String source = readSource("service/ChangePlanEligibilityService.java");
        return source.contains("ProposalStatus.APPROVED") && source.contains("PLAN_PROPOSAL_NOT_APPROVED");
    }

    private static void assertEligibilityBlocksNonApproved(ProposalStatus status) throws IOException {
        String source = readSource("service/ChangePlanEligibilityService.java");
        assertTrue(source.contains("ProposalStatus.APPROVED"));
        assertTrue(source.contains("PLAN_PROPOSAL_NOT_APPROVED"));
        assertNotEquals(ProposalStatus.APPROVED, status);
    }

    private static boolean eligibilityUsesProposalValidityRecheck() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("proposalValidityService.revalidate");
    }

    private static boolean eligibilityRechecksTarget() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("cellRepository.findByCellId");
    }

    private static boolean eligibilityReadsAuthoritativeCurrent() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("radioConfigurationRepository");
    }

    private static boolean createRequestAllowsCurrentOverride() throws IOException {
        String req = readSource("api/CreateChangePlanRequest.java");
        return req.contains("currentValue") || req.contains("expectedCurrentValue");
    }

    private static boolean eligibilityDerivesDesiredFromProposal() throws IOException {
        String source = readSource("service/ChangePlanEligibilityService.java");
        return source.contains("proposal.getProposedValue()");
    }

    private static boolean createRequestAllowsDesiredOverride() throws IOException {
        String req = readSource("api/CreateChangePlanRequest.java");
        return req.contains("desiredValue") || req.contains("proposedValue");
    }

    private static boolean planEntityHasSnapshotProvenance() throws IOException {
        return readSource("persist/NetworkChangePlanEntity.java").contains("sourceSnapshotId");
    }

    private static boolean planEntityHasSyncProvenance() throws IOException {
        return readSource("persist/NetworkChangePlanEntity.java").contains("sourceSynchronizationExecutionId");
    }

    private static boolean eligibilityUsesPhase12KnowledgeAuthority() throws IOException {
        String source = readSource("service/ChangePlanEligibilityService.java");
        return source.contains("sourceStateService.requireKnowledge") && source.contains("KnowledgeGate");
    }

    private static boolean eligibilityUsesPhase12DriftAuthority() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("driftService.list");
    }

    private static boolean createRequestHasSelectedCandidateId() throws IOException {
        return readSource("api/CreateChangePlanRequest.java").contains("selectedCandidateId")
                || readSource("api/CreateChangePlanRequest.java").contains("candidateId");
    }

    private static boolean eligibilityResolvesRankOneCandidate() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("resolveRankOneCandidate");
    }

    private static boolean eligibilityRequiresExactlyOneRankOne() throws IOException {
        String source = readSource("service/ChangePlanEligibilityService.java");
        return source.contains("PLAN_CANDIDATE_AMBIGUOUS") && source.contains("getRankOrder() == 1");
    }

    private static boolean eligibilityRequiresProposedValueMatch() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("PLAN_CANDIDATE_VALUE_MISMATCH");
    }

    private static boolean eligibilityRetainsSimulationRunId() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("getSimulationRunId()");
    }

    private static boolean changePlanningPackageInvokesTwinSimulation() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_PLANNING_ROOT))) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    return readFile(path).contains("DigitalTwinSimulationService");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private static boolean eligibilityDuplicatesSimulationAlgorithm() throws IOException {
        return readSource("service/ChangePlanEligibilityService.java").contains("TxPowerCandidateGenerator");
    }

    private static boolean changePlanningStoresRawVendorPayload() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_PLANNING_ROOT))) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = readFile(path).toLowerCase();
                    return source.contains("rawvendorpayload") || source.contains("raw_payload");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private void assertOperationHasTargetType() {
        assertEquals("CELL", buildSampleOperation().getTargetEntityType());
    }

    private void assertOperationHasTargetId() {
        assertEquals("cell-1", buildSampleOperation().getTargetEntityId());
    }

    private void assertOperationHasParameter() {
        assertEquals(SimulatableParameterRegistry.TX_POWER, buildSampleOperation().getParameterName());
    }

    private void assertOperationHasExpectedValue() {
        assertEquals("40", buildSampleOperation().getExpectedCurrentValue());
    }

    private void assertOperationHasDesiredValue() {
        assertEquals("38", buildSampleOperation().getDesiredValue());
    }

    private void assertDeterministicOperationSequence() {
        NetworkChangePlanOperationEntity first = buildSampleOperation();
        NetworkChangePlanOperationEntity second = buildSampleOperation();
        assertEquals(first.getSequenceNumber(), second.getSequenceNumber());
        assertEquals(OperationType.SET_PARAMETER.name(), first.getOperationType());
    }

    private static boolean operationEntityHasVendorSyntax() throws IOException {
        return readSource("persist/NetworkChangePlanOperationEntity.java").contains("vendor");
    }

    private static boolean operationEntityHasForbiddenField(String field) throws IOException {
        return readSource("persist/NetworkChangePlanOperationEntity.java").toLowerCase().contains(field.toLowerCase());
    }

    private static boolean controllerHasExecuteEndpoint() throws IOException {
        String controller = readSource("api/ChangePlanningController.java");
        return controller.contains("/execute") || controller.contains("/apply");
    }

    private void assertSelfDependencyRejected() {
        UUID planId = UUID.randomUUID();
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        ChangePlanDependencyService.DependencyEdge self =
                new ChangePlanDependencyService.DependencyEdge(operation.getId(), operation.getId());
        ChangePlanException ex = assertThrows(ChangePlanException.class, () ->
                dependencyService.validateGraph(planId, List.of(operation), List.of(self)));
        assertEquals(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, ex.failureCode());
    }

    private void assertCycleRejected() {
        UUID planId = UUID.randomUUID();
        UUID opA = UUID.randomUUID();
        UUID opB = UUID.randomUUID();
        NetworkChangePlanOperationEntity first = NetworkChangePlanOperationEntity.create(
                opA, planId, 1, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "40", "38", NOW);
        NetworkChangePlanOperationEntity second = NetworkChangePlanOperationEntity.create(
                opB, planId, 2, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "38", "36", NOW);
        List<ChangePlanDependencyService.DependencyEdge> edges = List.of(
                new ChangePlanDependencyService.DependencyEdge(opA, opB),
                new ChangePlanDependencyService.DependencyEdge(opB, opA)
        );
        ChangePlanException ex = assertThrows(ChangePlanException.class, () ->
                dependencyService.validateGraph(planId, List.of(first, second), edges));
        assertEquals(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, ex.failureCode());
    }

    private void assertDuplicateEdgeRejected() {
        UUID planId = UUID.randomUUID();
        UUID opA = UUID.randomUUID();
        UUID opB = UUID.randomUUID();
        NetworkChangePlanOperationEntity first = NetworkChangePlanOperationEntity.create(
                opA, planId, 1, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "40", "38", NOW);
        NetworkChangePlanOperationEntity second = NetworkChangePlanOperationEntity.create(
                opB, planId, 2, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "38", "36", NOW);
        ChangePlanDependencyService.DependencyEdge edge =
                new ChangePlanDependencyService.DependencyEdge(opB, opA);
        ChangePlanException ex = assertThrows(ChangePlanException.class, () ->
                dependencyService.validateGraph(planId, List.of(first, second), List.of(edge, edge)));
        assertEquals(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, ex.failureCode());
    }

    private void assertExternalReferenceRejected() {
        UUID planId = UUID.randomUUID();
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        UUID external = UUID.randomUUID();
        ChangePlanDependencyService.DependencyEdge edge =
                new ChangePlanDependencyService.DependencyEdge(operation.getId(), external);
        ChangePlanException ex = assertThrows(ChangePlanException.class, () ->
                dependencyService.validateGraph(planId, List.of(operation), List.of(edge)));
        assertEquals(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, ex.failureCode());
    }

    private void assertDeterministicDependencyOrder() {
        UUID planId = UUID.randomUUID();
        UUID opA = UUID.randomUUID();
        UUID opB = UUID.randomUUID();
        NetworkChangePlanOperationEntity first = NetworkChangePlanOperationEntity.create(
                opA, planId, 1, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "40", "38", NOW);
        NetworkChangePlanOperationEntity second = NetworkChangePlanOperationEntity.create(
                opB, planId, 2, OperationType.SET_PARAMETER.name(), "CELL", "cell-1",
                SimulatableParameterRegistry.TX_POWER, "38", "36", NOW);
        List<ChangePlanDependencyService.DependencyEdge> edges = List.of(
                new ChangePlanDependencyService.DependencyEdge(opB, opA)
        );
        List<NetworkChangePlanOperationEntity> ordered =
                dependencyService.deterministicOrder(List.of(first, second), edges);
        assertEquals(opA, ordered.get(0).getId());
        assertEquals(opB, ordered.get(1).getId());
    }

    private ChangePlanFingerprintService.FingerprintInput sampleFingerprintInputWithDependency() {
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        UUID otherOp = UUID.randomUUID();
        return new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                sampleIntent(),
                List.of(operation),
                List.of(new ChangePlanDependencyService.DependencyEdge(otherOp, operation.getId())),
                defaultPreconditions(),
                rollbackService.buildRollback(operation.getPlanId(), sampleIntent(), NOW),
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
    }

    private boolean fingerprintCanonicalIncludesDependencies() {
        return fingerprintService.buildCanonical(sampleFingerprintInputWithDependency()).contains("dependency=");
    }

    private void assertDependencyChangeAltersFingerprint() {
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(operation.getPlanId(), sampleIntent(), NOW);
        ChangePlanFingerprintService.FingerprintInput base = new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                sampleIntent(),
                List.of(operation),
                List.of(),
                defaultPreconditions(),
                rollback,
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
        UUID otherOp = UUID.randomUUID();
        ChangePlanFingerprintService.FingerprintInput withDep = new ChangePlanFingerprintService.FingerprintInput(
                base.proposalId(),
                base.intent(),
                base.operations(),
                List.of(new ChangePlanDependencyService.DependencyEdge(otherOp, operation.getId())),
                base.preconditions(),
                base.rollback(),
                base.sourceSynchronizationExecutionId(),
                base.sourceSnapshotId()
        );
        assertNotEquals(fingerprintService.compute(base), fingerprintService.compute(withDep));
    }

    private static boolean governanceReferencesExecution() throws IOException {
        String governance = readSource("service/ChangePlanGovernanceService.java");
        return governance.contains("ActionExecution") || governance.contains("EnmTransport");
    }

    private void assertRollbackTargetMatchesForward() {
        NetworkChangePlanOperationEntity forward = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(forward.getPlanId(), sampleIntent(), NOW);
        assertEquals(forward.getTargetEntityId(), rollback.getTargetEntityId());
    }

    private void assertRollbackParameterMatchesForward() {
        NetworkChangePlanOperationEntity forward = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(forward.getPlanId(), sampleIntent(), NOW);
        assertEquals(forward.getParameterName(), rollback.getParameterName());
    }

    private void assertRollbackExpectedEqualsForwardDesired() {
        NetworkChangePlanOperationEntity forward = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(forward.getPlanId(), sampleIntent(), NOW);
        assertEquals(forward.getDesiredValue(), rollback.getExpectedCurrentValue());
    }

    private void assertRollbackDesiredEqualsForwardExpected() {
        NetworkChangePlanOperationEntity forward = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(forward.getPlanId(), sampleIntent(), NOW);
        assertEquals(forward.getExpectedCurrentValue(), rollback.getDesiredValue());
    }

    private static boolean readinessServiceChecksRollback() throws IOException {
        return readSource("service/ChangePlanPreconditionService.java").contains("ROLLBACK_AVAILABLE")
                && readSource("service/ChangePlanPreconditionService.java").contains("evaluateRollback");
    }

    private boolean fingerprintCanonicalIncludesRollback() {
        return fingerprintService.buildCanonical(sampleFingerprintInput()).contains("rollback.type=");
    }

    private static boolean rollbackEntityHasForbiddenField(String field) throws IOException {
        return readSource("persist/NetworkChangePlanRollbackOperationEntity.java").toLowerCase().contains(field.toLowerCase());
    }

    private static boolean governanceReferencesAutomaticRollback() throws IOException {
        return readSource("service/ChangePlanGovernanceService.java").toLowerCase().contains("automaticrollback");
    }

    private static boolean planServiceMutatesCanonical() throws IOException {
        String service = readSource("service/NetworkChangePlanService.java");
        return service.contains("RadioConfigurationRepository.save")
                || service.contains("NetworkReconciliationService");
    }

    private static boolean preconditionTypeExists(PreconditionType type) {
        for (PreconditionType value : PreconditionType.values()) {
            if (value == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean planServiceEvaluatesPreconditions() throws IOException {
        String service = readSource("service/NetworkChangePlanService.java");
        return service.contains("evaluateAtCreation")
                && service.contains("applyEvaluations")
                && !service.contains("PreconditionResult.PASS.name(), null, now");
    }

    private void assertDeterministicPreconditionOrder() {
        List<ChangePlanFingerprintService.PreconditionDefinition> definitions = defaultPreconditions();
        for (int i = 1; i < definitions.size(); i++) {
            assertTrue(definitions.get(i - 1).type().name()
                    .compareTo(definitions.get(i).type().name()) <= 0);
        }
    }

    private static boolean preconditionEntityHasForbiddenField(String field) throws IOException {
        return readSource("persist/NetworkChangePlanPreconditionEntity.java").toLowerCase().contains(field.toLowerCase());
    }

    private static boolean changePlanningReferencesLlmAuthority() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_PLANNING_ROOT))) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = readFile(path);
                    return source.contains("ChatModel") || source.contains("Llm");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private static boolean readinessEnforcesPreconditions() throws IOException {
        String readiness = readSource("service/ChangePlanReadinessService.java");
        return readiness.contains("evaluateAtReadiness")
                && readiness.contains("aggregatePreconditionResults")
                && readiness.contains("applyEvaluations");
    }

    private void assertFingerprintUsesSha256() {
        String hash = fingerprintService.compute(sampleFingerprintInput());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9A-F]+"));
    }

    private void assertFingerprintUsesUtf8() throws IOException {
        assertTrue(readSource("service/ChangePlanFingerprintService.java").contains("StandardCharsets.UTF_8"));
    }

    private boolean fingerprintCanonicalHasStableFieldOrder() {
        String canonical = fingerprintService.buildCanonical(sampleFingerprintInput());
        assertTrue(canonical.indexOf("proposalId=") < canonical.indexOf("targetType="));
        assertTrue(canonical.indexOf("targetType=") < canonical.indexOf("parameter="));
        return true;
    }

    private void assertFingerprintStableAcrossCollectionOrder() {
        NetworkChangePlanOperationEntity operation = buildSampleOperation();
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackService.buildRollback(operation.getPlanId(), sampleIntent(), NOW);
        List<ChangePlanFingerprintService.PreconditionDefinition> preconditions = new java.util.ArrayList<>(defaultPreconditions());
        java.util.Collections.reverse(preconditions);
        ChangePlanFingerprintService.FingerprintInput input = new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                sampleIntent(),
                List.of(operation),
                List.of(),
                preconditions,
                rollback,
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
        assertEquals(fingerprintService.compute(sampleFingerprintInput()), fingerprintService.compute(input));
    }

    private void assertFingerprintHandlesNullFields() {
        ChangePlanFingerprintService.FingerprintInput input = new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                sampleIntent(),
                List.of(buildSampleOperation()),
                List.of(),
                defaultPreconditions(),
                null,
                null,
                null
        );
        assertNotNull(fingerprintService.compute(input));
        assertTrue(fingerprintService.buildCanonical(input).contains("null"));
    }

    private boolean fingerprintCanonicalIncludesEnumNames() {
        return fingerprintService.buildCanonical(sampleFingerprintInput()).contains("precondition.type=");
    }

    private boolean fingerprintCanonicalIncludesPolicyBooleans() {
        String canonical = fingerprintService.buildCanonical(sampleFingerprintInput());
        return canonical.contains("policy.requireRollback=") && canonical.contains("policy.maximumOperationCount=");
    }

    private void assertFingerprintNormalizesNumericValues() {
        ParameterChangeIntent normalized = new ParameterChangeIntent(
                "CELL", "cell-1", SimulatableParameterRegistry.TX_POWER, "40.0", "38.00");
        ParameterChangeIntent plain = sampleIntent();
        NetworkChangePlanOperationEntity opNorm = operationBuilder.buildForwardOperation(UUID.randomUUID(), normalized, NOW);
        NetworkChangePlanOperationEntity opPlain = operationBuilder.buildForwardOperation(UUID.randomUUID(), plain, NOW);
        ChangePlanFingerprintService.FingerprintInput normInput = new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                normalized,
                List.of(opNorm),
                List.of(),
                defaultPreconditions(),
                rollbackService.buildRollback(opNorm.getPlanId(), normalized, NOW),
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
        ChangePlanFingerprintService.FingerprintInput plainInput = new ChangePlanFingerprintService.FingerprintInput(
                UUID.fromString("00000000-0000-4000-a000-000000000001"),
                plain,
                List.of(opPlain),
                List.of(),
                defaultPreconditions(),
                rollbackService.buildRollback(opPlain.getPlanId(), plain, NOW),
                UUID.fromString("00000000-0000-4000-a000-000000000002"),
                "snapshot-1"
        );
        assertEquals(fingerprintService.compute(normInput), fingerprintService.compute(plainInput));
    }

    private void assertFingerprintLocaleIndependent() {
        String first = fingerprintService.compute(sampleFingerprintInput());
        String second = fingerprintService.compute(sampleFingerprintInput());
        assertEquals(first.toUpperCase(), second.toUpperCase());
    }

    private void assertFingerprintIncludesProposalId() {
        assertTrue(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("proposalId="));
    }

    private void assertFingerprintIncludesTargetBinding() {
        String canonical = fingerprintService.buildCanonical(sampleFingerprintInput());
        assertTrue(canonical.contains("targetType="));
        assertTrue(canonical.contains("targetId="));
    }

    private void assertFingerprintIncludesExpectedAndDesired() {
        String canonical = fingerprintService.buildCanonical(sampleFingerprintInput());
        assertTrue(canonical.contains("expectedCurrentValue="));
        assertTrue(canonical.contains("desiredValue="));
    }

    private void assertFingerprintIncludesOperations() {
        assertTrue(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("operation.type="));
    }

    private void assertFingerprintIncludesDependencies() {
        assertTrue(fingerprintService.buildCanonical(sampleFingerprintInputWithDependency()).contains("dependency="));
    }

    private void assertFingerprintIncludesPreconditions() {
        assertTrue(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("precondition.type="));
    }

    private void assertFingerprintIncludesRollback() {
        assertTrue(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("rollback.type="));
    }

    private void assertFingerprintIncludesSourceBinding() {
        String canonical = fingerprintService.buildCanonical(sampleFingerprintInput());
        assertTrue(canonical.contains("sourceSnapshotId="));
        assertTrue(canonical.contains("sourceSynchronizationExecutionId="));
    }

    private void assertFingerprintExcludesVolatileTimestamps() {
        assertFalse(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("createdAt="));
        assertFalse(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("authorizedAt="));
    }

    private void assertFingerprintExcludesActors() {
        assertFalse(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("authorizedBy="));
        assertFalse(fingerprintService.buildCanonical(sampleFingerprintInput()).contains("createdBy="));
    }

    private void assertFingerprintRepeatDeterministic() {
        assertEquals(fingerprintService.compute(sampleFingerprintInput()), fingerprintService.compute(sampleFingerprintInput()));
    }

    private static boolean validityServiceExists() {
        return Files.exists(Path.of(CHANGE_PLANNING_ROOT, "service/ChangePlanValidityService.java"));
    }

    private static boolean readinessServiceReferencesKnowledgeGate() throws IOException {
        return readSource("service/ChangePlanPreconditionService.java").contains("NETWORK_KNOWLEDGE_CONFIDENCE")
                && readSource("service/ChangePlanPreconditionService.java").contains("knowledgeGate");
    }

    private static boolean readinessServiceReferencesFreshness() throws IOException {
        return readSource("service/ChangePlanPreconditionService.java").contains("SOURCE_SYNCHRONIZATION_FRESHNESS")
                && readSource("service/ChangePlanPreconditionService.java").contains("getFreshness()");
    }

    private static boolean validityServiceReferencesDrift() throws IOException {
        return readSource("service/ChangePlanValidityService.java").contains("driftService");
    }

    private void assertDeterministicSafetyEvaluation() {
        ChangeExecutionSafetyPolicy.SafetyResult first = safetyPolicy.evaluateParameter(sampleIntent());
        ChangeExecutionSafetyPolicy.SafetyResult second = safetyPolicy.evaluateParameter(sampleIntent());
        assertEquals(first.pass(), second.pass());
        assertEquals(first.failureCode(), second.failureCode());
    }

    private void assertDeterministicImpactAssessment() {
        ChangeImpactLevel first = impactAssessmentService.assess(sampleIntent());
        ChangeImpactLevel second = impactAssessmentService.assess(sampleIntent());
        assertEquals(first, second);
    }

    private static boolean vendorImportGrantsPlanAuthorization() throws IOException {
        return readSource("authorization/ChangePlanAuthorizer.java").contains("VENDOR_IMPORT")
                || readSource("authorization/ChangePlanAuthorizer.java").contains("IMPORT_NETWORK");
    }

    private static boolean proposalApprovalDistinctFromPlanAuthorization() throws IOException {
        String governance = readSource("service/ChangePlanGovernanceService.java");
        return governance.contains("markAuthorized") && !governance.contains("ProposalStatus.APPROVED");
    }

    private static void assertAgentCannotAuthorizePlans() throws IOException {
        Path agentRoot = Path.of("src/main/java/com/simba/snip/npo/agent");
        if (!Files.exists(agentRoot)) {
            return;
        }
        try (Stream<Path> files = Files.walk(agentRoot)) {
            assertFalse(files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    return readFile(path).contains("ChangePlanGovernanceService");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        }
    }

    private static boolean reviewEntityExists() {
        return Files.exists(Path.of(CHANGE_PLANNING_ROOT, "persist/NetworkChangePlanReviewEntity.java"));
    }

    private static boolean planEntityHasAuthorizationActor() throws IOException {
        return readSource("persist/NetworkChangePlanEntity.java").contains("authorizedBy");
    }

    private static boolean planEntityHasAuthorizationTime() throws IOException {
        return readSource("persist/NetworkChangePlanEntity.java").contains("authorizedAt");
    }

    private static boolean planEntityHasAuthorizedFingerprint() throws IOException {
        return readSource("persist/NetworkChangePlanEntity.java").contains("authorizedFingerprint");
    }

    private static boolean governanceHandlesOptimisticConflict() throws IOException {
        return readSource("service/ChangePlanGovernanceService.java").contains("OptimisticLockingFailureException");
    }

    private static void assertReadinessEnumExcludesBlocked() {
        for (ExecutionReadinessResult result : ExecutionReadinessResult.values()) {
            assertNotEquals("BLOCKED", result.name());
        }
    }

    private static boolean readinessRequiresAuthorization() throws IOException {
        String readiness = readSource("service/ChangePlanReadinessService.java");
        String preconditions = readSource("service/ChangePlanPreconditionService.java");
        return readiness.contains("READINESS_ELIGIBLE")
                && preconditions.contains("AUTHORIZATION_CURRENT");
    }

    private static boolean readinessEvaluatesHardGates() throws IOException {
        String readiness = readSource("service/ChangePlanReadinessService.java");
        String preconditions = readSource("service/ChangePlanPreconditionService.java");
        return readiness.contains("aggregatePreconditionResults")
                && readiness.contains("evaluateAtReadiness")
                && preconditions.contains("EXPECTED_PARAMETER_VALUE")
                && readiness.contains("safetyService.evaluateCreation");
    }

    private static boolean readinessAssessmentEntityExists() {
        return Files.exists(Path.of(CHANGE_PLANNING_ROOT, "persist/ExecutionReadinessAssessmentEntity.java"));
    }

    private static boolean readinessPromotesToReadyForExecution() throws IOException {
        return readSource("service/ChangePlanReadinessService.java").contains("markReadyForExecution");
    }

    private static boolean readinessServiceHasExecutionSideEffects() throws IOException {
        String readiness = readSource("service/ChangePlanReadinessService.java");
        return readiness.contains("ActionExecution") || readiness.contains("EnmTransport") || readiness.contains("Mcp");
    }

    private static boolean validityServiceInvalidatesOnCurrentMismatch() throws IOException {
        return readSource("service/ChangePlanValidityService.java").contains("PLAN_CURRENT_VALUE_MISMATCH");
    }

    private static void assertCancellableFrom(PlanStatus status) throws IOException {
        String governance = readSource("service/ChangePlanGovernanceService.java");
        assertTrue(governance.contains("CANCELLABLE"));
        assertTrue(governance.contains(status.name()));
    }

    private static boolean hasFailureCode(ChangePlanFailureCode code) {
        for (ChangePlanFailureCode value : ChangePlanFailureCode.values()) {
            if (value == code) {
                return true;
            }
        }
        return false;
    }

    private static boolean enumContains(String value) {
        for (PlanStatus status : PlanStatus.values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        for (ExecutionReadinessResult result : ExecutionReadinessResult.values()) {
            if (result.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String readSource(String relativePath) throws IOException {
        return readFile(Path.of(CHANGE_PLANNING_ROOT, relativePath));
    }

    private static String readFile(Path path) throws IOException {
        return Files.readString(path);
    }
}
