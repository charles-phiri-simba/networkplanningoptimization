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
@Table(name = "simulation_scenario")
public class SimulationScenarioEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "twin_id", nullable = false)
    private NetworkTwinEntity twin;

    @Column(name = "baseline_twin_version", nullable = false)
    private int baselineTwinVersion;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private boolean synthetic;

    public static SimulationScenarioEntity create(
            UUID id,
            NetworkTwinEntity twin,
            int baselineTwinVersion,
            String name,
            String description,
            String status,
            Instant createdAt,
            String createdBy,
            boolean synthetic
    ) {
        SimulationScenarioEntity entity = new SimulationScenarioEntity();
        entity.id = id;
        entity.twin = twin;
        entity.baselineTwinVersion = baselineTwinVersion;
        entity.name = name;
        entity.description = description;
        entity.status = status;
        entity.createdAt = createdAt;
        entity.createdBy = createdBy;
        entity.synthetic = synthetic;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public NetworkTwinEntity getTwin() {
        return twin;
    }

    public int getBaselineTwinVersion() {
        return baselineTwinVersion;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
