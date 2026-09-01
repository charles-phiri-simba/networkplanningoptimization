package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRecoveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangeExecutionRecoveryRepository extends JpaRepository<NetworkChangeExecutionRecoveryEntity, UUID> {

    List<NetworkChangeExecutionRecoveryEntity> findByExecutionIdOrderByEvaluatedAtAsc(UUID executionId);
}
