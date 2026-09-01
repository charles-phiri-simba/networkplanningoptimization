package com.simba.snip.npo.changeexecution.adapter.simulator;

import com.simba.snip.npo.changeexecution.entity.SimulatorExecutionCellStateEntity;
import com.simba.snip.npo.changeexecution.repository.SimulatorExecutionCellStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SimulatorExecutionStateStore {

    public record CellState(String value, long revision, Instant updatedAt) {
    }

    private final SimulatorExecutionCellStateRepository repository;
    private final Clock clock;

    public SimulatorExecutionStateStore(SimulatorExecutionCellStateRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<CellState> read(String targetId, String cellId, String parameterName) {
        return repository.findByTargetIdAndCellIdAndParameterName(targetId, cellId, parameterName)
                .map(entity -> new CellState(entity.getParameterValue(), entity.getRevision(), entity.getUpdatedAt()));
    }

    @Transactional
    public CellState initializeIfAbsent(String targetId, String cellId, String parameterName, String initialValue) {
        Optional<SimulatorExecutionCellStateEntity> existing = repository.findByTargetIdAndCellIdAndParameterName(
                targetId, cellId, parameterName);
        if (existing.isPresent()) {
            SimulatorExecutionCellStateEntity entity = existing.get();
            return new CellState(entity.getParameterValue(), entity.getRevision(), entity.getUpdatedAt());
        }
        Instant now = clock.instant();
        SimulatorExecutionCellStateEntity created = SimulatorExecutionCellStateEntity.create(
                UUID.randomUUID(),
                targetId,
                cellId,
                parameterName,
                initialValue,
                now
        );
        repository.save(created);
        return new CellState(created.getParameterValue(), created.getRevision(), created.getUpdatedAt());
    }

    @Transactional
    public Optional<CellState> applyIfCurrentMatches(
            String targetId,
            String cellId,
            String parameterName,
            String expectedCurrentValue,
            String desiredValue
    ) {
        SimulatorExecutionCellStateEntity entity = repository.findByTargetIdAndCellIdAndParameterName(
                targetId, cellId, parameterName).orElse(null);
        if (entity == null || !normalize(entity.getParameterValue()).equals(normalize(expectedCurrentValue))) {
            return Optional.empty();
        }
        long revisionBefore = entity.getRevision();
        entity.applyValue(desiredValue, clock.instant());
        repository.save(entity);
        return Optional.of(new CellState(entity.getParameterValue(), revisionBefore, entity.getUpdatedAt()));
    }

    @Transactional
    public void reset(String targetId, String cellId, String parameterName, String value) {
        repository.findByTargetIdAndCellIdAndParameterName(targetId, cellId, parameterName)
                .ifPresentOrElse(
                        entity -> {
                            entity.applyValue(value, clock.instant());
                            repository.save(entity);
                        },
                        () -> repository.save(SimulatorExecutionCellStateEntity.create(
                                UUID.randomUUID(), targetId, cellId, parameterName, value, clock.instant()))
                );
    }

    @Transactional
    public void deleteScope(String targetId, String cellId, String parameterName) {
        repository.findByTargetIdAndCellIdAndParameterName(targetId, cellId, parameterName)
                .ifPresent(repository::delete);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip();
    }
}
