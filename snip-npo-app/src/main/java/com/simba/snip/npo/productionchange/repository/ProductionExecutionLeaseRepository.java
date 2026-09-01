package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionLeaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductionExecutionLeaseRepository extends JpaRepository<ProductionExecutionLeaseEntity, UUID> {

    Optional<ProductionExecutionLeaseEntity> findByProductionTargetIdAndCellIdAndParameterAndStatus(
            String productionTargetId,
            String cellId,
            String parameter,
            String status
    );
}
