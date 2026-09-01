package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionRecoveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductionExecutionRecoveryRepository extends JpaRepository<ProductionExecutionRecoveryEntity, UUID> {
}
