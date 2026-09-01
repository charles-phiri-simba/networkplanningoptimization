package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangeExecutionVerificationRepository extends JpaRepository<NetworkChangeExecutionVerificationEntity, UUID> {

    List<NetworkChangeExecutionVerificationEntity> findByExecutionIdOrderByObservedAtAsc(UUID executionId);
}
