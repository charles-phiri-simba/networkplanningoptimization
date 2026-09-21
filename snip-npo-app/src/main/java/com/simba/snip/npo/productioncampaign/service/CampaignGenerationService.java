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
public class CampaignGenerationService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignGenerationService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long allocateReleaseGeneration(UUID campaignId) {
        lockGenerationRow("campaign_release_generation", campaignId);
        Long next = jdbc.query(
                """
                UPDATE campaign_release_generation
                   SET current_generation = current_generation + 1, updated_at = :now, version = version + 1
                 WHERE campaign_id = :id
                   AND current_generation < 9223372036854775807
                RETURNING current_generation
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("now", Timestamp.from(Instant.now())),
                rs -> rs.next() ? rs.getLong(1) : null
        );
        if (next == null) {
            throw new CampaignException(ProductionReasonCode.GENERATION_OVERFLOW, "release generation overflow or missing");
        }
        return next;
    }

    @Transactional
    public long invalidateControlGeneration(UUID campaignId) {
        lockGenerationRow("campaign_control_generation", campaignId);
        Long next = jdbc.query(
                """
                UPDATE campaign_control_generation
                   SET current_generation = current_generation + 1, updated_at = :now, version = version + 1
                 WHERE campaign_id = :id
                   AND current_generation < 9223372036854775807
                RETURNING current_generation
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("now", Timestamp.from(Instant.now())),
                rs -> rs.next() ? rs.getLong(1) : null
        );
        if (next == null) {
            throw new CampaignException(ProductionReasonCode.GENERATION_OVERFLOW, "control generation overflow or missing");
        }
        jdbc.update(
                """
                UPDATE campaign_cohort_release
                   SET state = 'STALE'
                 WHERE campaign_id = :id AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        return next;
    }

    public long currentReleaseGeneration(UUID campaignId) {
        Long value = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_release_generation WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                Long.class
        );
        return value == null ? 0L : value;
    }

    public long currentControlGeneration(UUID campaignId) {
        Long value = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_control_generation WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                Long.class
        );
        return value == null ? 1L : value;
    }

    private void lockGenerationRow(String table, UUID campaignId) {
        Long current = jdbc.queryForObject(
                "SELECT current_generation FROM " + table + " WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                Long.class
        );
        if (current == null) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, table + " row missing");
        }
        if (current == Long.MAX_VALUE) {
            throw new CampaignException(ProductionReasonCode.GENERATION_OVERFLOW, table + " overflow");
        }
    }
}
