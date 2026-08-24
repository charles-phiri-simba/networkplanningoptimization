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

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(nullable = false)
    private Double value;

    private String unit;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false)
    private boolean synthetic;

    public UUID getId() {
        return id;
    }

    public CellEntity getCell() {
        return cell;
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

    public String getSource() {
        return source;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
