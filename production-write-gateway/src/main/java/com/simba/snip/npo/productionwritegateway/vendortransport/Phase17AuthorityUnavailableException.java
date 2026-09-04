package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;

public class Phase17AuthorityUnavailableException extends RuntimeException {

    public Phase17AuthorityUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public Phase17DenialCode denialCode() {
        return Phase17DenialCode.P17_AUTHORITY_UNAVAILABLE;
    }
}
