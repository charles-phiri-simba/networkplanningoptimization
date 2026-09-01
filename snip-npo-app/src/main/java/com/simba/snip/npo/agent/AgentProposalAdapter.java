package com.simba.snip.npo.agent;

import com.simba.snip.npo.action.ActionProposalService;
import com.simba.snip.npo.action.ActionType;
import com.simba.snip.npo.persist.ProposedActionEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AgentProposalAdapter {

    private final ActionProposalService proposalService;

    public AgentProposalAdapter(ActionProposalService proposalService) {
        this.proposalService = proposalService;
    }

    public ProposedActionEntity propose(UUID runId, UUID assuranceCaseId, AgentOutputs.CandidateAction candidate) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (candidate.actionType() == ActionType.SIMULATE_CELL_PARAMETER_CHANGE) {
            parameters.put("parameter", "txPower");
            parameters.put("dryRun", true);
            parameters.put("cellId", candidate.targetId());
        }
        return proposalService.propose(
                assuranceCaseId,
                candidate.actionType().name(),
                candidate.capabilityId(),
                candidate.targetType(),
                candidate.targetId(),
                parameters,
                candidate.rationale(),
                "AGENT",
                runId,
                AgentRegistry.DECISION
        );
    }
}
