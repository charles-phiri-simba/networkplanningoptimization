package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.domain.DomainRules;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.planning.PlanningBounds;
import com.simba.snip.npo.planning.PlanningException;
import com.simba.snip.npo.planning.PlanningFailureCode;
import com.simba.snip.npo.planning.api.CreatePlanningScenarioRequest;
import com.simba.snip.npo.planning.api.PlanningAlternativeRequest;
import com.simba.snip.npo.planning.api.PlanningCellRequest;
import com.simba.snip.npo.planning.api.PlanningIntentRequest;
import com.simba.snip.npo.planning.api.PlanningScenarioDetailDto;
import com.simba.snip.npo.planning.api.PlanningScenarioSummaryDto;
import com.simba.snip.npo.planning.api.ReplacePlanningScenarioRequest;
import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioCellEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioEntity;
import com.simba.snip.npo.planning.repository.PlanningAlternativeRepository;
import com.simba.snip.npo.planning.repository.PlanningCellIntentRepository;
import com.simba.snip.npo.planning.repository.PlanningEvaluationItemRepository;
import com.simba.snip.npo.planning.repository.PlanningEvaluationRepository;
import com.simba.snip.npo.planning.repository.PlanningScenarioCellRepository;
import com.simba.snip.npo.planning.repository.PlanningScenarioRepository;
import com.simba.snip.npo.twin.SimulatableParameterDefinition;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import com.simba.snip.npo.twin.TwinScopeType;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanningScenarioService {

    private final PlanningScenarioRepository scenarioRepository;
    private final PlanningScenarioCellRepository cellRepository;
    private final PlanningAlternativeRepository alternativeRepository;
    private final PlanningCellIntentRepository intentRepository;
    private final PlanningEvaluationRepository evaluationRepository;
    private final PlanningEvaluationItemRepository itemRepository;
    private final CellRepository inventoryCells;
    private final PlanningFingerprintService fingerprintService;
    private final PlanningMapper mapper;
    private final PlanningMetrics metrics;
    private final EntityManager entityManager;

    public PlanningScenarioService(
            PlanningScenarioRepository scenarioRepository,
            PlanningScenarioCellRepository cellRepository,
            PlanningAlternativeRepository alternativeRepository,
            PlanningCellIntentRepository intentRepository,
            PlanningEvaluationRepository evaluationRepository,
            PlanningEvaluationItemRepository itemRepository,
            CellRepository inventoryCells,
            PlanningFingerprintService fingerprintService,
            PlanningMapper mapper,
            PlanningMetrics metrics,
            EntityManager entityManager
    ) {
        this.scenarioRepository = scenarioRepository;
        this.cellRepository = cellRepository;
        this.alternativeRepository = alternativeRepository;
        this.intentRepository = intentRepository;
        this.evaluationRepository = evaluationRepository;
        this.itemRepository = itemRepository;
        this.inventoryCells = inventoryCells;
        this.fingerprintService = fingerprintService;
        this.mapper = mapper;
        this.metrics = metrics;
        this.entityManager = entityManager;
    }

    @Transactional
    public PlanningScenarioDetailDto create(CreatePlanningScenarioRequest request) {
        ValidatedGraph validated = validate(
                request.name(),
                request.description(),
                request.cells(),
                request.alternatives()
        );
        String createdBy = DomainRules.requireDomainId(request.createdBy(), "createdBy");
        Instant now = Instant.now();
        PlanningScenarioEntity scenario = scenarioRepository.save(PlanningScenarioEntity.create(
                UUID.randomUUID(),
                validated.name(),
                validated.description(),
                createdBy,
                now
        ));
        persistGraph(scenario.getId(), validated, List.of());
        metrics.incrementScenariosCreated();
        return toDetail(loadGraph(scenario.getId()));
    }

    @Transactional(readOnly = true)
    public List<PlanningScenarioSummaryDto> list() {
        return scenarioRepository.findAllByOrderByUpdatedAtDesc().stream()
                .limit(PlanningBounds.MAX_SCENARIO_LIST)
                .map(scenario -> {
                    PlanningScenarioGraph graph = loadGraph(scenario.getId());
                    EvaluationView view = evaluationView(graph);
                    return mapper.toSummary(graph, view.view());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanningScenarioDetailDto get(UUID scenarioId) {
        return toDetail(requireGraph(scenarioId));
    }

    @Transactional
    public PlanningScenarioDetailDto replace(UUID scenarioId, ReplacePlanningScenarioRequest request) {
        PlanningScenarioEntity scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.SCENARIO_NOT_FOUND, "scenario not found"));
        if (request.rowVersion() == null || request.rowVersion() != scenario.getRowVersion()) {
            throw new PlanningException(PlanningFailureCode.SCENARIO_VERSION_CONFLICT, "scenario version conflict");
        }
        if (evaluationRepository.findByScenarioIdAndStatus(scenarioId, "IN_PROGRESS").isPresent()) {
            throw new PlanningException(PlanningFailureCode.EVALUATION_IN_PROGRESS, "evaluation in progress");
        }
        ValidatedGraph validated = validate(
                request.name(),
                request.description(),
                request.cells(),
                request.alternatives()
        );
        List<PlanningAlternativeEntity> existingAlts = alternativeRepository.findByScenarioIdOrderByOrdinalAsc(scenarioId);
        Set<UUID> referencedAltIds = itemRepository.findByAlternativeIdIn(
                existingAlts.stream().map(PlanningAlternativeEntity::getId).toList()
        ).stream().map(PlanningEvaluationItemEntity::getAlternativeId).collect(Collectors.toSet());
        for (PlanningAlternativeEntity existing : existingAlts) {
            boolean stillPresent = validated.alternatives().stream().anyMatch(a -> a.ordinal() == existing.getOrdinal());
            if (!stillPresent && referencedAltIds.contains(existing.getId())) {
                throw new DomainValidationException(
                        "cannot remove an alternative referenced by historical evaluation"
                );
            }
        }
        scenario.replaceMetadata(validated.name(), validated.description(), Instant.now());
        persistGraph(scenarioId, validated, existingAlts);
        metrics.incrementScenariosUpdated();
        return toDetail(loadGraph(scenarioId));
    }

    public PlanningScenarioGraph requireGraph(UUID scenarioId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new PlanningException(PlanningFailureCode.SCENARIO_NOT_FOUND, "scenario not found");
        }
        return loadGraph(scenarioId);
    }

    public PlanningScenarioGraph loadGraph(UUID scenarioId) {
        PlanningScenarioEntity scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.SCENARIO_NOT_FOUND, "scenario not found"));
        List<PlanningScenarioCellEntity> cells = cellRepository.findByScenarioIdOrderByOrdinalAsc(scenarioId);
        List<PlanningAlternativeEntity> alternatives = alternativeRepository.findByScenarioIdOrderByOrdinalAsc(scenarioId);
        List<PlanningCellIntentEntity> intents = alternatives.isEmpty()
                ? List.of()
                : intentRepository.findByAlternativeIdIn(alternatives.stream().map(PlanningAlternativeEntity::getId).toList());
        List<PlanningEvaluationEntity> evaluations = evaluationRepository.findByScenarioIdOrderByCreatedAtDesc(scenarioId);
        return new PlanningScenarioGraph(scenario, cells, alternatives, intents, evaluations);
    }

    public EvaluationView evaluationView(PlanningScenarioGraph graph) {
        List<PlanningEvaluationEntity> evaluations = graph.evaluations();
        PlanningEvaluationEntity inProgress = evaluations.stream()
                .filter(e -> "IN_PROGRESS".equals(e.getStatus()))
                .findFirst()
                .orElse(null);
        if (inProgress != null) {
            return new EvaluationView("EVALUATING", inProgress.getId());
        }
        String currentIntent = fingerprintService.intentFingerprint(graph.scenario().getId(), graph.alternatives(), graph.intents());
        PlanningEvaluationEntity latestTerminal = evaluations.stream()
                .filter(e -> !"IN_PROGRESS".equals(e.getStatus()))
                .max(Comparator.comparing(PlanningEvaluationEntity::getCreatedAt))
                .orElse(null);
        if (latestTerminal == null) {
            return new EvaluationView("NOT_EVALUATED", null);
        }
        PlanningEvaluationEntity matching = evaluations.stream()
                .filter(e -> !"IN_PROGRESS".equals(e.getStatus()))
                .filter(e -> currentIntent.equals(e.getIntentFingerprint()))
                .max(Comparator.comparing(PlanningEvaluationEntity::getCreatedAt))
                .orElse(null);
        if (matching == null) {
            return new EvaluationView("STALE", latestTerminal.getId());
        }
        return new EvaluationView(switch (matching.getStatus()) {
            case "SUCCEEDED" -> "EVALUATED";
            case "PARTIAL" -> "PARTIAL";
            case "FAILED" -> "FAILED";
            default -> "NOT_EVALUATED";
        }, matching.getId());
    }

    public record EvaluationView(String view, UUID currentEvaluationId) {
    }

    private PlanningScenarioDetailDto toDetail(PlanningScenarioGraph graph) {
        EvaluationView view = evaluationView(graph);
        return mapper.toDetail(graph, view.view(), view.currentEvaluationId());
    }

    private void persistGraph(UUID scenarioId, ValidatedGraph validated, List<PlanningAlternativeEntity> existingAlts) {
        Map<Integer, PlanningAlternativeEntity> existingByOrdinal = existingAlts.stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getOrdinal, a -> a));
        Set<Integer> keepOrdinals = validated.alternatives().stream().map(ValidatedAlternative::ordinal).collect(Collectors.toSet());
        for (PlanningAlternativeEntity extra : existingAlts) {
            if (!keepOrdinals.contains(extra.getOrdinal())) {
                intentRepository.deleteByAlternativeId(extra.getId());
                alternativeRepository.delete(extra);
            }
        }
        cellRepository.deleteByScenarioId(scenarioId);
        entityManager.flush();
        int cellOrdinal = 1;
        for (String cellId : validated.cellIds()) {
            cellRepository.save(PlanningScenarioCellEntity.create(UUID.randomUUID(), scenarioId, cellId, cellOrdinal++));
        }
        for (ValidatedAlternative alternative : validated.alternatives()) {
            PlanningAlternativeEntity stored = existingByOrdinal.get(alternative.ordinal());
            if (stored == null) {
                stored = alternativeRepository.save(PlanningAlternativeEntity.create(
                        UUID.randomUUID(),
                        scenarioId,
                        alternative.name(),
                        alternative.ordinal()
                ));
            } else {
                stored.rename(alternative.name());
                alternativeRepository.save(stored);
                intentRepository.deleteByAlternativeId(stored.getId());
                entityManager.flush();
            }
            for (ValidatedIntent intent : alternative.intents()) {
                intentRepository.save(PlanningCellIntentEntity.create(
                        UUID.randomUUID(),
                        stored.getId(),
                        intent.cellId(),
                        intent.parameterId(),
                        intent.intendedValue()
                ));
            }
        }
    }

    private ValidatedGraph validate(
            String name,
            String description,
            List<PlanningCellRequest> cells,
            List<PlanningAlternativeRequest> alternatives
    ) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("name is required");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 128) {
            throw new DomainValidationException("name exceeds 128 characters");
        }
        String trimmedDescription = description == null ? "" : description.trim();
        if (trimmedDescription.length() > 1024) {
            throw new DomainValidationException("description exceeds 1024 characters");
        }
        if (cells == null || cells.isEmpty()) {
            throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "at least one cell is required");
        }
        if (cells.size() > PlanningBounds.MAX_CELLS_PER_SCENARIO) {
            throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "cell bound exceeded");
        }
        List<String> cellIds = new ArrayList<>();
        Set<String> uniqueCells = new HashSet<>();
        for (PlanningCellRequest cell : cells) {
            if (cell == null || cell.cellId() == null || cell.cellId().isBlank()) {
                throw new PlanningException(PlanningFailureCode.CELL_UNKNOWN, "cellId is required");
            }
            String cellId = cell.cellId().trim();
            if (!uniqueCells.add(cellId)) {
                throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "duplicate cell");
            }
            if (inventoryCells.findByCellId(cellId).isEmpty()) {
                throw new PlanningException(PlanningFailureCode.CELL_UNKNOWN, "unknown cell: " + cellId);
            }
            cellIds.add(cellId);
        }
        if (alternatives == null || alternatives.isEmpty()) {
            throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "at least one alternative is required");
        }
        if (alternatives.size() > PlanningBounds.MAX_ALTERNATIVES_PER_SCENARIO) {
            throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "alternative bound exceeded");
        }
        Set<Integer> ordinals = new HashSet<>();
        Set<String> names = new HashSet<>();
        List<ValidatedAlternative> validatedAlts = new ArrayList<>();
        int totalIntents = 0;
        for (PlanningAlternativeRequest alternative : alternatives) {
            if (alternative == null || alternative.ordinal() == null) {
                throw new DomainValidationException("alternative ordinal is required");
            }
            if (!ordinals.add(alternative.ordinal())) {
                throw new DomainValidationException("duplicate alternative ordinal");
            }
            if (alternative.name() == null || alternative.name().isBlank()) {
                throw new DomainValidationException("alternative name is required");
            }
            String altName = alternative.name().trim();
            if (altName.length() > 128) {
                throw new DomainValidationException("alternative name exceeds 128 characters");
            }
            if (!names.add(altName.toLowerCase(Locale.ROOT))) {
                throw new DomainValidationException("duplicate alternative name");
            }
            List<PlanningIntentRequest> intents = alternative.intents() == null ? List.of() : alternative.intents();
            if (intents.size() > PlanningBounds.MAX_INTENTS_PER_ALTERNATIVE) {
                throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "intent bound exceeded");
            }
            Set<String> intentCells = new HashSet<>();
            List<ValidatedIntent> validatedIntents = new ArrayList<>();
            for (PlanningIntentRequest intent : intents) {
                if (intent == null || intent.cellId() == null) {
                    throw new PlanningException(PlanningFailureCode.CELL_NOT_IN_SCENARIO, "intent cell is required");
                }
                String cellId = intent.cellId().trim();
                if (!uniqueCells.contains(cellId)) {
                    throw new PlanningException(PlanningFailureCode.CELL_NOT_IN_SCENARIO, "cell not in scenario: " + cellId);
                }
                if (!intentCells.add(cellId)) {
                    throw new DomainValidationException("duplicate intent cell in alternative");
                }
                String parameterId = intent.parameterId() == null ? "" : intent.parameterId().trim();
                SimulatableParameterDefinition definition;
                try {
                    definition = SimulatableParameterRegistry.requireEnabled(parameterId, TwinScopeType.CELL);
                } catch (DomainValidationException ex) {
                    throw new PlanningException(PlanningFailureCode.UNSUPPORTED_PARAMETER, ex.getMessage());
                }
                if (intent.intendedValue() == null) {
                    throw new PlanningException(PlanningFailureCode.PARAMETER_OUT_OF_RANGE, "intendedValue is required");
                }
                try {
                    SimulatableParameterRegistry.requireInRange(definition, intent.intendedValue());
                } catch (DomainValidationException ex) {
                    throw new PlanningException(PlanningFailureCode.PARAMETER_OUT_OF_RANGE, ex.getMessage());
                }
                validatedIntents.add(new ValidatedIntent(cellId, definition.parameterId(), intent.intendedValue()));
                totalIntents++;
            }
            validatedAlts.add(new ValidatedAlternative(alternative.ordinal(), altName, validatedIntents));
        }
        if (ordinals.size() != alternatives.size() || !ordinals.contains(1) || ordinals.stream().max(Integer::compareTo).orElse(0) != alternatives.size()) {
            throw new DomainValidationException("alternative ordinals must be contiguous from 1");
        }
        if (totalIntents == 0) {
            throw new PlanningException(PlanningFailureCode.EMPTY_INTENTS, "at least one intent is required");
        }
        if (totalIntents > PlanningBounds.MAX_SIMULATION_RUNS_PER_EVALUATION) {
            throw new PlanningException(PlanningFailureCode.BOUNDS_EXCEEDED, "evaluation fan-out exceeded");
        }
        validatedAlts.sort(Comparator.comparingInt(ValidatedAlternative::ordinal));
        return new ValidatedGraph(trimmedName, trimmedDescription, cellIds, validatedAlts);
    }

    private record ValidatedIntent(String cellId, String parameterId, BigDecimal intendedValue) {
    }

    private record ValidatedAlternative(int ordinal, String name, List<ValidatedIntent> intents) {
    }

    private record ValidatedGraph(
            String name,
            String description,
            List<String> cellIds,
            List<ValidatedAlternative> alternatives
    ) {
    }
}
