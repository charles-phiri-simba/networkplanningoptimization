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
@Table(name = "production_gateway_evidence")
public class ProductionGatewayEvidenceEntity {

    @Id
    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "evidence_type", nullable = false, length = 32)
    private String evidenceType;

    @Column(name = "evidence_version", nullable = false)
    private int evidenceVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "produced_at", nullable = false)
    private Instant producedAt;

    @Column(nullable = false, length = 32)
    private String producer;

    public UUID getEvidenceId() { return evidenceId; }
    public UUID getAttemptId() { return attemptId; }
    public String getEvidenceType() { return evidenceType; }
    public int getEvidenceVersion() { return evidenceVersion; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getProducedAt() { return producedAt; }
    public String getProducer() { return producer; }
}
