package com.simba.snip.npo.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryEventContractTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final TelemetryEventValidator validator = new TelemetryEventValidator();

    @Test
    void canonicalJsonRoundTripAndKey() throws Exception {
        String json = """
                {
                  "eventId": "evt-123",
                  "eventType": "CELL_KPI_OBSERVED",
                  "schemaVersion": "1.0",
                  "source": "SNIP_SIMULATOR",
                  "cellId": "CELL-001",
                  "metric": "BLER_DL",
                  "value": 0.12,
                  "unit": "ratio",
                  "eventTime": "2026-08-24T10:15:00Z",
                  "ingestedAt": "2026-08-24T10:15:01Z",
                  "synthetic": true
                }
                """;
        TelemetryEvent event = mapper.readValue(json, TelemetryEvent.class);
        validator.validate(event);
        assertEquals("CELL-001", event.kafkaKey());
        assertEquals(Instant.parse("2026-08-24T10:15:00Z"), event.eventTime());
        String written = mapper.writeValueAsString(event);
        TelemetryEvent again = mapper.readValue(written, TelemetryEvent.class);
        assertEquals(event.eventId(), again.eventId());
        assertEquals(event.cellId(), again.cellId());
        assertEquals(event.value(), again.value());
    }

    @Test
    void missingRequiredFieldIsUnrecoverable() {
        TelemetryEvent event = new TelemetryEvent(
                null, "CELL_KPI_OBSERVED", "1.0", "SNIP_SIMULATOR", "CELL-001",
                "BLER_DL", 0.12, "ratio", Instant.parse("2026-08-24T10:15:00Z"), null, true
        );
        UnrecoverableTelemetryException ex = assertThrows(UnrecoverableTelemetryException.class, () -> validator.validate(event));
        assertTrue(ex.getMessage().contains("eventId"));
    }

    @Test
    void unsupportedSchemaAndMetricAreUnrecoverable() {
        Instant t = Instant.parse("2026-08-24T10:15:00Z");
        assertThrows(UnrecoverableTelemetryException.class, () -> validator.validate(new TelemetryEvent(
                "e1", "CELL_KPI_OBSERVED", "2.0", "SNIP_SIMULATOR", "CELL-001",
                "BLER_DL", 0.12, "ratio", t, null, true
        )));
        assertThrows(UnrecoverableTelemetryException.class, () -> validator.validate(new TelemetryEvent(
                "e1", "CELL_KPI_OBSERVED", "1.0", "SNIP_SIMULATOR", "CELL-001",
                "NOT_A_METRIC", 0.12, "ratio", t, null, true
        )));
        assertThrows(UnrecoverableTelemetryException.class, () -> validator.validate(new TelemetryEvent(
                "e1", "CELL_KPI_OBSERVED", "1.0", "SNIP_SIMULATOR", "CELL-001",
                "BLER_DL", 1.4, "ratio", t, null, true
        )));
    }
}
