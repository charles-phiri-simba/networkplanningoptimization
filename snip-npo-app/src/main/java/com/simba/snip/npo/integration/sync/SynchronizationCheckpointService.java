package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import com.simba.snip.npo.persist.SynchronizationCheckpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SynchronizationCheckpointService {

    public static final String SYNTHETIC_CHECKPOINT_TYPE = "SYNTHETIC_SEQUENCE";

    private final SynchronizationCheckpointRepository repository;

    public SynchronizationCheckpointService(SynchronizationCheckpointRepository repository) {
        this.repository = repository;
    }

    public Optional<SynchronizationCheckpointEntity> find(String sourceSystem, String scope) {
        return repository.findBySourceSystemAndSynchronizationScope(sourceSystem, scope);
    }

    @Transactional
    public SynchronizationCheckpointEntity require(String sourceSystem, String connectorId, String scope, Instant now) {
        return repository.findBySourceSystemAndSynchronizationScope(sourceSystem, scope)
                .orElseGet(() -> repository.save(SynchronizationCheckpointEntity.create(
                        UUID.randomUUID(),
                        sourceSystem,
                        connectorId,
                        scope,
                        SYNTHETIC_CHECKPOINT_TYPE,
                        SimulatorEnmSyncState.CHECKPOINT_ZERO,
                        null,
                        null,
                        null,
                        0L,
                        SynchronizationCheckpointStatus.UNVERIFIED.name(),
                        now
                )));
    }

    @Transactional
    public boolean advanceIfAuthoritative(
            String sourceSystem,
            String scope,
            UUID executionId,
            String snapshotId,
            Instant startedAt,
            Instant completedAt,
            String checkpointValue,
            String sourceVersion,
            SynchronizationMode mode,
            String completeness,
            long fencingToken,
            Instant observedAt,
            Instant now
    ) {
        Optional<SynchronizationCheckpointEntity> existing = repository.findBySourceSystemAndSynchronizationScope(
                sourceSystem, scope);
        if (existing.isEmpty()) {
            return false;
        }
        SynchronizationCheckpointEntity checkpoint = existing.get();
        if (fencingToken < checkpoint.getFencingToken()) {
            return false;
        }
        checkpoint.advance(
                executionId,
                snapshotId,
                startedAt,
                completedAt,
                checkpointValue,
                sourceVersion,
                mode.name(),
                completeness,
                fencingToken,
                SynchronizationCheckpointStatus.VALID.name(),
                observedAt,
                now
        );
        repository.save(checkpoint);
        return true;
    }

    @Transactional
    public void markStatus(String sourceSystem, String scope, SynchronizationCheckpointStatus status, Instant now) {
        repository.findBySourceSystemAndSynchronizationScope(sourceSystem, scope).ifPresent(checkpoint -> {
            checkpoint.markStatus(status.name(), now);
            repository.save(checkpoint);
        });
    }
}
