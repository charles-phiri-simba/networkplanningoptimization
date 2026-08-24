package com.simba.snip.npo.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NpoApplication.class)
@AutoConfigureMockMvc
class RecommendationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void canonicalBlerQuestionReturnsCitedRecommendationAndKpiContext() throws Exception {
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
                .andExpect(jsonPath("$.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.citations[0].sourceId").exists())
                .andExpect(jsonPath("$.recommendation", containsString("read-only")))
                .andExpect(jsonPath("$.contextUsed.id").value("cell-midband-001"))
                .andExpect(jsonPath("$.contextUsed.kpis.bler").value(0.12))
                .andExpect(jsonPath("$.contextUsed.kpis.band").value("mid"))
                .andExpect(jsonPath("$.retrievalMode").value("lexical"))
                .andExpect(jsonPath("$.retrievalLatencyMs").exists())
                .andExpect(jsonPath("$.generationLatencyMs").exists())
                .andExpect(jsonPath("$.totalLatencyMs").exists());
    }

    @Test
    void emptyRetrievalDoesNotFabricateCitations() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "How do I bake sourdough bread at high altitude?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrievalEmpty").value(true))
                .andExpect(jsonPath("$.citations", hasSize(0)))
                .andExpect(jsonPath("$.recommendation", containsString("will not invent")));
    }
}
