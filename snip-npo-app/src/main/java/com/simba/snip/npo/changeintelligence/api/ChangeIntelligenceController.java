package com.simba.snip.npo.changeintelligence.api;

import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalGovernanceService;
import com.simba.snip.npo.changeintelligence.service.NetworkChangeProposalGenerationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ChangeIntelligenceController {

    private final ChangeProposalAuthorizer authorizer;
    private final NetworkChangeProposalGenerationService generationService;
    private final ChangeProposalGovernanceService governanceService;
    private final ChangeProposalQueryService queryService;

    public ChangeIntelligenceController(
            ChangeProposalAuthorizer authorizer,
            NetworkChangeProposalGenerationService generationService,
            ChangeProposalGovernanceService governanceService,
            ChangeProposalQueryService queryService
    ) {
        this.authorizer = authorizer;
        this.generationService = generationService;
        this.governanceService = governanceService;
        this.queryService = queryService;
    }

    @PostMapping(
            value = "/api/v1/change-intelligence/proposals",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangeProposalDetailDto generate(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission,
            @RequestBody GenerateChangeProposalRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireGenerate();
        GenerationInitiator initiator = request.generationInitiator() == null
                ? GenerationInitiator.MANUAL
                : request.generationInitiator();
        NetworkChangeProposalEntity proposal = generationService.generate(
                new NetworkChangeProposalGenerationService.GenerationRequest(
                        request.targetEntityType(),
                        request.targetEntityId(),
                        request.parameterName(),
                        request.assuranceCaseId(),
                        request.decisionReference(),
                        initiator,
                        request.requestedBy() == null ? "api-user" : request.requestedBy()
                ));
        return queryService.require(proposal.getId());
    }

    @GetMapping("/api/v1/change-intelligence/proposals")
    public List<ChangeProposalSummaryDto> list(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireView();
        return queryService.list();
    }

    @GetMapping("/api/v1/change-intelligence/proposals/{proposalId}")
    public ChangeProposalDetailDto get(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID proposalId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrReview();
        return queryService.require(proposalId);
    }

    @GetMapping("/api/v1/change-intelligence/proposals/{proposalId}/evidence")
    public Map<String, Object> evidence(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID proposalId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrReview();
        return queryService.evidence(proposalId);
    }

    @PostMapping(
            value = "/api/v1/change-intelligence/proposals/{proposalId}/approve",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangeProposalDetailDto approve(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID proposalId,
            @RequestBody ReviewChangeProposalRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireApprove();
        governanceService.approve(
                proposalId,
                request.reviewer() == null ? "reviewer" : request.reviewer(),
                request.comment()
        );
        return queryService.require(proposalId);
    }

    @PostMapping(
            value = "/api/v1/change-intelligence/proposals/{proposalId}/reject",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangeProposalDetailDto reject(
            @RequestHeader(value = ChangeProposalAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID proposalId,
            @RequestBody ReviewChangeProposalRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireReject();
        governanceService.reject(
                proposalId,
                request.reviewer() == null ? "reviewer" : request.reviewer(),
                request.reasonCode(),
                request.comment()
        );
        return queryService.require(proposalId);
    }
}
