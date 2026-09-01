package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connector_session")
public class ConnectorSessionEntity {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "credential_ref", nullable = false, length = 128)
    private String credentialRef;

    @Column(name = "credential_version", length = 64)
    private String credentialVersion;

    @Column(name = "trust_profile_id", nullable = false, length = 64)
    private String trustProfileId;

    @Column(name = "endpoint_ref", nullable = false, length = 128)
    private String endpointRef;

    @Column(name = "server_certificate_fingerprint", length = 128)
    private String serverCertificateFingerprint;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(nullable = false, length = 32)
    private String status;

    public static ConnectorSessionEntity open(
            UUID sessionId,
            UUID executionId,
            String connectorId,
            String sourceSystem,
            String credentialRef,
            String credentialVersion,
            String trustProfileId,
            String endpointRef,
            Instant startedAt
    ) {
        ConnectorSessionEntity entity = new ConnectorSessionEntity();
        entity.sessionId = sessionId;
        entity.executionId = executionId;
        entity.connectorId = connectorId;
        entity.sourceSystem = sourceSystem;
        entity.credentialRef = credentialRef;
        entity.credentialVersion = credentialVersion;
        entity.trustProfileId = trustProfileId;
        entity.endpointRef = endpointRef;
        entity.startedAt = startedAt;
        entity.status = "OPEN";
        return entity;
    }

    public void close(Instant endedAt, String fingerprint, String status) {
        this.endedAt = endedAt;
        this.serverCertificateFingerprint = fingerprint;
        this.status = status;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getCredentialRef() {
        return credentialRef;
    }

    public String getCredentialVersion() {
        return credentialVersion;
    }

    public String getStatus() {
        return status;
    }

    public String getServerCertificateFingerprint() {
        return serverCertificateFingerprint;
    }
}
