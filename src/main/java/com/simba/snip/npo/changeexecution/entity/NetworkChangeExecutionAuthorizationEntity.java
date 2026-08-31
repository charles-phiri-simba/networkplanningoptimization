package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_authorization")
public class NetworkChangeExecutionAuthorizationEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "authorization_type", nullable = false, length = 32)
    private String authorizationType;

    @Column(nullable = false, length = 128)
    private String authorizer;

    @Column(name = "authorized_fingerprint", nullable = false, length = 64)
    private String authorizedFingerprint;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "execution_version", nullable = false)
    private long executionVersion;

    public static NetworkChangeExecutionAuthorizationEntity create(
            UUID id,
            UUID executionId,
            String authorizationType,
            String authorizer,
            String authorizedFingerprint,
            long executionVersion,
            Instant authorizedAt
    ) {
        NetworkChangeExecutionAuthorizationEntity entity = new NetworkChangeExecutionAuthorizationEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.authorizationType = authorizationType;
        entity.authorizer = authorizer;
        entity.authorizedFingerprint = authorizedFingerprint;
        entity.executionVersion = executionVersion;
        entity.authorizedAt = authorizedAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public String getAuthorizationType() { return authorizationType; }
    public String getAuthorizer() { return authorizer; }
    public String getAuthorizedFingerprint() { return authorizedFingerprint; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public long getExecutionVersion() { return executionVersion; }
}
