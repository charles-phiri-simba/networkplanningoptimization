package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionStateStore;
import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.changeplanning.service.ChangePlanValidityService;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutionFinalPreflightService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final ChangePlanValidityService planValidityService;
    private final ExecutionAuthorizationService authorizationService;
    private final ExecutionFingerprintService fingerprintService;
    private final ExecutionTargetRegistry targetRegistry;
    private final SimulatorExecutionStateStore simulatorStateStore;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public ExecutionFinalPreflightService(
            ChangeExecutionProperties properties,
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            ChangePlanValidityService planValidityService,
            ExecutionAuthorizationService authorizationService,
            ExecutionFingerprintService fingerprintService,
            ExecutionTargetRegistry targetRegistry,
            SimulatorExecutionStateStore simulatorStateStore,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.planRepository = planRepository;
        this.rollbackRepository = rollbackRepository;
        this.operationRepository = operationRepository;
        this.planValidityService = planValidityService;
        this.authorizationService = authorizationService;
        this.fingerprintService = fingerprintService;
        this.targetRegistry = targetRegistry;
        this.simulatorStateStore = simulatorStateStore;
        this.auditService = auditService;
        this.clock = clock;
    }

    public void runForwardPreflight(NetworkChangeExecutionEntity execution) {
        Instant now = clock.instant();
        auditService.append(execution.getId(), ExecutionAuditEventType.FINAL_PREFLIGHT_STARTED.name(), "system", null);
        authorizationService.requireCurrentAuthorization(execution);
        if (execution.getExecutionWindowOpensAt() != null && now.isBefore(execution.getExecutionWindowOpensAt())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_WINDOW_CLOSED, "execution window not open");
        }
        if (execution.getExecutionWindowClosesAt() != null && now.isAfter(execution.getExecutionWindowClosesAt())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_WINDOW_CLOSED, "execution window closed");
        }
        NetworkChangePlanEntity plan = planRepository.findById(execution.getPlanId())
                .orElseThrow(() -> new DomainNotFoundException("changePlan", execution.getPlanId().toString()));
        if (plan.getPlanVersion() != execution.getPlanVersion()
                || !plan.getFingerprint().equals(execution.getPlanFingerprint())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE, "plan binding changed");
        }
        ChangePlanValidityService.ValidityResult validity = planValidityService.revalidate(plan);
        if (!validity.valid()) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_PLAN_NOT_READY,
                    validity.reason()
            );
        }
        ExecutionTargetDescriptor target = targetRegistry.require(execution.getExecutionTargetId());
        targetRegistry.requirePermitted(target, properties);
        if (!target.targetId().equals(execution.getExecutionTargetId())
                || !target.targetType().name().equals(execution.getExecutionTargetType())
                || !target.environment().name().equals(execution.getExecutionTargetEnvironment())
                || !target.adapterProfileId().equals(execution.getAdapterProfileId())
                || !target.capabilityProfileVersion().equals(execution.getCapabilityProfileVersion())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE, "target binding changed");
        }
        List<NetworkChangeExecutionOperationEntity> operations =
                operationRepository.findByExecutionIdOrderBySequenceNumberAsc(execution.getId());
        if (operations.size() != 1) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING, "single operation");
        }
        var rollback = rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(execution.getPlanId())
                .stream().findFirst().orElse(null);
        String currentFingerprint = fingerprintService.compute(
                new ExecutionFingerprintService.FingerprintInput(
                        plan,
                        target,
                        operations,
                        rollback,
                        execution.getExecutionWindowOpensAt(),
                        execution.getExecutionWindowClosesAt()
                ));
        if (!currentFingerprint.equals(execution.getAuthorizedExecutionFingerprint())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE, "execution binding changed");
        }
        NetworkChangeExecutionOperationEntity operation = operations.get(0);
        if (properties.isRequireCurrentValueMatch()) {
            assertTargetCurrentMatches(execution, operation);
        }
        auditService.append(execution.getId(), ExecutionAuditEventType.FINAL_PREFLIGHT_PASSED.name(), "system", null);
    }

    private void assertTargetCurrentMatches(NetworkChangeExecutionEntity execution, NetworkChangeExecutionOperationEntity operation) {
        simulatorStateStore.initializeIfAbsent(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                operation.getParameterName(),
                operation.getExpectedCurrentValue()
        );
        String actual = simulatorStateStore.read(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                operation.getParameterName()
        ).map(SimulatorExecutionStateStore.CellState::value).orElse(null);
        if (actual == null || !valuesEqual(actual, operation.getExpectedCurrentValue())) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_CURRENT_VALUE_MISMATCH,
                    actual == null ? "unknown" : actual
            );
        }
    }

    private boolean valuesEqual(String left, String right) {
        try {
            return new BigDecimal(left.strip()).compareTo(new BigDecimal(right.strip())) == 0;
        } catch (NumberFormatException ex) {
            return left.strip().equals(right.strip());
        }
    }
}
