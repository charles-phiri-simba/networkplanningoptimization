package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionChangeControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionChangeControlRepository extends JpaRepository<ProductionChangeControlEntity, UUID> {

    List<ProductionChangeControlEntity> findByProductionChangeIdOrderByValidatedAtDesc(UUID productionChangeId);

    Optional<ProductionChangeControlEntity> findFirstByProductionChangeIdOrderByValidatedAtDesc(UUID productionChangeId);
}
