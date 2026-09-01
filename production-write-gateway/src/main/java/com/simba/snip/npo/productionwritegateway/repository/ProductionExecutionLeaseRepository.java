package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionLeaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductionExecutionLeaseRepository extends JpaRepository<ProductionExecutionLeaseEntity, UUID> {

    Optional<ProductionExecutionLeaseEntity> findFirstByProductionTargetIdAndCellIdAndParameterAndStatus(
            String productionTargetId,
            String cellId,
            String parameter,
            String status
    );
}
