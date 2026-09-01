package com.simba.snip.npo.changeexecution.exception;

import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;

public class ChangeExecutionException extends RuntimeException {

    private final ExecutionFailureCode failureCode;

    public ChangeExecutionException(ExecutionFailureCode failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public ExecutionFailureCode failureCode() {
        return failureCode;
    }
}
