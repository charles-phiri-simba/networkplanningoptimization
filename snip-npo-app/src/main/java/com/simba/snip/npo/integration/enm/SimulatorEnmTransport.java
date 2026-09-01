package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.CanonicalEntityType;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ericsson.enm.EnmCell;
import com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage;
import com.simba.snip.npo.integration.ericsson.enm.EnmManagedElement;
import com.simba.snip.npo.integration.ericsson.enm.EnmRadioFunction;
import com.simba.snip.npo.integration.sync.VendorIncrementalBatch;
import com.simba.snip.npo.integration.sync.VendorIncrementalChange;
import com.simba.snip.npo.integration.sync.VendorIncrementalChangeType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimulatorEnmTransport implements EnmTransport {

    public static final String TOKEN_PAGE_2 = "sim-token-page-2";
    public static final String TOKEN_CYCLE = "sim-token-cycle";
    public static final String TOKEN_REPEAT = "sim-token-repeat";
    public static final String TOKEN_INVALID = "sim-token-invalid";

    private final SimulatorEnmScenarioController controller;
    private final SimulatorEnmSyncState syncState;
    private Duration lastRetryAfter = Duration.ZERO;
    private boolean open;

    public SimulatorEnmTransport(SimulatorEnmScenarioController controller, SimulatorEnmSyncState syncState) {
        this.controller = controller;
        this.syncState = syncState;
    }

    @Override
    public void open(ImportExecutionContext context) {
        context.assertContinuing();
        SimulatorEnmScenario scenario = controller.scenario();
        if (scenario == SimulatorEnmScenario.AUTH_401) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_AUTHENTICATION_FAILED, "simulator authentication failed");
        }
        if (scenario == SimulatorEnmScenario.AUTH_403) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_AUTHORIZATION_DENIED, "simulator authorization denied");
        }
        open = true;
    }

    @Override
    public EnmInventoryPage fetchFirstPage(ImportExecutionContext context, int pageSize) {
        return fetch(context, null, pageSize, true);
    }

    @Override
    public EnmInventoryPage fetchContinuation(ImportExecutionContext context, String continuationToken, int pageSize) {
        return fetch(context, continuationToken, pageSize, false);
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public VendorIncrementalBatch fetchIncremental(SynchronizationExecutionContext context) {
        if (!open) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_PROTOCOL_ERROR, "simulator session is not open");
        }
        context.assertContinuing();
        SimulatorEnmScenario scenario = controller.scenario();
        String scope = context.importContext().lease().sourceScope();
        String starting = context.startingCheckpoint();
        int startingSeq = syncState.parseSequence(starting);
        if (startingSeq < 0) {
            throw new VendorConnectorException(ImportFailureCode.CHECKPOINT_REJECTED, "synthetic checkpoint rejected");
        }
        if (scenario == SimulatorEnmScenario.CHECKPOINT_EXPIRED) {
            throw new VendorConnectorException(ImportFailureCode.CHECKPOINT_EXPIRED, "synthetic checkpoint expired");
        }
        if (scenario == SimulatorEnmScenario.CHECKPOINT_REJECTED) {
            throw new VendorConnectorException(ImportFailureCode.CHECKPOINT_REJECTED, "synthetic checkpoint rejected");
        }
        if (scenario == SimulatorEnmScenario.SEQUENCE_GAP) {
            throw new VendorConnectorException(ImportFailureCode.SEQUENCE_GAP, "synthetic sequence gap");
        }
        if (startingSeq == 0) {
            throw new VendorConnectorException(ImportFailureCode.INCREMENTAL_NOT_SUPPORTED, "incremental requires trusted baseline");
        }
        int current = syncState.currentSequence(scope);
        if (startingSeq > current) {
            throw new VendorConnectorException(ImportFailureCode.SEQUENCE_GAP, "starting checkpoint ahead of simulator state");
        }
        String batchId = context.importContext().executionId().toString();
        if (batchId.equals(syncState.lastBatchId())) {
            return replayBatch(context, scope, starting, startingSeq);
        }
        List<VendorIncrementalChange> changes = new ArrayList<>();
        if (scenario == SimulatorEnmScenario.NO_CHANGES) {
            // deliberate empty batch
        } else if (scenario == SimulatorEnmScenario.SOURCE_CHANGES
                || scenario == SimulatorEnmScenario.INCREMENTAL_SUCCESS
                || scenario == SimulatorEnmScenario.DRIFT_DETECTED) {
            changes.add(new VendorIncrementalChange(
                    VendorIncrementalChangeType.UPSERT,
                    CanonicalEntityType.CELL,
                    "CELL-002",
                    "CELL-SIM-002"
            ));
        } else if (scenario == SimulatorEnmScenario.DRIFT_RESOLVED) {
            changes.add(new VendorIncrementalChange(
                    VendorIncrementalChangeType.UPSERT,
                    CanonicalEntityType.CELL,
                    "CELL-002",
                    "CELL-SIM-002"
            ));
        } else if (scenario == SimulatorEnmScenario.EXPLICIT_REMOVE) {
            changes.add(new VendorIncrementalChange(
                    VendorIncrementalChangeType.REMOVE,
                    CanonicalEntityType.CELL,
                    "CELL-001",
                    "CELL-SIM-001"
            ));
        }
        String resulting = syncState.advanceCheckpoint(scope);
        syncState.rememberBatchId(batchId);
        return new VendorIncrementalBatch(
                context.importContext().lease().sourceSystem(),
                "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER",
                context.importContext().executionId(),
                starting,
                resulting,
                "sim-v" + resulting,
                Instant.now(),
                List.copyOf(changes),
                true,
                true
        );
    }

    private VendorIncrementalBatch replayBatch(
            SynchronizationExecutionContext context,
            String scope,
            String starting,
            int startingSeq
    ) {
        String resulting = SimulatorEnmSyncState.CHECKPOINT_PREFIX + startingSeq;
        return new VendorIncrementalBatch(
                context.importContext().lease().sourceSystem(),
                "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER",
                context.importContext().executionId(),
                starting,
                resulting,
                "sim-v" + resulting,
                Instant.now(),
                List.of(),
                true,
                true
        );
    }

    @Override
    public Duration lastRetryAfter() {
        return lastRetryAfter;
    }

    @Override
    public void close() {
        open = false;
    }

    private EnmInventoryPage fetch(ImportExecutionContext context, String token, int pageSize, boolean first) {
        if (!open) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_PROTOCOL_ERROR, "simulator session is not open");
        }
        context.assertContinuing();
        lastRetryAfter = Duration.ZERO;
        SimulatorEnmScenario scenario = controller.scenario();
        if (scenario == SimulatorEnmScenario.TIMEOUT) {
            sleep(context.requestTimeout().plusMillis(50));
            throw new VendorConnectorException(ImportFailureCode.VENDOR_TIMEOUT, "simulator request timed out");
        }
        if (scenario == SimulatorEnmScenario.RATE_LIMIT_429 && controller.decrementRateLimit() > 0) {
            lastRetryAfter = Duration.ofMillis(20);
            throw new VendorConnectorException(ImportFailureCode.VENDOR_RATE_LIMITED, "simulator rate limited");
        }
        if (scenario == SimulatorEnmScenario.UNAVAILABLE_503 && controller.decrementUnavailable() > 0) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_UNAVAILABLE, "simulator unavailable");
        }
        if (scenario == SimulatorEnmScenario.MALFORMED) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_RESPONSE_INVALID, "simulator response is malformed");
        }
        if (scenario == SimulatorEnmScenario.EMPTY_INVALID_CONTINUATION && first) {
            return new EnmInventoryPage("page-empty", TOKEN_INVALID, false, List.of(), List.of(), List.of());
        }
        if (!first && TOKEN_INVALID.equals(token)) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_PAGINATION_INVALID, "malformed continuation");
        }
        if (!first && TOKEN_REPEAT.equals(token)) {
            return numbered("page-1", TOKEN_REPEAT, false, 1);
        }
        if (!first && TOKEN_CYCLE.equals(token)) {
            return numbered("page-1", TOKEN_CYCLE, false, 1);
        }
        if (first && scenario == SimulatorEnmScenario.REPEATED_CONTINUATION) {
            return numbered("page-1", TOKEN_REPEAT, false, 1);
        }
        if (first && scenario == SimulatorEnmScenario.CONTINUATION_CYCLE) {
            return numbered("page-1", TOKEN_CYCLE, false, 1);
        }
        if (first && scenario == SimulatorEnmScenario.FAIL_AFTER_FIRST_PAGE) {
            return numbered("page-1", TOKEN_PAGE_2, false, 1);
        }
        if (!first && scenario == SimulatorEnmScenario.FAIL_AFTER_FIRST_PAGE) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_UNAVAILABLE, "simulator failed after first page");
        }
        if (first && scenario == SimulatorEnmScenario.PARTIAL_AFTER_FIRST_PAGE) {
            return numbered("page-1", TOKEN_PAGE_2, false, 1);
        }
        if (!first && scenario == SimulatorEnmScenario.PARTIAL_AFTER_FIRST_PAGE) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_TIMEOUT, "simulator partial after first page");
        }
        if (scenario == SimulatorEnmScenario.ENTITY_LIMIT) {
            return overflowEntities();
        }
        if (scenario == SimulatorEnmScenario.PAGE_LIMIT && first) {
            return numbered("page-1", TOKEN_PAGE_2, false, 1);
        }
        if (scenario == SimulatorEnmScenario.PAGE_LIMIT && TOKEN_PAGE_2.equals(token)) {
            return numbered("page-2", "sim-token-page-3", false, 2);
        }
        if (scenario == SimulatorEnmScenario.SUCCESS_MULTI_PAGE && first) {
            return numbered("page-1", TOKEN_PAGE_2, false, 1);
        }
        if (scenario == SimulatorEnmScenario.SUCCESS_MULTI_PAGE && TOKEN_PAGE_2.equals(token)) {
            return numbered("page-2", null, true, 2);
        }
        if (token != null && token.startsWith("sim-token-page-")) {
            int n = Integer.parseInt(token.substring("sim-token-page-".length()));
            return numbered("page-" + n, "sim-token-page-" + (n + 1), false, 2);
        }
        return numbered("page-1", null, true, 1);
    }

    private static EnmInventoryPage numbered(String identity, String token, boolean last, int page) {
        EnmInventoryPage slice = page(page, 10);
        return new EnmInventoryPage(
                identity, token, last, slice.managedElements(), slice.radioFunctions(), slice.cells());
    }

    private static EnmInventoryPage page(int page, int pageSize) {
        List<EnmManagedElement> mes = new ArrayList<>();
        List<EnmRadioFunction> rfs = new ArrayList<>();
        List<EnmCell> cells = new ArrayList<>();
        if (page == 1) {
            mes.add(new EnmManagedElement("ME-001", "SubNetwork=ON,ManagedElement=ME-001", "Sim Site", "SITE-SIM-001"));
            rfs.add(new EnmRadioFunction(
                    "RF-001",
                    "SubNetwork=ON,ManagedElement=ME-001,GNBDUFunction=1",
                    "ME-001",
                    "Sim gNB",
                    "GNB-SIM-001",
                    "SITE-SIM-001"
            ));
            cells.add(new EnmCell(
                    "CELL-001",
                    "GNBDUFunction=1,NRCellDU=1",
                    "RF-001",
                    "Sim Cell 1",
                    "CELL-SIM-001",
                    "GNB-SIM-001",
                    460
            ));
        } else {
            cells.add(new EnmCell(
                    "CELL-002",
                    "GNBDUFunction=1,NRCellDU=2",
                    "RF-001",
                    "Sim Cell 2",
                    "CELL-SIM-002",
                    "GNB-SIM-001",
                    460
            ));
        }
        if (pageSize < 1) {
            return new EnmInventoryPage("page-" + page, null, true, List.of(), List.of(), List.of());
        }
        return new EnmInventoryPage("page-" + page, null, true, mes, rfs, cells);
    }

    private static EnmInventoryPage overflowEntities() {
        List<EnmCell> cells = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            String id = String.format("CELL-%03d", i);
            cells.add(new EnmCell(id, "NRCellDU=" + i, "RF-001", "Cell " + i, "CELL-SIM-" + i, "GNB-SIM-001", 460));
        }
        return new EnmInventoryPage(
                "page-1",
                null,
                true,
                List.of(new EnmManagedElement("ME-001", "ME=ME-001", "Sim Site", "SITE-SIM-001")),
                List.of(new EnmRadioFunction("RF-001", "GNBDU=1", "ME-001", "Sim gNB", "GNB-SIM-001", "SITE-SIM-001")),
                cells
        );
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(1L, duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VendorConnectorException(ImportFailureCode.CONNECTOR_CANCELLED, "simulator wait interrupted", ex);
        }
    }
}
