package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.TransportCertificationState;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TransportCertificationLifecycleService {

    private static final Map<String, Transition> TRANSITIONS = Map.ofEntries(
            Map.entry(key(null, TransportCertificationState.DRAFT),
                    new Transition(Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY), false)),
            Map.entry(key(TransportCertificationState.DRAFT, TransportCertificationState.INTERFACE_VERIFIED),
                    new Transition(Set.of(Phase17CertificationPermission.VENDOR_INTERFACE_REVIEW), false)),
            Map.entry(key(TransportCertificationState.INTERFACE_VERIFIED, TransportCertificationState.LAB_CERTIFICATION_PENDING),
                    new Transition(Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY), false)),
            Map.entry(key(TransportCertificationState.LAB_CERTIFICATION_PENDING, TransportCertificationState.LAB_CERTIFIED),
                    new Transition(Set.of(
                            Phase17CertificationPermission.TRANSPORT_CERTIFY,
                            Phase17CertificationPermission.CAPABILITY_CERTIFY,
                            Phase17CertificationPermission.SECURITY_CERTIFY), false)),
            Map.entry(key(TransportCertificationState.LAB_CERTIFIED, TransportCertificationState.PREPROD_CERTIFICATION_PENDING),
                    new Transition(Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY), false)),
            Map.entry(key(TransportCertificationState.PREPROD_CERTIFICATION_PENDING, TransportCertificationState.PREPROD_CERTIFIED),
                    new Transition(Set.of(
                            Phase17CertificationPermission.TRANSPORT_CERTIFY,
                            Phase17CertificationPermission.CAPABILITY_CERTIFY,
                            Phase17CertificationPermission.SECURITY_CERTIFY), true)),
            Map.entry(key(TransportCertificationState.PREPROD_CERTIFIED, TransportCertificationState.PRODUCTION_REGISTRATION_PENDING),
                    new Transition(Set.of(Phase17CertificationPermission.TARGET_ONBOARD_CREATE), false)),
            Map.entry(key(TransportCertificationState.PRODUCTION_REGISTRATION_PENDING, TransportCertificationState.PRODUCTION_REGISTERED),
                    new Transition(Set.of(
                            Phase17CertificationPermission.TARGET_ONBOARD_REVIEW,
                            Phase17CertificationPermission.TARGET_ONBOARD_APPROVE), true)),
            Map.entry(key(TransportCertificationState.SUSPENDED, TransportCertificationState.PRODUCTION_REGISTERED),
                    new Transition(Set.of(Phase17CertificationPermission.TARGET_REACTIVATE), false))
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final Phase17SeparationOfDutiesPolicy sod;
    private final Clock clock;

    public TransportCertificationLifecycleService(
            NamedParameterJdbcTemplate jdbc,
            Phase17SeparationOfDutiesPolicy sod,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.sod = sod;
        this.clock = clock;
    }

    @Transactional
    public void transition(
            UUID certificationId,
            TransportCertificationState from,
            TransportCertificationState to,
            String actorPrincipalId,
            Set<String> heldPermissions,
            String securityCertifierPrincipalId
    ) {
        sod.requirePrincipal(actorPrincipalId, "actor");
        sod.denyAgentOrMcp(actorPrincipalId);
        if (to == TransportCertificationState.REVOKED && from == TransportCertificationState.REVOKED) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "unknown transition");
        }
        if (from == TransportCertificationState.REVOKED) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "REVOKED cannot reactivate");
        }
        Transition transition = TRANSITIONS.get(key(from, to));
        if (to == TransportCertificationState.EXPIRED) {
            requireHeld(heldPermissions, Phase17CertificationPermission.SYSTEM_EXPIRY, "SYSTEM_EXPIRY required");
        } else if (to == TransportCertificationState.SUSPENDED) {
            if (!holds(heldPermissions, Phase17CertificationPermission.TARGET_SUSPEND)
                    && !holds(heldPermissions, Phase17CertificationPermission.SYSTEM_SAFETY)) {
                throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION,
                        "TARGET_SUSPEND or SYSTEM_SAFETY required");
            }
        } else if (to == TransportCertificationState.REVOKED) {
            if (!holds(heldPermissions, Phase17CertificationPermission.TRANSPORT_CERTIFY)
                    && !holds(heldPermissions, Phase17CertificationPermission.SECURITY_CERTIFY)
                    && !holds(heldPermissions, Phase17CertificationPermission.TARGET_ONBOARD_APPROVE)) {
                throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "REVOKED requires certify authority");
            }
        } else if (transition == null) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "unknown transition");
        }
        if (transition != null) {
            for (String required : transition.permissions()) {
                if (heldPermissions == null || !heldPermissions.contains(required)) {
                    throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "missing " + required);
                }
            }
            if (to == TransportCertificationState.LAB_CERTIFIED) {
                if (!heldPermissions.contains(Phase17CertificationPermission.SECURITY_CERTIFY)) {
                    throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "SECURITY_CERTIFY required");
                }
            }
            if (transition.securityIndependentOfTransport()
                    && to == TransportCertificationState.PREPROD_CERTIFIED) {
                sod.requirePrincipal(securityCertifierPrincipalId, "security certifier");
                sod.requireDistinct(actorPrincipalId, securityCertifierPrincipalId,
                        "SECURITY_CERTIFY must differ from TRANSPORT_CERTIFY");
            }
            if (to == TransportCertificationState.PRODUCTION_REGISTERED && from == TransportCertificationState.PRODUCTION_REGISTRATION_PENDING) {
                if (heldPermissions.contains(Phase17CertificationPermission.TRANSPORT_CERTIFY)
                        && heldPermissions.contains(Phase17CertificationPermission.SECURITY_CERTIFY)
                        && actorPrincipalId.equals(securityCertifierPrincipalId)) {
                    throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION,
                            "SECURITY_CERTIFY must differ from TRANSPORT_CERTIFY for PRODUCTION_REGISTERED");
                }
            }
        }
        Instant now = clock.instant();
        int rows = jdbc.update(
                "UPDATE transport_certification SET state = :to, updated_at = :now "
                        + "WHERE transport_certification_id = :id AND state = :from",
                new MapSqlParameterSource()
                        .addValue("to", to.name())
                        .addValue("now", java.sql.Timestamp.from(now))
                        .addValue("id", certificationId)
                        .addValue("from", from == null ? "DRAFT" : from.name())
        );
        if (from != null && rows != 1 && certificationId != null) {
            throw new Phase17Exception(Phase17DenialCode.P17_CERTIFICATION_STALE, "concurrent transition rejected");
        }
    }

    public boolean isProductionRegisteredExecutable() {
        return false;
    }

    private static String key(TransportCertificationState from, TransportCertificationState to) {
        return (from == null ? "NONE" : from.name()) + "->" + to.name();
    }

    private static boolean holds(Set<String> heldPermissions, String required) {
        return heldPermissions != null && heldPermissions.contains(required);
    }

    private static void requireHeld(Set<String> heldPermissions, String required, String message) {
        if (!holds(heldPermissions, required)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, message);
        }
    }

    private record Transition(Set<String> permissions, boolean securityIndependentOfTransport) {
    }
}
