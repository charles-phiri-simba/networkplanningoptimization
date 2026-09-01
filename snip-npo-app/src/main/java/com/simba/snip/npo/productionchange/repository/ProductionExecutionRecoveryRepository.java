package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionRecoveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionExecutionRecoveryRepository extends JpaRepository<ProductionExecutionRecoveryEntity, UUID> {

    List<ProductionExecutionRecoveryEntity> findByProductionChangeIdOrderBySignaledAtAsc(UUID productionChangeId);
}
