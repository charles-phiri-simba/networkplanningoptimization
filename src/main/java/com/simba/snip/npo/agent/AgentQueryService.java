package com.simba.snip.npo.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.api.AgentRunDetailDto;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.AgentCaseMemoryRepository;
import com.simba.snip.npo.persist.AgentPlanEntity;
import com.simba.snip.npo.persist.AgentPlanRepository;
import com.simba.snip.npo.persist.AgentPlanStepRepository;
import com.simba.snip.npo.persist.AgentRunEntity;
import com.simba.snip.npo.persist.AgentRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentQueryService {

    private final AgentRunRepository runRepository;
    private final AgentPlanRepository planRepository;
    private final AgentPlanStepRepository stepRepository;
    private final AgentCaseMemoryRepository caseMemoryRepository;
    private final AgentAuditService auditService;
    private final ObjectMapper objectMapper;

    public AgentQueryService(
            AgentRunRepository runRepository,
            AgentPlanRepository planRepository,
            AgentPlanStepRepository stepRepository,
            AgentCaseMemoryRepository caseMemoryRepository,
            AgentAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.caseMemoryRepository = caseMemoryRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AgentRunDetailDto> list() {
        return runRepository.findAllByOrderByStartedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AgentRunDetailDto require(UUID runId) {
        AgentRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new DomainNotFoundException("agent run", runId.toString()));
        return toDto(run);
    }

    private AgentRunDetailDto toDto(AgentRunEntity run) {
        AgentPlanEntity plan = planRepository.findByRunId(run.getId()).orElse(null);
        return AgentMapper.toDto(
                run,
                plan,
                plan == null ? List.of() : stepRepository.findByPlanIdOrderByStepNumberAsc(plan.getId()),
                caseMemoryRepository.findByRunId(run.getId()).orElse(null),
                auditService.list(run.getId()),
                objectMapper
        );
    }
}
