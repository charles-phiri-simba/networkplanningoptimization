package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionRollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionExecutionRollbackRepository extends JpaRepository<ProductionExecutionRollbackEntity, UUID> {

    List<ProductionExecutionRollbackEntity> findByProductionChangeIdOrderByCreatedAtDesc(UUID productionChangeId);

    Optional<ProductionExecutionRollbackEntity> findFirstByProductionChangeIdOrderByCreatedAtDesc(UUID productionChangeId);
}
