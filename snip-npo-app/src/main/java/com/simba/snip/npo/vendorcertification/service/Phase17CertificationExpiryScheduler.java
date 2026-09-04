package com.simba.snip.npo.vendorcertification.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Administrative expiry detection only. Never mutates the vendor network.
 * Cascade writes go exclusively through {@link CertificationInvalidationService}.
 */
@Component
public class Phase17CertificationExpiryScheduler {

    public static final String SYSTEM_EXPIRY_ACTOR = "snip.phase17.system-expiry";

    private final NamedParameterJdbcTemplate jdbc;
    private final CertificationInvalidationService invalidation;
    private final Clock clock;

    public Phase17CertificationExpiryScheduler(
            NamedParameterJdbcTemplate jdbc,
            CertificationInvalidationService invalidation,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.invalidation = invalidation;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${snip.phase17.expiry-scan-ms:3600000}")
    public void markExpired() {
        Timestamp now = Timestamp.from(clock.instant());
        List<ExpiredRow> due = jdbc.query(
                "SELECT trc.transport_certification_id, p.certification_expiry, ptc.production_target_id "
                        + "FROM transport_certification trc "
                        + "JOIN vendor_write_transport_profile p "
                        + "  ON p.transport_profile_version_id = trc.transport_profile_version_id "
                        + "LEFT JOIN transport_certification_bundle tcb "
                        + "  ON tcb.transport_profile_version_id = trc.transport_profile_version_id "
                        + "LEFT JOIN production_target_certification ptc "
                        + "  ON ptc.bundle_version_id = tcb.bundle_version_id "
                        + "WHERE trc.state NOT IN ('EXPIRED','REVOKED','SUSPENDED') "
                        + "AND p.certification_expiry IS NOT NULL AND p.certification_expiry < :now "
                        + "ORDER BY trc.transport_certification_id, ptc.production_target_id",
                new MapSqlParameterSource("now", now),
                (rs, row) -> new ExpiredRow(
                        (UUID) rs.getObject("transport_certification_id"),
                        rs.getTimestamp("certification_expiry").toInstant(),
                        rs.getString("production_target_id")
                )
        );
        UUID previousCert = null;
        String previousTarget = null;
        for (ExpiredRow row : due) {
            if (row.certificationId().equals(previousCert)
                    && java.util.Objects.equals(row.productionTargetId(), previousTarget)) {
                continue;
            }
            previousCert = row.certificationId();
            previousTarget = row.productionTargetId();
            invalidation.invalidateExpired(row.certificationId(), row.productionTargetId(), row.expiry());
        }
    }

    private record ExpiredRow(UUID certificationId, java.time.Instant expiry, String productionTargetId) {
    }
}
