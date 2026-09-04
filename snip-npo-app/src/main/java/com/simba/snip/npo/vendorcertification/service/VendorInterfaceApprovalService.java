package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.audit.Phase17CertificationAuditService;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
public class VendorInterfaceApprovalService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Phase17SeparationOfDutiesPolicy sod;
    private final CertificationInvalidationService invalidation;
    private final Phase17CertificationAuditService audit;
    private final Clock clock;

    public VendorInterfaceApprovalService(
            NamedParameterJdbcTemplate jdbc,
            Phase17SeparationOfDutiesPolicy sod,
            CertificationInvalidationService invalidation,
            Phase17CertificationAuditService audit,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.sod = sod;
        this.invalidation = invalidation;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void revoke(UUID approvalId, String actor, String productionTargetId) {
        sod.requirePrincipal(actor, "revoker");
        sod.denyAgentOrMcp(actor);
        sod.requirePermission(Phase17CertificationPermission.VENDOR_INTERFACE_REVIEW,
                Phase17CertificationPermission.VENDOR_INTERFACE_REVIEW);
        invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.APPROVAL_REVOKED,
                "vendor_interface_approval",
                approvalId.toString(),
                approvalId,
                "REVOKED",
                clock.instant(),
                productionTargetId,
                ActorPrincipal.of(actor)
        ));
        audit.append("APPROVAL", approvalId.toString(), approvalId.toString(),
                "INTERFACE_APPROVED".equals("x") ? "INTERFACE_APPROVED" : "DOCUMENTATION_APPROVAL_REVOKED",
                actor, "{}");
    }
}
