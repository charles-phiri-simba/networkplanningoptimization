package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlanningCellIntentRepository extends JpaRepository<PlanningCellIntentEntity, UUID> {

    List<PlanningCellIntentEntity> findByAlternativeIdOrderByCellIdAsc(UUID alternativeId);

    List<PlanningCellIntentEntity> findByAlternativeIdIn(Collection<UUID> alternativeIds);

    void deleteByAlternativeId(UUID alternativeId);
}
