package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KpiObservationRepository extends JpaRepository<KpiObservationEntity, UUID> {

    List<KpiObservationEntity> findByCell_IdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
            UUID cellId,
            Instant since
    );

    List<KpiObservationEntity> findByCell_IdAndMetricOrderByObservedAtAsc(UUID cellId, String metric);

    @EntityGraph(attributePaths = "cell")
    Optional<KpiObservationEntity> findByEventId(String eventId);

    long countByEventId(String eventId);
}
