package com.simba.snip.npo.productionwritegateway.entity;

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

    public UUID getControlId() {
        return controlId;
    }

    public void setControlId(UUID controlId) {
        this.controlId = controlId;
    }

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }
}
