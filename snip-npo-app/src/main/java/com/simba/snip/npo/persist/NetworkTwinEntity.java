package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_twin")
public class NetworkTwinEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "scope_type", nullable = false, length = 16)
    private String scopeType;

    @Column(name = "scope_id", nullable = false, length = 64)
    private String scopeId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "latest_version", nullable = false)
    private int latestVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "synchronized_at")
    private Instant synchronizedAt;

    @Column(nullable = false)
    private boolean synthetic;

    public static NetworkTwinEntity create(
            UUID id,
            String name,
            String scopeType,
            String scopeId,
            String status,
            Instant createdAt,
            boolean synthetic
    ) {
        NetworkTwinEntity entity = new NetworkTwinEntity();
        entity.id = id;
        entity.name = name;
        entity.scopeType = scopeType;
        entity.scopeId = scopeId;
        entity.status = status;
        entity.latestVersion = 0;
        entity.createdAt = createdAt;
        entity.synthetic = synthetic;
        return entity;
    }

    public void recordSynchronization(int version, Instant synchronizedAt, boolean synthetic) {
        this.latestVersion = version;
        this.synchronizedAt = synchronizedAt;
        this.synthetic = synthetic;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getStatus() {
        return status;
    }

    public int getLatestVersion() {
        return latestVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSynchronizedAt() {
        return synchronizedAt;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
