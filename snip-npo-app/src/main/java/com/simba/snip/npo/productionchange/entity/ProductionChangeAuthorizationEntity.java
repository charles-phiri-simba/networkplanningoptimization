package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_change_authorization")
public class ProductionChangeAuthorizationEntity {

    @Id
    @Column(name = "authorization_id")
    private UUID authorizationId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "authorizer_principal_id", nullable = false, length = 128)
    private String authorizerPrincipalId;

    @Column(name = "authorization_generation", nullable = false)
    private int authorizationGeneration;

    @Column(name = "production_fingerprint", nullable = false, length = 64)
    private String productionFingerprint;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static ProductionChangeAuthorizationEntity create(
            UUID authorizationId,
            UUID productionChangeId,
            String authorizerPrincipalId,
            int authorizationGeneration,
            String productionFingerprint,
            String status,
            Instant authorizedAt,
            Instant expiresAt
    ) {
        ProductionChangeAuthorizationEntity entity = new ProductionChangeAuthorizationEntity();
        entity.authorizationId = authorizationId;
        entity.productionChangeId = productionChangeId;
        entity.authorizerPrincipalId = authorizerPrincipalId;
        entity.authorizationGeneration = authorizationGeneration;
        entity.productionFingerprint = productionFingerprint;
        entity.status = status;
        entity.authorizedAt = authorizedAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public UUID getAuthorizationId() { return authorizationId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public String getAuthorizerPrincipalId() { return authorizerPrincipalId; }
    public int getAuthorizationGeneration() { return authorizationGeneration; }
    public String getProductionFingerprint() { return productionFingerprint; }
    public String getStatus() { return status; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public void setStatus(String status) { this.status = status; }
}
