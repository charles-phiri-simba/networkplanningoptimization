package com.simba.snip.npo.changeintelligence;

import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;

public class ChangeProposalException extends RuntimeException {

    private final ChangeProposalFailureCode failureCode;

    public ChangeProposalException(ChangeProposalFailureCode failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public ChangeProposalFailureCode failureCode() {
        return failureCode;
    }
}
