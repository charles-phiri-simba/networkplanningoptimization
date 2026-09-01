package com.simba.snip.npo.changeplanning.model;

public enum PreconditionResult {
    PASS,
    FAIL,
    UNKNOWN,
    STALE;

    public boolean countsAsPass() {
        return this == PASS;
    }
}
