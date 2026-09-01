package com.simba.snip.npo.twin;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.persist.SimulationLimitationEntity;
import com.simba.snip.npo.persist.SimulationLimitationRepository;
import com.simba.snip.npo.persist.SimulationResultMetricEntity;
import com.simba.snip.npo.persist.SimulationResultMetricRepository;
import com.simba.snip.npo.persist.SimulationRunEntity;
import com.simba.snip.npo.persist.SimulationRunRepository;
import com.simba.snip.npo.persist.SimulationScenarioChangeEntity;
import com.simba.snip.npo.persist.SimulationScenarioEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DigitalTwinSimulationService {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinSimulationService.class);

    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService scenarioService;
    private final CellParameterSimulationModel model;
    private final SimulationRunRepository runRepository;
    private final SimulationResultMetricRepository metricRepository;
    private final SimulationLimitationRepository limitationRepository;
    private final TwinJson twinJson;
    private final TwinMetrics metrics;

    public DigitalTwinSimulationService(
            TwinSynchronizationService synchronizationService,
            TwinScenarioService scenarioService,
            CellParameterSimulationModel model,
            SimulationRunRepository runRepository,
            SimulationResultMetricRepository metricRepository,
            SimulationLimitationRepository limitationRepository,
            TwinJson twinJson,
            TwinMetrics metrics
    ) {
        this.synchronizationService = synchronizationService;
        this.scenarioService = scenarioService;
        this.model = model;
        this.runRepository = runRepository;
        this.metricRepository = metricRepository;
        this.limitationRepository = limitationRepository;
        this.twinJson = twinJson;
        this.metrics = metrics;
    }

    @Transactional
    public Map<String, Object> executeFromMcp(Map<String, Object> arguments) {
        long startedNanos = System.nanoTime();
        metrics.incrementRunsStarted();
        log.info("simulationRunsStarted=1");
        try {
            if (!truthy(arguments.get("dryRun"))) {
                throw new DomainValidationException("simulation requires dryRun=true");
            }
            UUID scenarioId = uuid(arguments.get("scenarioId"), "scenarioId");
            UUID actionId = optionalUuid(arguments.get("actionId"));
            SimulationScenarioEntity scenario = scenarioService.requireScenario(scenarioId);
            SimulationScenarioChangeEntity change = scenarioService.requireChange(scenarioId);
            if (arguments.get("cellId") != null
                    && !String.valueOf(arguments.get("cellId")).equals(scenario.getTwin().getScopeId())) {
                throw new DomainValidationException("cellId does not match Twin scope");
            }
            if (arguments.get("parameter") != null
                    && !change.getParameterId().equals(String.valueOf(arguments.get("parameter")))) {
                SimulatableParameterRegistry.requireEnabled(String.valueOf(arguments.get("parameter")), TwinScopeType.CELL);
            }
            SimulatableParameterDefinition definition =
                    SimulatableParameterRegistry.requireEnabled(change.getParameterId(), TwinScopeType.CELL);
            NetworkTwinVersionEntity version = synchronizationService.requireVersion(
                    scenario.getTwin().getId(), scenario.getBaselineTwinVersion());
            synchronizationService.requireCurrentForSimulation(version);
            BigDecimal baseline = scenarioService.baselineTxPower(version);
            BigDecimal current = new BigDecimal(change.getCurrentValue());
            BigDecimal proposed = new BigDecimal(change.getProposedValue());
            if (current.compareTo(baseline) != 0) {
                throw new DomainValidationException(
                        "baseline/current-value mismatch: scenario currentValue=" + current + " twin txPower=" + baseline
                );
            }
            if (arguments.get("currentValue") != null
                    && new BigDecimal(String.valueOf(arguments.get("currentValue"))).compareTo(baseline) != 0) {
                throw new DomainValidationException("baseline/current-value mismatch");
            }
            SimulatableParameterRegistry.requireInRange(definition, current);
            SimulatableParameterRegistry.requireInRange(definition, proposed);
            CellParameterSimulationModel.ModelOutput output = model.predict(input(version, current, proposed));
            Instant started = Instant.now();
            Instant completed = Instant.now();
            UUID simulationId = UUID.randomUUID();
            runRepository.save(SimulationRunEntity.createSucceeded(
                    simulationId,
                    scenario,
                    scenario.getTwin(),
                    scenario.getBaselineTwinVersion(),
                    output.metadata().modelId(),
                    output.metadata().modelVersion(),
                    started,
                    completed,
                    output.confidence().name(),
                    twinJson.write(output.assumptions()),
                    version.getProvenance(),
                    actionId
            ));
            for (MetricComparison comparison : output.metrics()) {
                metricRepository.save(SimulationResultMetricEntity.create(
                        UUID.randomUUID(),
                        simulationId,
                        comparison.metric(),
                        comparison.baselineValue(),
                        comparison.candidateValue(),
                        comparison.delta(),
                        comparison.unit()
                ));
            }
            for (SimulationLimitation limitation : output.limitations()) {
                limitationRepository.save(SimulationLimitationEntity.create(
                        UUID.randomUUID(), simulationId, limitation.name()));
            }
            metrics.incrementRunsSucceeded();
            metrics.recordLatencyMs((System.nanoTime() - startedNanos) / 1_000_000);
            log.info(
                    "simulationRunsSucceeded=1 simulationId={} scenarioId={} twinId={} version={} actionId={} modelId={} modelVersion={}",
                    simulationId, scenarioId, scenario.getTwin().getId(), scenario.getBaselineTwinVersion(),
                    actionId, output.metadata().modelId(), output.metadata().modelVersion()
            );
            return toMcpResult(simulationId, scenario, version, output, arguments);
        } catch (RuntimeException ex) {
            metrics.incrementRunsFailed();
            log.info("simulationRunsFailed=1 error={}", ex.getMessage());
            throw ex;
        }
    }

    private CellParameterSimulationModel.SimulationInput input(
            NetworkTwinVersionEntity version,
            BigDecimal current,
            BigDecimal proposed
    ) {
        List<TwinSnapshot.MetricValue> currentMetrics = twinJson.metrics(version.getCurrentMetrics());
        List<TwinSnapshot.TemporalSummary> temporal = twinJson.temporal(version.getTemporalSummary());
        return new CellParameterSimulationModel.SimulationInput(
                current,
                proposed,
                metric(currentMetrics, CellParameterSimulationModel.METRIC_BLER_DL),
                metric(currentMetrics, CellParameterSimulationModel.METRIC_PRB_DL),
                metric(currentMetrics, CellParameterSimulationModel.METRIC_THROUGHPUT_DL),
                trend(temporal, CellParameterSimulationModel.METRIC_BLER_DL)
        );
    }

    private Map<String, Object> toMcpResult(
            UUID simulationId,
            SimulationScenarioEntity scenario,
            NetworkTwinVersionEntity version,
            CellParameterSimulationModel.ModelOutput output,
            Map<String, Object> arguments
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", arguments.get("actionId"));
        result.put("simulationId", simulationId.toString());
        result.put("scenarioId", scenario.getId().toString());
        result.put("twinId", scenario.getTwin().getId().toString());
        result.put("baselineTwinVersion", scenario.getBaselineTwinVersion());
        result.put("cellId", scenario.getTwin().getScopeId());
        result.put("modelId", output.metadata().modelId());
        result.put("modelVersion", output.metadata().modelVersion());
        result.put("modelType", output.metadata().modelType());
        result.put("confidence", output.confidence().name());
        result.put("limitations", output.limitations().stream().map(Enum::name).toList());
        result.put("assumptions", output.assumptions());
        result.put("metrics", output.metrics().stream().map(m -> Map.of(
                "metric", m.metric(),
                "baselineValue", m.baselineValue(),
                "candidateValue", m.candidateValue(),
                "delta", m.delta(),
                "unit", m.unit()
        )).toList());
        result.put("provenance", twinJson.provenance(version.getProvenance()));
        result.put("freshness", TwinFreshness.CURRENT.name());
        result.put("synthetic", true);
        result.put("dryRun", true);
        result.put("networkWriteAttempted", false);
        result.put("predictedImpact",
                "SYNTHETIC: snip.synthetic.cell-parameter.v1 dry-run against Twin version "
                        + scenario.getBaselineTwinVersion()
                        + ". This is not a vendor-certified RF prediction.");
        result.put("warnings", List.of(
                "synthetic=true",
                "dryRun=true is mandatory",
                "No live network write is performed"
        ));
        return result;
    }

    private static Double metric(List<TwinSnapshot.MetricValue> metrics, String name) {
        return metrics.stream()
                .filter(m -> name.equals(m.metric()))
                .map(TwinSnapshot.MetricValue::value)
                .findFirst()
                .orElse(null);
    }

    private static String trend(List<TwinSnapshot.TemporalSummary> temporal, String name) {
        return temporal.stream()
                .filter(t -> name.equals(t.metric()))
                .map(TwinSnapshot.TemporalSummary::trend)
                .findFirst()
                .orElse(null);
    }

    private static UUID uuid(Object value, String field) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new DomainValidationException("invalid scenario: " + field + " is required");
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("invalid scenario: " + field);
        }
    }

    private static UUID optionalUuid(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("actionId is invalid");
        }
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
