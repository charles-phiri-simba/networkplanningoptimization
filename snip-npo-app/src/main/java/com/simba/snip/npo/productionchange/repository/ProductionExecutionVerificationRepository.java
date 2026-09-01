package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionExecutionVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionExecutionVerificationRepository extends JpaRepository<ProductionExecutionVerificationEntity, UUID> {

    List<ProductionExecutionVerificationEntity> findByProductionChangeIdOrderByVerifiedAtAsc(UUID productionChangeId);

    Optional<ProductionExecutionVerificationEntity> findFirstByProductionChangeIdOrderByVerifiedAtDesc(UUID productionChangeId);
}
