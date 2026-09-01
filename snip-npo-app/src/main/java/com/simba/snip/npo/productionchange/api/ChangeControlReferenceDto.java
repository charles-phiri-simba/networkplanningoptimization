package com.simba.snip.npo.productionchange.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = false)
public class ChangeControlReferenceDto {

    private String system;
    private String reference;
    private String status;
    private String validatedByPrincipalId;
    private Instant validatedAt;
    private Instant validUntil;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("unknown property: " + name);
    }

    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getValidatedByPrincipalId() { return validatedByPrincipalId; }
    public void setValidatedByPrincipalId(String validatedByPrincipalId) { this.validatedByPrincipalId = validatedByPrincipalId; }
    public Instant getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Instant validatedAt) { this.validatedAt = validatedAt; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }

    public String system() { return system; }
    public String reference() { return reference; }
    public String status() { return status; }
    public String validatedByPrincipalId() { return validatedByPrincipalId; }
    public Instant validatedAt() { return validatedAt; }
    public Instant validUntil() { return validUntil; }
}
