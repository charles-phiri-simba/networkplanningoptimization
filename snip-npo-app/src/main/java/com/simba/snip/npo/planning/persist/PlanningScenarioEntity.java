package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planning_scenario")
public class PlanningScenarioEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    public static PlanningScenarioEntity create(
            UUID id,
            String name,
            String description,
            String createdBy,
            Instant now
    ) {
        PlanningScenarioEntity entity = new PlanningScenarioEntity();
        entity.id = id;
        entity.name = name;
        entity.description = description;
        entity.createdBy = createdBy;
        entity.createdAt = now;
        entity.updatedAt = now;
        entity.rowVersion = 1;
        return entity;
    }

    public void replaceMetadata(String name, String description, Instant updatedAt) {
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
        this.rowVersion = this.rowVersion + 1;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getRowVersion() {
        return rowVersion;
    }
}
