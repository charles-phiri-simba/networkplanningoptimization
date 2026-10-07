package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.api.SimulationDetailDto;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.planning.PlanningException;
import com.simba.snip.npo.planning.PlanningFailureCode;
import com.simba.snip.npo.planning.api.PlanningComparisonDto;
import com.simba.snip.npo.planning.api.PlanningComparisonRowDto;
import com.simba.snip.npo.planning.api.PlanningTruthDto;
import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import com.simba.snip.npo.planning.repository.PlanningEvaluationItemRepository;
import com.simba.snip.npo.twin.SimulationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanningComparisonService {

    private final PlanningScenarioService scenarioService;
    private final PlanningEvaluationItemRepository itemRepository;
    private final PlanningEvaluationService evaluationService;
    private final SimulationQueryService simulationQueryService;

    public PlanningComparisonService(
            PlanningScenarioService scenarioService,
            PlanningEvaluationItemRepository itemRepository,
            PlanningEvaluationService evaluationService,
            SimulationQueryService simulationQueryService
    ) {
        this.scenarioService = scenarioService;
        this.itemRepository = itemRepository;
        this.evaluationService = evaluationService;
        this.simulationQueryService = simulationQueryService;
    }

    @Transactional(readOnly = true)
    public PlanningComparisonDto compare(UUID scenarioId) {
        PlanningScenarioGraph graph = scenarioService.requireGraph(scenarioId);
        PlanningScenarioService.EvaluationView view = scenarioService.evaluationView(graph);
        if (view.currentEvaluationId() == null || "NOT_EVALUATED".equals(view.view()) || "STALE".equals(view.view())) {
            throw new DomainNotFoundException("planning comparison", scenarioId.toString());
        }
        PlanningEvaluationEntity evaluation = graph.evaluations().stream()
                .filter(e -> e.getId().equals(view.currentEvaluationId()))
                .findFirst()
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.EVALUATION_NOT_FOUND, "evaluation not found"));
        Map<UUID, PlanningAlternativeEntity> alts = graph.alternatives().stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getId, a -> a));
        List<PlanningEvaluationItemEntity> items = itemRepository.findByEvaluationId(evaluation.getId());
        boolean historical = evaluationService.toDto(evaluation.getId()).historical();
        List<PlanningComparisonRowDto> rows = new ArrayList<>();
        for (PlanningEvaluationItemEntity item : items.stream()
                .sorted(Comparator
                        .comparingInt((PlanningEvaluationItemEntity i) -> {
                            PlanningAlternativeEntity alt = alts.get(i.getAlternativeId());
                            return alt == null ? 0 : alt.getOrdinal();
                        })
                        .thenComparing(PlanningEvaluationItemEntity::getCellId))
                .toList()) {
            PlanningAlternativeEntity alt = alts.get(item.getAlternativeId());
            List<com.simba.snip.npo.api.MetricComparisonDto> metrics = List.of();
            if ("SUCCEEDED".equals(item.getOutcome()) && item.getSimulationRunId() != null) {
                SimulationDetailDto simulation = simulationQueryService.simulation(item.getSimulationRunId());
                metrics = simulation.metrics();
            }
            rows.add(new PlanningComparisonRowDto(
                    item.getAlternativeId(),
                    alt == null ? null : alt.getName(),
                    alt == null ? 0 : alt.getOrdinal(),
                    item.getCellId(),
                    item.getParameterId(),
                    item.getPinnedBaselineTxPower(),
                    item.getIntendedValue(),
                    metrics,
                    item.getConfidence(),
                    item.getSynthetic(),
                    item.getModelId(),
                    item.getModelVersion(),
                    item.getTwinId(),
                    item.getTwinVersion(),
                    item.getSimulationRunId(),
                    item.getOutcome()
            ));
        }
        return new PlanningComparisonDto(
                scenarioId,
                evaluation.getId(),
                evaluation.getStatus(),
                evaluation.getIntentFingerprint(),
                historical,
                rows,
                PlanningTruthDto.standard()
        );
    }
}
