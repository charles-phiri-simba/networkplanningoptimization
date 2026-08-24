package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NpoApplication.class)
@AutoConfigureMockMvc
class NetworkDomainApiTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void siteLookupWorks() throws Exception {
        mockMvc.perform(get("/api/v1/sites/SITE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value("SITE-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void gnbLookupIncludesSite() throws Exception {
        mockMvc.perform(get("/api/v1/gnbs/GNB-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gnbId").value("GNB-001"))
                .andExpect(jsonPath("$.siteId").value("SITE-001"));
    }

    @Test
    void cellLookupIncludesGnbAndSite() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellId").value("CELL-001"))
                .andExpect(jsonPath("$.gnbId").value("GNB-001"))
                .andExpect(jsonPath("$.siteId").value("SITE-001"))
                .andExpect(jsonPath("$.band").value("n78"));
    }

    @Test
    void unknownCellReturns404WithoutStack() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("cell not found"))
                .andExpect(jsonPath("$.id").value("CELL-MISSING"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void cellContextAssemblesDomainGraph() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-001/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cell.cellId").value("CELL-001"))
                .andExpect(jsonPath("$.gnb.gnbId").value("GNB-001"))
                .andExpect(jsonPath("$.site.siteId").value("SITE-001"))
                .andExpect(jsonPath("$.kpis", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.kpis[0].synthetic").value(true))
                .andExpect(jsonPath("$.radioConfiguration", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.neighbours", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.provenance.source").value("DEMO_SEED"))
                .andExpect(jsonPath("$.provenance.synthetic").value(true))
                .andExpect(jsonPath("$.telemetry", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.telemetry[0].trend").exists());
    }

    @Test
    void cellTelemetryIsReadOnlyAndIncludesLastN() throws Exception {
        mockMvc.perform(get("/api/v1/cells/CELL-001/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.metric=='BLER_DL')].current.value").exists());
        mockMvc.perform(get("/api/v1/cells/CELL-001/telemetry/BLER_DL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("BLER_DL"))
                .andExpect(jsonPath("$.current.value").value(0.12))
                .andExpect(jsonPath("$.history", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.trend").value("INSUFFICIENT_DATA"));
    }
}
