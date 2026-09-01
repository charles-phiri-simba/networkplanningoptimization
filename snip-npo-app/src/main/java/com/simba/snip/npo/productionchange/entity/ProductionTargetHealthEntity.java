package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_target_health")
public class ProductionTargetHealthEntity {

    @Id
    @Column(name = "health_id")
    private UUID healthId;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(name = "health_state", nullable = false, length = 16)
    private String healthState;

    @Column(name = "outcome_unknown_count", nullable = false)
    private int outcomeUnknownCount;

    @Column(name = "verification_failure_count", nullable = false)
    private int verificationFailureCount;

    @Column(name = "last_checked_at", nullable = false)
    private Instant lastCheckedAt;

    public static ProductionTargetHealthEntity initial(UUID healthId, String productionTargetId, Instant now) {
        ProductionTargetHealthEntity entity = new ProductionTargetHealthEntity();
        entity.healthId = healthId;
        entity.productionTargetId = productionTargetId;
        entity.healthState = "HEALTHY";
        entity.outcomeUnknownCount = 0;
        entity.verificationFailureCount = 0;
        entity.lastCheckedAt = now;
        return entity;
    }

    public UUID getHealthId() { return healthId; }
    public String getProductionTargetId() { return productionTargetId; }
    public String getHealthState() { return healthState; }
    public int getOutcomeUnknownCount() { return outcomeUnknownCount; }
    public int getVerificationFailureCount() { return verificationFailureCount; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }

    public void setHealthState(String healthState) { this.healthState = healthState; }
    public void setOutcomeUnknownCount(int outcomeUnknownCount) { this.outcomeUnknownCount = outcomeUnknownCount; }
    public void setVerificationFailureCount(int verificationFailureCount) { this.verificationFailureCount = verificationFailureCount; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
}
