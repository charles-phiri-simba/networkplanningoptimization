package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_twin_version")
public class NetworkTwinVersionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "twin_id", nullable = false)
    private NetworkTwinEntity twin;

    @Column(nullable = false)
    private int version;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "synchronized_at", nullable = false)
    private Instant synchronizedAt;

    @Column(name = "source_event_time")
    private Instant sourceEventTime;

    @Column(name = "source_context_version", nullable = false, columnDefinition = "TEXT")
    private String sourceContextVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String provenance;

    @Column(name = "cell_state", nullable = false, columnDefinition = "TEXT")
    private String cellState;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "current_metrics", nullable = false, columnDefinition = "TEXT")
    private String currentMetrics;

    @Column(name = "temporal_summary", nullable = false, columnDefinition = "TEXT")
    private String temporalSummary;

    @Column(name = "neighbour_summary", nullable = false, columnDefinition = "TEXT")
    private String neighbourSummary;

    public static NetworkTwinVersionEntity create(
            UUID id,
            NetworkTwinEntity twin,
            int version,
            Instant capturedAt,
            Instant synchronizedAt,
            Instant sourceEventTime,
            String sourceContextVersion,
            String provenance,
            String cellState,
            String configuration,
            String currentMetrics,
            String temporalSummary,
            String neighbourSummary
    ) {
        NetworkTwinVersionEntity entity = new NetworkTwinVersionEntity();
        entity.id = id;
        entity.twin = twin;
        entity.version = version;
        entity.capturedAt = capturedAt;
        entity.synchronizedAt = synchronizedAt;
        entity.sourceEventTime = sourceEventTime;
        entity.sourceContextVersion = sourceContextVersion;
        entity.provenance = provenance;
        entity.cellState = cellState;
        entity.configuration = configuration;
        entity.currentMetrics = currentMetrics;
        entity.temporalSummary = temporalSummary;
        entity.neighbourSummary = neighbourSummary;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public NetworkTwinEntity getTwin() {
        return twin;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Instant getSynchronizedAt() {
        return synchronizedAt;
    }

    public Instant getSourceEventTime() {
        return sourceEventTime;
    }

    public String getSourceContextVersion() {
        return sourceContextVersion;
    }

    public String getProvenance() {
        return provenance;
    }

    public String getCellState() {
        return cellState;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getCurrentMetrics() {
        return currentMetrics;
    }

    public String getTemporalSummary() {
        return temporalSummary;
    }

    public String getNeighbourSummary() {
        return neighbourSummary;
    }
}
