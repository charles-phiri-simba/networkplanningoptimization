package com.simba.snip.npo.integration;

import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ImportExecutionGuard implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ImportExecutionGuard.class);

    private final ScheduledExecutorService scheduler;
    private final IntegrationRuntimeProperties properties;
    private final ImportLeaseService leaseService;
    private final NetworkImportBatchService batchService;
    private final IntegrationMetrics metrics;

    public ImportExecutionGuard(
            IntegrationRuntimeProperties properties,
            ImportLeaseService leaseService,
            NetworkImportBatchService batchService,
            IntegrationMetrics metrics
    ) {
        this.properties = properties;
        this.leaseService = leaseService;
        this.batchService = batchService;
        this.metrics = metrics;
        this.scheduler = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "snip-import-runtime");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Closeable start(UUID executionId, ImportLease lease) {
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
            try {
                leaseService.heartbeat(lease);
            } catch (RuntimeException ex) {
                log.warn(
                        "importHeartbeatFailed executionId={} sourceSystem={} sourceScope={} fencingToken={} error={}",
                        executionId, lease.sourceSystem(), lease.sourceScope(), lease.fencingToken(), ex.getMessage()
                );
            }
        }, properties.getHeartbeatInterval().toMillis(), properties.getHeartbeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        ScheduledFuture<?> watchdog = scheduler.schedule(
                () -> timeout(executionId, lease),
                properties.getExecutionTimeout().toMillis(),
                TimeUnit.MILLISECONDS
        );
        return () -> {
            heartbeat.cancel(false);
            watchdog.cancel(false);
        };
    }

    private void timeout(UUID executionId, ImportLease lease) {
        boolean timedOut = batchService.terminalize(
                executionId,
                ImportExecutionStatus.TIMED_OUT.name(),
                java.time.Instant.now(),
                ImportFailureCode.EXECUTION_TIMEOUT,
                true,
                "import execution exceeded configured timeout"
        );
        if (timedOut) {
            metrics.incrementTimeouts();
            batchService.appendAudit(
                    executionId,
                    ImportAuditEventType.IMPORT_TIMED_OUT,
                    java.time.Instant.now(),
                    "failureCode=EXECUTION_TIMEOUT"
            );
            leaseService.release(lease);
            log.warn(
                    "importTimedOut executionId={} sourceSystem={} sourceScope={} fencingToken={}",
                    executionId, lease.sourceSystem(), lease.sourceScope(), lease.fencingToken()
            );
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void shutdown() {
        scheduler.shutdownNow();
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
