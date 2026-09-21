package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.CampaignConstants;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public final class TxPowerUnitValidator {

    private TxPowerUnitValidator() {
    }

    public static void requireCanonicalDbm(String unit) {
        if (unit == null || !CampaignConstants.UNIT_DBM.equals(unit)) {
            throw new CampaignException(
                    ProductionReasonCode.UNSUPPORTED_PARAMETER_UNIT,
                    "Phase 18 campaign mutation unit must be canonical dBm"
            );
        }
    }

    public static void requireCellTxPower(String objectType, String parameter) {
        if (!CampaignConstants.OBJECT_CELL.equals(objectType)
                || !CampaignConstants.PARAMETER_TX_POWER.equals(parameter)) {
            throw new CampaignException(
                    ProductionReasonCode.UNSUPPORTED_OBJECT_OR_PARAMETER,
                    "Phase 18 campaign mutation is CELL/txPower only"
            );
        }
    }
}
