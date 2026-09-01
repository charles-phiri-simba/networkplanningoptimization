package com.simba.snip.npo.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.api.ActionDetailDto;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.ActionApprovalRepository;
import com.simba.snip.npo.persist.ActionResultRepository;
import com.simba.snip.npo.persist.PolicyDecisionRepository;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.persist.ProposedActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActionQueryService {

    private final ProposedActionRepository actionRepository;
    private final PolicyDecisionRepository policyDecisionRepository;
    private final ActionApprovalRepository approvalRepository;
    private final ActionResultRepository resultRepository;
    private final ActionAuditService auditService;
    private final ObjectMapper objectMapper;

    public ActionQueryService(
            ProposedActionRepository actionRepository,
            PolicyDecisionRepository policyDecisionRepository,
            ActionApprovalRepository approvalRepository,
            ActionResultRepository resultRepository,
            ActionAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.actionRepository = actionRepository;
        this.policyDecisionRepository = policyDecisionRepository;
        this.approvalRepository = approvalRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ActionDetailDto> list() {
        return actionRepository.findAllByOrderByProposedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ActionDetailDto require(UUID actionId) {
        ProposedActionEntity action = actionRepository.findById(actionId)
                .orElseThrow(() -> new DomainNotFoundException("action", actionId.toString()));
        return toDto(action);
    }

    private ActionDetailDto toDto(ProposedActionEntity action) {
        return ActionMapper.toDto(
                action,
                policyDecisionRepository.findByActionId(action.getId()).orElse(null),
                approvalRepository.findByActionId(action.getId()).orElse(null),
                resultRepository.findByActionId(action.getId()).orElse(null),
                auditService.list(action.getId()),
                objectMapper
        );
    }
}
