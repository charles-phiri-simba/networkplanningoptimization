package com.simba.snip.npo.productioncampaign.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateProductionCampaignRequest {

    private String productionTargetId;
    private String objective;
    private String changeControlReference;
    private List<Item> items = new ArrayList<>();

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("unknown property: " + name);
    }

    public String getProductionTargetId() {
        return productionTargetId;
    }

    public void setProductionTargetId(String productionTargetId) {
        this.productionTargetId = productionTargetId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getChangeControlReference() {
        return changeControlReference;
    }

    public void setChangeControlReference(String changeControlReference) {
        this.changeControlReference = changeControlReference;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public static class Item {
        private UUID phase14PlanId;
        private String phase14PlanFingerprint;
        private UUID phase15ExecutionId;
        private String phase15ExecutionFingerprint;
        private String cellId;
        private BigDecimal expectedValue;
        private BigDecimal desiredValue;
        private BigDecimal rollbackValue;
        private String unit;
        private String objectType;
        private String parameter;

        @JsonAnySetter
        public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("unknown property: " + name);
        }

        public UUID getPhase14PlanId() { return phase14PlanId; }
        public void setPhase14PlanId(UUID phase14PlanId) { this.phase14PlanId = phase14PlanId; }
        public String getPhase14PlanFingerprint() { return phase14PlanFingerprint; }
        public void setPhase14PlanFingerprint(String phase14PlanFingerprint) { this.phase14PlanFingerprint = phase14PlanFingerprint; }
        public UUID getPhase15ExecutionId() { return phase15ExecutionId; }
        public void setPhase15ExecutionId(UUID phase15ExecutionId) { this.phase15ExecutionId = phase15ExecutionId; }
        public String getPhase15ExecutionFingerprint() { return phase15ExecutionFingerprint; }
        public void setPhase15ExecutionFingerprint(String phase15ExecutionFingerprint) { this.phase15ExecutionFingerprint = phase15ExecutionFingerprint; }
        public String getCellId() { return cellId; }
        public void setCellId(String cellId) { this.cellId = cellId; }
        public BigDecimal getExpectedValue() { return expectedValue; }
        public void setExpectedValue(BigDecimal expectedValue) { this.expectedValue = expectedValue; }
        public BigDecimal getDesiredValue() { return desiredValue; }
        public void setDesiredValue(BigDecimal desiredValue) { this.desiredValue = desiredValue; }
        public BigDecimal getRollbackValue() { return rollbackValue; }
        public void setRollbackValue(BigDecimal rollbackValue) { this.rollbackValue = rollbackValue; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getObjectType() { return objectType; }
        public void setObjectType(String objectType) { this.objectType = objectType; }
        public String getParameter() { return parameter; }
        public void setParameter(String parameter) { this.parameter = parameter; }
    }
}
