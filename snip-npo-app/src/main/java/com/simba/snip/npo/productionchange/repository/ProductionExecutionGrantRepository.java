package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionExecutionGrantRepository extends JpaRepository<ProductionExecutionGrantEntity, UUID> {

    List<ProductionExecutionGrantEntity> findByProductionChangeIdOrderByIssuedAtDesc(UUID productionChangeId);

    Optional<ProductionExecutionGrantEntity> findFirstByProductionChangeIdAndGrantTypeAndStatus(
            UUID productionChangeId,
            String grantType,
            String status
    );

    List<ProductionExecutionGrantEntity> findByTargetIdAndStatus(String targetId, String status);

    List<ProductionExecutionGrantEntity> findByStatusAndExpiresAtBefore(String status, Instant expiresAt);

    List<ProductionExecutionGrantEntity> findByProductionChangeIdAndStatus(UUID productionChangeId, String status);

    long countByTargetIdAndStatus(String targetId, String status);
}
