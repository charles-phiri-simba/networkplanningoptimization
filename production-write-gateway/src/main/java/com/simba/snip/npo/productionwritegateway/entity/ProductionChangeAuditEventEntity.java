package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "safe_payload_json", nullable = false, columnDefinition = "jsonb")
    private String safePayloadJson;

    @Column(name = "chain_integrity", nullable = false, length = 16)
    private String chainIntegrity;

    public UUID getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(UUID auditEventId) {
        this.auditEventId = auditEventId;
    }

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }

    public String getPreviousEventHash() {
        return previousEventHash;
    }

    public void setPreviousEventHash(String previousEventHash) {
        this.previousEventHash = previousEventHash;
    }

    public String getEventHash() {
        return eventHash;
    }

    public void setEventHash(String eventHash) {
        this.eventHash = eventHash;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActorPrincipalId() {
        return actorPrincipalId;
    }

    public void setActorPrincipalId(String actorPrincipalId) {
        this.actorPrincipalId = actorPrincipalId;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(String reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public String getSafePayloadJson() {
        return safePayloadJson;
    }

    public void setSafePayloadJson(String safePayloadJson) {
        this.safePayloadJson = safePayloadJson;
    }

    public String getChainIntegrity() {
        return chainIntegrity;
    }

    public void setChainIntegrity(String chainIntegrity) {
        this.chainIntegrity = chainIntegrity;
    }
}
