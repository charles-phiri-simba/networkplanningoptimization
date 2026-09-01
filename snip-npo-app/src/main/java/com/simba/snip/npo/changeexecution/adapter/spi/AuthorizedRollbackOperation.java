package com.simba.snip.npo.changeexecution.adapter.spi;

public record AuthorizedRollbackOperation(
        String operationType,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String expectedCurrentValue,
        String desiredValue
) {
}
