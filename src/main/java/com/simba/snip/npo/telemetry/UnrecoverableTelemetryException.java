package com.simba.snip.npo.telemetry;

public class UnrecoverableTelemetryException extends RuntimeException {

    public UnrecoverableTelemetryException(String message) {
        super(message);
    }

    public UnrecoverableTelemetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
