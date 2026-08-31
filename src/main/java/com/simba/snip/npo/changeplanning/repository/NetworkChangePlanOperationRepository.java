package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanOperationRepository extends JpaRepository<NetworkChangePlanOperationEntity, UUID> {

    List<NetworkChangePlanOperationEntity> findByPlanIdOrderBySequenceNumberAsc(UUID planId);
}
