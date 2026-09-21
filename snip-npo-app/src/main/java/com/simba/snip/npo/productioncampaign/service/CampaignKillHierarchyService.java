package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Kill hierarchy: inherited global gateway kill → target → campaign → cohort → item.
 * Abort means no further forward mutations; it is not automatic rollback.
 */
@Service
public class CampaignKillHierarchyService {

    private static final Set<String> ITEM_BLOCKED = Set.of("BLOCKED", "STALE", "SUSPENDED", "ABORTED");

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignKillHierarchyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void assertSendAllowed(UUID campaignId, UUID cohortId, UUID itemId, String targetId) {
        assertTargetEligible(targetId);
        assertCampaignNotAborted(campaignId);
        assertCampaignCurrent(campaignId);
        if (cohortId != null) {
            assertCohortCurrent(cohortId);
        }
        if (itemId != null) {
            assertItemCurrent(itemId);
        }
    }

    public void assertCampaignNotAborted(UUID campaignId) {
        Integer aborted = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_change_campaign
                 WHERE campaign_id = :id AND (abort_state = 'ABORTED' OR state = 'ABORTED')
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (aborted != null && aborted > 0) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "campaign abort denies further forward mutations"
            );
        }
    }

    public void assertCampaignCurrent(UUID campaignId) {
        Integer off = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_change_campaign
                 WHERE campaign_id = :id AND (enabled = FALSE OR state IN ('SUSPENDED', 'STALE', 'EXPIRED'))
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (off != null && off > 0) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign currentness kill denies send");
        }
    }

    public void assertCohortCurrent(UUID cohortId) {
        Integer off = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort
                 WHERE cohort_id = :id AND state IN ('STALE', 'SUSPENDED', 'BLOCKED')
                """,
                new MapSqlParameterSource("id", cohortId),
                Integer.class
        );
        if (off != null && off > 0) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "cohort kill denies send");
        }
    }

    public void assertItemCurrent(UUID itemId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                String.class
        );
        if (state != null && ITEM_BLOCKED.contains(state)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "item kill denies send: " + state);
        }
    }

    public void assertTargetEligible(String targetId) {
        Integer targetOff = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_network_target
                 WHERE target_id = :id AND (enabled = FALSE OR target_state IN ('SUSPENDED', 'DISABLED'))
                """,
                new MapSqlParameterSource("id", targetId),
                Integer.class
        );
        if (targetOff != null && targetOff > 0) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_TARGET_DISABLED,
                    "target kill denies campaign send"
            );
        }
    }
}
