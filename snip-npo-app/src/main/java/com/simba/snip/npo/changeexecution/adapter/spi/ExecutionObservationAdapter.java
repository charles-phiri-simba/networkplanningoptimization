package com.simba.snip.npo.changeexecution.adapter.spi;

public interface ExecutionObservationAdapter {

    String targetId();

    ObservationResult observeCurrentValue(ExecutionContext context, String parameterName, Long minimumRevision);

    ObservationResult verifyForward(
            AuthorizedExecutionOperation operation,
            ExecutionContext context,
            Long minimumRevision
    );

    ObservationResult verifyRollback(
            AuthorizedRollbackOperation operation,
            ExecutionContext context,
            Long minimumRevision
    );
}
