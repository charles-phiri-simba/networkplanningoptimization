package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.stereotype.Service;

@Service
public class VendorCapabilityCertificationService {

    private final Phase17SeparationOfDutiesPolicy sod;

    public VendorCapabilityCertificationService(Phase17SeparationOfDutiesPolicy sod) {
        this.sod = sod;
    }

    public void requireCellTxPower(String objectType, String parameter, String actor) {
        sod.requirePrincipal(actor, "capability certifier");
        sod.denyAgentOrMcp(actor);
        if (!"CELL".equals(objectType) || !"txPower".equals(parameter)) {
            throw new Phase17Exception(Phase17DenialCode.P17_CAPABILITY_NOT_CERTIFIED, "CELL/txPower only");
        }
    }
}
