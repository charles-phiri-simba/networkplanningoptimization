package com.simba.snip.npo.changeplanning.api;

public record ChangePlanRollbackDto(
        int sequenceNumber,
        String operationType,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String expectedCurrentValue,
        String desiredValue
) {
}
