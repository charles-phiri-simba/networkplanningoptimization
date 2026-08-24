package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snip")
public class SnipProperties {

    private String corpusDir = "testdata/corpus";
    private String kpiFile = "testdata/kpis.json";
    /** {@code stub} for CI. {@code spring-ai} for the local LLM path. */
    private String generator = "stub";
    /** {@code lexical} (default) or {@code vector}. */
    private String retrievalMode = "lexical";
    private int retrieveTopK = 3;
    /** Minimum vector similarity (0–1). Ignored by lexical retrieval. */
    private double retrieveMinScore = 0.45;
    private String chatModel = "qwen2.5:7b";
    private String embeddingModel = "nomic-embed-text";

    public String getCorpusDir() {
        return corpusDir;
    }

    public void setCorpusDir(String corpusDir) {
        this.corpusDir = corpusDir;
    }

    public String getKpiFile() {
        return kpiFile;
    }

    public void setKpiFile(String kpiFile) {
        this.kpiFile = kpiFile;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }

    public void setRetrievalMode(String retrievalMode) {
        this.retrievalMode = retrievalMode;
    }

    public int getRetrieveTopK() {
        return retrieveTopK;
    }

    public void setRetrieveTopK(int retrieveTopK) {
        this.retrieveTopK = retrieveTopK;
    }

    public double getRetrieveMinScore() {
        return retrieveMinScore;
    }

    public void setRetrieveMinScore(double retrieveMinScore) {
        this.retrieveMinScore = retrieveMinScore;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
