package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.CertificationCurrentnessSnapshot;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable PostgreSQL currentness reader. Not a local-memory authority.
 * Fail closed when the authority is unavailable.
 */
@Component
public class ProductionCertificationAuthority {

    private static final String CURRENTNESS_SQL = """
            SELECT
              tc.production_target_id,
              pto.onboarding_version_id,
              tcb.bundle_version_id,
              tcb.interface_definition_version_id,
              vid.status AS interface_status,
              vid.documentation_status,
              via.approval_status,
              tcb.transport_profile_version_id,
              tcb.artifact_digest,
              tcb.capability_cert_version_id,
              tcb.security_cert_version_id,
              tcb.credential_profile_version_id,
              tcb.tls_profile_version_id,
              tcb.network_policy_profile_version_id,
              tcb.endpoint_profile_version_id,
              ptov.vendor_software_version,
              tcb.vendor_version_predicate,
              vth.health_state,
              tc.status AS target_cert_status,
              pto.status AS pto_status,
              tc.expires_at,
              tcb.status AS bundle_status,
              trc.state AS transport_cert_state,
              pep.approved_fqdn,
              pep.approved_port,
              pep.tls_server_identity,
              pep.network_domain,
              pep.route_zone_id,
              pep.vendor AS endpoint_vendor,
              pep.platform AS endpoint_platform,
              pep.version_no AS endpoint_version_no,
              ptp.hostname_verification_required,
              ptp.status AS tls_status,
              pnpp.status AS network_status,
              pcp.production_target_id AS credential_target_id,
              pcp.status AS credential_status,
              vcc.object_type,
              vcc.parameter,
              vwtp.atomic_certified,
              vwtp.expected_state_strategy,
              vwtp.status AS transport_profile_status,
              vvc.status AS compatibility_status
            FROM production_target_certification tc
            JOIN production_target_onboarding pto ON pto.production_target_id = tc.production_target_id
            JOIN production_target_onboarding_version ptov ON ptov.onboarding_version_id = tc.onboarding_version_id
            JOIN transport_certification_bundle tcb ON tcb.bundle_version_id = tc.bundle_version_id
            JOIN vendor_interface_definition vid ON vid.interface_definition_version_id = tcb.interface_definition_version_id
            JOIN vendor_interface_approval via ON via.approval_id = tcb.interface_approval_id
            LEFT JOIN vendor_transport_health vth
              ON vth.production_target_id = tc.production_target_id
             AND vth.transport_profile_version_id = tcb.transport_profile_version_id
            JOIN transport_certification trc ON trc.transport_profile_version_id = tcb.transport_profile_version_id
            LEFT JOIN production_endpoint_profile pep ON pep.endpoint_profile_version_id = tcb.endpoint_profile_version_id
            JOIN production_tls_profile ptp ON ptp.tls_profile_version_id = tcb.tls_profile_version_id
            JOIN production_network_policy_profile pnpp ON pnpp.network_policy_profile_version_id = tcb.network_policy_profile_version_id
            JOIN production_credential_profile pcp ON pcp.credential_profile_version_id = tcb.credential_profile_version_id
            JOIN vendor_capability_certification vcc ON vcc.capability_cert_version_id = tcb.capability_cert_version_id
            JOIN vendor_write_transport_profile vwtp ON vwtp.transport_profile_version_id = tcb.transport_profile_version_id
            LEFT JOIN vendor_version_compatibility vvc
              ON vvc.transport_profile_version_id = tcb.transport_profile_version_id
             AND vvc.status = 'ACTIVE'
            WHERE tc.production_target_id = :targetId
              AND tc.status = 'CURRENT'
              AND tcb.status = 'ACTIVE'
            ORDER BY tc.certified_at DESC
            LIMIT 1
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public ProductionCertificationAuthority(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Optional<ResolvedCurrentness> readCurrent(String productionTargetId) {
        if (productionTargetId == null || productionTargetId.isBlank()) {
            return Optional.empty();
        }
        try {
            return jdbc.query(CURRENTNESS_SQL, new MapSqlParameterSource("targetId", productionTargetId), rs -> {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Instant expires = rs.getTimestamp("expires_at") == null
                        ? null
                        : rs.getTimestamp("expires_at").toInstant();
                CertificationCurrentnessSnapshot snapshot = new CertificationCurrentnessSnapshot(
                        rs.getString("production_target_id"),
                        uuid(rs.getObject("onboarding_version_id")),
                        uuid(rs.getObject("bundle_version_id")),
                        uuid(rs.getObject("interface_definition_version_id")),
                        rs.getString("interface_status"),
                        rs.getString("approval_status"),
                        uuid(rs.getObject("transport_profile_version_id")),
                        rs.getString("artifact_digest"),
                        uuid(rs.getObject("capability_cert_version_id")),
                        uuid(rs.getObject("security_cert_version_id")),
                        uuid(rs.getObject("credential_profile_version_id")),
                        uuid(rs.getObject("tls_profile_version_id")),
                        uuid(rs.getObject("network_policy_profile_version_id")),
                        uuid(rs.getObject("endpoint_profile_version_id")),
                        rs.getString("vendor_software_version"),
                        rs.getString("vendor_version_predicate"),
                        rs.getString("health_state"),
                        rs.getString("target_cert_status"),
                        expires,
                        clock.instant(),
                        1L
                );
                return Optional.of(new ResolvedCurrentness(
                        snapshot,
                        rs.getString("bundle_status"),
                        rs.getString("transport_cert_state"),
                        rs.getString("approved_fqdn"),
                        rs.getObject("approved_port") == null ? null : rs.getInt("approved_port"),
                        rs.getString("tls_server_identity"),
                        rs.getString("network_domain"),
                        rs.getString("route_zone_id"),
                        rs.getString("endpoint_vendor"),
                        rs.getString("endpoint_platform"),
                        rs.getObject("endpoint_version_no") == null ? null : rs.getInt("endpoint_version_no"),
                        rs.getObject("hostname_verification_required") != null
                                && rs.getBoolean("hostname_verification_required"),
                        rs.getString("tls_status"),
                        rs.getString("network_status"),
                        rs.getString("credential_target_id"),
                        rs.getString("credential_status"),
                        rs.getString("object_type"),
                        rs.getString("parameter"),
                        rs.getBoolean("atomic_certified"),
                        rs.getString("expected_state_strategy"),
                        rs.getString("transport_profile_status"),
                        rs.getString("compatibility_status"),
                        rs.getString("pto_status") == null ? snapshot.targetCertificationStatus() : rs.getString("pto_status"),
                        rs.getString("documentation_status")
                ));
            });
        } catch (DataAccessException ex) {
            throw new Phase17AuthorityUnavailableException("durable certification authority unavailable", ex);
        }
    }

    private static UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID id) {
            return id;
        }
        return UUID.fromString(value.toString());
    }

    public record ResolvedCurrentness(
            CertificationCurrentnessSnapshot snapshot,
            String bundleStatus,
            String transportCertState,
            String approvedFqdn,
            Integer approvedPort,
            String tlsServerIdentity,
            String networkDomain,
            String routeZoneId,
            String endpointVendor,
            String endpointPlatform,
            Integer endpointVersionNo,
            boolean hostnameVerificationRequired,
            String tlsStatus,
            String networkStatus,
            String credentialTargetId,
            String credentialStatus,
            String objectType,
            String parameter,
            boolean atomicCertified,
            String expectedStateStrategy,
            String transportProfileStatus,
            String compatibilityStatus,
            String onboardingStatus,
            String documentationStatus
    ) {
    }
}
