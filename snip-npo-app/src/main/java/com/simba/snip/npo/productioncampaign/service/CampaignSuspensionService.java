package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class CampaignSuspensionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignGenerationService generationService;

    public CampaignSuspensionService(NamedParameterJdbcTemplate jdbc, CampaignGenerationService generationService) {
        this.jdbc = jdbc;
        this.generationService = generationService;
    }

    @Transactional
    public void operatorPause(UUID campaignId, UUID revisionId, String preState, AuthenticatedActor actor) {
        if (actor == null || !actor.authorities().contains(CampaignPermission.CAMPAIGN_PAUSE)) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "CAMPAIGN_PAUSE is required"
            );
        }
        persist("OPERATOR_PAUSE", campaignId, revisionId, "OPERATOR_PAUSE", preState);
        generationService.invalidateControlGeneration(campaignId);
    }

    @Transactional
    public void safetySuspend(UUID campaignId, UUID revisionId, String reasonCode, String preState) {
        persist("SAFETY_SUSPENSION", campaignId, revisionId, reasonCode == null ? "SAFETY_SUSPEND" : reasonCode, preState);
        generationService.invalidateControlGeneration(campaignId);
    }

    private void persist(String type, UUID campaignId, UUID revisionId, String reason, String preState) {
        jdbc.update(
                """
                INSERT INTO campaign_suspension
                    (suspension_id, campaign_id, revision_id, suspension_type, state, reason_code, pre_state, created_at)
                VALUES (:id, :campaignId, :revisionId, :type, 'ACTIVE', :reason, :preState, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("campaignId", campaignId)
                        .addValue("revisionId", revisionId)
                        .addValue("type", type)
                        .addValue("reason", reason)
                        .addValue("preState", preState)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
    }
}
