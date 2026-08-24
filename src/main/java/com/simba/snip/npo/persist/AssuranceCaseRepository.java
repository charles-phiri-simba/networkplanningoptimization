package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssuranceCaseRepository extends JpaRepository<AssuranceCaseEntity, UUID> {

    @EntityGraph(value = "AssuranceCaseEntity.evidence")
    @Query("select c from AssuranceCaseEntity c where c.id = :id")
    Optional<AssuranceCaseEntity> loadById(@Param("id") UUID id);

    @EntityGraph(value = "AssuranceCaseEntity.evidence")
    Optional<AssuranceCaseEntity> findFirstByAffectedEntityIdAndCaseTypeAndStatusIn(
            String affectedEntityId,
            String caseType,
            Collection<String> statuses
    );

    @EntityGraph(value = "AssuranceCaseEntity.evidence")
    List<AssuranceCaseEntity> findByAffectedEntityIdOrderByDetectedAtDesc(String affectedEntityId);

    @EntityGraph(value = "AssuranceCaseEntity.evidence")
    List<AssuranceCaseEntity> findAllByOrderByDetectedAtDesc();
}
