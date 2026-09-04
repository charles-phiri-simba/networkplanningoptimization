package com.simba.snip.npo.vendorcertification.exception;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;

public class Phase17Exception extends RuntimeException {

    private final Phase17DenialCode denialCode;

    public Phase17Exception(Phase17DenialCode denialCode, String message) {
        super(message);
        this.denialCode = denialCode;
    }

    public Phase17DenialCode denialCode() {
        return denialCode;
    }
}
