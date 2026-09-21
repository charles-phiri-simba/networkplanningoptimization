package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CampaignLeaseService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignLeaseService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long acquire(UUID campaignId, String holderId) {
        try {
            jdbc.queryForMap(
                    "SELECT campaign_id, fencing_token, status FROM campaign_lease WHERE campaign_id = :id FOR UPDATE",
                    new MapSqlParameterSource("id", campaignId)
            );
            Instant now = Instant.now();
            int updated = jdbc.update(
                    """
                    UPDATE campaign_lease
                       SET holder_id = :holder,
                           fencing_token = fencing_token + 1,
                           acquired_at = :now,
                           expires_at = :expires,
                           status = 'ACTIVE',
                           version = version + 1
                     WHERE campaign_id = :id
                       AND (status = 'NONE' OR status = 'EXPIRED' OR expires_at IS NULL OR expires_at <= :now)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", campaignId)
                            .addValue("holder", holderId)
                            .addValue("now", Timestamp.from(now))
                            .addValue("expires", Timestamp.from(now.plus(5, ChronoUnit.MINUTES)))
            );
            if (updated != 1) {
                throw new CampaignException(
                        ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE,
                        "campaign orchestration lease is held or unavailable"
                );
            }
            Long fence = jdbc.queryForObject(
                    "SELECT fencing_token FROM campaign_lease WHERE campaign_id = :id",
                    new MapSqlParameterSource("id", campaignId),
                    Long.class
            );
            return fence == null ? 0L : fence;
        } catch (CampaignException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE,
                    "campaign lease lock timed out or database unavailable",
                    ex
            );
        }
    }

    public long currentFence(UUID campaignId) {
        Long fence = jdbc.queryForObject(
                "SELECT fencing_token FROM campaign_lease WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                Long.class
        );
        return fence == null ? 0L : fence;
    }

    public void assertCurrent(UUID campaignId, long expectedFence) {
        var row = jdbc.queryForMap(
                "SELECT fencing_token, status, expires_at FROM campaign_lease WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId)
        );
        long fence = row.get("fencing_token") == null ? 0L : ((Number) row.get("fencing_token")).longValue();
        String status = String.valueOf(row.get("status"));
        if (fence != expectedFence || !"ACTIVE".equals(status)) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE,
                    "campaign fencing token is stale or lease is not active"
            );
        }
    }
}
