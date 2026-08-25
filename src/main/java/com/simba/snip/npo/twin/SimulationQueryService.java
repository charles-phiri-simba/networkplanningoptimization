package com.simba.snip.npo.twin;

import com.simba.snip.npo.api.MetricComparisonDto;
import com.simba.snip.npo.api.ScenarioDetailDto;
import com.simba.snip.npo.api.SimulationComparisonDto;
import com.simba.snip.npo.api.SimulationDetailDto;
import com.simba.snip.npo.api.TwinDetailDto;
import com.simba.snip.npo.api.TwinVersionDetailDto;
import com.simba.snip.npo.api.TwinVersionSummaryDto;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.persist.NetworkTwinVersionRepository;
import com.simba.snip.npo.persist.SimulationLimitationEntity;
import com.simba.snip.npo.persist.SimulationLimitationRepository;
import com.simba.snip.npo.persist.SimulationResultMetricEntity;
import com.simba.snip.npo.persist.SimulationResultMetricRepository;
import com.simba.snip.npo.persist.SimulationRunEntity;
import com.simba.snip.npo.persist.SimulationRunRepository;
import com.simba.snip.npo.persist.SimulationScenarioChangeEntity;
import com.simba.snip.npo.persist.SimulationScenarioEntity;
import com.simba.snip.npo.persist.SimulationScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SimulationQueryService {

    private static final Logger log = LoggerFactory.getLogger(SimulationQueryService.class);

    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService scenarioService;
    private final NetworkTwinVersionRepository versionRepository;
    private final SimulationScenarioRepository scenarioRepository;
    private final SimulationRunRepository runRepository;
    private final SimulationResultMetricRepository metricRepository;
    private final SimulationLimitationRepository limitationRepository;
    private final TwinJson twinJson;
    private final TwinMetrics metrics;

    public SimulationQueryService(
            TwinSynchronizationService synchronizationService,
            TwinScenarioService scenarioService,
            NetworkTwinVersionRepository versionRepository,
            SimulationScenarioRepository scenarioRepository,
            SimulationRunRepository runRepository,
            SimulationResultMetricRepository metricRepository,
            SimulationLimitationRepository limitationRepository,
            TwinJson twinJson,
            TwinMetrics metrics
    ) {
        this.synchronizationService = synchronizationService;
        this.scenarioService = scenarioService;
        this.versionRepository = versionRepository;
        this.scenarioRepository = scenarioRepository;
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.limitationRepository = limitationRepository;
        this.twinJson = twinJson;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public TwinDetailDto twin(UUID twinId) {
        NetworkTwinEntity twin = synchronizationService.requireTwin(twinId);
        TwinFreshness freshness = twin.getLatestVersion() < 1
                ? TwinFreshness.EXPIRED
                : synchronizationService.freshness(synchronizationService.requireVersion(twinId, twin.getLatestVersion()));
        return toTwin(twin, freshness);
    }

    @Transactional(readOnly = true)
    public List<TwinVersionSummaryDto> versions(UUID twinId) {
        synchronizationService.requireTwin(twinId);
        return versionRepository.findByTwin_IdOrderByVersionAsc(twinId).stream()
                .map(v -> new TwinVersionSummaryDto(
                        v.getId(),
                        v.getVersion(),
                        v.getCapturedAt(),
                        v.getSynchronizedAt(),
                        v.getSourceEventTime(),
                        v.getSourceContextVersion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TwinVersionDetailDto version(UUID twinId, int versionNumber) {
        NetworkTwinVersionEntity version = synchronizationService.requireVersion(twinId, versionNumber);
        TwinFreshness freshness = synchronizationService.freshness(version);
        TwinJson.ServingHolder serving = twinJson.serving(version.getCellState());
        return new TwinVersionDetailDto(
                version.getId(),
                version.getTwin().getId(),
                version.getVersion(),
                version.getCapturedAt(),
                version.getSynchronizedAt(),
                version.getSourceEventTime(),
                version.getSourceContextVersion(),
                freshness.name(),
                twinJson.provenance(version.getProvenance()),
                serving.cell(),
                serving.serving(),
                twinJson.radios(version.getConfiguration()),
                twinJson.metrics(version.getCurrentMetrics()),
                twinJson.temporal(version.getTemporalSummary()),
                twinJson.neighbours(version.getNeighbourSummary())
        );
    }

    @Transactional(readOnly = true)
    public List<ScenarioDetailDto> scenarios(UUID twinId) {
        synchronizationService.requireTwin(twinId);
        return scenarioRepository.findByTwin_IdOrderByCreatedAtDesc(twinId).stream()
                .map(this::toScenario)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioDetailDto scenario(UUID scenarioId) {
        return toScenario(scenarioService.requireScenario(scenarioId));
    }

    @Transactional(readOnly = true)
    public SimulationDetailDto simulation(UUID simulationId) {
        SimulationRunEntity run = runRepository.findById(simulationId)
                .orElseThrow(() -> new com.simba.snip.npo.domain.DomainNotFoundException("simulation", simulationId.toString()));
        return toSimulation(run);
    }

    @Transactional(readOnly = true)
    public SimulationComparisonDto compare(UUID leftId, UUID rightId) {
        SimulationDetailDto left = simulation(leftId);
        SimulationDetailDto right = simulation(rightId);
        if (!"SUCCEEDED".equals(left.status()) || !"SUCCEEDED".equals(right.status())) {
            throw new DomainValidationException("comparison requires two completed simulations");
        }
        if (!left.twinId().equals(right.twinId()) || left.baselineTwinVersion() != right.baselineTwinVersion()) {
            throw new DomainValidationException("comparison requires the same Twin baseline version");
        }
        Map<String, MetricComparisonDto> rightByMetric = right.metrics().stream()
                .collect(Collectors.toMap(MetricComparisonDto::metric, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Map<String, Object>> tradeoffs = new ArrayList<>();
        for (MetricComparisonDto leftMetric : left.metrics()) {
            MetricComparisonDto rightMetric = rightByMetric.get(leftMetric.metric());
            if (rightMetric == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("metric", leftMetric.metric());
            row.put("unit", leftMetric.unit());
            row.put("leftBaseline", leftMetric.baselineValue());
            row.put("leftCandidate", leftMetric.candidateValue());
            row.put("leftDelta", leftMetric.delta());
            row.put("rightBaseline", rightMetric.baselineValue());
            row.put("rightCandidate", rightMetric.candidateValue());
            row.put("rightDelta", rightMetric.delta());
            row.put("deltaDifference", round(rightMetric.delta() - leftMetric.delta()));
            tradeoffs.add(row);
        }
        metrics.incrementComparisons();
        log.info(
                "scenarioComparisons=1 left={} right={} twinId={} version={}",
                leftId, rightId, left.twinId(), left.baselineTwinVersion()
        );
        return new SimulationComparisonDto(
                leftId,
                rightId,
                left.twinId(),
                left.baselineTwinVersion(),
                left.confidence(),
                right.confidence(),
                left.limitations(),
                right.limitations(),
                tradeoffs,
                false
        );
    }

    public TwinDetailDto toTwin(NetworkTwinEntity twin, TwinFreshness freshness) {
        return new TwinDetailDto(
                twin.getId(),
                twin.getName(),
                twin.getScopeType(),
                twin.getScopeId(),
                twin.getStatus(),
                twin.getLatestVersion(),
                twin.getCreatedAt(),
                twin.getSynchronizedAt(),
                twin.isSynthetic(),
                freshness.name()
        );
    }

    private ScenarioDetailDto toScenario(SimulationScenarioEntity scenario) {
        SimulationScenarioChangeEntity change = scenarioService.requireChange(scenario.getId());
        return new ScenarioDetailDto(
                scenario.getId(),
                scenario.getTwin().getId(),
                scenario.getBaselineTwinVersion(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getStatus(),
                scenario.getCreatedAt(),
                scenario.getCreatedBy(),
                scenario.isSynthetic(),
                change.getParameterId(),
                change.getCurrentValue(),
                change.getProposedValue(),
                change.getUnit()
        );
    }

    private SimulationDetailDto toSimulation(SimulationRunEntity run) {
        List<MetricComparisonDto> metricsDto = metricRepository.findBySimulationIdOrderByMetricAsc(run.getId()).stream()
                .map(this::toMetric)
                .toList();
        List<String> limitations = limitationRepository.findBySimulationIdOrderByCodeAsc(run.getId()).stream()
                .map(SimulationLimitationEntity::getCode)
                .toList();
        return new SimulationDetailDto(
                run.getId(),
                run.getScenario().getId(),
                run.getTwin().getId(),
                run.getBaselineTwinVersion(),
                run.getModelId(),
                run.getModelVersion(),
                CellParameterSimulationModel.MODEL_TYPE,
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.isSynthetic(),
                run.getConfidence(),
                twinJson.strings(run.getAssumptions()),
                limitations,
                metricsDto,
                twinJson.provenance(run.getProvenance()),
                run.getActionId()
        );
    }

    private MetricComparisonDto toMetric(SimulationResultMetricEntity entity) {
        return new MetricComparisonDto(
                entity.getMetric(),
                entity.getBaselineValue(),
                entity.getCandidateValue(),
                entity.getDelta(),
                entity.getUnit()
        );
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }
}
