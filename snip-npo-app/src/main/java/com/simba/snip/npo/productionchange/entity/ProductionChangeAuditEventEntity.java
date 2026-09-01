package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_change_audit_event")
public class ProductionChangeAuditEventEntity {

    @Id
    @Column(name = "audit_event_id")
    private UUID auditEventId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "previous_event_hash", nullable = false, length = 64)
    private String previousEventHash;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_principal_id", nullable = false, length = 128)
    private String actorPrincipalId;

    @Column(name = "reason_codes", length = 1024)
    private String reasonCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_payload_json", nullable = false, columnDefinition = "jsonb")
    private String safePayloadJson;

    @Column(name = "chain_integrity", nullable = false, length = 16)
    private String chainIntegrity;

    public static ProductionChangeAuditEventEntity create(
            UUID auditEventId,
            UUID productionChangeId,
            long sequenceNumber,
            String eventType,
            int eventVersion,
            String previousEventHash,
            String eventHash,
            Instant occurredAt,
            String actorPrincipalId,
            String reasonCodes,
            String safePayloadJson,
            String chainIntegrity
    ) {
        ProductionChangeAuditEventEntity entity = new ProductionChangeAuditEventEntity();
        entity.auditEventId = auditEventId;
        entity.productionChangeId = productionChangeId;
        entity.sequenceNumber = sequenceNumber;
        entity.eventType = eventType;
        entity.eventVersion = eventVersion;
        entity.previousEventHash = previousEventHash;
        entity.eventHash = eventHash;
        entity.occurredAt = occurredAt;
        entity.actorPrincipalId = actorPrincipalId;
        entity.reasonCodes = reasonCodes;
        entity.safePayloadJson = safePayloadJson;
        entity.chainIntegrity = chainIntegrity;
        return entity;
    }

    public UUID getAuditEventId() { return auditEventId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public String getPreviousEventHash() { return previousEventHash; }
    public String getEventHash() { return eventHash; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActorPrincipalId() { return actorPrincipalId; }
    public String getReasonCodes() { return reasonCodes; }
    public String getSafePayloadJson() { return safePayloadJson; }
    public String getChainIntegrity() { return chainIntegrity; }
}
