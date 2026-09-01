package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkKnowledgeStatusRepository extends JpaRepository<NetworkKnowledgeStatusEntity, UUID> {

    Optional<NetworkKnowledgeStatusEntity> findBySourceSystemAndSynchronizationScope(
            String sourceSystem,
            String synchronizationScope
    );

    List<NetworkKnowledgeStatusEntity> findAllByOrderBySourceSystemAscSynchronizationScopeAsc();
}
