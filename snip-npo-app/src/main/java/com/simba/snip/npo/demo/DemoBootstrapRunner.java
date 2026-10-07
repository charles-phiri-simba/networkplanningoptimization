package com.simba.snip.npo.demo;

import com.simba.snip.npo.api.TwinDetailDto;
import com.simba.snip.npo.assurance.AssuranceDetectionService;
import com.simba.snip.npo.assurance.CaseStatus;
import com.simba.snip.npo.assurance.CaseType;
import com.simba.snip.npo.assurance.Severity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.integration.sync.SynchronizationExecutionResult;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.SiteRepository;
import com.simba.snip.npo.twin.SimulationQueryService;
import com.simba.snip.npo.twin.TwinFreshness;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deterministic demo bootstrap. Active only with Spring profile {@code demo}.
 * Does not create proposals, plans, or sandbox executions.
 */
@Component
@Profile("demo")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBootstrapRunner.class);

    private final DemoProperties properties;
    private final SiteRepository siteRepository;
    private final GnbRepository gnbRepository;
    private final CellRepository cellRepository;
    private final SimulatorEnmScenarioController scenarios;
    private final VendorImportAuthorizer vendorImportAuthorizer;
    private final SynchronizationControlPlane controlPlane;
    private final NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    private final KnowledgeGate knowledgeGate;
    private final TwinSynchronizationService twinSynchronizationService;
    private final SimulationQueryService simulationQueryService;
    private final AssuranceDetectionService assuranceDetectionService;

    public DemoBootstrapRunner(
            DemoProperties properties,
            SiteRepository siteRepository,
            GnbRepository gnbRepository,
            CellRepository cellRepository,
            SimulatorEnmScenarioController scenarios,
            VendorImportAuthorizer vendorImportAuthorizer,
            SynchronizationControlPlane controlPlane,
            NetworkKnowledgeStatusRepository knowledgeStatusRepository,
            KnowledgeGate knowledgeGate,
            TwinSynchronizationService twinSynchronizationService,
            SimulationQueryService simulationQueryService,
            AssuranceDetectionService assuranceDetectionService
    ) {
        this.properties = properties;
        this.siteRepository = siteRepository;
        this.gnbRepository = gnbRepository;
        this.cellRepository = cellRepository;
        this.scenarios = scenarios;
        this.vendorImportAuthorizer = vendorImportAuthorizer;
        this.controlPlane = controlPlane;
        this.knowledgeStatusRepository = knowledgeStatusRepository;
        this.knowledgeGate = knowledgeGate;
        this.twinSynchronizationService = twinSynchronizationService;
        this.simulationQueryService = simulationQueryService;
        this.assuranceDetectionService = assuranceDetectionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        verifyV21Inventory();
        establishTrustedKnowledge();
        Map<String, String> featuredTwins = synchronizeFeaturedTwins();
        writeFeaturedTwinRegistry(featuredTwins);
        AssuranceCaseEntity featured = evaluateFeaturedAssurance();
        log.info(
                "SNIP demo bootstrap ready cellId={} caseId={} caseType={} severity={}",
                DemoInventory.FEATURED_CELL,
                featured.getId(),
                featured.getCaseType(),
                featured.getSeverity()
        );
    }

    void verifyV21Inventory() {
        for (String siteId : DemoInventory.SITE_IDS) {
            if (siteRepository.findBySiteId(siteId).isEmpty()) {
                throw new IllegalStateException("SNIP demo inventory missing site " + siteId);
            }
        }
        for (String gnbId : DemoInventory.GNB_IDS) {
            if (gnbRepository.findByGnbId(gnbId).isEmpty()) {
                throw new IllegalStateException("SNIP demo inventory missing gNB " + gnbId);
            }
        }
        for (String cellId : DemoInventory.CELL_IDS) {
            if (cellRepository.findByCellId(cellId).isEmpty()) {
                throw new IllegalStateException("SNIP demo inventory missing cell " + cellId);
            }
        }
    }

    void establishTrustedKnowledge() {
        if (!properties.isTriggerSimulatorKnowledgeBaseline()) {
            throw new IllegalStateException("SNIP demo knowledge baseline is mandatory");
        }
        if (knowledgeRecommendable()) {
            log.info("SNIP demo knowledge already recommendable; skipping ENM simulator import");
            verifyV21Inventory();
            return;
        }
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        SynchronizationExecutionResult result = vendorImportAuthorizer.callWith(
                VendorImportAuthorizer.PERMISSION,
                () -> controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)
        );
        NetworkKnowledgeStatusEntity knowledge = knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope(
                        DemoInventory.KNOWLEDGE_SOURCE, DemoInventory.KNOWLEDGE_SCOPE)
                .orElseThrow(() -> new IllegalStateException(
                        "SNIP demo knowledge status missing after ENM simulator import"));
        KnowledgeGate.GateResult gate = knowledgeGate.evaluate(knowledge.getConfidence());
        if (!gate.allowsRecommendation()) {
            throw new IllegalStateException(
                    "SNIP demo knowledge is not recommendable: confidence="
                            + knowledge.getConfidence()
                            + (result.overlapSkipped() ? " overlapSkipped=true" : ""));
        }
        verifyV21Inventory();
    }

    private boolean knowledgeRecommendable() {
        return knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope(
                        DemoInventory.KNOWLEDGE_SOURCE, DemoInventory.KNOWLEDGE_SCOPE)
                .map(status -> knowledgeGate.evaluate(status.getConfidence()).allowsRecommendation())
                .orElse(false);
    }

    Map<String, String> synchronizeFeaturedTwins() {
        if (!properties.isSynchronizeFeaturedTwins()) {
            throw new IllegalStateException("SNIP demo featured twin synchronization is required");
        }
        Map<String, String> twins = new LinkedHashMap<>();
        for (String cellId : properties.getFeaturedTwinCells()) {
            NetworkTwinEntity twin = twinSynchronizationService.synchronizeCell(cellId);
            TwinDetailDto detail = simulationQueryService.twin(twin.getId());
            if (!TwinFreshness.CURRENT.name().equals(detail.freshness())) {
                throw new IllegalStateException(
                        "SNIP demo featured twin is not CURRENT: cellId=" + cellId
                                + " freshness=" + detail.freshness());
            }
            twins.put(cellId, twin.getId().toString());
        }
        return twins;
    }

    AssuranceCaseEntity evaluateFeaturedAssurance() {
        if (!properties.isBootstrapAssurance()) {
            throw new IllegalStateException("SNIP demo Assurance bootstrap is required");
        }
        for (String cellId : properties.getFeaturedAssuranceCells()) {
            assuranceDetectionService.evaluateCell(cellId);
        }
        Optional<AssuranceCaseEntity> featured = assuranceDetectionService.evaluateCell(DemoInventory.FEATURED_CELL);
        if (featured.isEmpty()) {
            throw new IllegalStateException("SNIP demo featured CELL-001 did not produce an Assurance case");
        }
        AssuranceCaseEntity stored = featured.get();
        if (!CaseType.DEGRADING_RADIO_QUALITY.name().equals(stored.getCaseType())
                || !Severity.CRITICAL.name().equals(stored.getSeverity())
                || !CaseStatus.OPEN.name().equals(stored.getStatus())) {
            throw new IllegalStateException(
                    "SNIP demo featured CELL-001 Assurance is not OPEN CRITICAL DEGRADING_RADIO_QUALITY: "
                            + stored.getCaseType() + " " + stored.getSeverity() + " " + stored.getStatus());
        }
        return stored;
    }

    private void writeFeaturedTwinRegistry(Map<String, String> twins) {
        Path dir = Path.of(System.getProperty("user.dir"), ".snip-demo");
        Path file = dir.resolve("featured-twins.properties");
        try {
            Files.createDirectories(dir);
            String body = twins.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.writeString(file, body, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("SNIP demo could not write featured twin registry (readiness may use other checks): {}",
                    ex.getMessage());
        }
    }
}
