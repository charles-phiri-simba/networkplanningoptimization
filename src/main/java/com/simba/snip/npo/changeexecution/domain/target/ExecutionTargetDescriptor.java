package com.simba.snip.npo.changeexecution.domain.target;

import com.simba.snip.npo.changeexecution.domain.ExecutionTargetCapability;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetEnvironment;
import com.simba.snip.npo.changeexecution.domain.ExecutionTargetType;

import java.util.EnumSet;
import java.util.Set;

public record ExecutionTargetDescriptor(
        String targetId,
        ExecutionTargetType targetType,
        ExecutionTargetEnvironment environment,
        String adapterProfileId,
        String capabilityProfileVersion,
        Set<ExecutionTargetCapability> capabilities
) {
    public ExecutionTargetDescriptor {
        capabilities = capabilities == null ? EnumSet.noneOf(ExecutionTargetCapability.class) : Set.copyOf(capabilities);
    }

    public boolean supports(ExecutionTargetCapability capability) {
        return capabilities.contains(capability);
    }
}
