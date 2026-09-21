package com.simba.snip.npo.productioncampaign.exception;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public class CampaignException extends RuntimeException {

    private final ProductionReasonCode reasonCode;

    public CampaignException(ProductionReasonCode reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public CampaignException(ProductionReasonCode reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public ProductionReasonCode reasonCode() {
        return reasonCode;
    }
}
