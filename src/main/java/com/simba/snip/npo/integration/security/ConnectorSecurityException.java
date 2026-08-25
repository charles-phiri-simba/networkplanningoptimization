package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;

public class ConnectorSecurityException extends ImportRuntimeException {

    public ConnectorSecurityException(ImportFailureCode failureCode, String message) {
        super(failureCode, sanitize(message), null);
    }

    public ConnectorSecurityException(ImportFailureCode failureCode, String message, Throwable cause) {
        super(failureCode, sanitize(message), cause);
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "connector security failure";
        }
        return message;
    }
}
