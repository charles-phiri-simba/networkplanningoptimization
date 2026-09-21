package com.simba.snip.npo.productioncampaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_execution_item")
public class CampaignExecutionItemEntity {

    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "revision_id", nullable = false)
    private UUID revisionId;

    @Column(name = "cohort_id", nullable = false)
    private UUID cohortId;

    @Column(name = "item_sequence", nullable = false)
    private int itemSequence;

    @Column(name = "phase14_plan_id")
    private UUID phase14PlanId;

    @Column(name = "phase14_plan_fingerprint", length = 64)
    private String phase14PlanFingerprint;

    @Column(name = "phase15_execution_id")
    private UUID phase15ExecutionId;

    @Column(name = "phase15_execution_fingerprint", length = 64)
    private String phase15ExecutionFingerprint;

    @Column(name = "production_change_id")
    private UUID productionChangeId;

    @Column(name = "production_fingerprint", length = 64)
    private String productionFingerprint;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(name = "object_type", nullable = false, length = 16)
    private String objectType;

    @Column(name = "cell_id", nullable = false, length = 128)
    private String cellId;

    @Column(nullable = false, length = 64)
    private String parameter;

    @Column(name = "expected_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal expectedValue;

    @Column(name = "desired_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal desiredValue;

    @Column(name = "rollback_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal rollbackValue;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(name = "item_fingerprint", nullable = false, length = 64)
    private String itemFingerprint;

    @Column(nullable = false, length = 64)
    private String state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public UUID getCohortId() {
        return cohortId;
    }

    public void setCohortId(UUID cohortId) {
        this.cohortId = cohortId;
    }

    public int getItemSequence() {
        return itemSequence;
    }

    public void setItemSequence(int itemSequence) {
        this.itemSequence = itemSequence;
    }

    public UUID getPhase14PlanId() {
        return phase14PlanId;
    }

    public void setPhase14PlanId(UUID phase14PlanId) {
        this.phase14PlanId = phase14PlanId;
    }

    public String getPhase14PlanFingerprint() {
        return phase14PlanFingerprint;
    }

    public void setPhase14PlanFingerprint(String phase14PlanFingerprint) {
        this.phase14PlanFingerprint = phase14PlanFingerprint;
    }

    public UUID getPhase15ExecutionId() {
        return phase15ExecutionId;
    }

    public void setPhase15ExecutionId(UUID phase15ExecutionId) {
        this.phase15ExecutionId = phase15ExecutionId;
    }

    public String getPhase15ExecutionFingerprint() {
        return phase15ExecutionFingerprint;
    }

    public void setPhase15ExecutionFingerprint(String phase15ExecutionFingerprint) {
        this.phase15ExecutionFingerprint = phase15ExecutionFingerprint;
    }

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public String getProductionFingerprint() {
        return productionFingerprint;
    }

    public void setProductionFingerprint(String productionFingerprint) {
        this.productionFingerprint = productionFingerprint;
    }

    public String getProductionTargetId() {
        return productionTargetId;
    }

    public void setProductionTargetId(String productionTargetId) {
        this.productionTargetId = productionTargetId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public BigDecimal getDesiredValue() {
        return desiredValue;
    }

    public void setDesiredValue(BigDecimal desiredValue) {
        this.desiredValue = desiredValue;
    }

    public BigDecimal getRollbackValue() {
        return rollbackValue;
    }

    public void setRollbackValue(BigDecimal rollbackValue) {
        this.rollbackValue = rollbackValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getItemFingerprint() {
        return itemFingerprint;
    }

    public void setItemFingerprint(String itemFingerprint) {
        this.itemFingerprint = itemFingerprint;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
