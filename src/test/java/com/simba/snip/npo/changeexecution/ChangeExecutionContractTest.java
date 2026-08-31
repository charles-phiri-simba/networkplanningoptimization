package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.changeexecution.api.AuthorizeExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CancelExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CreateExecutionRequest;
import com.simba.snip.npo.changeexecution.api.ReviewExecutionRequest;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.SimulatorFailureMode;
import com.simba.snip.npo.changeexecution.service.ExecutionFailurePersistenceService;
import com.simba.snip.npo.changeexecution.service.NetworkChangeExecutionService;
import com.simba.snip.npo.changeexecution.service.RollbackExecutionService;
import com.simba.snip.npo.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeExecutionContractTest {

    private static final Path ROOT = Path.of("src/main/java/com/simba/snip/npo/changeexecution");

    @Test
    void mandatorySafetyConfigurationFailsClosed() {
        ChangeExecutionProperties defaults = new ChangeExecutionProperties();
        defaults.validate();
        assertFalse(defaults.isEnabled());
        assertEquals(1, defaults.getMaximumOperationCount());
        assertEquals(1, defaults.getMaximumForwardAttempts());
        assertTrue(defaults.isRequireExecutionReview());
        assertTrue(defaults.isRequireExecutionAuthorization());
        assertTrue(defaults.isRequireCurrentValueMatch());
        assertTrue(defaults.isRequireVerification());
        assertTrue(defaults.isRequireRollbackReview());
        assertTrue(defaults.isRequireRollbackAuthorization());
        assertFalse(defaults.isAutomaticRollbackEnabled());

        assertInvalid(p -> p.setMaximumOperationCount(2));
        assertInvalid(p -> p.setMaximumForwardAttempts(2));
        assertInvalid(p -> p.setRequireExecutionReview(false));
        assertInvalid(p -> p.setRequireExecutionAuthorization(false));
        assertInvalid(p -> p.setRequireCurrentValueMatch(false));
        assertInvalid(p -> p.setRequireVerification(false));
        assertInvalid(p -> p.setRequireRollbackReview(false));
        assertInvalid(p -> p.setRequireRollbackAuthorization(false));
        assertInvalid(p -> p.setAutomaticRollbackEnabled(true));
    }

    @Test
    void criticalFailurePersistenceUsesRequiresNew() {
        for (var method : ExecutionFailurePersistenceService.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("persist")) {
                continue;
            }
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertTrue(transactional != null, method.getName());
            assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(), method.getName());
        }
    }

    @Test
    void orchestrationDoesNotHoldOuterTransactionAcrossRequiresNewPersistence() throws Exception {
        assertFalse(NetworkChangeExecutionService.class.getDeclaredMethod("execute", java.util.UUID.class)
                .isAnnotationPresent(Transactional.class));
        assertFalse(NetworkChangeExecutionService.class.getDeclaredMethod("verify", java.util.UUID.class)
                .isAnnotationPresent(Transactional.class));
        assertFalse(RollbackExecutionService.class.getDeclaredMethod("executeRollback", java.util.UUID.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void createDtoExposesReferencesOnlyAndNoMutationSurface() {
        assertEquals(Set.of("planId", "executionTargetId"), components(CreateExecutionRequest.class));
        Set<String> forbidden = Set.of(
                "cellId", "parameter", "expected", "desired", "rollback", "operationType",
                "fingerprint", "endpoint", "protocol", "credential", "vendorCommand", "verificationResult");
        assertTrue(components(CreateExecutionRequest.class).stream().noneMatch(forbidden::contains));
        assertEquals(Set.of("authorizer"), components(AuthorizeExecutionRequest.class));
        assertEquals(Set.of("actor", "reason"), components(CancelExecutionRequest.class));
        assertEquals(Set.of("reviewer", "comment"), components(ReviewExecutionRequest.class));
    }

    @Test
    void lifecycleKeepsMutationAndVerificationStatesDistinct() {
        assertNotEquals(ExecutionStatus.APPLIED, ExecutionStatus.VERIFIED);
        assertNotEquals(ExecutionStatus.ROLLBACK_APPLIED, ExecutionStatus.ROLLED_BACK);
        assertTrue(ExecutionStatus.APPLIED.allowsVerify());
        assertTrue(ExecutionStatus.ROLLBACK_APPLIED.allowsVerify());
        assertFalse(ExecutionStatus.VERIFIED.allowsVerify());
        assertFalse(ExecutionStatus.ROLLED_BACK.allowsVerify());
        assertFalse(ExecutionStatus.MANUAL_INTERVENTION_REQUIRED.isActive());
    }

    @Test
    void stableFailureCodesCoverForwardVerificationLeaseAndRollback() {
        Set<String> codes = Arrays.stream(ExecutionFailureCode.values()).map(Enum::name).collect(Collectors.toSet());
        for (String required : Set.of(
                "EXECUTION_PLAN_NOT_READY", "EXECUTION_AUTHORIZATION_MISSING",
                "EXECUTION_AUTHORIZATION_STALE", "EXECUTION_CURRENT_VALUE_MISMATCH",
                "EXECUTION_KNOWLEDGE_LOW", "EXECUTION_KNOWLEDGE_UNKNOWN",
                "EXECUTION_SYNCHRONIZATION_STALE", "EXECUTION_RELEVANT_DRIFT_PRESENT",
                "EXECUTION_LEASE_UNAVAILABLE", "EXECUTION_FENCING_TOKEN_STALE",
                "EXECUTION_CONFLICT", "EXECUTION_OUTCOME_UNKNOWN",
                "EXECUTION_VERIFICATION_MISMATCH", "EXECUTION_VERIFICATION_TIMEOUT",
                "EXECUTION_VERIFICATION_UNKNOWN", "ROLLBACK_AUTHORIZATION_MISSING",
                "ROLLBACK_AUTHORIZATION_STALE", "ROLLBACK_CURRENT_VALUE_MISMATCH",
                "ROLLBACK_OPERATION_FAILED", "ROLLBACK_OUTCOME_UNKNOWN",
                "ROLLBACK_VERIFICATION_FAILED", "MANUAL_INTERVENTION_REQUIRED")) {
            assertTrue(codes.contains(required), required);
        }
    }

    @Test
    void simulatorFailureModesCoverEverySpecifiedBranch() {
        Set<String> modes = Arrays.stream(SimulatorFailureMode.values()).map(Enum::name).collect(Collectors.toSet());
        assertTrue(modes.containsAll(Set.of(
                "SUCCESS", "REJECT_BEFORE_APPLY", "TIMEOUT_BEFORE_APPLY", "TIMEOUT_AFTER_APPLY",
                "APPLY_WRONG_VALUE", "READBACK_TIMEOUT", "READBACK_STALE",
                "ROLLBACK_FAILURE", "ROLLBACK_TIMEOUT_AFTER_APPLY")));
    }

    @Test
    void leaseIsAcquiredBeforeFinalPreflightAndMutation() throws IOException {
        String source = source("service/NetworkChangeExecutionService.java");
        int acquire = source.indexOf("leaseService.acquire(");
        int preflight = source.indexOf("finalPreflightService.runForwardPreflight(");
        int mutation = source.indexOf("operationExecutionService.executeForward(");
        assertTrue(acquire >= 0 && acquire < preflight && preflight < mutation);
    }

    @Test
    void forwardAndRollbackAttemptLimitsPreventBlindRetry() throws IOException {
        String operation = source("service/ChangeOperationExecutionService.java");
        assertTrue(operation.contains("ensureForwardAttemptAllowed"));
        assertTrue(operation.contains("forwardAttempts >= properties.getMaximumForwardAttempts()"));
        assertTrue(operation.contains("rollbackAttempts >= 1"));
        String orchestration = source("service/NetworkChangeExecutionService.java");
        assertFalse(orchestration.toLowerCase(Locale.ROOT).contains("retry("));
    }

    @Test
    void migrationEnforcesActivePlanScopeAndOptimisticVersion() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V16__phase15_governed_change_execution.sql"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX network_change_execution_active_plan_idx"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX network_change_execution_active_scope_idx"));
        assertTrue(migration.contains("version BIGINT NOT NULL DEFAULT 0"));
        assertTrue(migration.contains("network_change_execution_lease_scope_unique"));
    }

    @Test
    void productionPackageHasNoCanonicalSynchronizationOrAutomaticExecutionPath() throws IOException {
        try (var files = Files.walk(ROOT)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(path);
                assertFalse(text.contains("RadioConfigurationRepository"), path.toString());
                assertFalse(text.contains("SynchronizationControlPlane"), path.toString());
                assertFalse(text.contains("@Scheduled"), path.toString());
                assertFalse(text.contains("EnmTransport"), path.toString());
                assertFalse(text.contains("CredentialHandle"), path.toString());
            }
        }
    }

    @Test
    void priorPhaseRegressionSurfaceAndMigrationsRemainPresent() {
        for (int version = 1; version <= 15; version++) {
            final String prefix = "V" + version + "__";
            try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
                assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith(prefix)), prefix);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        assertTrue(Files.exists(Path.of(
                "src/test/java/com/simba/snip/npo/changeplanning/ChangePlanningApiTest.java")));
        assertTrue(Files.exists(Path.of(
                "src/test/java/com/simba/snip/npo/changeintelligence/ChangeIntelligenceApiTest.java")));
    }

    @Test
    void goSimulatorTestsAndBuildPass() throws Exception {
        Process test = new ProcessBuilder("go", "test", "./...")
                .directory(Path.of("simulator").toFile()).redirectErrorStream(true).start();
        String testOutput = new String(test.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, test.waitFor(), testOutput);
        Process build = new ProcessBuilder("go", "build", "./cmd/simulator")
                .directory(Path.of("simulator").toFile()).redirectErrorStream(true).start();
        String buildOutput = new String(build.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, build.waitFor(), buildOutput);
    }

    @Test
    void catalogContainsNoPlaceholderOrInsufficientEvidence() {
        assertEquals(240, ChangeExecutionMatrixEvidenceCatalog.all().size());
        assertEquals(0, ChangeExecutionMatrixEvidenceCatalog.countByStatus(
                ChangeExecutionMatrixEvidenceCatalog.Status.EVIDENCE_INSUFFICIENT));
        for (var evidence : ChangeExecutionMatrixEvidenceCatalog.all().values()) {
            String requirement = evidence.requirement().toLowerCase(Locale.ROOT);
            assertFalse(requirement.matches("domain .* requirement \\d+"), evidence.requirement());
            assertEquals(ChangeExecutionMatrixEvidenceCatalog.Status.VERIFIED_PASS, evidence.status());
        }
    }

    private static Set<String> components(Class<?> record) {
        return Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName).collect(Collectors.toSet());
    }

    private static void assertInvalid(java.util.function.Consumer<ChangeExecutionProperties> mutation) {
        ChangeExecutionProperties properties = new ChangeExecutionProperties();
        mutation.accept(properties);
        assertThrows(DomainValidationException.class, properties::validate);
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
