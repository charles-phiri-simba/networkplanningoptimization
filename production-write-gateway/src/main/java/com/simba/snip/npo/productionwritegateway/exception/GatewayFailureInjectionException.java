package com.simba.snip.npo.productionwritegateway.exception;

import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;

public class GatewayFailureInjectionException extends RuntimeException {

    private final FailureInjectionPoint point;

    public GatewayFailureInjectionException(FailureInjectionPoint point) {
        super("test-only failure injection at " + point.name());
        this.point = point;
    }

    public FailureInjectionPoint point() {
        return point;
    }
}
