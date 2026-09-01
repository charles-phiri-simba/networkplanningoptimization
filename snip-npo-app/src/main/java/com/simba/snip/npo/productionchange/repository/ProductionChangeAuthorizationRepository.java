package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionChangeAuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionChangeAuthorizationRepository extends JpaRepository<ProductionChangeAuthorizationEntity, UUID> {

    List<ProductionChangeAuthorizationEntity> findByProductionChangeIdOrderByAuthorizedAtDesc(UUID productionChangeId);

    Optional<ProductionChangeAuthorizationEntity> findFirstByProductionChangeIdAndStatusOrderByAuthorizedAtDesc(
            UUID productionChangeId,
            String status
    );

    List<ProductionChangeAuthorizationEntity> findByProductionChangeIdAndStatus(UUID productionChangeId, String status);
}
