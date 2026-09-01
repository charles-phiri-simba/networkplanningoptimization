package com.simba.snip.npo.productionchange.exception;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public class ProductionChangeException extends RuntimeException {

    private final ProductionReasonCode reasonCode;

    public ProductionChangeException(ProductionReasonCode reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public ProductionChangeException(ProductionReasonCode reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public ProductionReasonCode reasonCode() {
        return reasonCode;
    }
}
