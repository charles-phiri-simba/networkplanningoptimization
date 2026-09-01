package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionGatewayAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionGatewayAttemptRepository extends JpaRepository<ProductionGatewayAttemptEntity, UUID> {

    long countByGrantId(UUID grantId);

    List<ProductionGatewayAttemptEntity> findByGrantId(UUID grantId);
}
