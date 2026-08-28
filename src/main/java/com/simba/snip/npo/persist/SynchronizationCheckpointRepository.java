package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SynchronizationCheckpointRepository extends JpaRepository<SynchronizationCheckpointEntity, UUID> {

    Optional<SynchronizationCheckpointEntity> findBySourceSystemAndSynchronizationScope(
            String sourceSystem,
            String synchronizationScope
    );
}
