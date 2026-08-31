package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkChangeExecutionAuthorizationRepository extends JpaRepository<NetworkChangeExecutionAuthorizationEntity, UUID> {

    List<NetworkChangeExecutionAuthorizationEntity> findByExecutionIdOrderByAuthorizedAtAsc(UUID executionId);

    Optional<NetworkChangeExecutionAuthorizationEntity> findFirstByExecutionIdAndAuthorizationTypeOrderByAuthorizedAtDesc(
            UUID executionId,
            String authorizationType
    );
}
