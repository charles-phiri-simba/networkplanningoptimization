package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkIntegrationConflictRepository extends JpaRepository<NetworkIntegrationConflictEntity, UUID> {

    List<NetworkIntegrationConflictEntity> findAllByOrderByDetectedAtDesc();

    List<NetworkIntegrationConflictEntity> findByImportIdOrderByDetectedAtAsc(UUID importId);

    List<NetworkIntegrationConflictEntity> findByCanonicalEntityIdOrderByDetectedAtAsc(String canonicalEntityId);
}
