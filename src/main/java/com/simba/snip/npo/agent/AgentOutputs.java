package com.simba.snip.npo.agent;

import com.simba.snip.npo.action.ActionType;
import com.simba.snip.npo.api.CitationDto;
import com.simba.snip.npo.network.CellContext;

import java.util.List;
import java.util.UUID;

public final class AgentOutputs {

    private AgentOutputs() {
    }

    public record KnowledgeResult(
            String summary,
            List<CitationDto> citations,
            List<String> retrievedSources,
            boolean insufficientEvidence
    ) {
    }

    public record ContextResult(
            String cellId,
            String siteId,
            String gnbId,
            List<String> currentKpis,
            List<String> historyTrends,
            List<String> configuration,
            List<String> neighbours,
            String provenance,
            boolean synthetic
    ) {
        public static ContextResult from(CellContext context) {
            return new ContextResult(
                    context.cell().cellId(),
                    context.site().siteId(),
                    context.gnb().gnbId(),
                    context.kpis().stream().limit(8).map(CellContext.KpiObservationView::formatted).toList(),
                    context.telemetry().stream()
                            .map(series -> series.metric() + "=" + series.trend())
                            .toList(),
                    context.radioConfiguration().stream()
                            .map(item -> item.parameterName() + "=" + item.parameterValue())
                            .toList(),
                    context.neighbours().stream().map(CellContext.NeighbourView::targetCellId).toList(),
                    context.provenance().source(),
                    context.provenance().synthetic()
            );
        }
    }

    public record AssuranceResult(
            UUID caseId,
            String caseType,
            String severity,
            String confidence,
            String status,
            List<String> operationalEvidence,
            List<String> missingEvidence
    ) {
    }

    public record CandidateAction(
            ActionType actionType,
            String capabilityId,
            String targetType,
            String targetId,
            String rationale
    ) {
    }

    public record DecisionResult(
            String summary,
            List<String> likelyContributors,
            List<String> recommendedChecks,
            List<String> missingEvidence,
            CandidateAction candidateAction,
            boolean humanReviewRequired
    ) {
    }

    public record PlannedStep(
            int stepNumber,
            AgentRole agentRole,
            String task,
            String requiredInputs,
            String expectedOutput
    ) {
    }
}
