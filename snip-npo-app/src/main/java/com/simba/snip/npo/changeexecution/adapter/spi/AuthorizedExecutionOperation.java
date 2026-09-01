package com.simba.snip.npo.changeexecution.adapter.spi;

public record AuthorizedExecutionOperation(
        String operationType,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String expectedCurrentValue,
        String desiredValue
) {
}
