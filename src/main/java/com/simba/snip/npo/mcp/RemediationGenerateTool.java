package com.simba.snip.npo.mcp;

import com.simba.snip.npo.action.ActionRules;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.DecisionSupportComposer;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RemediationGenerateTool implements McpTool {

    private final AssuranceCaseService assuranceCaseService;
    private final NetworkContextService networkContextService;

    public RemediationGenerateTool(
            AssuranceCaseService assuranceCaseService,
            NetworkContextService networkContextService
    ) {
        this.assuranceCaseService = assuranceCaseService;
        this.networkContextService = networkContextService;
    }

    @Override
    public String name() {
        return ActionRules.CAPABILITY_REMEDIATION;
    }

    @Override
    public String description() {
        return "Generate a structured remediation plan from an Assurance Case. Does not change the network.";
    }

    @Override
    public Map<String, Object> call(Map<String, Object> arguments) {
        UUID caseId = uuid(arguments.get("assuranceCaseId"));
        AssuranceCaseEntity assuranceCase = assuranceCaseService.findById(caseId)
                .orElseThrow(() -> new DomainNotFoundException("assurance case", caseId.toString()));
        var context = networkContextService.resolve(assuranceCase.getAffectedEntityId());
        List<String> checks = DecisionSupportComposer.recommendedChecks(assuranceCase);
        List<String> missing = DecisionSupportComposer.missingEvidence(assuranceCase, context);
        List<String> contributors = DecisionSupportComposer.likelyContributors(assuranceCase, context);
        List<String> evidence = new ArrayList<>();
        assuranceCase.getEvidence().forEach(item -> evidence.add(item.getDescription()));
        List<String> warnings = new ArrayList<>();
        warnings.add("Confirmed root cause is not established.");
        warnings.add("This artifact does not apply a network change.");
        warnings.addAll(missing);
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("actionId", String.valueOf(arguments.get("actionId")));
        plan.put("assuranceCaseId", caseId.toString());
        plan.put("summary", "Investigation plan for " + assuranceCase.getCaseType()
                + " on " + assuranceCase.getAffectedEntityId()
                + " (severity=" + assuranceCase.getSeverity()
                + ", confidence=" + assuranceCase.getConfidence() + ").");
        plan.put("recommendedChecks", checks);
        plan.put("suggestedNextSteps", List.of(
                "Review operational evidence listed on the Assurance Case.",
                "Prioritise interference, coverage, and congestion checks.",
                "Confirm findings with a human reviewer before any network change."
        ));
        plan.put("likelyContributors", contributors);
        plan.put("evidenceReferences", evidence);
        plan.put("warnings", warnings);
        plan.put("synthetic", true);
        return plan;
    }

    private static UUID uuid(Object value) {
        if (value == null) {
            throw new DomainValidationException("assuranceCaseId is required");
        }
        return UUID.fromString(String.valueOf(value));
    }
}
