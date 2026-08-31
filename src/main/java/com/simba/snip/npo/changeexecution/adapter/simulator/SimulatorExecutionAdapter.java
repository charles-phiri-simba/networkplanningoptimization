package com.simba.snip.npo.changeexecution.adapter.simulator;

import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedExecutionOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedRollbackOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionContext;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionMutationAdapter;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionObservationAdapter;
import com.simba.snip.npo.changeexecution.adapter.spi.MutationResult;
import com.simba.snip.npo.changeexecution.adapter.spi.ObservationResult;
import com.simba.snip.npo.changeexecution.domain.SimulatorFailureMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SimulatorExecutionAdapter implements ExecutionMutationAdapter, ExecutionObservationAdapter {

    public static final String TARGET_ID = "snip-simulator";

    private final SimulatorExecutionStateStore stateStore;
    private final Clock clock;
    private final AtomicReference<SimulatorFailureMode> failureMode = new AtomicReference<>(SimulatorFailureMode.SUCCESS);

    public SimulatorExecutionAdapter(SimulatorExecutionStateStore stateStore, Clock clock) {
        this.stateStore = stateStore;
        this.clock = clock;
    }

    public void setFailureMode(SimulatorFailureMode mode) {
        failureMode.set(mode == null ? SimulatorFailureMode.SUCCESS : mode);
    }

    public void clearFailureMode() {
        failureMode.set(SimulatorFailureMode.SUCCESS);
    }

    @Override
    public String targetId() {
        return TARGET_ID;
    }

    @Override
    public MutationResult execute(AuthorizedExecutionOperation operation, ExecutionContext context) {
        SimulatorFailureMode mode = failureMode.get();
        if (mode == SimulatorFailureMode.REJECT_BEFORE_APPLY) {
            return MutationResult.rejected("EXECUTION_OPERATION_REJECTED", "simulator rejected before apply");
        }
        if (mode == SimulatorFailureMode.TIMEOUT_BEFORE_APPLY) {
            return MutationResult.timeout("EXECUTION_OPERATION_TIMEOUT", "simulator timeout before apply", null, null);
        }
        stateStore.initializeIfAbsent(
                context.targetId(),
                context.cellId(),
                operation.parameterName(),
                operation.expectedCurrentValue()
        );
        var before = stateStore.read(context.targetId(), context.cellId(), operation.parameterName()).orElse(null);
        Long revisionBefore = before == null ? null : before.revision();
        var applied = stateStore.applyIfCurrentMatches(
                context.targetId(),
                context.cellId(),
                operation.parameterName(),
                operation.expectedCurrentValue(),
                resolveAppliedValue(operation.desiredValue(), mode)
        );
        if (applied.isEmpty()) {
            return MutationResult.rejected("EXECUTION_CURRENT_VALUE_MISMATCH", "simulator expected-state guard rejected mutation");
        }
        long revisionAfter = applied.get().revision() + 1;
        if (mode == SimulatorFailureMode.TIMEOUT_AFTER_APPLY) {
            return MutationResult.outcomeUnknown(
                    "EXECUTION_OUTCOME_UNKNOWN",
                    "simulator timeout after apply",
                    revisionBefore,
                    revisionAfter
            );
        }
        return MutationResult.applied(revisionBefore, revisionAfter, resolveAppliedValue(operation.desiredValue(), mode));
    }

    @Override
    public MutationResult rollback(AuthorizedRollbackOperation operation, ExecutionContext context) {
        SimulatorFailureMode mode = failureMode.get();
        if (mode == SimulatorFailureMode.ROLLBACK_FAILURE) {
            return MutationResult.rejected("ROLLBACK_OPERATION_FAILED", "simulator rollback rejected");
        }
        var before = stateStore.read(context.targetId(), context.cellId(), operation.parameterName()).orElse(null);
        Long revisionBefore = before == null ? null : before.revision();
        var applied = stateStore.applyIfCurrentMatches(
                context.targetId(),
                context.cellId(),
                operation.parameterName(),
                operation.expectedCurrentValue(),
                operation.desiredValue()
        );
        if (applied.isEmpty()) {
            return MutationResult.rejected("ROLLBACK_CURRENT_VALUE_MISMATCH", "rollback expected-state mismatch");
        }
        long revisionAfter = applied.get().revision() + 1;
        if (mode == SimulatorFailureMode.ROLLBACK_TIMEOUT_AFTER_APPLY) {
            return MutationResult.outcomeUnknown(
                    "ROLLBACK_OUTCOME_UNKNOWN",
                    "simulator rollback timeout after apply",
                    revisionBefore,
                    revisionAfter
            );
        }
        return MutationResult.applied(revisionBefore, revisionAfter, operation.desiredValue());
    }

    @Override
    public ObservationResult observeCurrentValue(ExecutionContext context, String parameterName, Long minimumRevision) {
        SimulatorFailureMode mode = failureMode.get();
        if (mode == SimulatorFailureMode.READBACK_TIMEOUT) {
            return ObservationResult.timeout("EXECUTION_VERIFICATION_TIMEOUT");
        }
        return stateStore.read(context.targetId(), context.cellId(), parameterName)
                .map(state -> {
                    if (minimumRevision != null && state.revision() < minimumRevision) {
                        return ObservationResult.stale(state.value(), state.revision(), state.updatedAt());
                    }
                    if (mode == SimulatorFailureMode.READBACK_STALE && minimumRevision != null) {
                        return ObservationResult.stale(state.value(), state.revision(), state.updatedAt());
                    }
                    return new ObservationResult(
                            com.simba.snip.npo.changeexecution.domain.VerificationOutcome.VERIFIED,
                            state.value(),
                            state.revision(),
                            state.updatedAt(),
                            null,
                            "simulator readback"
                    );
                })
                .orElseGet(() -> ObservationResult.unknown("EXECUTION_VERIFICATION_UNKNOWN", "simulator state unavailable"));
    }

    @Override
    public ObservationResult verifyForward(
            AuthorizedExecutionOperation operation,
            ExecutionContext context,
            Long minimumRevision
    ) {
        ObservationResult observation = observeCurrentValue(context, operation.parameterName(), minimumRevision);
        if (observation.outcome() != com.simba.snip.npo.changeexecution.domain.VerificationOutcome.VERIFIED) {
            return observation;
        }
        if (valuesEqual(observation.observedValue(), operation.desiredValue())) {
            return ObservationResult.verified(observation.observedValue(), observation.targetRevision(), Instant.now());
        }
        return ObservationResult.mismatch(observation.observedValue(), observation.targetRevision(), Instant.now());
    }

    @Override
    public ObservationResult verifyRollback(
            AuthorizedRollbackOperation operation,
            ExecutionContext context,
            Long minimumRevision
    ) {
        ObservationResult observation = observeCurrentValue(context, operation.parameterName(), minimumRevision);
        if (observation.outcome() != com.simba.snip.npo.changeexecution.domain.VerificationOutcome.VERIFIED) {
            return observation;
        }
        if (valuesEqual(observation.observedValue(), operation.desiredValue())) {
            return ObservationResult.verified(observation.observedValue(), observation.targetRevision(), Instant.now());
        }
        return ObservationResult.mismatch(observation.observedValue(), observation.targetRevision(), Instant.now());
    }

    private String resolveAppliedValue(String desiredValue, SimulatorFailureMode mode) {
        if (mode == SimulatorFailureMode.APPLY_WRONG_VALUE) {
            try {
                BigDecimal wrong = new BigDecimal(desiredValue).add(BigDecimal.ONE);
                return wrong.stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                return desiredValue + "-wrong";
            }
        }
        return desiredValue;
    }

    private boolean valuesEqual(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        try {
            return new BigDecimal(left.strip()).compareTo(new BigDecimal(right.strip())) == 0;
        } catch (NumberFormatException ex) {
            return left.strip().equals(right.strip());
        }
    }
}
