package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanRollbackOperationRepository extends JpaRepository<NetworkChangePlanRollbackOperationEntity, UUID> {

    List<NetworkChangePlanRollbackOperationEntity> findByPlanIdOrderBySequenceNumberAsc(UUID planId);
}
