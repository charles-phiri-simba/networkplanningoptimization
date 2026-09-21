package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExternalInterferenceService {

    private final NamedParameterJdbcTemplate jdbc;

    public ExternalInterferenceService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Detect conflicting mutation evidence for the active observation boundary.
     * Same-value unrelated mutation still counts as interference.
     */
    public void detectForActiveBoundary(UUID campaignId, UUID itemId, String cellId, String parameter) {
        Map<String, Object> campaign = jdbc.queryForMap(
                "SELECT production_target_id FROM production_change_campaign WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId)
        );
        String targetId = String.valueOf(campaign.get("production_target_id"));
        Integer boundaryPresent = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_observation_boundary
                 WHERE campaign_id = :campaignId AND item_id = :itemId
                """,
                new MapSqlParameterSource().addValue("campaignId", campaignId).addValue("itemId", itemId),
                Integer.class
        );
        if (boundaryPresent == null || boundaryPresent < 1) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "active observation boundary is absent"
            );
        }
        List<Map<String, Object>> conflicts = jdbc.queryForList(
                """
                SELECT n.production_change_id, n.production_target_id, n.cell_id, n.parameter, n.desired_value
                  FROM production_network_change n
                  JOIN campaign_execution_item i ON i.item_id = :itemId
                  JOIN LATERAL (
                        SELECT vendor_verified_at, observation_start, observation_end
                          FROM campaign_observation_boundary
                         WHERE campaign_id = :campaignId AND item_id = :itemId
                         ORDER BY created_at DESC
                         LIMIT 1
                  ) b ON TRUE
                 WHERE n.cell_id = :cellId
                   AND n.parameter = :parameter
                   AND n.production_target_id = :targetId
                   AND n.updated_at >= COALESCE(b.vendor_verified_at, b.observation_start)
                   AND (b.observation_end IS NULL OR n.updated_at <= b.observation_end)
                   AND (i.production_change_id IS NULL OR n.production_change_id <> i.production_change_id)
                """,
                new MapSqlParameterSource()
                        .addValue("itemId", itemId)
                        .addValue("campaignId", campaignId)
                        .addValue("cellId", cellId)
                        .addValue("parameter", parameter)
                        .addValue("targetId", targetId)
        );
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        record(campaignId, itemId, cellId, parameter, conflicts.get(0));
    }

    public boolean isInterference(
            String observedTargetId,
            String observedCellId,
            String observedParameter,
            String boundaryTargetId,
            String boundaryCellId,
            String boundaryParameter
    ) {
        return boundaryTargetId.equals(observedTargetId)
                && boundaryCellId.equals(observedCellId)
                && boundaryParameter.equals(observedParameter);
    }

    @Transactional
    public void record(UUID campaignId, UUID itemId, String cellId, String parameter, Map<String, Object> evidence) {
        UUID revisionId = jdbc.queryForObject(
                "SELECT revision_id FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                UUID.class
        );
        Map<String, Object> digestMaterial = new LinkedHashMap<>();
        digestMaterial.put("campaignId", campaignId.toString());
        digestMaterial.put("itemId", itemId.toString());
        digestMaterial.put("cellId", cellId);
        digestMaterial.put("parameter", parameter);
        digestMaterial.put("evidence", evidence);
        String digest = Sha256Hex.hash(CanonicalJson.serialize(digestMaterial));
        jdbc.update(
                """
                INSERT INTO campaign_external_interference
                    (interference_id, campaign_id, revision_id, item_id, cell_id, parameter, detected_at, evidence_digest)
                VALUES (:id, :campaignId, :revisionId, :itemId, :cellId, :parameter, :now, :digest)
                """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("campaignId", campaignId)
                        .addValue("revisionId", revisionId)
                        .addValue("itemId", itemId)
                        .addValue("cellId", cellId)
                        .addValue("parameter", parameter)
                        .addValue("now", Timestamp.from(Instant.now()))
                        .addValue("digest", digest)
        );
        throw new CampaignException(
                ProductionReasonCode.EXTERNAL_INTERFERENCE_DETECTED,
                "conflicting external mutation invalidates campaign progression"
        );
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
