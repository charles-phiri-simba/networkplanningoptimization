package com.simba.snip.npo.persist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assurance_case")
@NamedEntityGraph(name = "AssuranceCaseEntity.evidence", attributeNodes = @NamedAttributeNode("evidence"))
public class AssuranceCaseEntity {

    @Id
    private UUID id;

    @Column(name = "case_type", nullable = false, length = 64)
    private String caseType;

    @Column(name = "affected_entity_type", nullable = false, length = 32)
    private String affectedEntityType;

    @Column(name = "affected_entity_id", nullable = false, length = 64)
    private String affectedEntityId;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(nullable = false, length = 16)
    private String confidence;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "first_observed_at", nullable = false)
    private Instant firstObservedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(nullable = false)
    private boolean synthetic;

    @OneToMany(mappedBy = "assuranceCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("observedAt ASC")
    private List<AssuranceEvidenceEntity> evidence = new ArrayList<>();

    public static AssuranceCaseEntity create(
            UUID id,
            String caseType,
            String affectedEntityType,
            String affectedEntityId,
            String severity,
            String confidence,
            String status,
            Instant detectedAt,
            Instant firstObservedAt,
            Instant lastObservedAt,
            String ruleId,
            boolean synthetic
    ) {
        AssuranceCaseEntity entity = new AssuranceCaseEntity();
        entity.id = id;
        entity.caseType = caseType;
        entity.affectedEntityType = affectedEntityType;
        entity.affectedEntityId = affectedEntityId;
        entity.severity = severity;
        entity.confidence = confidence;
        entity.status = status;
        entity.detectedAt = detectedAt;
        entity.firstObservedAt = firstObservedAt;
        entity.lastObservedAt = lastObservedAt;
        entity.ruleId = ruleId;
        entity.synthetic = synthetic;
        return entity;
    }

    public void replaceEvidence(List<AssuranceEvidenceEntity> next) {
        evidence.clear();
        for (AssuranceEvidenceEntity item : next) {
            item.setAssuranceCase(this);
            evidence.add(item);
        }
    }

    public void updateObservation(Instant lastObservedAt, String severity, String confidence) {
        this.lastObservedAt = lastObservedAt;
        this.severity = severity;
        this.confidence = confidence;
    }

    public UUID getId() {
        return id;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getAffectedEntityType() {
        return affectedEntityType;
    }

    public String getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getFirstObservedAt() {
        return firstObservedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public String getRuleId() {
        return ruleId;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public List<AssuranceEvidenceEntity> getEvidence() {
        return evidence;
    }
}
