package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stable-property evaluation cases (stub + lexical). Real Ollama path is exercised outside CI.
 */
@SpringBootTest(classes = NpoApplication.class)
@AutoConfigureMockMvc
class RecommendationEvaluationTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void case1CanonicalBlerUsesCitedMidBandKnowledge() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What should I check if BLER is high on a mid-band cell?",
                                  "contextId": "cell-midband-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrievalEmpty").value(false))
                .andExpect(jsonPath("$.retrievalMode").value("lexical"))
                .andExpect(jsonPath("$.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.citations[0].sourceId").value("sample-bler-midband"))
                .andExpect(jsonPath("$.recommendation", containsString("BLER")));
    }

    @Test
    void case2UnsupportedQuestionDoesNotFabricateCitations() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "question": "How do I bake sourdough bread at high altitude?" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrievalEmpty").value(true))
                .andExpect(jsonPath("$.citations", hasSize(0)))
                .andExpect(jsonPath("$.retrievalHitCount").value(0))
                .andExpect(jsonPath("$.recommendation", containsString("will not invent")));
    }

    @Test
    void case3SyntheticKpiIsLabelledAndAttached() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What should I check if BLER is high on a mid-band cell?",
                                  "contextId": "cell-midband-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextUsed.id").value("cell-midband-001"))
                .andExpect(jsonPath("$.contextUsed.kpis.synthetic").value(true))
                .andExpect(jsonPath("$.contextUsed.kpis.bler").value(0.12))
                .andExpect(jsonPath("$.recommendation", containsString("cell-midband-001")));
    }

    @Test
    void case4Cell001ContextAwareRecommendation() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why might BLER be high on CELL-001?",
                                  "cellId": "CELL-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextFound").value(true))
                .andExpect(jsonPath("$.contextCellId").value("CELL-001"))
                .andExpect(jsonPath("$.contextEvidence.cellId").value("CELL-001"))
                .andExpect(jsonPath("$.contextEvidence.gnbId").value("GNB-001"))
                .andExpect(jsonPath("$.contextEvidence.siteId").value("SITE-001"))
                .andExpect(jsonPath("$.contextEvidence.synthetic").value(true))
                .andExpect(jsonPath("$.contextEvidence.source").value("DEMO_SEED"))
                .andExpect(jsonPath("$.retrievalEmpty").value(false))
                .andExpect(jsonPath("$.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.citations[0].sourceId").exists())
                .andExpect(jsonPath("$.kpiObservationCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.neighbourCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.recommendation", containsString("CELL-001")));
    }

    @Test
    void unknownCellRecommendationReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why might BLER be high on CELL-MISSING?",
                                  "cellId": "CELL-MISSING"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("cell not found"))
                .andExpect(jsonPath("$.id").value("CELL-MISSING"));
    }
}
