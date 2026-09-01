package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionMutationAdapter;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionObservationAdapter;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionAdapter;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetCapability;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetEnvironment;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetType;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ExecutionTargetRegistry {

    public static final String SIMULATOR_TARGET_ID = SimulatorExecutionAdapter.TARGET_ID;

    private final Map<String, ExecutionTargetDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, ExecutionMutationAdapter> mutationAdapters = new LinkedHashMap<>();
    private final Map<String, ExecutionObservationAdapter> observationAdapters = new LinkedHashMap<>();

    public ExecutionTargetRegistry(
            ChangeExecutionProperties properties,
            SimulatorExecutionAdapter simulatorAdapter
    ) {
        registerSimulator(simulatorAdapter);
        if (properties.getPermittedTargetTypes().contains(ExecutionTargetType.CONTROLLED_SANDBOX)) {
            // Explicit opt-in only; no default sandbox targets are registered.
        }
    }

    private void registerSimulator(SimulatorExecutionAdapter simulatorAdapter) {
        ExecutionTargetDescriptor descriptor = new ExecutionTargetDescriptor(
                SIMULATOR_TARGET_ID,
                ExecutionTargetType.SIMULATOR,
                ExecutionTargetEnvironment.SIMULATOR,
                "simulator-execution-v1",
                "1",
                EnumSet.of(
                        ExecutionTargetCapability.PARAMETER_WRITE,
                        ExecutionTargetCapability.PARAMETER_READBACK,
                        ExecutionTargetCapability.ROLLBACK,
                        ExecutionTargetCapability.EXPECTED_STATE_GUARD,
                        ExecutionTargetCapability.IDEMPOTENT_OPERATION
                )
        );
        descriptors.put(SIMULATOR_TARGET_ID, descriptor);
        mutationAdapters.put(SIMULATOR_TARGET_ID, simulatorAdapter);
        observationAdapters.put(SIMULATOR_TARGET_ID, simulatorAdapter);
    }

    public Optional<ExecutionTargetDescriptor> find(String targetId) {
        return Optional.ofNullable(descriptors.get(targetId));
    }

    public ExecutionTargetDescriptor require(String targetId) {
        return find(targetId).orElseThrow(() -> new ChangeExecutionException(
                ExecutionFailureCode.EXECUTION_TARGET_NOT_FOUND,
                targetId
        ));
    }

    public void requirePermitted(ExecutionTargetDescriptor descriptor, ChangeExecutionProperties properties) {
        if (!properties.getPermittedTargetTypes().contains(descriptor.targetType())) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_TARGET_NOT_ALLOWED,
                    descriptor.targetType().name()
            );
        }
        if (descriptor.targetType() == ExecutionTargetType.CONTROLLED_SANDBOX
                && descriptor.environment() != ExecutionTargetEnvironment.NON_PRODUCTION) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_TARGET_ENVIRONMENT_PROHIBITED,
                    descriptor.environment().name()
            );
        }
        requireCapabilities(descriptor, Set.of(
                ExecutionTargetCapability.PARAMETER_WRITE,
                ExecutionTargetCapability.PARAMETER_READBACK,
                ExecutionTargetCapability.ROLLBACK,
                ExecutionTargetCapability.EXPECTED_STATE_GUARD
        ));
    }

    public ExecutionMutationAdapter requireMutationAdapter(String targetId) {
        ExecutionMutationAdapter adapter = mutationAdapters.get(targetId);
        if (adapter == null) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_TARGET_NOT_FOUND,
                    targetId
            );
        }
        return adapter;
    }

    public ExecutionObservationAdapter requireObservationAdapter(String targetId) {
        ExecutionObservationAdapter adapter = observationAdapters.get(targetId);
        if (adapter == null) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_TARGET_NOT_FOUND,
                    targetId
            );
        }
        return adapter;
    }

    private void requireCapabilities(ExecutionTargetDescriptor descriptor, Set<ExecutionTargetCapability> required) {
        for (ExecutionTargetCapability capability : required) {
            if (!descriptor.supports(capability)) {
                throw new ChangeExecutionException(
                        ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING,
                        capability.name()
                );
            }
        }
    }
}
