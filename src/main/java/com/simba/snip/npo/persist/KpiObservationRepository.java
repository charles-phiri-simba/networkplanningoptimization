package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface KpiObservationRepository extends JpaRepository<KpiObservationEntity, UUID> {

    List<KpiObservationEntity> findByCell_IdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
            UUID cellId,
            Instant since
    );
}
