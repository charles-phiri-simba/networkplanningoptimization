package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkDriftObservationRepository extends JpaRepository<NetworkDriftObservationEntity, UUID> {

    List<NetworkDriftObservationEntity> findBySourceSystemAndSynchronizationScopeOrderByDetectedAtDesc(
            String sourceSystem,
            String synchronizationScope
    );

    List<NetworkDriftObservationEntity> findBySourceSystemAndSynchronizationScopeAndDriftStatusOrderByDetectedAtDesc(
            String sourceSystem,
            String synchronizationScope,
            String driftStatus
    );
}
