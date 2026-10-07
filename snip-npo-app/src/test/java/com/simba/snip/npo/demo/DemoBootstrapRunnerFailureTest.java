package com.simba.snip.npo.demo;

import com.simba.snip.npo.assurance.AssuranceDetectionService;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.SiteRepository;
import com.simba.snip.npo.twin.SimulationQueryService;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoBootstrapRunnerFailureTest {

    @Mock DemoProperties properties;
    @Mock SiteRepository sites;
    @Mock GnbRepository gnbs;
    @Mock CellRepository cells;
    @Mock SimulatorEnmScenarioController scenarios;
    @Mock VendorImportAuthorizer authorizer;
    @Mock SynchronizationControlPlane controlPlane;
    @Mock NetworkKnowledgeStatusRepository knowledge;
    @Mock KnowledgeGate gate;
    @Mock TwinSynchronizationService twins;
    @Mock SimulationQueryService queries;
    @Mock AssuranceDetectionService detection;

    @Test
    void missingFeaturedSiteFails() {
        when(sites.findBySiteId(anyString())).thenReturn(Optional.empty());
        DemoBootstrapRunner runner = new DemoBootstrapRunner(
                properties, sites, gnbs, cells, scenarios, authorizer, controlPlane,
                knowledge, gate, twins, queries, detection);
        assertThrows(IllegalStateException.class, runner::verifyV21Inventory);
    }
}
