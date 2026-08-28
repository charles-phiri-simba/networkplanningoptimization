package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;
import com.simba.snip.npo.integration.SourceSnapshot;
import com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage;
import com.simba.snip.npo.integration.ericsson.enm.EricssonEnmSnapshotMapper;
import com.simba.snip.npo.integration.security.ConnectorAccessMode;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorDescriptor;
import com.simba.snip.npo.integration.security.ConnectorImplementationType;
import com.simba.snip.npo.integration.security.ConnectorMode;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EricssonEnmConnector {

    public static final String CONNECTOR_ID = ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER;
    public static final String PLATFORM = "ENM";
    public static final Set<ConnectorCapability> READ_CAPABILITIES = Set.of(
            ConnectorCapability.INVENTORY_READ,
            ConnectorCapability.CONFIGURATION_READ,
            ConnectorCapability.PAGINATION,
            ConnectorCapability.SOURCE_VERSION,
            ConnectorCapability.READ_SITE,
            ConnectorCapability.READ_GNB,
            ConnectorCapability.READ_CELL,
            ConnectorCapability.READ_CONFIGURATION
    );

    private final EnmIntegrationProperties properties;
    private final SimulatorEnmTransport simulatorTransport;
    private final EricssonEnmSnapshotMapper mapper;
    private final EnmConnectorMetrics metrics;
    private final EnmImportTestHooks testHooks;

    public EricssonEnmConnector(
            EnmIntegrationProperties properties,
            SimulatorEnmTransport simulatorTransport,
            EricssonEnmSnapshotMapper mapper,
            EnmConnectorMetrics metrics,
            EnmImportTestHooks testHooks
    ) {
        this.properties = properties;
        this.simulatorTransport = simulatorTransport;
        this.mapper = mapper;
        this.metrics = metrics;
        this.testHooks = testHooks;
    }

    public ConnectorDescriptor descriptor(ConnectorDefinition definition) {
        ConnectorImplementationType type = definition.mode() == ConnectorMode.REAL
                ? ConnectorImplementationType.REAL
                : ConnectorImplementationType.SIMULATOR;
        return new ConnectorDescriptor(
                definition.connectorId(),
                definition.vendor(),
                PLATFORM,
                "INT",
                type,
                ConnectorAccessMode.READ_ONLY,
                READ_CAPABILITIES
        );
    }

    public AcquisitionResult acquire(ConnectorDefinition definition, ImportExecutionContext context) {
        context.assertContinuing();
        if (descriptor(definition).capabilities().stream().anyMatch(ConnectorCapability::mutatesNetwork)) {
            throw new VendorConnectorException(
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED, "ENM connector advertised a write capability");
        }
        EnmTransport transport = transportFor(definition);
        Instant started = Instant.now();
        List<EnmInventoryPage> pages = new ArrayList<>();
        Set<String> tokens = new HashSet<>();
        Set<String> pageIds = new HashSet<>();
        VendorRetryPolicy retry = new VendorRetryPolicy(
                properties.getMaxAttempts(),
                properties.getInitialBackoff(),
                properties.getMaxBackoff()
        );
        metrics.incrementSessions();
        try {
            transport.open(context);
            EnmInventoryPage page = fetchWithRetry(transport, context, retry, true, null);
            accumulate(page, pages, tokens, pageIds, context);
            testHooks.runAfterFirstPage();
            while (!page.lastPage() && page.continuationToken() != null && !page.continuationToken().isBlank()) {
                context.assertContinuing();
                String token = page.continuationToken();
                page = fetchWithRetry(transport, context, retry, false, token);
                accumulate(page, pages, tokens, pageIds, context);
            }
            context.assertContinuing();
            int entities = pages.stream().mapToInt(EnmInventoryPage::entityCount).sum();
            Instant completed = Instant.now();
            VendorSnapshot snapshot = new VendorSnapshot(
                    "enm-sim-" + context.executionId(),
                    context.executionId(),
                    definition.connectorId(),
                    definition.vendor().name(),
                    definition.sourceSystem(),
                    started,
                    completed,
                    SnapshotCompleteness.COMPLETE,
                    null,
                    pages.size(),
                    entities,
                    List.of(),
                    EnmApiProfile.simulatorV1().sourceVersion()
            );
            SourceSnapshot neutral = mapper.toNeutral(
                    snapshot.snapshotId(),
                    definition.sourceSystem(),
                    started,
                    true,
                    pages
            );
            metrics.recordPages(pages.size(), entities);
            return new AcquisitionResult(snapshot, neutral, pages);
        } catch (ImportRuntimeException ex) {
            metrics.incrementSessionFailures();
            if (ex.failureCode() == ImportFailureCode.LEASE_LOST) {
                metrics.incrementLeaseLostDuringAcquisition();
            }
            if (ex.failureCode() == ImportFailureCode.CONNECTOR_CANCELLED) {
                metrics.incrementCancellation();
            }
            if (completenessFor(ex.failureCode(), !pages.isEmpty()) == SnapshotCompleteness.PARTIAL) {
                metrics.incrementSnapshotPartial();
            } else {
                metrics.incrementSnapshotFailed();
            }
            Instant completed = Instant.now();
            SnapshotCompleteness completeness = completenessFor(ex.failureCode(), !pages.isEmpty());
            VendorSnapshot snapshot = new VendorSnapshot(
                    "enm-sim-" + context.executionId(),
                    context.executionId(),
                    definition.connectorId(),
                    definition.vendor().name(),
                    definition.sourceSystem(),
                    started,
                    completed,
                    completeness,
                    null,
                    pages.size(),
                    pages.stream().mapToInt(EnmInventoryPage::entityCount).sum(),
                    List.of(ex.failureCode().name()),
                    EnmApiProfile.simulatorV1().sourceVersion()
            );
            SourceSnapshot neutral = mapper.toNeutral(
                    snapshot.snapshotId(),
                    definition.sourceSystem(),
                    started,
                    false,
                    pages
            );
            throw new AcquisitionFailedException(snapshot, neutral, ex);
        } finally {
            transport.close();
        }
    }

    private EnmTransport transportFor(ConnectorDefinition definition) {
        if (definition.mode() == ConnectorMode.REAL
                || properties.productionSelected()) {
            return new UnconfiguredProductionEnmTransport();
        }
        return simulatorTransport;
    }

    private EnmInventoryPage fetchWithRetry(
            EnmTransport transport,
            ImportExecutionContext context,
            VendorRetryPolicy retry,
            boolean first,
            String token
    ) {
        ImportRuntimeException last = null;
        for (int attempt = 0; attempt < retry.maxAttempts(); attempt++) {
            context.assertContinuing();
            try {
                metrics.incrementVendorRequests();
                return first
                        ? transport.fetchFirstPage(context, properties.getPageSize())
                        : transport.fetchContinuation(context, token, properties.getPageSize());
            } catch (ImportRuntimeException ex) {
                last = ex;
                metrics.incrementVendorRequestFailures();
                if (!retry.retryable(ex.failureCode()) || attempt == retry.maxAttempts() - 1) {
                    throw ex;
                }
                metrics.incrementRetries();
                if (ex.failureCode() == ImportFailureCode.VENDOR_RATE_LIMITED) {
                    metrics.incrementThrottles();
                }
                Duration backoff = retry.backoff(attempt, transport.lastRetryAfter());
                context.assertBeforeRetry(backoff);
                sleep(backoff, context);
            }
        }
        throw last == null
                ? new VendorConnectorException(ImportFailureCode.VENDOR_UNAVAILABLE, "retry exhausted")
                : last;
    }

    private void accumulate(
            EnmInventoryPage page,
            List<EnmInventoryPage> pages,
            Set<String> tokens,
            Set<String> pageIds,
            ImportExecutionContext context
    ) {
        context.assertContinuing();
        if (page.pageIdentity() != null && !pageIds.add(page.pageIdentity()) && pages.size() > 0) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_PAGINATION_INVALID, "repeated page identity");
        }
        if (page.continuationToken() != null && !page.continuationToken().isBlank()
                && !tokens.add(page.continuationToken())) {
            throw new VendorConnectorException(ImportFailureCode.VENDOR_PAGINATION_INVALID, "repeated continuation token");
        }
        pages.add(page);
        if (pages.size() > properties.getMaxPages()) {
            throw new VendorConnectorException(ImportFailureCode.SNAPSHOT_LIMIT_EXCEEDED, "page limit exceeded");
        }
        int entities = pages.stream().mapToInt(EnmInventoryPage::entityCount).sum();
        if (entities > properties.getMaxEntities()) {
            throw new VendorConnectorException(ImportFailureCode.SNAPSHOT_LIMIT_EXCEEDED, "entity limit exceeded");
        }
        metrics.incrementPagesRead();
    }

    private static SnapshotCompleteness completenessFor(ImportFailureCode code, boolean hadPages) {
        if (code == ImportFailureCode.VENDOR_TIMEOUT && hadPages) {
            return SnapshotCompleteness.PARTIAL;
        }
        if (code == ImportFailureCode.SNAPSHOT_PARTIAL) {
            return SnapshotCompleteness.PARTIAL;
        }
        return SnapshotCompleteness.FAILED;
    }

    private static void sleep(Duration backoff, ImportExecutionContext context) {
        long remaining = backoff.toMillis();
        while (remaining > 0) {
            context.cancellationToken().throwIfCancelled();
            long slice = Math.min(20L, remaining);
            try {
                Thread.sleep(slice);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ImportRuntimeException(
                        ImportFailureCode.CONNECTOR_CANCELLED, "backoff interrupted", false, ex);
            }
            remaining -= slice;
        }
    }

    public record AcquisitionResult(VendorSnapshot vendorSnapshot, SourceSnapshot sourceSnapshot, List<EnmInventoryPage> pages) {
    }

    public static final class AcquisitionFailedException extends ImportRuntimeException {
        private final VendorSnapshot vendorSnapshot;
        private final SourceSnapshot sourceSnapshot;

        public AcquisitionFailedException(
                VendorSnapshot vendorSnapshot, SourceSnapshot sourceSnapshot, ImportRuntimeException cause
        ) {
            super(cause.failureCode(), cause.getMessage(), cause.retryable(), cause);
            this.vendorSnapshot = vendorSnapshot;
            this.sourceSnapshot = sourceSnapshot;
        }

        public VendorSnapshot vendorSnapshot() {
            return vendorSnapshot;
        }

        public SourceSnapshot sourceSnapshot() {
            return sourceSnapshot;
        }
    }
}
