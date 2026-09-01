package com.simba.snip.npo.action;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Explicit capability registry. Only registered and enabled capabilities may execute.
 */
public final class CapabilityRegistry {

    public record CapabilityDefinition(
            String capabilityId,
            String name,
            String version,
            String description,
            RiskLevel riskLevel,
            String owner,
            boolean enabled,
            boolean requiresApproval,
            boolean dryRunOnly,
            boolean compensationSupported
    ) {
    }

    private static final Map<String, CapabilityDefinition> CAPABILITIES = new LinkedHashMap<>();

    static {
        CAPABILITIES.put(ActionRules.CAPABILITY_REMEDIATION, new CapabilityDefinition(
                ActionRules.CAPABILITY_REMEDIATION,
                "Generate remediation plan",
                "1",
                "Produces a structured remediation artifact from an Assurance Case. Does not mutate the network.",
                RiskLevel.LOW,
                "SNIP_NPO",
                true,
                false,
                false,
                false
        ));
        CAPABILITIES.put(ActionRules.CAPABILITY_SIMULATION, new CapabilityDefinition(
                ActionRules.CAPABILITY_SIMULATION,
                "Simulate cell parameter change",
                "1",
                "Synthetic dry-run against a versioned Digital Twin. Not vendor-calibrated RF physics.",
                RiskLevel.MEDIUM,
                "SNIP_NPO",
                true,
                true,
                true,
                false
        ));
    }

    private CapabilityRegistry() {
    }

    public static Optional<CapabilityDefinition> find(String capabilityId) {
        return Optional.ofNullable(CAPABILITIES.get(capabilityId));
    }

    public static Collection<CapabilityDefinition> all() {
        return CAPABILITIES.values();
    }
}
