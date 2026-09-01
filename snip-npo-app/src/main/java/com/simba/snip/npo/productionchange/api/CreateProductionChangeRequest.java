package com.simba.snip.npo.productionchange.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateProductionChangeRequest {

    private UUID phase15ExecutionId;
    private String productionTargetId;
    private ChangeControlReferenceDto changeControlReference;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("unknown property: " + name);
    }

    public UUID getPhase15ExecutionId() { return phase15ExecutionId; }
    public void setPhase15ExecutionId(UUID phase15ExecutionId) { this.phase15ExecutionId = phase15ExecutionId; }
    public String getProductionTargetId() { return productionTargetId; }
    public void setProductionTargetId(String productionTargetId) { this.productionTargetId = productionTargetId; }
    public ChangeControlReferenceDto getChangeControlReference() { return changeControlReference; }
    public void setChangeControlReference(ChangeControlReferenceDto changeControlReference) { this.changeControlReference = changeControlReference; }

    public UUID phase15ExecutionId() { return phase15ExecutionId; }
    public String productionTargetId() { return productionTargetId; }
    public ChangeControlReferenceDto changeControlReference() { return changeControlReference; }
}
