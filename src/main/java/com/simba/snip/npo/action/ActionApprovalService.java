package com.simba.snip.npo.action;

import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainRules;
import com.simba.snip.npo.persist.ActionApprovalEntity;
import com.simba.snip.npo.persist.ActionApprovalRepository;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.persist.ProposedActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ActionApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ActionApprovalService.class);

    private final ProposedActionRepository actionRepository;
    private final ActionApprovalRepository approvalRepository;
    private final ActionAuditService auditService;
    private final ActionMetrics metrics;

    public ActionApprovalService(
            ProposedActionRepository actionRepository,
            ActionApprovalRepository approvalRepository,
            ActionAuditService auditService,
            ActionMetrics metrics
    ) {
        this.actionRepository = actionRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional
    public ProposedActionEntity decide(UUID actionId, ApprovalDecision decision, String decidedBy, String comment) {
        ProposedActionEntity action = actionRepository.findById(actionId)
                .orElseThrow(() -> new DomainNotFoundException("action", actionId.toString()));
        ActionLifecycle.requireApprovable(
                ActionStatus.valueOf(action.getStatus()),
                PolicyOutcome.valueOf(action.getPolicyDecision())
        );
        if (approvalRepository.findByActionId(actionId).isPresent()) {
            throw new com.simba.snip.npo.domain.DomainConflictException("action already has an approval decision");
        }
        String actor = DomainRules.requireDomainId(decidedBy, "decidedBy");
        Instant now = Instant.now();
        approvalRepository.save(ActionApprovalEntity.create(
                UUID.randomUUID(),
                actionId,
                decision.name(),
                actor,
                now,
                comment
        ));
        if (decision == ApprovalDecision.APPROVED) {
            action.setStatus(ActionStatus.APPROVED.name());
            metrics.incrementApproved();
            auditService.append(actionId, AuditEventType.ACTION_APPROVED, actor, comment == null ? "" : comment);
            log.info("actionsApproved=1 actionId={} decidedBy={}", actionId, actor);
        } else {
            action.setStatus(ActionStatus.REJECTED.name());
            metrics.incrementRejected();
            auditService.append(actionId, AuditEventType.ACTION_REJECTED, actor, comment == null ? "" : comment);
            log.info("actionsRejected=1 actionId={} decidedBy={}", actionId, actor);
        }
        return actionRepository.save(action);
    }
}
