package com.simba.snip.npo.productionwritegateway.audit;

import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.ProductionAuditCanonical;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import com.simba.snip.npo.productionwritegateway.entity.ProductionChangeAuditEventEntity;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import com.simba.snip.npo.productionwritegateway.repository.ProductionChangeAuditEventRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductionGatewayAuditService {

    public static final String GENESIS_MATERIAL = "SNIP-PHASE16-PRODUCTION-CHANGE-AUDIT-GENESIS-v1";

    private final ProductionChangeAuditEventRepository auditRepository;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionGatewayMetrics metrics;

    public ProductionGatewayAuditService(
            ProductionChangeAuditEventRepository auditRepository,
            ProductionNetworkChangeRepository changeRepository,
            ProductionGatewayMetrics metrics
    ) {
        this.auditRepository = auditRepository;
        this.changeRepository = changeRepository;
        this.metrics = metrics;
    }

    public static String genesisHash() {
        return Sha256Hex.genesisHash();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionChangeAuditEventEntity append(
            UUID productionChangeId,
            String eventType,
            String actorPrincipalId,
            List<String> reasonCodes,
            Map<String, Object> safePayload
    ) {
        Instant occurredAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var head = auditRepository.lockLatestForUpdate(productionChangeId);
        long nextSequence = head.map(h -> h.getSequenceNumber() + 1).orElse(1L);
        String previousHash = head.map(ProductionChangeAuditEventEntity::getEventHash).orElse(genesisHash());
        List<String> sortedReasons = reasonCodes == null
                ? List.of()
                : reasonCodes.stream().sorted().collect(Collectors.toList());
        Object payload = safePayload == null || safePayload.isEmpty() ? Map.of() : new TreeMap<>(safePayload);
        Map<String, Object> canonical = ProductionAuditCanonical.eventPayload(
                productionChangeId,
                eventType,
                1,
                nextSequence,
                occurredAt,
                actorPrincipalId,
                sortedReasons,
                payload
        );
        String eventHash = ProductionAuditCanonical.eventHash(previousHash, canonical);

        ProductionChangeAuditEventEntity entity = new ProductionChangeAuditEventEntity();
        entity.setAuditEventId(UUID.randomUUID());
        entity.setProductionChangeId(productionChangeId);
        entity.setSequenceNumber(nextSequence);
        entity.setEventType(eventType);
        entity.setEventVersion(1);
        entity.setPreviousEventHash(previousHash);
        entity.setEventHash(eventHash);
        entity.setOccurredAt(occurredAt);
        entity.setActorPrincipalId(actorPrincipalId);
        entity.setReasonCodes(sortedReasons.isEmpty() ? null : String.join(",", sortedReasons));
        entity.setSafePayloadJson(CanonicalJson.serialize(payload));
        entity.setChainIntegrity("VALID");
        return auditRepository.saveAndFlush(entity);
    }

    public void markChainInvalid(UUID productionChangeId) {
        metrics.incrementAuditChainInvalid();
        changeRepository.findById(productionChangeId).ifPresent(change -> {
            change.setAuditChainIntegrity("INVALID");
            changeRepository.save(change);
        });
    }
}
