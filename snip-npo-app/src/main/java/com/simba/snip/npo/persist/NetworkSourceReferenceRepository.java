package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkSourceReferenceRepository extends JpaRepository<NetworkSourceReferenceEntity, UUID> {

    Optional<NetworkSourceReferenceEntity> findByCanonicalEntityTypeAndCanonicalEntityIdAndAuthoritativeTrue(
            String canonicalEntityType, String canonicalEntityId
    );

    Optional<NetworkSourceReferenceEntity> findByCanonicalEntityTypeAndCanonicalEntityIdAndSourceSystemAndSourceEntityId(
            String canonicalEntityType, String canonicalEntityId, String sourceSystem, String sourceEntityId
    );

    List<NetworkSourceReferenceEntity> findBySourceSystemAndSourceStatus(String sourceSystem, String sourceStatus);

    List<NetworkSourceReferenceEntity> findByCanonicalEntityIdOrderByCanonicalEntityTypeAsc(String canonicalEntityId);
}
