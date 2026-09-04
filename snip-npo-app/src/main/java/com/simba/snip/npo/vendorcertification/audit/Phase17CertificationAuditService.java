package com.simba.snip.npo.vendorcertification.audit;

import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
public class Phase17CertificationAuditService {

    public static final String GENESIS = Sha256Hex.hash("SNIP-PHASE17-CERTIFICATION-AUDIT-GENESIS-v1");

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public Phase17CertificationAuditService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public void append(String subjectType, String subjectId, String subjectVersionId, String eventType, String actor, String payload) {
        if (payload != null && (payload.toLowerCase().contains("secret") || payload.contains("BEGIN CERTIFICATE"))) {
            throw new IllegalArgumentException("secrets forbidden in audit");
        }
        Long last = jdbc.query(
                "SELECT sequence_number, event_hash FROM phase17_certification_audit_event "
                        + "WHERE subject_type = :type AND subject_id = :id ORDER BY sequence_number DESC LIMIT 1",
                new MapSqlParameterSource().addValue("type", subjectType).addValue("id", subjectId),
                rs -> rs.next() ? rs.getLong("sequence_number") : null
        );
        String previous = last == null ? GENESIS : jdbc.query(
                "SELECT event_hash FROM phase17_certification_audit_event "
                        + "WHERE subject_type = :type AND subject_id = :id ORDER BY sequence_number DESC LIMIT 1",
                new MapSqlParameterSource().addValue("type", subjectType).addValue("id", subjectId),
                rs -> {
                    rs.next();
                    return rs.getString("event_hash");
                }
        );
        long seq = last == null ? 1L : last + 1;
        String eventHash = Sha256Hex.hash(previous + "|" + seq + "|" + eventType + "|" + (payload == null ? "" : payload));
        jdbc.update(
                "INSERT INTO phase17_certification_audit_event ("
                        + "event_id, subject_type, subject_id, subject_version_id, event_type, actor_principal_id, "
                        + "sequence_number, previous_event_hash, event_hash, payload_canonical, created_at) "
                        + "VALUES (:id,:type,:sid,:vid,:event,:actor,:seq,:prev,:hash,:payload,:created)",
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("type", subjectType)
                        .addValue("sid", subjectId)
                        .addValue("vid", subjectVersionId)
                        .addValue("event", eventType)
                        .addValue("actor", actor)
                        .addValue("seq", seq)
                        .addValue("prev", previous)
                        .addValue("hash", eventHash)
                        .addValue("payload", payload == null ? "{}" : payload)
                        .addValue("created", Timestamp.from(clock.instant()))
        );
    }
}
