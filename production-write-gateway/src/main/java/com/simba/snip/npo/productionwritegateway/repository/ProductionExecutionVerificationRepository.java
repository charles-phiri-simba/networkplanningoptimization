package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductionExecutionVerificationRepository
        extends JpaRepository<ProductionExecutionVerificationEntity, UUID> {
}
