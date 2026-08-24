package com.simba.snip.npo.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionPolicyEvaluatorTest {

    private final ActionPolicyEvaluator evaluator = new ActionPolicyEvaluator();

    @Test
    void remediationIsLowAllow() {
        assertEquals(RiskLevel.LOW, ActionSemantics.of(ActionType.GENERATE_REMEDIATION_PLAN).riskLevel());
        assertEquals(PolicyOutcome.ALLOW, evaluator.evaluate(ActionType.GENERATE_REMEDIATION_PLAN).decision());
        assertEquals(ActionRules.CAPABILITY_REMEDIATION, ActionSemantics.of(ActionType.GENERATE_REMEDIATION_PLAN).capabilityId());
    }

    @Test
    void simulationIsMediumRequireApproval() {
        assertEquals(RiskLevel.MEDIUM, ActionSemantics.of(ActionType.SIMULATE_CELL_PARAMETER_CHANGE).riskLevel());
        assertEquals(PolicyOutcome.REQUIRE_APPROVAL, evaluator.evaluate(ActionType.SIMULATE_CELL_PARAMETER_CHANGE).decision());
    }

    @Test
    void applyIsHighDenyWithNoCapability() {
        assertEquals(RiskLevel.HIGH, ActionSemantics.of(ActionType.APPLY_CELL_PARAMETER_CHANGE).riskLevel());
        assertEquals(PolicyOutcome.DENY, evaluator.evaluate(ActionType.APPLY_CELL_PARAMETER_CHANGE).decision());
        assertNull(ActionSemantics.of(ActionType.APPLY_CELL_PARAMETER_CHANGE).capabilityId());
    }

    @Test
    void simulationCannotExecuteBeforeApproval() {
        assertThrows(com.simba.snip.npo.domain.DomainConflictException.class,
                () -> ActionLifecycle.requireExecutable(ActionStatus.APPROVAL_REQUIRED, PolicyOutcome.REQUIRE_APPROVAL, false));
    }
}
