package com.simba.snip.npo.productioncampaign.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Durable campaign consequence of authoritative P17 invalidation. Gateway remains
 * independently deny-closed. Does not mutate consumed grants or MAY_HAVE_SENT
 * outcome resolution. Idempotent: already-suspended campaigns with no unused ACTIVE
 * release are left unchanged.
 */
@Service
public class CampaignP17InvalidationCascade {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignGenerationService generationService;
    private final CampaignAuditService auditService;

    public CampaignP17InvalidationCascade(
            NamedParameterJdbcTemplate jdbc,
            CampaignGenerationService generationService,
            CampaignAuditService auditService
    ) {
        this.jdbc = jdbc;
        this.generationService = generationService;
        this.auditService = auditService;
    }

    @Transactional
    public void cascadeForTarget(String productionTargetId) {
        if (productionTargetId == null || productionTargetId.isBlank()) {
            return;
        }
        List<UUID> campaignIds = jdbc.query(
                """
                SELECT campaign_id FROM production_change_campaign
                 WHERE production_target_id = :target
                   AND abort_state IS DISTINCT FROM 'ABORTED'
                   AND state NOT IN ('COMPLETED', 'ABORTED', 'EXPIRED')
                 ORDER BY campaign_id
                 FOR UPDATE
                """,
                new MapSqlParameterSource("target", productionTargetId),
                (rs, rowNum) -> (UUID) rs.getObject("campaign_id")
        );
        Instant now = Instant.now();
        for (UUID campaignId : campaignIds) {
            Integer generationPresent = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campaign_control_generation WHERE campaign_id = :id",
                    new MapSqlParameterSource("id", campaignId),
                    Integer.class
            );
            if (generationPresent == null || generationPresent < 1) {
                continue;
            }
            applyOnce(campaignId, now);
        }
    }

    private void applyOnce(UUID campaignId, Instant now) {
        String state = jdbc.queryForObject(
                "SELECT state FROM production_change_campaign WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        Integer activeUnused = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if ("SUSPENDED".equals(state) && (activeUnused == null || activeUnused == 0)) {
            return;
        }
        UUID revisionId = jdbc.queryForObject(
                """
                SELECT r.revision_id
                  FROM production_change_campaign c
                  JOIN campaign_revision r ON r.campaign_id = c.campaign_id
                   AND r.revision_number = c.current_revision_number
                 WHERE c.campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
        generationService.invalidateControlGeneration(campaignId);
        if (!"SUSPENDED".equals(state) && !"STALE".equals(state)) {
            jdbc.update(
                    """
                    INSERT INTO campaign_suspension
                        (suspension_id, campaign_id, revision_id, suspension_type, state, reason_code, pre_state, created_at)
                    VALUES (:sid, :campaignId, :revisionId, 'SAFETY_SUSPENSION', 'ACTIVE', 'P17_INVALIDATION', :pre, :now)
                    """,
                    new MapSqlParameterSource()
                            .addValue("sid", UUID.randomUUID())
                            .addValue("campaignId", campaignId)
                            .addValue("revisionId", revisionId)
                            .addValue("pre", state)
                            .addValue("now", Timestamp.from(now))
            );
            jdbc.update(
                    """
                    UPDATE production_change_campaign
                       SET state = 'SUSPENDED', updated_at = :now, version = version + 1
                     WHERE campaign_id = :id
                       AND state NOT IN ('COMPLETED', 'ABORTED', 'EXPIRED')
                    """,
                    new MapSqlParameterSource().addValue("id", campaignId).addValue("now", Timestamp.from(now))
            );
        }
        jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'STALE', updated_at = :now
                 WHERE campaign_id = :id
                   AND state IN ('REQUESTED', 'UNDER_REVIEW', 'REVIEWED', 'AUTHORIZED')
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("now", Timestamp.from(now))
        );
        auditService.append(campaignId, "CAMPAIGN_SAFETY_SUSPENDED", "SYSTEM:P17_INVALIDATION", java.util.Map.of(
                "source", "P17",
                "preState", String.valueOf(state)
        ));
    }
}
