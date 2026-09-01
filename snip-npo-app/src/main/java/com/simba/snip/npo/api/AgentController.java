package com.simba.snip.npo.api;

import com.simba.snip.npo.agent.AgentOrchestrationService;
import com.simba.snip.npo.agent.AgentQueryService;
import com.simba.snip.npo.agent.AgentRunCommand;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AgentController {

    private final AgentOrchestrationService orchestrationService;
    private final AgentQueryService queryService;

    public AgentController(AgentOrchestrationService orchestrationService, AgentQueryService queryService) {
        this.orchestrationService = orchestrationService;
        this.queryService = queryService;
    }

    @PostMapping(path = "/api/v1/agent-runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AgentRunDetailDto start(@RequestBody CreateAgentRunRequest request) {
        UUID runId = orchestrationService.start(new AgentRunCommand(
                request.objective(),
                request.assuranceCaseId(),
                request.initiatedBy(),
                request.maxSteps(),
                request.maxAgentCalls(),
                request.maxRetries(),
                request.timeoutMs()
        ));
        return queryService.require(runId);
    }

    @GetMapping("/api/v1/agent-runs")
    public List<AgentRunDetailDto> list() {
        return queryService.list();
    }

    @GetMapping("/api/v1/agent-runs/{runId}")
    public AgentRunDetailDto get(@PathVariable UUID runId) {
        return queryService.require(runId);
    }
}
