package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SynchronizationSourceStateRepository extends JpaRepository<SynchronizationSourceStateEntity, UUID> {

    Optional<SynchronizationSourceStateEntity> findBySourceSystemAndSynchronizationScope(
            String sourceSystem,
            String synchronizationScope
    );

    List<SynchronizationSourceStateEntity> findAllByOrderBySourceSystemAscSynchronizationScopeAsc();
}
