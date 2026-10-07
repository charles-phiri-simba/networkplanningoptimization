package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.planning.api.PlanningAlternativeDto;
import com.simba.snip.npo.planning.api.PlanningCellIntentDto;
import com.simba.snip.npo.planning.api.PlanningEvaluationDto;
import com.simba.snip.npo.planning.api.PlanningEvaluationItemDto;
import com.simba.snip.npo.planning.api.PlanningScenarioCellDto;
import com.simba.snip.npo.planning.api.PlanningScenarioDetailDto;
import com.simba.snip.npo.planning.api.PlanningScenarioSummaryDto;
import com.simba.snip.npo.planning.api.PlanningTruthDto;
import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PlanningMapper {

    public PlanningScenarioSummaryDto toSummary(PlanningScenarioGraph graph, String evaluationView) {
        PlanningScenarioEntity scenario = graph.scenario();
        return new PlanningScenarioSummaryDto(
                scenario.getId(),
                scenario.getName(),
                scenario.getCreatedBy(),
                scenario.getCreatedAt(),
                scenario.getUpdatedAt(),
                scenario.getRowVersion(),
                graph.cells().size(),
                graph.alternatives().size(),
                evaluationView,
                PlanningTruthDto.standard()
        );
    }

    public PlanningScenarioDetailDto toDetail(PlanningScenarioGraph graph, String evaluationView, UUID currentEvaluationId) {
        PlanningScenarioEntity scenario = graph.scenario();
        Map<UUID, List<PlanningCellIntentEntity>> intentsByAlt = graph.intents().stream()
                .collect(Collectors.groupingBy(PlanningCellIntentEntity::getAlternativeId));
        List<PlanningAlternativeDto> alternatives = graph.alternatives().stream()
                .sorted(Comparator.comparingInt(PlanningAlternativeEntity::getOrdinal))
                .map(alt -> new PlanningAlternativeDto(
                        alt.getId(),
                        alt.getName(),
                        alt.getOrdinal(),
                        intentsByAlt.getOrDefault(alt.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(PlanningCellIntentEntity::getCellId))
                                .map(intent -> new PlanningCellIntentDto(
                                        intent.getId(),
                                        intent.getCellId(),
                                        intent.getParameterId(),
                                        intent.getIntendedValue()
                                ))
                                .toList()
                ))
                .toList();
        return new PlanningScenarioDetailDto(
                scenario.getId(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getCreatedBy(),
                scenario.getCreatedAt(),
                scenario.getUpdatedAt(),
                scenario.getRowVersion(),
                graph.cells().stream()
                        .sorted(Comparator.comparingInt(c -> c.getOrdinal()))
                        .map(c -> new PlanningScenarioCellDto(c.getId(), c.getCellId(), c.getOrdinal()))
                        .toList(),
                alternatives,
                evaluationView,
                currentEvaluationId,
                PlanningTruthDto.standard()
        );
    }

    public PlanningEvaluationItemDto toItem(
            PlanningEvaluationItemEntity item,
            PlanningAlternativeEntity alternative
    ) {
        return new PlanningEvaluationItemDto(
                item.getId(),
                item.getEvaluationId(),
                item.getAlternativeId(),
                alternative == null ? null : alternative.getName(),
                alternative == null ? 0 : alternative.getOrdinal(),
                item.getCellId(),
                item.getParameterId(),
                item.getIntendedValue(),
                item.getOutcome(),
                item.getFailureCode(),
                item.getFailureMessage(),
                item.getTwinId(),
                item.getTwinVersion(),
                item.getPinnedBaselineTxPower(),
                item.getConfigurationFingerprint(),
                item.getSimulationScenarioId(),
                item.getSimulationRunId(),
                item.getModelId(),
                item.getModelVersion(),
                item.getSynthetic(),
                item.getConfidence(),
                item.isReusedExistingRun()
        );
    }

    public PlanningEvaluationDto toEvaluation(
            PlanningEvaluationEntity evaluation,
            List<PlanningEvaluationItemDto> items,
            boolean historical
    ) {
        return new PlanningEvaluationDto(
                evaluation.getId(),
                evaluation.getScenarioId(),
                evaluation.getStatus(),
                evaluation.getIntentFingerprint(),
                evaluation.getAdmissionFingerprint(),
                evaluation.getCreatedBy(),
                evaluation.getCreatedAt(),
                evaluation.getCompletedAt(),
                evaluation.getItemCount(),
                evaluation.getSucceededCount(),
                evaluation.getFailedCount(),
                historical,
                items,
                PlanningTruthDto.standard()
        );
    }
}
