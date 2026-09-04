package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Re-reads authoritative Phase 16 production authorization at the Phase 17 send boundary.
 * No persisted L4 flag.
 */
@Component
public class Phase16AuthorizationCurrentnessReader {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public Phase16AuthorizationCurrentnessReader(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public boolean isCurrent(
            UUID productionChangeId,
            UUID executionId,
            String expectedFingerprint,
            int expectedGeneration
    ) {
        if (productionChangeId == null || executionId == null
                || expectedFingerprint == null || expectedFingerprint.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(jdbc.query(
                    "SELECT a.status, a.production_fingerprint, a.authorization_generation, a.expires_at, "
                            + "c.phase15_execution_id "
                            + "FROM production_change_authorization a "
                            + "JOIN production_network_change c ON c.production_change_id = a.production_change_id "
                            + "WHERE a.production_change_id = :changeId "
                            + "ORDER BY a.authorized_at DESC LIMIT 1",
                    new MapSqlParameterSource("changeId", productionChangeId),
                    rs -> {
                        if (!rs.next()) {
                            return false;
                        }
                        if (!"ACTIVE".equals(rs.getString("status"))) {
                            return false;
                        }
                        if (!expectedFingerprint.equals(rs.getString("production_fingerprint"))) {
                            return false;
                        }
                        if (expectedGeneration != rs.getInt("authorization_generation")) {
                            return false;
                        }
                        Object execution = rs.getObject("phase15_execution_id");
                        if (execution == null || !executionId.equals(execution instanceof UUID u ? u : UUID.fromString(execution.toString()))) {
                            return false;
                        }
                        Instant expires = rs.getTimestamp("expires_at") == null
                                ? null
                                : rs.getTimestamp("expires_at").toInstant();
                        return expires == null || !expires.isBefore(clock.instant());
                    }
            ));
        } catch (RuntimeException ex) {
            throw new Phase17AuthorityUnavailableException("phase16 authorization authority unavailable", ex);
        }
    }
}
