package com.simba.snip.npo.productioncampaign.service;

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
public class ForwardProgressionClosureService {

    private final NamedParameterJdbcTemplate jdbc;

    public ForwardProgressionClosureService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void determineRecoveryRequired(UUID campaignId, UUID itemId) {
        int closed = jdbc.update(
                """
                UPDATE campaign_forward_progression_closure
                   SET state = 'CLOSED', closed_at = :now, version = version + 1
                 WHERE campaign_id = :campaignId
                   AND state = 'OPEN'
                """,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        if (closed != 1) {
            throw new CampaignException(
                    ProductionReasonCode.FORWARD_PROGRESSION_CLOSED,
                    "forward progression is already closed or unavailable"
            );
        }
        int items = jdbc.update(
                """
                UPDATE campaign_execution_item
                   SET state = 'RECOVERY_REQUIRED', updated_at = :now, version = version + 1
                 WHERE item_id = :itemId
                   AND state NOT IN ('COMPLETED', 'NOT_SENT', 'PLANNED', 'RELEASED')
                """,
                new MapSqlParameterSource()
                        .addValue("itemId", itemId)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        if (items != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "RECOVERY_REQUIRED requires a governed non-terminal item row"
            );
        }
        jdbc.update(
                """
                UPDATE production_change_campaign
                   SET state = 'RECOVERY_REQUIRED', updated_at = :now, version = version + 1
                 WHERE campaign_id = :campaignId
                """,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        jdbc.update(
                """
                INSERT INTO campaign_recovery (campaign_id, revision_id, state, version)
                SELECT c.campaign_id, r.revision_id, 'REQUIRED', 0
                  FROM production_change_campaign c
                  JOIN campaign_revision r ON r.campaign_id = c.campaign_id
                   AND r.revision_number = c.current_revision_number
                 WHERE c.campaign_id = :campaignId
                ON CONFLICT (campaign_id) DO UPDATE
                   SET state = 'REQUIRED'
                """,
                new MapSqlParameterSource("campaignId", campaignId)
        );
    }

    public void assertOpen(UUID campaignId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = :campaignId",
                new MapSqlParameterSource("campaignId", campaignId),
                String.class
        );
        if (!"OPEN".equals(state)) {
            throw new CampaignException(
                    ProductionReasonCode.FORWARD_PROGRESSION_CLOSED,
                    "forward-progressing action is forbidden after closure"
            );
        }
    }

    public void assertClosed(UUID campaignId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = :campaignId",
                new MapSqlParameterSource("campaignId", campaignId),
                String.class
        );
        if (!"CLOSED".equals(state)) {
            throw new CampaignException(
                    ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE,
                    "campaign recovery/rollback requires durable forward closure"
            );
        }
    }
}
