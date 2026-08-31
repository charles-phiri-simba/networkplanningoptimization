package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NetworkChangeExecutionRollbackEntityRepository extends JpaRepository<NetworkChangeExecutionRollbackEntity, UUID> {

    Optional<NetworkChangeExecutionRollbackEntity> findByExecutionId(UUID executionId);
}
