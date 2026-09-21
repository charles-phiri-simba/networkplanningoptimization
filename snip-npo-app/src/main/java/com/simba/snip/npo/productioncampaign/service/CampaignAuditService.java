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
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignAuditService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignAuditService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void append(UUID campaignId, String eventType, String actorPrincipalId, Map<String, Object> payload) {
        jdbc.queryForObject(
                "SELECT campaign_id FROM production_change_campaign WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
        Integer latest = jdbc.query(
                """
                SELECT sequence, event_hash FROM campaign_audit_event
                 WHERE campaign_id = :id
                 ORDER BY sequence DESC
                 LIMIT 1
                 FOR UPDATE
                """,
                new MapSqlParameterSource("id", campaignId),
                rs -> rs.next() ? rs.getInt("sequence") : null
        );
        int next = latest == null ? 1 : latest + 1;
        String previous = latest == null
                ? Sha256Hex.hash("SNIP-PHASE18-CAMPAIGN-AUDIT-GENESIS-v1")
                : jdbc.queryForObject(
                "SELECT event_hash FROM campaign_audit_event WHERE campaign_id = :id AND sequence = :seq",
                new MapSqlParameterSource().addValue("id", campaignId).addValue("seq", latest),
                String.class
        );
        Instant now = Instant.now();
        String payloadJson = CanonicalJson.serialize(payload);
        String payloadDigest = Sha256Hex.hash(payloadJson);
        Map<String, Object> chain = new java.util.LinkedHashMap<>();
        chain.put("campaignId", campaignId.toString());
        chain.put("sequence", next);
        chain.put("previousEventHash", previous);
        chain.put("eventType", eventType);
        chain.put("actorPrincipalId", actorPrincipalId);
        chain.put("eventAt", now.toString());
        chain.put("payloadDigest", payloadDigest);
        String eventHash = Sha256Hex.hash(CanonicalJson.serialize(chain));
        jdbc.update(
                """
                INSERT INTO campaign_audit_event
                    (event_id, campaign_id, sequence, previous_event_hash, event_hash, event_type,
                     actor_principal_id, event_at, payload_digest, payload_json)
                VALUES
                    (:eventId, :campaignId, :sequence, :previous, :hash, :type, :actor, :at, :digest, :payload)
                """,
                new MapSqlParameterSource()
                        .addValue("eventId", UUID.randomUUID())
                        .addValue("campaignId", campaignId)
                        .addValue("sequence", next)
                        .addValue("previous", previous)
                        .addValue("hash", eventHash)
                        .addValue("type", eventType)
                        .addValue("actor", actorPrincipalId)
                        .addValue("at", Timestamp.from(now))
                        .addValue("digest", payloadDigest)
                        .addValue("payload", payloadJson)
        );
        if (eventHash == null || eventHash.length() != 64) {
            throw new CampaignException(ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID, "campaign audit hash invalid");
        }
    }
}
