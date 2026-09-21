package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignCohortReleaseService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignGenerationService generationService;
    private final CampaignItemLifecycleService itemLifecycle;

    public CampaignCohortReleaseService(
            NamedParameterJdbcTemplate jdbc,
            CampaignGenerationService generationService,
            CampaignItemLifecycleService itemLifecycle
    ) {
        this.generationService = generationService;
        this.itemLifecycle = itemLifecycle;
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID release(
            UUID campaignId,
            UUID revisionId,
            UUID cohortId,
            long fence,
            AuthenticatedActor releaser
    ) {
        Map<String, Object> campaign = jdbc.queryForMap(
                """
                SELECT fingerprint, current_revision_number, state, abort_state
                  FROM production_change_campaign WHERE campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        String campaignState = String.valueOf(campaign.get("state"));
        if ("SUSPENDED".equals(campaignState)
                || "STALE".equals(campaignState)
                || "ABORTED".equals(campaignState)
                || "ABORTED".equals(String.valueOf(campaign.get("abort_state")))) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "invalidated campaign cannot release a cohort"
            );
        }
        Integer authorizationGeneration = jdbc.queryForObject(
                """
                SELECT authorization_generation FROM campaign_revision WHERE revision_id = :id
                """,
                new MapSqlParameterSource("id", revisionId),
                Integer.class
        );
        List<Map<String, Object>> items = jdbc.query(
                """
                SELECT item_id, item_fingerprint FROM campaign_execution_item
                 WHERE cohort_id = :cohortId
                 ORDER BY item_sequence ASC
                """,
                new MapSqlParameterSource("cohortId", cohortId),
                (rs, rowNum) -> Map.of(
                        "item_id", rs.getObject("item_id").toString(),
                        "item_fingerprint", rs.getString("item_fingerprint")
                )
        );
        List<String> orderedIds = new ArrayList<>();
        List<String> orderedFingerprints = new ArrayList<>();
        for (Map<String, Object> item : items) {
            orderedIds.add(String.valueOf(item.get("item_id")));
            orderedFingerprints.add(String.valueOf(item.get("item_fingerprint")));
        }
        long control = generationService.currentControlGeneration(campaignId);
        long releaseGeneration = generationService.allocateReleaseGeneration(campaignId);
        Instant now = Instant.now();
        String fingerprint = CampaignReleaseFingerprintFactory.hash(CampaignReleaseFingerprintFactory.material(
                campaignId.toString(),
                ((Number) campaign.get("current_revision_number")).intValue(),
                String.valueOf(campaign.get("fingerprint")),
                cohortId.toString(),
                orderedIds,
                orderedFingerprints,
                releaseGeneration,
                control,
                fence,
                latestHealthDigest(campaignId, "network_observation_health"),
                latestHealthDigest(campaignId, "operational_safety_health"),
                authorizationGeneration == null ? 0 : authorizationGeneration,
                releaser.actorId(),
                now
        ));
        UUID releaseId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO campaign_cohort_release (
                    release_id, campaign_id, revision_id, cohort_id, release_generation, control_generation,
                    campaign_fence, release_fingerprint, state, releaser_principal_id, released_at)
                VALUES (
                    :releaseId, :campaignId, :revisionId, :cohortId, :releaseGeneration, :control,
                    :fence, :fingerprint, 'ACTIVE', :actor, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("releaseId", releaseId)
                        .addValue("campaignId", campaignId)
                        .addValue("revisionId", revisionId)
                        .addValue("cohortId", cohortId)
                        .addValue("releaseGeneration", releaseGeneration)
                        .addValue("control", control)
                        .addValue("fence", fence)
                        .addValue("fingerprint", fingerprint)
                        .addValue("actor", releaser.actorId())
                        .addValue("now", Timestamp.from(now))
        );
        java.util.List<UUID> planned = jdbc.query(
                "SELECT item_id FROM campaign_execution_item WHERE cohort_id = :cohortId AND state = 'PLANNED' ORDER BY item_sequence",
                new MapSqlParameterSource("cohortId", cohortId),
                (rs, rowNum) -> (UUID) rs.getObject("item_id")
        );
        for (UUID itemId : planned) {
            itemLifecycle.transition(itemId, "PLANNED", "CURRENT_EXACT_COHORT_RELEASE");
        }
        jdbc.update(
                """
                UPDATE campaign_cohort
                   SET state = 'RELEASED', updated_at = :now, version = version + 1
                 WHERE cohort_id = :cohortId
                """,
                new MapSqlParameterSource().addValue("cohortId", cohortId).addValue("now", Timestamp.from(now))
        );
        return releaseId;
    }

    public void assertCurrentRelease(UUID campaignId, long releaseGeneration) {
        Long current = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_release_generation WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                Long.class
        );
        if (current == null || current != releaseGeneration) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "stale release generation cannot be resurrected"
            );
        }
        Integer active = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND release_generation = :gen AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("gen", releaseGeneration),
                Integer.class
        );
        if (active == null || active != 1) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "release is not the current ACTIVE generation"
            );
        }
    }

    private String latestHealthDigest(UUID campaignId, String column) {
        try {
            String value = jdbc.queryForObject(
                    "SELECT " + column + " FROM campaign_observation_boundary WHERE campaign_id = :id ORDER BY created_at DESC LIMIT 1",
                    new MapSqlParameterSource("id", campaignId),
                    String.class
            );
            if (value == null || value.isBlank()) {
                return CampaignReleaseFingerprintFactory.NONE_DIGEST;
            }
            return com.simba.snip.npo.productionchange.protocol.Sha256Hex.hash(
                    "{\"column\":\"" + column + "\",\"value\":\"" + value + "\"}"
            );
        } catch (Exception ex) {
            return CampaignReleaseFingerprintFactory.NONE_DIGEST;
        }
    }
}
