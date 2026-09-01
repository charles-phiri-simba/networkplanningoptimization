package com.simba.snip.npo.telemetry;

import org.springframework.stereotype.Component;

@Component
public class TelemetryEventValidator {

    public void validate(TelemetryEvent event) {
        if (event == null) {
            throw new UnrecoverableTelemetryException("event is required");
        }
        requireText(event.eventId(), "eventId");
        requireText(event.eventType(), "eventType");
        requireText(event.schemaVersion(), "schemaVersion");
        requireText(event.source(), "source");
        requireText(event.cellId(), "cellId");
        requireText(event.metric(), "metric");
        requireText(event.unit(), "unit");
        if (event.eventTime() == null) {
            throw new UnrecoverableTelemetryException("eventTime is required");
        }
        if (event.value() == null || !Double.isFinite(event.value())) {
            throw new UnrecoverableTelemetryException("value must be a finite number");
        }
        if (event.synthetic() == null) {
            throw new UnrecoverableTelemetryException("synthetic is required");
        }
        if (!TelemetryEvent.SCHEMA_V1.equals(event.schemaVersion())) {
            throw new UnrecoverableTelemetryException("unsupported schemaVersion: " + event.schemaVersion());
        }
        if (!TelemetryEvent.TYPE_CELL_KPI_OBSERVED.equals(event.eventType())) {
            throw new UnrecoverableTelemetryException("unsupported eventType: " + event.eventType());
        }
        if (!TelemetryCatalog.SUPPORTED_METRICS.contains(event.metric())) {
            throw new UnrecoverableTelemetryException("unsupported metric: " + event.metric());
        }
        if (TelemetryCatalog.isRatioUnit(event.unit()) && (event.value() < 0.0 || event.value() > 1.0)) {
            throw new UnrecoverableTelemetryException("ratio value must be between 0 and 1 inclusive");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new UnrecoverableTelemetryException(field + " is required");
        }
    }
}
