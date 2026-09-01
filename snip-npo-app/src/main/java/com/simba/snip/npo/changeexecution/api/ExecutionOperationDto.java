package com.simba.snip.npo.changeexecution.api;

public record ExecutionOperationDto(
        int sequenceNumber,
        String operationType,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String expectedCurrentValue,
        String desiredValue
) {
}
