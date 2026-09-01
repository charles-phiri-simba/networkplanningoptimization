package com.simba.snip.npo.productionchange.policy;

import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProductionChangeWindowPolicy {

    public void requireOpen(Instant now, Instant windowStart, Instant windowEnd) {
        if (windowStart == null || windowEnd == null) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_WINDOW_CLOSED,
                    "change window is unknown; deny"
            );
        }
        if (now.isBefore(windowStart) || !now.isBefore(windowEnd)) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_WINDOW_CLOSED,
                    "change window is closed"
            );
        }
    }

    public boolean isOpen(Instant now, Instant windowStart, Instant windowEnd) {
        return windowStart != null && windowEnd != null && !now.isBefore(windowStart) && now.isBefore(windowEnd);
    }
}
