package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan_readiness_assessment")
public class ExecutionReadinessAssessmentEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Column(nullable = false, length = 16)
    private String result;

    @Column(name = "assessed_fingerprint", nullable = false, length = 64)
    private String assessedFingerprint;

    @Column(name = "reason_codes", nullable = false, length = 512)
    private String reasonCodes;

    @Column(name = "evidence_summary", length = 512)
    private String evidenceSummary;

    public static ExecutionReadinessAssessmentEntity create(
            UUID id,
            UUID planId,
            Instant assessedAt,
            String result,
            String assessedFingerprint,
            String reasonCodes,
            String evidenceSummary
    ) {
        ExecutionReadinessAssessmentEntity entity = new ExecutionReadinessAssessmentEntity();
        entity.id = id;
        entity.planId = planId;
        entity.assessedAt = assessedAt;
        entity.result = result;
        entity.assessedFingerprint = assessedFingerprint;
        entity.reasonCodes = reasonCodes;
        entity.evidenceSummary = evidenceSummary;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public Instant getAssessedAt() { return assessedAt; }
    public String getResult() { return result; }
    public String getAssessedFingerprint() { return assessedFingerprint; }
    public String getReasonCodes() { return reasonCodes; }
    public String getEvidenceSummary() { return evidenceSummary; }
}
