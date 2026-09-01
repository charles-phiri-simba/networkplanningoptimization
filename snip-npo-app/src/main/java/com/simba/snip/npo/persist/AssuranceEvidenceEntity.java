package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assurance_evidence")
public class AssuranceEvidenceEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assurance_case_id", nullable = false)
    private AssuranceCaseEntity assuranceCase;

    @Column(name = "evidence_type", nullable = false, length = 64)
    private String evidenceType;

    @Column(length = 64)
    private String metric;

    private Double value;

    @Column(length = 32)
    private String unit;

    @Column(length = 32)
    private String trend;

    @Column(name = "observed_at")
    private Instant observedAt;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(nullable = false, length = 512)
    private String description;

    public static AssuranceEvidenceEntity create(
            UUID id,
            String evidenceType,
            String metric,
            Double value,
            String unit,
            String trend,
            Instant observedAt,
            String source,
            boolean synthetic,
            String description
    ) {
        AssuranceEvidenceEntity entity = new AssuranceEvidenceEntity();
        entity.id = id;
        entity.evidenceType = evidenceType;
        entity.metric = metric;
        entity.value = value;
        entity.unit = unit;
        entity.trend = trend;
        entity.observedAt = observedAt;
        entity.source = source;
        entity.synthetic = synthetic;
        entity.description = description;
        return entity;
    }

    void setAssuranceCase(AssuranceCaseEntity assuranceCase) {
        this.assuranceCase = assuranceCase;
    }

    public UUID getId() {
        return id;
    }

    public AssuranceCaseEntity getAssuranceCase() {
        return assuranceCase;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public String getMetric() {
        return metric;
    }

    public Double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getTrend() {
        return trend;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public String getSource() {
        return source;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public String getDescription() {
        return description;
    }
}
