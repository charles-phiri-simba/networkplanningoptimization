package com.simba.snip.npo.twin;

import com.simba.snip.npo.api.ScenarioChangeRequest;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainRules;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.persist.SimulationScenarioChangeEntity;
import com.simba.snip.npo.persist.SimulationScenarioChangeRepository;
import com.simba.snip.npo.persist.SimulationScenarioEntity;
import com.simba.snip.npo.persist.SimulationScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TwinScenarioService {

    private static final Logger log = LoggerFactory.getLogger(TwinScenarioService.class);

    private final TwinSynchronizationService synchronizationService;
    private final SimulationScenarioRepository scenarioRepository;
    private final SimulationScenarioChangeRepository changeRepository;
    private final TwinJson twinJson;
    private final TwinMetrics metrics;

    public TwinScenarioService(
            TwinSynchronizationService synchronizationService,
            SimulationScenarioRepository scenarioRepository,
            SimulationScenarioChangeRepository changeRepository,
            TwinJson twinJson,
            TwinMetrics metrics
    ) {
        this.synchronizationService = synchronizationService;
        this.scenarioRepository = scenarioRepository;
        this.changeRepository = changeRepository;
        this.twinJson = twinJson;
        this.metrics = metrics;
    }

    @Transactional
    public SimulationScenarioEntity create(UUID twinId, String name, String description, String createdBy,
                                           Integer baselineTwinVersion, ScenarioChangeRequest change) {
        NetworkTwinEntity twin = synchronizationService.requireTwin(twinId);
        int version = baselineTwinVersion == null ? twin.getLatestVersion() : baselineTwinVersion;
        if (version < 1) {
            throw new DomainValidationException("Twin has no synchronized version");
        }
        NetworkTwinVersionEntity snapshot = synchronizationService.requireVersion(twinId, version);
        if (change == null || change.parameterId() == null) {
            throw new DomainValidationException("scenario change is required");
        }
        SimulatableParameterDefinition definition =
                SimulatableParameterRegistry.requireEnabled(change.parameterId(), TwinScopeType.CELL);
        BigDecimal baseline = baselineTxPower(snapshot);
        BigDecimal current = change.currentValue() == null ? baseline : decimal(change.currentValue(), "currentValue");
        if (current.compareTo(baseline) != 0) {
            throw new DomainValidationException(
                    "baseline/current-value mismatch: scenario currentValue=" + current + " twin txPower=" + baseline
            );
        }
        BigDecimal proposed = decimal(change.proposedValue(), "proposedValue");
        SimulatableParameterRegistry.requireInRange(definition, current);
        SimulatableParameterRegistry.requireInRange(definition, proposed);
        SimulationScenarioEntity scenario = scenarioRepository.save(SimulationScenarioEntity.create(
                UUID.randomUUID(),
                twin,
                version,
                DomainRules.requireDomainId(name == null || name.isBlank() ? "txPower scenario" : name, "name"),
                description == null || description.isBlank() ? "Hypothetical txPower change" : description.trim(),
                ScenarioStatus.ACTIVE.name(),
                Instant.now(),
                DomainRules.requireDomainId(createdBy == null || createdBy.isBlank() ? "demo-user" : createdBy, "createdBy"),
                true
        ));
        changeRepository.save(SimulationScenarioChangeEntity.create(
                UUID.randomUUID(),
                scenario,
                definition.parameterId(),
                current.stripTrailingZeros().toPlainString(),
                proposed.stripTrailingZeros().toPlainString(),
                definition.unit()
        ));
        metrics.incrementScenariosCreated();
        log.info(
                "simulationScenariosCreated=1 scenarioId={} twinId={} version={} parameter={} {}->{}",
                scenario.getId(), twinId, version, definition.parameterId(), current, proposed
        );
        return scenario;
    }

    @Transactional(readOnly = true)
    public SimulationScenarioEntity requireScenario(UUID scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new DomainNotFoundException("scenario", scenarioId.toString()));
    }

    @Transactional(readOnly = true)
    public SimulationScenarioChangeEntity requireChange(UUID scenarioId) {
        return changeRepository.findByScenario_Id(scenarioId)
                .orElseThrow(() -> new DomainValidationException("invalid scenario: missing change"));
    }

    public BigDecimal baselineTxPower(NetworkTwinVersionEntity version) {
        return twinJson.radios(version.getConfiguration()).stream()
                .filter(p -> SimulatableParameterRegistry.TX_POWER.equals(p.parameterName()))
                .map(p -> new BigDecimal(p.parameterValue()))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("Twin baseline has no txPower"));
    }

    private static BigDecimal decimal(Double value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return BigDecimal.valueOf(value);
    }
}
