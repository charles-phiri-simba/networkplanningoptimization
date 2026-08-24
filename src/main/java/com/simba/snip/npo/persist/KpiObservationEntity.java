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
@Table(name = "kpi_observation")
public class KpiObservationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    private CellEntity cell;

    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(nullable = false)
    private Double value;

    private String unit;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private boolean synthetic;

    public static KpiObservationEntity create(
            UUID id,
            CellEntity cell,
            String eventId,
            String metric,
            Double value,
            String unit,
            Instant eventTime,
            Instant ingestedAt,
            String source,
            boolean synthetic
    ) {
        KpiObservationEntity entity = new KpiObservationEntity();
        entity.id = id;
        entity.cell = cell;
        entity.eventId = eventId;
        entity.metric = metric;
        entity.value = value;
        entity.unit = unit;
        entity.observedAt = eventTime;
        entity.ingestedAt = ingestedAt;
        entity.source = source;
        entity.synthetic = synthetic;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public CellEntity getCell() {
        return cell;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMetric() {
        return metric;
    }

    public Double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getEventTime() {
        return observedAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public String getSource() {
        return source;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
