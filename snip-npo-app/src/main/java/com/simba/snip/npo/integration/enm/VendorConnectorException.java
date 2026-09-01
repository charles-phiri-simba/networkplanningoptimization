package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;

public final class VendorConnectorException extends ImportRuntimeException {

    public VendorConnectorException(ImportFailureCode failureCode, String message) {
        super(failureCode, message, retryable(failureCode), null);
    }

    public VendorConnectorException(ImportFailureCode failureCode, String message, Throwable cause) {
        super(failureCode, message, retryable(failureCode), cause);
    }

    private static boolean retryable(ImportFailureCode code) {
        return code == ImportFailureCode.VENDOR_UNAVAILABLE
                || code == ImportFailureCode.VENDOR_RATE_LIMITED
                || code == ImportFailureCode.VENDOR_TIMEOUT;
    }
}
