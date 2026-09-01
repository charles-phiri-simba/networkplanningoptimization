package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkChangeExecutionAttemptRepository extends JpaRepository<NetworkChangeExecutionAttemptEntity, UUID> {

    List<NetworkChangeExecutionAttemptEntity> findByExecutionIdOrderByAttemptNumberAsc(UUID executionId);

    Optional<NetworkChangeExecutionAttemptEntity>
    findFirstByExecutionIdAndDirectionOrderByAttemptNumberDesc(UUID executionId, String direction);

    long countByExecutionIdAndDirection(UUID executionId, String direction);
}
