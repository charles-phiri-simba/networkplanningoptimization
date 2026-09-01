package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionTargetHealthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductionTargetHealthRepository extends JpaRepository<ProductionTargetHealthEntity, UUID> {

    Optional<ProductionTargetHealthEntity> findByProductionTargetId(String productionTargetId);
}
