package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_execution_verification")
public class ProductionExecutionVerificationEntity {

    @Id
    @Column(name = "verification_id")
    private UUID verificationId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(nullable = false, length = 32)
    private String result;

    @Column(name = "observed_value", columnDefinition = "numeric")
    private BigDecimal observedValue;

    @Column(name = "desired_value", columnDefinition = "numeric")
    private BigDecimal desiredValue;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    public static ProductionExecutionVerificationEntity create(
            UUID verificationId,
            UUID productionChangeId,
            UUID attemptId,
            String result,
            BigDecimal observedValue,
            BigDecimal desiredValue,
            Instant verifiedAt
    ) {
        ProductionExecutionVerificationEntity entity = new ProductionExecutionVerificationEntity();
        entity.verificationId = verificationId;
        entity.productionChangeId = productionChangeId;
        entity.attemptId = attemptId;
        entity.result = result;
        entity.observedValue = observedValue;
        entity.desiredValue = desiredValue;
        entity.verifiedAt = verifiedAt;
        return entity;
    }

    public UUID getVerificationId() { return verificationId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public UUID getAttemptId() { return attemptId; }
    public String getResult() { return result; }
    public BigDecimal getObservedValue() { return observedValue; }
    public BigDecimal getDesiredValue() { return desiredValue; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
