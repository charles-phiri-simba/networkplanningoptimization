package com.simba.snip.npo.planning;

public class PlanningException extends RuntimeException {

    private final PlanningFailureCode failureCode;

    public PlanningException(PlanningFailureCode failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public PlanningFailureCode failureCode() {
        return failureCode;
    }
}
