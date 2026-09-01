package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan_rollback_operation")
public class NetworkChangePlanRollbackOperationEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    @Column(name = "target_entity_type", nullable = false, length = 32)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false, length = 128)
    private String targetEntityId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "expected_current_value", nullable = false, length = 32)
    private String expectedCurrentValue;

    @Column(name = "desired_value", nullable = false, length = 32)
    private String desiredValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static NetworkChangePlanRollbackOperationEntity create(
            UUID id,
            UUID planId,
            int sequenceNumber,
            String operationType,
            String targetEntityType,
            String targetEntityId,
            String parameterName,
            String expectedCurrentValue,
            String desiredValue,
            Instant createdAt
    ) {
        NetworkChangePlanRollbackOperationEntity entity = new NetworkChangePlanRollbackOperationEntity();
        entity.id = id;
        entity.planId = planId;
        entity.sequenceNumber = sequenceNumber;
        entity.operationType = operationType;
        entity.targetEntityType = targetEntityType;
        entity.targetEntityId = targetEntityId;
        entity.parameterName = parameterName;
        entity.expectedCurrentValue = expectedCurrentValue;
        entity.desiredValue = desiredValue;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getOperationType() { return operationType; }
    public String getTargetEntityType() { return targetEntityType; }
    public String getTargetEntityId() { return targetEntityId; }
    public String getParameterName() { return parameterName; }
    public String getExpectedCurrentValue() { return expectedCurrentValue; }
    public String getDesiredValue() { return desiredValue; }
    public Instant getCreatedAt() { return createdAt; }
}
