package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkChangeExecutionRepository extends JpaRepository<NetworkChangeExecutionEntity, UUID> {

    List<NetworkChangeExecutionEntity> findAllByOrderByCreatedAtDesc();

    Optional<NetworkChangeExecutionEntity> findFirstByPlanIdAndStatusInOrderByCreatedAtDesc(
            UUID planId,
            List<String> activeStatuses
    );

    Optional<NetworkChangeExecutionEntity> findFirstByExecutionTargetIdAndCellIdAndParameterNameAndStatusInOrderByCreatedAtDesc(
            String executionTargetId,
            String cellId,
            String parameterName,
            List<String> activeStatuses
    );
}
