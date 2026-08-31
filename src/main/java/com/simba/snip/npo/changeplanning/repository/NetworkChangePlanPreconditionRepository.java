package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanPreconditionRepository extends JpaRepository<NetworkChangePlanPreconditionEntity, UUID> {

    List<NetworkChangePlanPreconditionEntity> findByPlanIdOrderBySequenceNumberAsc(UUID planId);
}
