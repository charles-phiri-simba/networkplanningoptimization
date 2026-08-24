package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NpoApplication.class)
@AutoConfigureMockMvc
@Transactional
class AssuranceApiTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryProjectionService projectionService;

    @Autowired
    private com.simba.snip.npo.assurance.AssuranceCaseService assuranceCaseService;

    @Test
    void emptyAssuranceForHealthyLookupIsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-002/assurance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownCellAssuranceIs404() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-MISSING/assurance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("cell not found"));
    }

    @Test
    void unknownCaseIs404() throws Exception {
        mockMvc.perform(get("/api/v1/assurance/cases/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("assurance case not found"));
    }

    @Test
    void highBlerLoadExposesCaseAndAssessment() throws Exception {
        Instant t0 = Instant.parse("2026-08-24T11:30:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            projectionService.project(event("p3-api-bler-" + i, "CELL-001", "BLER_DL", bler[i], ts));
            projectionService.project(event("p3-api-prb-" + i, "CELL-001", "PRB_UTILIZATION_DL", prb[i], ts));
        }
        AssuranceCaseEntity stored = assuranceCaseService.listForCell("CELL-001").get(0);
        String caseId = stored.getId().toString();

        mockMvc.perform(get("/api/v1/assurance/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].caseType").value("DEGRADING_RADIO_QUALITY"));

        mockMvc.perform(get("/api/v1/assurance/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.affectedEntityId").value("CELL-001"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.synthetic").value(true))
                .andExpect(jsonPath("$.evidence", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/v1/cells/CELL-001/assurance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(caseId));

        mockMvc.perform(get("/api/v1/assurance/cases/" + caseId + "/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assuranceCaseId").value(caseId))
                .andExpect(jsonPath("$.humanReviewRequired").value(true))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.caseType").value("DEGRADING_RADIO_QUALITY"))
                .andExpect(jsonPath("$.operationalEvidence", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.likelyContributors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.recommendedChecks", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.missingEvidence", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.retrievalEmpty").value(false))
                .andExpect(jsonPath("$.summary", containsString("humanReviewRequired")))
                .andExpect(jsonPath("$.summary", containsString("not established")));
    }

    private static TelemetryEvent event(String eventId, String cellId, String metric, double value, Instant eventTime) {
        return new TelemetryEvent(
                eventId,
                TelemetryEvent.TYPE_CELL_KPI_OBSERVED,
                TelemetryEvent.SCHEMA_V1,
                TelemetryEvent.SOURCE_SIMULATOR,
                cellId,
                metric,
                value,
                "ratio",
                eventTime,
                null,
                true
        );
    }
}
