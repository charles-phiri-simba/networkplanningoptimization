package com.simba.snip.npo.action;

import org.springframework.stereotype.Component;

/**
 * Deterministic policy. The LLM has no authority here.
 */
@Component
public class ActionPolicyEvaluator {

    public record Evaluation(PolicyOutcome decision, String policyId, String reason) {
    }

    public Evaluation evaluate(ActionType actionType) {
        ActionSemantics semantics = ActionSemantics.of(actionType);
        String reason = switch (semantics.policy()) {
            case ALLOW -> "GENERATE_REMEDIATION_PLAN is a non-mutating remediation artifact and is allowed.";
            case REQUIRE_APPROVAL -> "SIMULATE_CELL_PARAMETER_CHANGE is synthetic but requires explicit human approval.";
            case DENY -> "APPLY_CELL_PARAMETER_CHANGE is a live network mutation and is denied. No MCP invocation.";
        };
        return new Evaluation(semantics.policy(), ActionRules.POLICY_ID, reason);
    }
}
