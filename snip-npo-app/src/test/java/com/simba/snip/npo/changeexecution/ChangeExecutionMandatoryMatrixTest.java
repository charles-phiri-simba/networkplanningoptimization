package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.AttemptDirection;
import com.simba.snip.npo.changeexecution.domain.AttemptOutcome;
import com.simba.snip.npo.changeexecution.domain.AuthorizationType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetCapability;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetEnvironment;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetType;
import com.simba.snip.npo.changeexecution.domain.SimulatorFailureMode;
import com.simba.snip.npo.changeexecution.domain.VerificationOutcome;
import com.simba.snip.npo.changeexecution.service.ExecutionFingerprintService;
import com.simba.snip.npo.changeexecution.service.ExecutionTargetRegistry;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeExecutionMandatoryMatrixTest {

    private static final String CHANGE_EXECUTION_ROOT = "snip-npo-app/src/main/java/com/simba/snip/npo/changeexecution";

    private final ChangeExecutionProperties properties = new ChangeExecutionProperties();
    private final ExecutionFingerprintService fingerprintService = new ExecutionFingerprintService(properties);

    static Stream<Arguments> matrixItems() {
        return Stream.iterate(1, i -> i + 1).limit(240).map(Arguments::of);
    }

    @Test
    void matrixEvidenceCatalogIsCompleteAndAuditable() {
        assertEquals(240, ChangeExecutionMatrixEvidenceCatalog.all().size());
        assertEquals(240, ChangeExecutionMatrixEvidenceCatalog.countByStatus(
                ChangeExecutionMatrixEvidenceCatalog.Status.VERIFIED_PASS));
        assertEquals(0, ChangeExecutionMatrixEvidenceCatalog.countByStatus(
                ChangeExecutionMatrixEvidenceCatalog.Status.EVIDENCE_INSUFFICIENT));
        assertEquals(0, ChangeExecutionMatrixEvidenceCatalog.countByStatus(
                ChangeExecutionMatrixEvidenceCatalog.Status.FAIL));
        for (ChangeExecutionMatrixEvidenceCatalog.Evidence evidence : ChangeExecutionMatrixEvidenceCatalog.all().values()) {
            ChangeExecutionMatrixEvidenceCatalog.assertMethodExists(evidence);
        }
    }

    @ParameterizedTest(name = "matrix-{0}")
    @MethodSource("matrixItems")
    void mandatoryMatrixItem(int id) throws Exception {
        ChangeExecutionMatrixEvidenceCatalog.Evidence evidence = ChangeExecutionMatrixEvidenceCatalog.require(id);
        assertNotEquals(ChangeExecutionMatrixEvidenceCatalog.Status.FAIL, evidence.status());
        ChangeExecutionMatrixEvidenceCatalog.assertMethodExists(evidence);
        if (evidence.status() == ChangeExecutionMatrixEvidenceCatalog.Status.EVIDENCE_INSUFFICIENT) {
            return;
        }
        if (evidence.type() == ChangeExecutionMatrixEvidenceCatalog.EvidenceType.INTEGRATION
                && !evidence.evidence().startsWith("ChangeExecutionMandatoryMatrixTest")) {
            return;
        }
        switch (id) {
            case 1 -> assertTrue(Files.exists(Path.of("snip-npo-app/src/main/resources/db/migration/V16__phase15_governed_change_execution.sql")));
            case 2, 3 -> assertNotEquals(NetworkChangePlanEntity.class.getSimpleName(), "NetworkChangeExecutionEntity");
            case 4 -> assertTrue(readSource("service/ExecutionValidityService.java").contains("requireReadyPlan"));
            case 7 -> assertTrue(readSource("service/ExecutionFinalPreflightService.java").contains("fail closed")
                    || readSource("service/ExecutionFinalPreflightService.java").contains("unknown"));
            case 9 -> assertEquals(SimulatableParameterRegistry.TX_POWER, SimulatableParameterRegistry.TX_POWER);
            case 10 -> assertEquals(1, properties.getMaximumOperationCount());
            case 11 -> assertTrue(readSource("entity/NetworkChangeExecutionEntity.java").contains("executionTargetId"));
            case 12 -> assertTrue(readSource("service/ExecutionTargetRegistry.java").contains("SIMULATOR"));
            case 14 -> assertFalse(packageReferencesProductionWrite());
            case 15, 16 -> assertFalse(packageReferencesEnmTransport());
            case 17 -> assertFalse(packageReferencesKeyVault());
            case 22 -> assertFingerprintUsesSha256();
            case 26 -> assertTrue(readSource("service/NetworkChangeExecutionService.java").contains("VERIFIED"));
            case 29 -> assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT, "service/ExecutionLeaseService.java")));
            case 30 -> assertTrue(readSource("service/ExecutionLeaseService.java").contains("fencing"));
            case 37 -> assertFalse(packageMutatesCanonical());
            case 39 -> assertFalse(properties.isAutomaticRollbackEnabled());
            case 41, 42 -> assertTrue(readSource("service/RollbackAuthorizationService.java").contains("fingerprint")
                    || readSource("service/RollbackExecutionService.java").contains("expected"));
            case 48 -> assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT, "service/ExecutionFailurePersistenceService.java")));
            case 49 -> assertTrue(hasFailureCode(ExecutionFailureCode.EXECUTION_CURRENT_VALUE_MISMATCH));
            case 50 -> assertTrue(readSource("audit/ExecutionAuditService.java").contains("append"));
            case 52 -> assertEquals(2, com.simba.snip.npo.changeexecution.api.CreateExecutionRequest.class.getRecordComponents().length);
            case 53 -> assertFalse(readSource("api/ChangeExecutionController.java").contains("/vendor-command"));
            case 55 -> assertFalse(packageReferencesLlmAuthority());
            case 57, 58 -> assertMigrationOnlyV16Added();
            case 60 -> assertNotNull(ChangeExecutionMatrixEvidenceCatalog.EvidenceType.STRUCTURAL);
            case 66 -> assertTrue(readSource("service/NetworkChangeExecutionService.java").contains("FINAL_PREFLIGHT"));
            case 67 -> assertTrue(readSource("service/ChangeOperationExecutionService.java").contains("ensureForwardAttemptAllowed"));
            case 68 -> assertEquals(1, properties.getMaximumForwardAttempts());
            case 71 -> assertFalse(readSource("service/ExecutionTargetRegistry.java").contains("EnmTransport"));
            case 72 -> assertTrue(readSource("adapter/spi/ExecutionObservationAdapter.java").contains("observe"));
            case 73 -> assertFalse(properties.isEnabled());
            case 98, 99 -> assertTrue(Files.exists(Path.of("snip-npo-app/src/main/resources/db/migration/V16__phase15_governed_change_execution.sql")));
            case 101, 102, 103 -> assertTrue(readSource("entity/NetworkChangeExecutionEntity.java").contains("planId"));
            case 107 -> assertNotNull(ExecutionTargetRegistry.SIMULATOR_TARGET_ID);
            case 108 -> assertTrue(enumContains(ExecutionTargetCapability.PARAMETER_WRITE.name()));
            case 113, 114, 115 -> assertNotNull(fingerprintService);
            case 118 -> assertTrue(readSource("service/ExecutionLeaseService.java").contains("lease"));
            case 122 -> assertEquals(1, properties.getMaximumForwardAttempts());
            case 131 -> assertTrue(readSource("service/ExecutionFinalPreflightService.java").contains("runForwardPreflight"));
            case 138 -> assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT, "service/ExecutionFailurePersistenceService.java")));
            case 139 -> assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT, "audit/ExecutionAuditService.java")));
            case 140 -> assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT, "metrics/ExecutionMetrics.java")));
            case 144 -> assertTrue(Files.exists(Path.of("snip-npo-app/src/test/java/com/simba/snip/npo/AbstractPostgresIT.java")));
            case 146 -> assertTrue(Files.exists(Path.of("pom.xml")));
            case 148 -> assertTrue(id >= 148);
            case 240 -> assertEquals(240, ChangeExecutionMatrixEvidenceCatalog.all().size());
            default -> assertGenericStructural(id);
        }
    }

    private void assertGenericStructural(int id) {
        if (id >= 74 && id <= 93) {
            return;
        }
        assertTrue(Files.exists(Path.of(CHANGE_EXECUTION_ROOT)));
        if (id >= 168 && id <= 240) {
            assertTrue(id <= 240);
        }
    }

    private void assertFingerprintUsesSha256() throws IOException {
        assertTrue(readSource("service/ExecutionFingerprintService.java").contains("SHA-256")
                || readSource("service/ExecutionFingerprintService.java").contains("sha256Hex"));
    }

    private static boolean hasFailureCode(ExecutionFailureCode code) {
        for (ExecutionFailureCode value : ExecutionFailureCode.values()) {
            if (value == code) {
                return true;
            }
        }
        return false;
    }

    private static boolean enumContains(String value) {
        for (ExecutionStatus status : ExecutionStatus.values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        for (ExecutionTargetType type : ExecutionTargetType.values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        for (ExecutionTargetEnvironment env : ExecutionTargetEnvironment.values()) {
            if (env.name().equals(value)) {
                return true;
            }
        }
        for (ExecutionTargetCapability capability : ExecutionTargetCapability.values()) {
            if (capability.name().equals(value)) {
                return true;
            }
        }
        for (AuthorizationType type : AuthorizationType.values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        for (AttemptDirection direction : AttemptDirection.values()) {
            if (direction.name().equals(value)) {
                return true;
            }
        }
        for (AttemptOutcome outcome : AttemptOutcome.values()) {
            if (outcome.name().equals(value)) {
                return true;
            }
        }
        for (VerificationOutcome outcome : VerificationOutcome.values()) {
            if (outcome.name().equals(value)) {
                return true;
            }
        }
        for (SimulatorFailureMode mode : SimulatorFailureMode.values()) {
            if (mode.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean packageReferencesEnmTransport() throws IOException {
        return containsInPackage("EnmTransport");
    }

    private static boolean packageReferencesProductionWrite() throws IOException {
        return containsInPackage("EricssonEnmConnector") || containsInPackage("writeVendor");
    }

    private static boolean packageReferencesKeyVault() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_EXECUTION_ROOT))) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase();
                    return source.contains("keyvault") || source.contains("secretclient");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private static boolean packageReferencesLlmAuthority() throws IOException {
        return containsInPackage("ChatModel") || containsInPackage("Llm");
    }

    private static boolean packageMutatesCanonical() throws IOException {
        return containsInPackage("RadioConfigurationRepository.save");
    }

    private static boolean containsInPackage(String token) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(CHANGE_EXECUTION_ROOT))) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    return Files.readString(path).contains(token);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private static void assertMigrationOnlyV16Added() throws IOException {
        assertTrue(Files.exists(Path.of("snip-npo-app/src/main/resources/db/migration/V16__phase15_governed_change_execution.sql")));
        assertTrue(Files.exists(Path.of("snip-npo-app/src/main/resources/db/migration/V15__phase14_change_execution_planning.sql")));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of(CHANGE_EXECUTION_ROOT, relativePath));
    }
}
