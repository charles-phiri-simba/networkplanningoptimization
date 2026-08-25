package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connector_security_audit_event")
public class ConnectorSecurityAuditEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "credential_ref", length = 128)
    private String credentialRef;

    @Column(name = "credential_version", length = 64)
    private String credentialVersion;

    @Column(name = "endpoint_ref", length = 128)
    private String endpointRef;

    @Column(name = "trust_profile_id", length = 64)
    private String trustProfileId;

    @Column(name = "server_certificate_fingerprint", length = 128)
    private String serverCertificateFingerprint;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    public static ConnectorSecurityAuditEventEntity create(
            UUID eventId,
            UUID sessionId,
            UUID executionId,
            String connectorId,
            String eventType,
            Instant occurredAt,
            String credentialRef,
            String credentialVersion,
            String endpointRef,
            String trustProfileId,
            String serverCertificateFingerprint,
            String failureCode,
            String details
    ) {
        ConnectorSecurityAuditEventEntity entity = new ConnectorSecurityAuditEventEntity();
        entity.eventId = eventId;
        entity.sessionId = sessionId;
        entity.executionId = executionId;
        entity.connectorId = connectorId;
        entity.eventType = eventType;
        entity.occurredAt = occurredAt;
        entity.credentialRef = credentialRef;
        entity.credentialVersion = credentialVersion;
        entity.endpointRef = endpointRef;
        entity.trustProfileId = trustProfileId;
        entity.serverCertificateFingerprint = serverCertificateFingerprint;
        entity.failureCode = failureCode;
        entity.details = details;
        return entity;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCredentialRef() {
        return credentialRef;
    }

    public String getCredentialVersion() {
        return credentialVersion;
    }

    public String getDetails() {
        return details;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
