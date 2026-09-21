package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class CampaignGovernanceIdempotencyService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignGovernanceIdempotencyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void record(String idempotencyKey, UUID campaignId, String command, String requestDigestMaterial) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CampaignException(
                    ProductionReasonCode.GOVERNANCE_IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key is required for authority-bearing campaign commands"
            );
        }
        String digest = Sha256Hex.hash(command + ":" + (requestDigestMaterial == null ? "" : requestDigestMaterial));
        try {
            jdbc.update(
                    """
                    INSERT INTO campaign_governance_idempotency
                        (idempotency_key, campaign_id, command, request_digest, created_at)
                    VALUES (:key, :campaignId, :command, :digest, :now)
                    """,
                    new MapSqlParameterSource()
                            .addValue("key", idempotencyKey)
                            .addValue("campaignId", campaignId)
                            .addValue("command", command)
                            .addValue("digest", digest)
                            .addValue("now", Timestamp.from(Instant.now()))
            );
        } catch (DuplicateKeyException ex) {
            Integer matches = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM campaign_governance_idempotency
                     WHERE idempotency_key = :key AND command = :command AND request_digest = :digest
                    """,
                    new MapSqlParameterSource()
                            .addValue("key", idempotencyKey)
                            .addValue("command", command)
                            .addValue("digest", digest),
                    Integer.class
            );
            if (matches == null || matches != 1) {
                throw new CampaignException(
                        ProductionReasonCode.GOVERNANCE_IDEMPOTENCY_CONFLICT,
                        "governance idempotency key reused with a different command payload",
                        ex
                );
            }
        }
    }
}
