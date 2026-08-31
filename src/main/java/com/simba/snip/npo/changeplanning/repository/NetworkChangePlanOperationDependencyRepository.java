package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanOperationDependencyRepository extends JpaRepository<NetworkChangePlanOperationDependencyEntity, UUID> {

    List<NetworkChangePlanOperationDependencyEntity> findByPlanId(UUID planId);
}
