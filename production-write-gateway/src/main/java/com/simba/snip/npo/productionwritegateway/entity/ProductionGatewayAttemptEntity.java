package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_gateway_attempt")
public class ProductionGatewayAttemptEntity {

    @Id
    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(nullable = false, length = 48)
    private String status;

    @Column(name = "send_phase", nullable = false, length = 32)
    private String sendPhase;

    @Column(name = "mutation_outcome", length = 32)
    private String mutationOutcome;

    @Column(name = "operation_binding_hash", nullable = false, length = 64)
    private String operationBindingHash;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(name = "production_fingerprint", nullable = false, length = 64)
    private String productionFingerprint;

    @Column(name = "gateway_instance_id", length = 64)
    private String gatewayInstanceId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private long version;

    public UUID getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(UUID attemptId) {
        this.attemptId = attemptId;
    }

    public UUID getGrantId() {
        return grantId;
    }

    public void setGrantId(UUID grantId) {
        this.grantId = grantId;
    }

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public String getProductionTargetId() {
        return productionTargetId;
    }

    public void setProductionTargetId(String productionTargetId) {
        this.productionTargetId = productionTargetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSendPhase() {
        return sendPhase;
    }

    public void setSendPhase(String sendPhase) {
        this.sendPhase = sendPhase;
    }

    public String getMutationOutcome() {
        return mutationOutcome;
    }

    public void setMutationOutcome(String mutationOutcome) {
        this.mutationOutcome = mutationOutcome;
    }

    public String getOperationBindingHash() {
        return operationBindingHash;
    }

    public void setOperationBindingHash(String operationBindingHash) {
        this.operationBindingHash = operationBindingHash;
    }

    public long getFencingToken() {
        return fencingToken;
    }

    public void setFencingToken(long fencingToken) {
        this.fencingToken = fencingToken;
    }

    public String getProductionFingerprint() {
        return productionFingerprint;
    }

    public void setProductionFingerprint(String productionFingerprint) {
        this.productionFingerprint = productionFingerprint;
    }

    public String getGatewayInstanceId() {
        return gatewayInstanceId;
    }

    public void setGatewayInstanceId(String gatewayInstanceId) {
        this.gatewayInstanceId = gatewayInstanceId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
