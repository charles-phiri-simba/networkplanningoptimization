package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "gnb")
public class GnbEntity {

    @Id
    private UUID id;

    @Column(name = "gnb_id", nullable = false, unique = true, length = 64)
    private String gnbId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    private String vendor;
    private String model;

    @Column(nullable = false, length = 32)
    private String status;

    public UUID getId() {
        return id;
    }

    public String getGnbId() {
        return gnbId;
    }

    public String getName() {
        return name;
    }

    public SiteEntity getSite() {
        return site;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public String getStatus() {
        return status;
    }
}
