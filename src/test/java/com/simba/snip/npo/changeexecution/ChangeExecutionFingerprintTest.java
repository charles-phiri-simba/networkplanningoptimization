package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetCapability;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetEnvironment;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetType;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.service.ExecutionFingerprintService;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChangeExecutionFingerprintTest {

    private final ChangeExecutionProperties properties = new ChangeExecutionProperties();
    private final ExecutionFingerprintService service = new ExecutionFingerprintService(properties);

    @Test
    void fingerprintIsDeterministicUppercaseSha256() {
        var input = input("target-a", "46.0", "44.00", null, null);
        String first = service.compute(input);
        assertEquals(first, service.compute(input));
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9A-F]{64}"));
    }

    @Test
    void fingerprintNormalizesCanonicalNumericValues() {
        assertEquals(
                service.compute(input("target-a", "46.0", "44.00", null, null)),
                service.compute(input("target-a", "46.000", "44", null, null)));
    }

    @Test
    void fingerprintIncludesPlanTargetOperationRollbackAndWindowBindings() {
        var base = input("target-a", "46", "44", Instant.parse("2026-08-31T10:00:00Z"),
                Instant.parse("2026-08-31T11:00:00Z"));
        String fingerprint = service.compute(base);
        assertNotEquals(fingerprint, service.compute(input("target-b", "46", "44",
                base.executionWindowOpensAt(), base.executionWindowClosesAt())));
        assertNotEquals(fingerprint, service.compute(input("target-a", "46", "43",
                base.executionWindowOpensAt(), base.executionWindowClosesAt())));
        assertNotEquals(fingerprint, service.compute(input("target-a", "46", "44",
                base.executionWindowOpensAt(), base.executionWindowClosesAt().plusSeconds(1))));
    }

    @Test
    void fingerprintExcludesExecutionIdentityActorsAndDynamicEvidence() {
        var input = input("target-a", "46", "44", null, null);
        assertEquals(service.compute(input), service.compute(input));
    }

    @Test
    void policyChangesAlterExecutionFingerprint() {
        var input = input("target-a", "46", "44", null, null);
        String before = service.compute(input);
        properties.setRequireVerification(false);
        assertNotEquals(before, service.compute(input));
    }

    @Test
    void rollbackFingerprintBindsExecutionIdentityAndCompleteRollbackOperation() {
        NetworkChangePlanEntity plan = plan();
        ExecutionTargetDescriptor target = target("target-a");
        NetworkChangePlanRollbackOperationEntity rollback = rollback("44", "46", "txPower");
        UUID firstExecution = UUID.randomUUID();
        assertNotEquals(
                service.computeRollbackFingerprint(firstExecution, plan, target, rollback),
                service.computeRollbackFingerprint(UUID.randomUUID(), plan, target, rollback));
        assertNotEquals(
                service.computeRollbackFingerprint(firstExecution, plan, target, rollback),
                service.computeRollbackFingerprint(firstExecution, plan, target,
                        rollback("44", "45", "txPower")));
    }

    private ExecutionFingerprintService.FingerprintInput input(
            String targetId,
            String expected,
            String desired,
            Instant opens,
            Instant closes
    ) {
        NetworkChangeExecutionOperationEntity operation = NetworkChangeExecutionOperationEntity.create(
                UUID.randomUUID(), UUID.randomUUID(), 1, "SET_PARAMETER", "CELL", "CELL-001",
                "txPower", expected, desired, Instant.EPOCH);
        return new ExecutionFingerprintService.FingerprintInput(
                plan(), target(targetId), List.of(operation), rollback(desired, expected, "txPower"), opens, closes);
    }

    private NetworkChangePlanEntity plan() {
        NetworkChangePlanEntity plan = mock(NetworkChangePlanEntity.class);
        when(plan.getFingerprint()).thenReturn("A".repeat(64));
        when(plan.getPlanVersion()).thenReturn(3);
        return plan;
    }

    private NetworkChangePlanRollbackOperationEntity rollback(String expected, String desired, String parameter) {
        NetworkChangePlanRollbackOperationEntity rollback = mock(NetworkChangePlanRollbackOperationEntity.class);
        when(rollback.getSequenceNumber()).thenReturn(1);
        when(rollback.getOperationType()).thenReturn("SET_PARAMETER");
        when(rollback.getTargetEntityType()).thenReturn("CELL");
        when(rollback.getTargetEntityId()).thenReturn("CELL-001");
        when(rollback.getParameterName()).thenReturn(parameter);
        when(rollback.getExpectedCurrentValue()).thenReturn(expected);
        when(rollback.getDesiredValue()).thenReturn(desired);
        return rollback;
    }

    private ExecutionTargetDescriptor target(String targetId) {
        return new ExecutionTargetDescriptor(
                targetId, ExecutionTargetType.SIMULATOR, ExecutionTargetEnvironment.SIMULATOR,
                "adapter-v1", "capability-v1", EnumSet.allOf(ExecutionTargetCapability.class));
    }
}
