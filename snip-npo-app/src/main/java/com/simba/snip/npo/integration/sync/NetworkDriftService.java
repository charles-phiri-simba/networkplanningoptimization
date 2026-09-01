package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.NetworkDriftObservationEntity;
import com.simba.snip.npo.persist.NetworkDriftObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NetworkDriftService {

    private final NetworkDriftObservationRepository repository;

    public NetworkDriftService(NetworkDriftObservationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NetworkDriftObservationEntity recordOpen(
            String sourceSystem,
            String connectorId,
            String scope,
            NetworkDriftType driftType,
            String entityType,
            String entityId,
            UUID executionId,
            long fencingToken,
            String summary,
            Instant detectedAt
    ) {
        return repository.save(NetworkDriftObservationEntity.open(
                UUID.randomUUID(),
                sourceSystem,
                connectorId,
                scope,
                driftType.name(),
                entityType,
                entityId,
                executionId,
                fencingToken,
                summary,
                detectedAt
        ));
    }

    @Transactional
    public void resolveApplicable(
            String sourceSystem,
            String scope,
            UUID resolutionExecutionId,
            long fencingToken,
            Instant resolvedAt
    ) {
        List<NetworkDriftObservationEntity> open = repository
                .findBySourceSystemAndSynchronizationScopeAndDriftStatusOrderByDetectedAtDesc(
                        sourceSystem, scope, NetworkDriftStatus.OPEN.name());
        for (NetworkDriftObservationEntity drift : open) {
            drift.resolve(resolutionExecutionId, fencingToken, resolvedAt);
            repository.save(drift);
        }
    }

    public List<NetworkDriftObservationEntity> list(String sourceSystem, String scope) {
        return repository.findBySourceSystemAndSynchronizationScopeOrderByDetectedAtDesc(sourceSystem, scope);
    }
}
