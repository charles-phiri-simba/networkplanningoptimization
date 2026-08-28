package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceProvenanceRepository extends JpaRepository<SourceProvenanceEntity, UUID> {
    List<SourceProvenanceEntity> findByImportExecutionId(UUID importExecutionId);

    List<SourceProvenanceEntity> findByCanonicalEntityId(String canonicalEntityId);
}
