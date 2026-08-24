package com.simba.snip.npo.action;

import com.simba.snip.npo.domain.DomainValidationException;

public record ActionSemantics(ActionType actionType, RiskLevel riskLevel, PolicyOutcome policy, String capabilityId) {

    public static ActionSemantics of(ActionType actionType) {
        return switch (actionType) {
            case GENERATE_REMEDIATION_PLAN -> new ActionSemantics(
                    actionType, RiskLevel.LOW, PolicyOutcome.ALLOW, ActionRules.CAPABILITY_REMEDIATION);
            case SIMULATE_CELL_PARAMETER_CHANGE -> new ActionSemantics(
                    actionType, RiskLevel.MEDIUM, PolicyOutcome.REQUIRE_APPROVAL, ActionRules.CAPABILITY_SIMULATION);
            case APPLY_CELL_PARAMETER_CHANGE -> new ActionSemantics(
                    actionType, RiskLevel.HIGH, PolicyOutcome.DENY, null);
        };
    }

    public static ActionType requireType(String raw) {
        try {
            return ActionType.valueOf(raw);
        } catch (RuntimeException ex) {
            throw new DomainValidationException("unsupported actionType: " + raw);
        }
    }
}
