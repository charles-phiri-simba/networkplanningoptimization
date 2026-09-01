package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan_precondition")
public class NetworkChangePlanPreconditionEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "precondition_type", nullable = false, length = 64)
    private String preconditionType;

    @Column(name = "expected_condition", nullable = false, length = 512)
    private String expectedCondition;

    @Column(name = "observed_value", length = 512)
    private String observedValue;

    @Column(nullable = false, length = 16)
    private String result;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "evidence_reference", length = 256)
    private String evidenceReference;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    public static NetworkChangePlanPreconditionEntity create(
            UUID id,
            UUID planId,
            String preconditionType,
            String expectedCondition,
            String observedValue,
            String result,
            String reasonCode,
            Instant checkedAt,
            String evidenceReference,
            int sequenceNumber
    ) {
        NetworkChangePlanPreconditionEntity entity = new NetworkChangePlanPreconditionEntity();
        entity.id = id;
        entity.planId = planId;
        entity.preconditionType = preconditionType;
        entity.expectedCondition = expectedCondition;
        entity.observedValue = observedValue;
        entity.result = result;
        entity.reasonCode = reasonCode;
        entity.checkedAt = checkedAt;
        entity.evidenceReference = evidenceReference;
        entity.sequenceNumber = sequenceNumber;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public String getPreconditionType() { return preconditionType; }
    public String getExpectedCondition() { return expectedCondition; }
    public String getObservedValue() { return observedValue; }
    public String getResult() { return result; }
    public String getReasonCode() { return reasonCode; }
    public Instant getCheckedAt() { return checkedAt; }
    public String getEvidenceReference() { return evidenceReference; }
    public int getSequenceNumber() { return sequenceNumber; }

    public void updateEvaluation(
            String observedValue,
            String result,
            String reasonCode,
            Instant checkedAt,
            String evidenceReference
    ) {
        this.observedValue = observedValue;
        this.result = result;
        this.reasonCode = reasonCode;
        this.checkedAt = checkedAt;
        this.evidenceReference = evidenceReference;
    }
}
