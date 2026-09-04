package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.TransportCertificationState;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import com.simba.snip.npo.vendorcertification.service.TransportCertificationLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.Clock;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Phase17LifecycleTest {

    @Test
    void unknownAndRevokedTransitionsDenied() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        TransportCertificationLifecycleService service = new TransportCertificationLifecycleService(
                jdbc, new Phase17SeparationOfDutiesPolicy(), Clock.systemUTC());
        assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.REVOKED,
                TransportCertificationState.PRODUCTION_REGISTERED,
                "human-1",
                Set.of(Phase17CertificationPermission.TARGET_REACTIVATE),
                "sec-1"));
        assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.DRAFT,
                TransportCertificationState.PRODUCTION_REGISTERED,
                "human-1",
                Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY),
                "sec-1"));
        Phase17Exception missingSecurity = assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.LAB_CERTIFICATION_PENDING,
                TransportCertificationState.LAB_CERTIFIED,
                "human-1",
                Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY, Phase17CertificationPermission.CAPABILITY_CERTIFY),
                "sec-1"));
        assertEquals(Phase17DenialCode.P17_SOD_VIOLATION, missingSecurity.denialCode());
        service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.LAB_CERTIFICATION_PENDING,
                TransportCertificationState.LAB_CERTIFIED,
                "human-1",
                Set.of(
                        Phase17CertificationPermission.TRANSPORT_CERTIFY,
                        Phase17CertificationPermission.CAPABILITY_CERTIFY,
                        Phase17CertificationPermission.SECURITY_CERTIFY),
                "sec-1");
        assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.REVOKED,
                "human-1",
                Set.of(),
                "sec-1"));
        assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.EXPIRED,
                "human-1",
                Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY),
                "sec-1"));
        assertThrows(Phase17Exception.class, () -> service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.SUSPENDED,
                "human-1",
                Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY),
                "sec-1"));
        service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.EXPIRED,
                "snip.phase17.system-expiry",
                Set.of(Phase17CertificationPermission.SYSTEM_EXPIRY),
                "sec-1");
        service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.SUSPENDED,
                "safety-1",
                Set.of(Phase17CertificationPermission.SYSTEM_SAFETY),
                "sec-1");
        service.transition(
                java.util.UUID.randomUUID(),
                TransportCertificationState.PRODUCTION_REGISTERED,
                TransportCertificationState.REVOKED,
                "human-1",
                Set.of(Phase17CertificationPermission.TRANSPORT_CERTIFY),
                "sec-1");
    }
}
