package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PreconditionType;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChangePlanFingerprintService {

    private final ChangePlanningProperties properties;

    public ChangePlanFingerprintService(ChangePlanningProperties properties) {
        this.properties = properties;
    }

    public record FingerprintInput(
            UUID proposalId,
            ParameterChangeIntent intent,
            List<NetworkChangePlanOperationEntity> operations,
            List<ChangePlanDependencyService.DependencyEdge> dependencies,
            List<PreconditionDefinition> preconditions,
            NetworkChangePlanRollbackOperationEntity rollback,
            UUID sourceSynchronizationExecutionId,
            String sourceSnapshotId
    ) {
    }

    public record PreconditionDefinition(PreconditionType type, String expectedCondition) {
    }

    public String compute(FingerprintInput input) {
        String canonical = buildCanonical(input);
        return sha256Hex(canonical);
    }

    public String buildCanonical(FingerprintInput input) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "proposalId", input.proposalId().toString());
        appendField(sb, "targetType", input.intent().targetType());
        appendField(sb, "targetId", input.intent().targetId());
        appendField(sb, "parameter", input.intent().parameter());
        appendField(sb, "expectedCurrentValue", normalizeNumeric(input.intent().expectedCurrentValue()));
        appendField(sb, "desiredValue", normalizeNumeric(input.intent().desiredValue()));
        List<NetworkChangePlanOperationEntity> ops = input.operations().stream()
                .sorted(Comparator.comparingInt(NetworkChangePlanOperationEntity::getSequenceNumber))
                .toList();
        for (NetworkChangePlanOperationEntity op : ops) {
            appendField(sb, "operation.sequence", String.valueOf(op.getSequenceNumber()));
            appendField(sb, "operation.type", op.getOperationType());
            appendField(sb, "operation.targetType", op.getTargetEntityType());
            appendField(sb, "operation.targetId", op.getTargetEntityId());
            appendField(sb, "operation.parameter", op.getParameterName());
            appendField(sb, "operation.expected", normalizeNumeric(op.getExpectedCurrentValue()));
            appendField(sb, "operation.desired", normalizeNumeric(op.getDesiredValue()));
        }
        List<ChangePlanDependencyService.DependencyEdge> deps = input.dependencies().stream()
                .sorted(Comparator.comparing(e -> e.operationId().toString() + "->" + e.dependsOnOperationId()))
                .toList();
        for (ChangePlanDependencyService.DependencyEdge dep : deps) {
            appendField(sb, "dependency", dep.operationId() + "->" + dep.dependsOnOperationId());
        }
        List<PreconditionDefinition> preconditions = input.preconditions().stream()
                .sorted(Comparator.comparing(p -> p.type().name()))
                .toList();
        for (PreconditionDefinition precondition : preconditions) {
            appendField(sb, "precondition.type", precondition.type().name());
            appendField(sb, "precondition.expected", precondition.expectedCondition());
        }
        if (input.rollback() != null) {
            appendField(sb, "rollback.sequence", String.valueOf(input.rollback().getSequenceNumber()));
            appendField(sb, "rollback.type", input.rollback().getOperationType());
            appendField(sb, "rollback.expected", normalizeNumeric(input.rollback().getExpectedCurrentValue()));
            appendField(sb, "rollback.desired", normalizeNumeric(input.rollback().getDesiredValue()));
        }
        appendField(sb, "sourceSynchronizationExecutionId",
                input.sourceSynchronizationExecutionId() == null ? null : input.sourceSynchronizationExecutionId().toString());
        appendField(sb, "sourceSnapshotId", input.sourceSnapshotId());
        appendField(sb, "policy.requireRollback", String.valueOf(properties.isRequireRollback()));
        appendField(sb, "policy.requireCurrentValueMatch", String.valueOf(properties.isRequireCurrentValueMatch()));
        appendField(sb, "policy.requireHighOrMediumKnowledge", String.valueOf(properties.isRequireHighOrMediumKnowledge()));
        appendField(sb, "policy.maximumOperationCount", String.valueOf(properties.getMaximumOperationCount()));
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value) {
        sb.append(key).append('=');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value);
        }
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
