package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionChangeControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductionChangeControlRepository extends JpaRepository<ProductionChangeControlEntity, UUID> {

    Optional<ProductionChangeControlEntity> findFirstByProductionChangeIdOrderByValidatedAtDesc(UUID productionChangeId);
}
