package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionTargetHealthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductionTargetHealthRepository extends JpaRepository<ProductionTargetHealthEntity, UUID> {

    Optional<ProductionTargetHealthEntity> findByProductionTargetId(String productionTargetId);
}
