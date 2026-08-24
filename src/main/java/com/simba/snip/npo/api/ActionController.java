package com.simba.snip.npo.api;

import com.simba.snip.npo.action.ActionApprovalService;
import com.simba.snip.npo.action.ActionExecutionService;
import com.simba.snip.npo.action.ActionProposalService;
import com.simba.snip.npo.action.ActionQueryService;
import com.simba.snip.npo.action.ApprovalDecision;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ActionController {

    private final ActionProposalService proposalService;
    private final ActionApprovalService approvalService;
    private final ActionExecutionService executionService;
    private final ActionQueryService queryService;

    public ActionController(
            ActionProposalService proposalService,
            ActionApprovalService approvalService,
            ActionExecutionService executionService,
            ActionQueryService queryService
    ) {
        this.proposalService = proposalService;
        this.approvalService = approvalService;
        this.executionService = executionService;
        this.queryService = queryService;
    }

    @PostMapping(path = "/api/v1/assurance/cases/{caseId}/actions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ActionDetailDto propose(@PathVariable UUID caseId, @RequestBody ProposeActionRequest request) {
        var stored = proposalService.propose(
                caseId,
                request.actionType(),
                request.capabilityId(),
                request.targetType(),
                request.targetId(),
                request.parameters(),
                request.rationale(),
                request.proposedBy()
        );
        return queryService.require(stored.getId());
    }

    @GetMapping("/api/v1/actions")
    public List<ActionDetailDto> list() {
        return queryService.list();
    }

    @GetMapping("/api/v1/actions/{actionId}")
    public ActionDetailDto get(@PathVariable UUID actionId) {
        return queryService.require(actionId);
    }

    @PostMapping(path = "/api/v1/actions/{actionId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ActionDetailDto approve(@PathVariable UUID actionId, @RequestBody ApprovalRequest request) {
        approvalService.decide(actionId, ApprovalDecision.APPROVED, request.decidedBy(), request.comment());
        return queryService.require(actionId);
    }

    @PostMapping(path = "/api/v1/actions/{actionId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ActionDetailDto reject(@PathVariable UUID actionId, @RequestBody ApprovalRequest request) {
        approvalService.decide(actionId, ApprovalDecision.REJECTED, request.decidedBy(), request.comment());
        return queryService.require(actionId);
    }

    @PostMapping("/api/v1/actions/{actionId}/execute")
    public ActionDetailDto execute(@PathVariable UUID actionId) {
        executionService.execute(actionId);
        return queryService.require(actionId);
    }
}
