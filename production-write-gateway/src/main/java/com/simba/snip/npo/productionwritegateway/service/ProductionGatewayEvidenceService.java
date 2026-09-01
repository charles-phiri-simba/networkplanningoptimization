package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionwritegateway.entity.ProductionGatewayEvidenceEntity;
import com.simba.snip.npo.productionwritegateway.repository.ProductionGatewayEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionGatewayEvidenceService {

    private final ProductionGatewayEvidenceRepository evidenceRepository;

    public ProductionGatewayEvidenceService(ProductionGatewayEvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionGatewayEvidenceEntity persist(UUID attemptId, String evidenceType, String safePayloadJson) {
        ProductionGatewayEvidenceEntity entity = new ProductionGatewayEvidenceEntity();
        entity.setEvidenceId(UUID.randomUUID());
        entity.setAttemptId(attemptId);
        entity.setEvidenceType(evidenceType);
        entity.setEvidenceVersion(1);
        entity.setPayloadJson(safePayloadJson);
        entity.setProducedAt(Instant.now());
        entity.setProducer("GATEWAY");
        return evidenceRepository.saveAndFlush(entity);
    }
}
