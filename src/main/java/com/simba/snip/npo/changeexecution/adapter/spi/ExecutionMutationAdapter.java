package com.simba.snip.npo.changeexecution.adapter.spi;

public interface ExecutionMutationAdapter {

    String targetId();

    MutationResult execute(AuthorizedExecutionOperation operation, ExecutionContext context);

    MutationResult rollback(AuthorizedRollbackOperation operation, ExecutionContext context);
}
