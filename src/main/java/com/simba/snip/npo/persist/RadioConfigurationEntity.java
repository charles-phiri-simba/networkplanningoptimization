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
@Table(name = "radio_configuration")
public class RadioConfigurationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    private CellEntity cell;

    @Column(name = "parameter_name", nullable = false, length = 128)
    private String parameterName;

    @Column(name = "parameter_value", nullable = false, length = 128)
    private String parameterValue;

    private String unit;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    public UUID getId() {
        return id;
    }

    public CellEntity getCell() {
        return cell;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }
}
