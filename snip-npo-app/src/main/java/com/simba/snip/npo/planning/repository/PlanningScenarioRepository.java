package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningScenarioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanningScenarioRepository extends JpaRepository<PlanningScenarioEntity, UUID> {

    List<PlanningScenarioEntity> findAllByOrderByUpdatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PlanningScenarioEntity s where s.id = :id")
    Optional<PlanningScenarioEntity> lockById(@Param("id") UUID id);
}
