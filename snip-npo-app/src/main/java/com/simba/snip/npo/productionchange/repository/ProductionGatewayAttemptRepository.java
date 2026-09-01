package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionGatewayAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionGatewayAttemptRepository extends JpaRepository<ProductionGatewayAttemptEntity, UUID> {

    List<ProductionGatewayAttemptEntity> findByProductionChangeIdOrderByStartedAtAsc(UUID productionChangeId);

    Optional<ProductionGatewayAttemptEntity> findFirstByGrantIdOrderByStartedAtDesc(UUID grantId);

    Optional<ProductionGatewayAttemptEntity> findFirstByProductionChangeIdOrderByStartedAtDesc(UUID productionChangeId);
}
