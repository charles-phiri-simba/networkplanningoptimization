package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangeExecutionOperationRepository extends JpaRepository<NetworkChangeExecutionOperationEntity, UUID> {

    List<NetworkChangeExecutionOperationEntity> findByExecutionIdOrderBySequenceNumberAsc(UUID executionId);
}
