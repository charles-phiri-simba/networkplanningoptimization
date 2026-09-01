package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionGatewayEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductionGatewayEvidenceRepository extends JpaRepository<ProductionGatewayEvidenceEntity, UUID> {
}
