package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    private UUID id;

    @Column(name = "site_id", nullable = false, unique = true, length = 64)
    private String siteId;

    @Column(nullable = false)
    private String name;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false, length = 32)
    private String status;

    public static SiteEntity create(
            UUID id, String siteId, String name, Double latitude, Double longitude, String status
    ) {
        SiteEntity entity = new SiteEntity();
        entity.id = id;
        entity.siteId = siteId;
        entity.name = name;
        entity.latitude = latitude;
        entity.longitude = longitude;
        entity.status = status;
        return entity;
    }

    public void applyInventory(String name, Double latitude, Double longitude, String status) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getStatus() {
        return status;
    }
}
