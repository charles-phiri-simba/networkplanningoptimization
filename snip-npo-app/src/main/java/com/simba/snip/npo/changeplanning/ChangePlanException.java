package com.simba.snip.npo.changeplanning;

import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;

public class ChangePlanException extends RuntimeException {

    private final ChangePlanFailureCode failureCode;

    public ChangePlanException(ChangePlanFailureCode failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public ChangePlanFailureCode failureCode() {
        return failureCode;
    }
}
