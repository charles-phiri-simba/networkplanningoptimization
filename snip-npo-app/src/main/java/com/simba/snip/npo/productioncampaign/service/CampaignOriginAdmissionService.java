package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.CampaignConstants;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionchange.service.ProductionAdmissionService;
import com.simba.snip.npo.productionchange.service.ProductionChangeControlService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Create-or-return campaign-to-P16 handoff. Origin is taken from durable
 * CampaignHandoffId state, never from caller JSON.
 */
@Service
public class CampaignOriginAdmissionService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionAdmissionService admissionService;

    public CampaignOriginAdmissionService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionAdmissionService admissionService
    ) {
        this.changeRepository = changeRepository;
        this.admissionService = admissionService;
    }

    @Transactional
    public ProductionNetworkChangeEntity createOrReturn(
            String campaignHandoffId,
            UUID phase15ExecutionId,
            String productionTargetId,
            ProductionChangeControlService.ChangeControlReference changeControl,
            ActorPrincipal requester
    ) {
        if (campaignHandoffId == null || !campaignHandoffId.matches("^[0-9a-f]{64}$")) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "CampaignHandoffId is required for campaign-originated admission"
            );
        }
        return changeRepository.findByCampaignHandoffId(campaignHandoffId)
                .map(existing -> {
                    if (!CampaignConstants.ORIGIN_PRODUCTION_CAMPAIGN.equals(existing.getExecutionOrigin())) {
                        throw new CampaignException(
                                ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED,
                                "campaign-bound production change cannot be treated as standalone"
                        );
                    }
                    return existing;
                })
                .orElseGet(() -> persistNew(campaignHandoffId, phase15ExecutionId, productionTargetId, changeControl, requester));
    }

    private ProductionNetworkChangeEntity persistNew(
            String campaignHandoffId,
            UUID phase15ExecutionId,
            String productionTargetId,
            ProductionChangeControlService.ChangeControlReference changeControl,
            ActorPrincipal requester
    ) {
        try {
            ProductionNetworkChangeEntity created = admissionService.admitCampaignOriginated(
                    phase15ExecutionId, productionTargetId, changeControl, requester, campaignHandoffId
            );
            return created;
        } catch (DataIntegrityViolationException ex) {
            return changeRepository.findByCampaignHandoffId(campaignHandoffId)
                    .orElseThrow(() -> new CampaignException(
                            ProductionReasonCode.HANDOFF_IDEMPOTENCY_CONFLICT,
                            "campaign handoff uniqueness conflict",
                            ex
                    ));
        }
    }
}
