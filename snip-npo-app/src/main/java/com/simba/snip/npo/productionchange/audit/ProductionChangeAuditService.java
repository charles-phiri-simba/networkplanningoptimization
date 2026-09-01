package com.simba.snip.npo.productionchange.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.productionchange.domain.AuditChainIntegrity;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionChangeAuditEventEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.ProductionAuditCanonical;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import com.simba.snip.npo.productionchange.repository.ProductionChangeAuditEventRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class ProductionChangeAuditService {

    private static final Logger log = LoggerFactory.getLogger(ProductionChangeAuditService.class);
    private static final TypeReference<TreeMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ProductionChangeAuditEventRepository auditEventRepository;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate independentTx;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductionChangeAuditService(
            ProductionChangeAuditEventRepository auditEventRepository,
            ProductionNetworkChangeRepository changeRepository,
            ProductionChangeMetrics metrics,
            Clock clock,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.auditEventRepository = auditEventRepository;
        this.changeRepository = changeRepository;
        this.metrics = metrics;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.independentTx = new TransactionTemplate(transactionManager);
        this.independentTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public ProductionChangeAuditEventEntity append(
            UUID productionChangeId,
            ProductionAuditEventType eventType,
            String actorPrincipalId,
            List<String> reasonCodes,
            Map<String, Object> safePayload
    ) {
        ProductionNetworkChangeEntity change = changeRepository.lockById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found for audit"
                ));
        if (AuditChainIntegrity.INVALID.name().equals(change.getAuditChainIntegrity())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID,
                    "audit chain INVALID blocks further mutation and grant activity"
            );
        }
        Instant occurredAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        ProductionChangeAuditEventEntity head = auditEventRepository.lockLatestForUpdate(productionChangeId)
                .orElse(null);
        long nextSequence = 1L;
        String previousHash = Sha256Hex.genesisHash();
        if (head != null) {
            nextSequence = head.getSequenceNumber() + 1;
            previousHash = head.getEventHash();
        }
        List<String> sortedReasons = sorted(reasonCodes);
        Object payload = safePayload == null ? Map.of() : new TreeMap<>(safePayload);
        Map<String, Object> canonical = ProductionAuditCanonical.eventPayload(
                productionChangeId,
                eventType.name(),
                1,
                nextSequence,
                occurredAt,
                actorPrincipalId,
                sortedReasons,
                payload
        );
        String eventHash = ProductionAuditCanonical.eventHash(previousHash, canonical);
        ProductionChangeAuditEventEntity event = ProductionChangeAuditEventEntity.create(
                UUID.randomUUID(),
                productionChangeId,
                nextSequence,
                eventType.name(),
                1,
                previousHash,
                eventHash,
                occurredAt,
                actorPrincipalId,
                String.join(",", sortedReasons),
                CanonicalJson.serialize(payload),
                AuditChainIntegrity.VALID.name()
        );
        return auditEventRepository.save(event);
    }

    @Transactional
    public AuditChainIntegrity verify(UUID productionChangeId) {
        entityManager.clear();
        try {
            List<ProductionChangeAuditEventEntity> events =
                    auditEventRepository.findByProductionChangeIdOrderBySequenceNumberAsc(productionChangeId);
            String expectedPrevious = Sha256Hex.genesisHash();
            long expectedSequence = 1L;
            for (ProductionChangeAuditEventEntity event : events) {
                if (event.getSequenceNumber() != expectedSequence) {
                    return markInvalid(productionChangeId, "sequence gap");
                }
                if (!expectedPrevious.equals(event.getPreviousEventHash())) {
                    return markInvalid(productionChangeId, "previous hash mismatch");
                }
                Map<String, Object> canonical = ProductionAuditCanonical.eventPayload(
                        event.getProductionChangeId(),
                        event.getEventType(),
                        event.getEventVersion(),
                        event.getSequenceNumber(),
                        event.getOccurredAt(),
                        event.getActorPrincipalId(),
                        splitReasons(event.getReasonCodes()),
                        parsePayload(event.getSafePayloadJson())
                );
                String recomputed = ProductionAuditCanonical.eventHash(event.getPreviousEventHash(), canonical);
                if (!recomputed.equals(event.getEventHash())) {
                    return markInvalid(productionChangeId, "event hash mismatch");
                }
                expectedPrevious = event.getEventHash();
                expectedSequence++;
            }
            changeRepository.findById(productionChangeId).ifPresent(change -> {
                if (!AuditChainIntegrity.VALID.name().equals(change.getAuditChainIntegrity())) {
                    change.setAuditChainIntegrity(AuditChainIntegrity.VALID.name());
                    change.setUpdatedAt(clock.instant());
                }
            });
            return AuditChainIntegrity.VALID;
        } catch (ProductionChangeException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("audit chain verification unavailable for productionChange: {}", ex.getClass().getSimpleName());
            independentTx.executeWithoutResult(status ->
                    changeRepository.updateAuditChainIntegrity(
                            productionChangeId,
                            AuditChainIntegrity.UNAVAILABLE.name(),
                            ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID.name(),
                            clock.instant()
                    ));
            return AuditChainIntegrity.UNAVAILABLE;
        }
    }

    public void requireMutable(ProductionNetworkChangeEntity change) {
        AuditChainIntegrity integrity = AuditChainIntegrity.valueOf(change.getAuditChainIntegrity());
        if (integrity.blocksMutation()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID,
                    "audit chain " + integrity + " blocks mutation and grant issuance"
            );
        }
    }

    private AuditChainIntegrity markInvalid(UUID productionChangeId, String detail) {
        log.warn("production audit chain INVALID: {}", detail);
        metrics.incrementAuditChainInvalid();
        independentTx.executeWithoutResult(status ->
                changeRepository.updateAuditChainIntegrity(
                        productionChangeId,
                        AuditChainIntegrity.INVALID.name(),
                        ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID.name(),
                        clock.instant()
                ));
        throw new ProductionChangeException(
                ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID,
                "audit chain INVALID: " + detail
        );
    }

    private List<String> sorted(List<String> reasonCodes) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            return List.of();
        }
        return reasonCodes.stream().sorted().toList();
    }

    private List<String> splitReasons(String reasonCodes) {
        if (reasonCodes == null || reasonCodes.isBlank()) {
            return List.of();
        }
        return List.of(reasonCodes.split(","));
    }

    private Object parsePayload(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return Map.of();
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            if (parsed instanceof String nested && nested.trim().startsWith("{")) {
                parsed = objectMapper.readValue(nested, MAP_TYPE);
            }
            if (parsed instanceof Map<?, ?>) {
                return objectMapper.convertValue(parsed, MAP_TYPE);
            }
            return parsed;
        } catch (Exception ex) {
            return json;
        }
    }
}
