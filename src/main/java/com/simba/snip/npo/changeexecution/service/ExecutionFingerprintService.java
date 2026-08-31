package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ExecutionFingerprintService {

    private final ChangeExecutionProperties properties;

    public ExecutionFingerprintService(ChangeExecutionProperties properties) {
        this.properties = properties;
    }

    public record FingerprintInput(
            NetworkChangePlanEntity plan,
            ExecutionTargetDescriptor target,
            List<NetworkChangeExecutionOperationEntity> operations,
            NetworkChangePlanRollbackOperationEntity rollback,
            Instant executionWindowOpensAt,
            Instant executionWindowClosesAt
    ) {
    }

    public String compute(FingerprintInput input) {
        return sha256Hex(buildCanonical(input));
    }

    public String computeRollbackFingerprint(
            java.util.UUID executionId,
            NetworkChangePlanEntity plan,
            ExecutionTargetDescriptor target,
            NetworkChangePlanRollbackOperationEntity rollback
    ) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "executionId", executionId == null ? null : executionId.toString());
        appendField(sb, "planFingerprint", plan.getFingerprint());
        appendField(sb, "planVersion", String.valueOf(plan.getPlanVersion()));
        appendField(sb, "targetId", target.targetId());
        appendField(sb, "targetType", target.targetType().name());
        appendField(sb, "targetEnvironment", target.environment().name());
        appendField(sb, "adapterProfileId", target.adapterProfileId());
        appendField(sb, "capabilityProfileVersion", target.capabilityProfileVersion());
        if (rollback != null) {
            appendField(sb, "rollback.sequence", String.valueOf(rollback.getSequenceNumber()));
            appendField(sb, "rollback.type", rollback.getOperationType());
            appendField(sb, "rollback.targetType", rollback.getTargetEntityType());
            appendField(sb, "rollback.targetId", rollback.getTargetEntityId());
            appendField(sb, "rollback.parameter", rollback.getParameterName());
            appendField(sb, "rollback.expected", normalizeNumeric(rollback.getExpectedCurrentValue()));
            appendField(sb, "rollback.desired", normalizeNumeric(rollback.getDesiredValue()));
        }
        appendField(sb, "policy.maximumOperationCount", String.valueOf(properties.getMaximumOperationCount()));
        appendField(sb, "policy.maximumForwardAttempts", String.valueOf(properties.getMaximumForwardAttempts()));
        appendField(sb, "policy.requireCurrentValueMatch", String.valueOf(properties.isRequireCurrentValueMatch()));
        appendField(sb, "policy.requireVerification", String.valueOf(properties.isRequireVerification()));
        appendField(sb, "policy.requireRollbackReview", String.valueOf(properties.isRequireRollbackReview()));
        appendField(sb, "policy.requireRollbackAuthorization", String.valueOf(properties.isRequireRollbackAuthorization()));
        return sha256Hex(sb.toString());
    }

    private String buildCanonical(FingerprintInput input) {
        StringBuilder sb = new StringBuilder();
        NetworkChangePlanEntity plan = input.plan();
        ExecutionTargetDescriptor target = input.target();
        appendField(sb, "planFingerprint", plan.getFingerprint());
        appendField(sb, "planVersion", String.valueOf(plan.getPlanVersion()));
        appendField(sb, "targetId", target.targetId());
        appendField(sb, "targetType", target.targetType().name());
        appendField(sb, "targetEnvironment", target.environment().name());
        appendField(sb, "adapterProfileId", target.adapterProfileId());
        appendField(sb, "capabilityProfileVersion", target.capabilityProfileVersion());
        for (NetworkChangeExecutionOperationEntity operation : input.operations()) {
            appendField(sb, "operation.sequence", String.valueOf(operation.getSequenceNumber()));
            appendField(sb, "operation.type", operation.getOperationType());
            appendField(sb, "operation.targetType", operation.getTargetEntityType());
            appendField(sb, "operation.targetId", operation.getTargetEntityId());
            appendField(sb, "operation.parameter", operation.getParameterName());
            appendField(sb, "operation.expected", normalizeNumeric(operation.getExpectedCurrentValue()));
            appendField(sb, "operation.desired", normalizeNumeric(operation.getDesiredValue()));
        }
        NetworkChangePlanRollbackOperationEntity rollback = input.rollback();
        if (rollback != null) {
            appendField(sb, "rollback.sequence", String.valueOf(rollback.getSequenceNumber()));
            appendField(sb, "rollback.type", rollback.getOperationType());
            appendField(sb, "rollback.targetType", rollback.getTargetEntityType());
            appendField(sb, "rollback.targetId", rollback.getTargetEntityId());
            appendField(sb, "rollback.parameter", rollback.getParameterName());
            appendField(sb, "rollback.expected", normalizeNumeric(rollback.getExpectedCurrentValue()));
            appendField(sb, "rollback.desired", normalizeNumeric(rollback.getDesiredValue()));
        }
        appendField(sb, "window.opensAt", input.executionWindowOpensAt() == null ? null : input.executionWindowOpensAt().toString());
        appendField(sb, "window.closesAt", input.executionWindowClosesAt() == null ? null : input.executionWindowClosesAt().toString());
        appendField(sb, "policy.maximumOperationCount", String.valueOf(properties.getMaximumOperationCount()));
        appendField(sb, "policy.maximumForwardAttempts", String.valueOf(properties.getMaximumForwardAttempts()));
        appendField(sb, "policy.requireCurrentValueMatch", String.valueOf(properties.isRequireCurrentValueMatch()));
        appendField(sb, "policy.requireVerification", String.valueOf(properties.isRequireVerification()));
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value) {
        sb.append(key).append('=');
        sb.append(value == null ? "null" : value);
        sb.append(';');
    }

    private String normalizeNumeric(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.strip()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return value.strip();
        }
    }

    private String sha256Hex(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
