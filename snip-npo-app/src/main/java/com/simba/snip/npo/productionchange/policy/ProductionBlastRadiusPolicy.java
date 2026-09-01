package com.simba.snip.npo.productionchange.policy;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Component;

@Component
public class ProductionBlastRadiusPolicy {

    private final ProductionChangeProperties properties;

    public ProductionBlastRadiusPolicy(ProductionChangeProperties properties) {
        this.properties = properties;
    }

    public void requireSingleCellParameterOperation(int cellCount, int parameterCount, int operationCount) {
        if (cellCount != properties.getMaximumCellsPerExecution()
                || parameterCount != properties.getMaximumParametersPerExecution()
                || operationCount != properties.getMaximumOperationsPerExecution()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_SCOPE_DENIED,
                    "blast radius is limited to 1 cell / 1 parameter / 1 operation"
            );
        }
    }

    public void requireTxPower(String parameter) {
        if (!"txPower".equals(parameter)) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_SCOPE_DENIED,
                    "only txPower is permitted"
            );
        }
    }
}
