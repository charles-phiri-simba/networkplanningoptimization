package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionChangeAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductionChangeAuditEventRepository extends JpaRepository<ProductionChangeAuditEventEntity, UUID> {

    @Query(value = """
            SELECT * FROM production_change_audit_event
             WHERE production_change_id = :productionChangeId
             ORDER BY sequence_number DESC
             LIMIT 1
             FOR UPDATE
            """, nativeQuery = true)
    Optional<ProductionChangeAuditEventEntity> lockLatestForUpdate(@Param("productionChangeId") UUID productionChangeId);
}
