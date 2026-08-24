package com.simba.snip.npo.api;

import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.AssuranceMapper;
import com.simba.snip.npo.assurance.DecisionIntelligenceService;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.network.NetworkDomainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AssuranceController {

    private final AssuranceCaseService assuranceCaseService;
    private final DecisionIntelligenceService decisionIntelligenceService;
    private final NetworkDomainService domainService;

    public AssuranceController(
            AssuranceCaseService assuranceCaseService,
            DecisionIntelligenceService decisionIntelligenceService,
            NetworkDomainService domainService
    ) {
        this.assuranceCaseService = assuranceCaseService;
        this.decisionIntelligenceService = decisionIntelligenceService;
        this.domainService = domainService;
    }

    @GetMapping("/api/v1/assurance/cases")
    public List<AssuranceCaseDto> list() {
        return assuranceCaseService.listAll().stream().map(AssuranceMapper::toDto).toList();
    }

    @GetMapping("/api/v1/assurance/cases/{caseId}")
    public AssuranceCaseDto get(@PathVariable UUID caseId) {
        return assuranceCaseService.findById(caseId)
                .map(AssuranceMapper::toDto)
                .orElseThrow(() -> new DomainNotFoundException("assurance case", caseId.toString()));
    }

    @GetMapping("/api/v1/cells/{cellId}/assurance")
    public List<AssuranceCaseDto> forCell(@PathVariable String cellId) {
        domainService.requireCell(cellId);
        return assuranceCaseService.listForCell(cellId).stream().map(AssuranceMapper::toDto).toList();
    }

    @GetMapping("/api/v1/assurance/cases/{caseId}/assessment")
    public DecisionAssessmentDto assessment(@PathVariable UUID caseId) {
        return decisionIntelligenceService.assess(caseId);
    }
}
