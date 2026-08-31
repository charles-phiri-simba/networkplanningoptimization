package com.simba.snip.npo.changeexecution.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecutionDetailDto(
        UUID executionId,
        UUID planId,
        int planVersion,
        String planFingerprint,
        String executionTargetId,
        String executionTargetType,
        String executionTargetEnvironment,
        String adapterProfileId,
        String capabilityProfileVersion,
        String cellId,
        String parameterName,
        String executionFingerprint,
        String authorizedExecutionFingerprint,
        String status,
        String requestedBy,
        Instant requestedAt,
        String reviewedBy,
        Instant reviewedAt,
        String authorizedBy,
        Instant authorizedAt,
        Instant executionWindowOpensAt,
        Instant executionWindowClosesAt,
        Instant startedAt,
        Instant completedAt,
        String failureCode,
        String failureDetailSafe,
        String verificationStatus,
        String recoveryStatus,
        String rollbackStatus,
        List<ExecutionOperationDto> operations
) {
}
