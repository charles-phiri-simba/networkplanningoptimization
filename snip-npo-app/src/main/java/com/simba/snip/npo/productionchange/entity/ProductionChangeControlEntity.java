package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_change_control")
public class ProductionChangeControlEntity {

    @Id
    @Column(name = "control_id")
    private UUID controlId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(nullable = false, length = 32)
    private String system;

    @Column(nullable = false, length = 256)
    private String reference;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "validated_by_principal_id", nullable = false, length = 128)
    private String validatedByPrincipalId;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    @Column(name = "valid_until")
    private Instant validUntil;

    public static ProductionChangeControlEntity create(
            UUID controlId,
            UUID productionChangeId,
            String system,
            String reference,
            String status,
            String validatedByPrincipalId,
            Instant validatedAt,
            Instant validUntil
    ) {
        ProductionChangeControlEntity entity = new ProductionChangeControlEntity();
        entity.controlId = controlId;
        entity.productionChangeId = productionChangeId;
        entity.system = system;
        entity.reference = reference;
        entity.status = status;
        entity.validatedByPrincipalId = validatedByPrincipalId;
        entity.validatedAt = validatedAt;
        entity.validUntil = validUntil;
        return entity;
    }

    public UUID getControlId() { return controlId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public String getSystem() { return system; }
    public String getReference() { return reference; }
    public String getStatus() { return status; }
    public String getValidatedByPrincipalId() { return validatedByPrincipalId; }
    public Instant getValidatedAt() { return validatedAt; }
    public Instant getValidUntil() { return validUntil; }
}
