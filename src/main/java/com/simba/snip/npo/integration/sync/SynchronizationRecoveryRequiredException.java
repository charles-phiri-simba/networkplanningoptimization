package com.simba.snip.npo.integration.sync;

public class SynchronizationRecoveryRequiredException extends RuntimeException {

    public SynchronizationRecoveryRequiredException(String message) {
        super(message);
    }
}
