package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductionExecutionGrantEntity g set g.status = 'REVOKED', g.version = g.version + 1 "
            + "where g.productionChangeId = :productionChangeId and g.status = 'ISSUED'")
    int revokeIssuedWhereIssued(@Param("productionChangeId") UUID productionChangeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductionExecutionGrantEntity g set g.status = 'REVOKED', g.version = g.version + 1 "
            + "where g.targetId = :targetId and g.status = 'ISSUED'")
    int revokeIssuedByTargetIdWhereIssued(@Param("targetId") String targetId);
}
