package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "network_source")
public class NetworkSourceEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, unique = true, length = 64)
    private String sourceSystem;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(nullable = false, length = 32)
    private String mode;

    @Column(name = "read_only", nullable = false)
    private boolean readOnly;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "schema_version", nullable = false, length = 64)
    private String schemaVersion;

    public UUID getId() {
        return id;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getVendor() {
        return vendor;
    }

    public String getMode() {
        return mode;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }
}
