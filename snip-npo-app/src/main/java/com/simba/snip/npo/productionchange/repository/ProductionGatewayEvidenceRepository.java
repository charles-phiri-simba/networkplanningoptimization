package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionGatewayEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionGatewayEvidenceRepository extends JpaRepository<ProductionGatewayEvidenceEntity, UUID> {

    List<ProductionGatewayEvidenceEntity> findByAttemptIdOrderByProducedAtAsc(UUID attemptId);
}
