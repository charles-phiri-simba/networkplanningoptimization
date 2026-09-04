package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;

public class Phase17SendDeniedException extends RuntimeException {

    private final Phase17DenialCode denialCode;

    public Phase17SendDeniedException(Phase17DenialCode denialCode, String message) {
        super(message);
        this.denialCode = denialCode;
    }

    public Phase17DenialCode denialCode() {
        return denialCode;
    }
}
