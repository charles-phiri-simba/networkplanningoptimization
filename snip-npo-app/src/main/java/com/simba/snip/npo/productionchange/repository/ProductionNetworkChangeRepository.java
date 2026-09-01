package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionNetworkChangeRepository extends JpaRepository<ProductionNetworkChangeEntity, UUID> {

    List<ProductionNetworkChangeEntity> findAllByOrderByCreatedAtDesc();

    Optional<ProductionNetworkChangeEntity> findFirstByPhase15ExecutionIdAndProductionTargetIdAndChangeControlReferenceOrderByCreatedAtDesc(
            UUID phase15ExecutionId,
            String productionTargetId,
            String changeControlReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ProductionNetworkChangeEntity c WHERE c.productionChangeId = :id")
    Optional<ProductionNetworkChangeEntity> lockById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductionNetworkChangeEntity c SET c.auditChainIntegrity = :integrity, "
            + "c.reasonCode = :reasonCode, c.updatedAt = :updatedAt "
            + "WHERE c.productionChangeId = :id")
    int updateAuditChainIntegrity(
            @Param("id") UUID id,
            @Param("integrity") String integrity,
            @Param("reasonCode") String reasonCode,
            @Param("updatedAt") Instant updatedAt
    );
}
