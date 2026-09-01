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
@Table(name = "cell")
public class CellEntity {

    @Id
    private UUID id;

    @Column(name = "cell_id", nullable = false, unique = true, length = 64)
    private String cellId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gnb_id", nullable = false)
    private GnbEntity gnb;

    @Column(nullable = false, length = 32)
    private String technology;

    @Column(nullable = false, length = 32)
    private String band;

    private Integer arfcn;
    private Integer pci;

    @Column(name = "bandwidth_mhz")
    private Integer bandwidthMhz;

    @Column(name = "duplex_mode", length = 16)
    private String duplexMode;

    @Column(nullable = false, length = 32)
    private String status;

    public static CellEntity create(
            UUID id,
            String cellId,
            String name,
            GnbEntity gnb,
            String technology,
            String band,
            Integer arfcn,
            Integer pci,
            Integer bandwidthMhz,
            String duplexMode,
            String status
    ) {
        CellEntity entity = new CellEntity();
        entity.id = id;
        entity.cellId = cellId;
        entity.name = name;
        entity.gnb = gnb;
        entity.technology = technology;
        entity.band = band;
        entity.arfcn = arfcn;
        entity.pci = pci;
        entity.bandwidthMhz = bandwidthMhz;
        entity.duplexMode = duplexMode;
        entity.status = status;
        return entity;
    }

    public void applyInventory(
            String name,
            String technology,
            String band,
            Integer arfcn,
            Integer pci,
            Integer bandwidthMhz,
            String duplexMode,
            String status
    ) {
        this.name = name;
        this.technology = technology;
        this.band = band;
        this.arfcn = arfcn;
        this.pci = pci;
        this.bandwidthMhz = bandwidthMhz;
        this.duplexMode = duplexMode;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getCellId() {
        return cellId;
    }

    public String getName() {
        return name;
    }

    public GnbEntity getGnb() {
        return gnb;
    }

    public String getTechnology() {
        return technology;
    }

    public String getBand() {
        return band;
    }

    public Integer getArfcn() {
        return arfcn;
    }

    public Integer getPci() {
        return pci;
    }

    public Integer getBandwidthMhz() {
        return bandwidthMhz;
    }

    public String getDuplexMode() {
        return duplexMode;
    }

    public String getStatus() {
        return status;
    }
}
